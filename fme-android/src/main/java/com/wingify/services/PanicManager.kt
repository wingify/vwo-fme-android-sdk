/**
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wingify.services

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.models.Settings
import java.util.concurrent.atomic.AtomicBoolean

/** How a settings snapshot was obtained. */
enum class SettingsControlApplyMode {
    /** Init / explicit fetch: latch panic and refresh from sdkMetaInfo. */
    APPLY,
    /** Polling: never latch; confirm panic via collector ping. */
    POLLING,
    /** Post-ping settings pull: record snapshot id; log stale panic; do not re-latch. */
    POST_PING_FETCH,
}

/** Outcome of applying settings or DaCDN control to [PanicManager]. */
data class ControlResult(
    /** Whether panic is latched after this apply. */
    val isPanicActive: Boolean,
    /** True when a force settings fetch should run next. */
    val shouldTriggerForceRefresh: Boolean = false,
    /** True when the refresh was caused by panic clearing, so SDK_REENABLED can be logged. */
    val isRefreshFromPanicClear: Boolean = false,
    /** True when a collector ping is needed to confirm panic (polling never latches itself). */
    val needsCollectPingConfirmation: Boolean = false,
)

/**
 * Panic-mode kill-switch and forced settings refresh from settings snapshots and DaCDN responses.
 */
class PanicManager {

    @Volatile
    private var panicActive: Boolean = false
    private var lastPanicTimestamp: String? = null
    private var lastForceRefreshTimestamp: String? = null
    /** Latest refresh command not yet committed (may be newer than the in-flight pull). */
    private var pendingForceRefreshTimestamp: String? = null
    /** Timestamp bound when the refresh mutex was acquired; committed on success. */
    private var inFlightForceRefreshTimestamp: String? = null
    private var lastSettingsTimestamp: String? = null
    private val refreshInProgress = AtomicBoolean(false)
    private val pingConfirmationInProgress = AtomicBoolean(false)
    private var onPanicActivated: (() -> Unit)? = null

    internal var loggerService: LoggerService? = null

    /**
     * Registers a hook invoked when panic latches from off to on.
     * Used to discard queued events before they can be flushed.
     *
     * @param handler Callback to run on latch, or null to clear the hook.
     */
    fun setOnPanicActivated(handler: (() -> Unit)?) {
        onPanicActivated = handler
    }

    /**
     * Returns whether panic mode is currently latched.
     *
     * @return true when public APIs and batch flushes should be blocked.
     */
    fun isPanicActive(): Boolean = panicActive

    /**
     * Returns whether [settings] is newer than the last applied snapshot.
     * Older or equal [Settings.lastUpdatedOn] (falling back to [Settings.version]) is skipped.
     *
     * @param settings Candidate settings snapshot.
     * @return true when the snapshot should be applied.
     */
    fun shouldApplySettingsSnapshot(settings: Settings): Boolean {
        val incoming = snapshotTimestamp(settings) ?: return false
        return !isStale(incoming, lastSettingsTimestamp)
    }

    /**
     * Applies panic and force-refresh control from a settings snapshot.
     *
     * [SettingsControlApplyMode.APPLY] latches panic and refresh from sdkMetaInfo.
     * [SettingsControlApplyMode.POLLING] never latches; it may request a collector ping.
     * Force refresh from polled `sdkMetaInfo.refresh` is handled via [evaluateForceRefreshFromPollMeta].
     * [SettingsControlApplyMode.POST_PING_FETCH] records the snapshot id and logs a
     * stale panic timestamp; it never re-latches.
     * Stale or duplicate snapshots are ignored.
     *
     * @param settings Settings snapshot to evaluate.
     * @param mode How the snapshot was obtained.
     * @return Control flags for refresh and collector-ping confirmation.
     */
    @Synchronized
    fun applySettingsControl(
        settings: Settings,
        mode: SettingsControlApplyMode = SettingsControlApplyMode.APPLY,
    ): ControlResult {
        val snapshotTs = snapshotTimestamp(settings) ?: return ControlResult(panicActive)
        if (isStale(snapshotTs, lastSettingsTimestamp)) {
            return ControlResult(panicActive)
        }
        lastSettingsTimestamp = snapshotTs
        if (mode == SettingsControlApplyMode.POST_PING_FETCH) {
            val panicTs = settings.sdkMetaInfo?.panic?.toString()?.takeIf { it.isNotEmpty() }
            if (panicTs != null && isStale(panicTs, lastPanicTimestamp)) {
                latchIfFreshPanic(panicTs, panicActive)
            }
            return ControlResult(panicActive)
        }
        if (mode == SettingsControlApplyMode.POLLING) return evaluatePolling(settings)
        return applyFromSdkMeta(settings)
    }

    /**
     * Applies `panicMode` / `forceRefresh` from a collect or batch 200 response.
     * Empty or non-object bodies clear panic. Stale timestamps are ignored.
     *
     * @param responseData Raw response body from collect or batch.
     * @return Control flags for refresh after applying the response.
     */
    @Synchronized
    fun applyResponseControl(responseData: String): ControlResult {
        val wasActive = panicActive
        val trimmed = responseData.trim()
        if (trimmed.isEmpty()) return clearViaResponse(wasActive)
        return try {
            val element = JsonParser.parseString(trimmed)
            if (!element.isJsonObject) return clearViaResponse(wasActive)
            applyDaCdnObject(element.asJsonObject, wasActive)
        } catch (e: Exception) {
            loggerService?.log(LogLevelEnum.WARN, "COLLECT_RESPONSE_PARSE_ERROR", mapOf("err" to e.message))
            ControlResult(panicActive)
        }
    }

    /**
     * Tries to acquire the force-refresh mutex and binds [inFlightForceRefreshTimestamp]
     * to the current pending command (if any).
     *
     * @return false if a refresh is already in flight.
     */
    @Synchronized
    fun tryStartRefresh(): Boolean {
        if (!refreshInProgress.compareAndSet(false, true)) return false
        inFlightForceRefreshTimestamp = pendingForceRefreshTimestamp
        return true
    }

    /**
     * Releases the force-refresh mutex and logs completion.
     * [isRefreshFromPanicClear] logs SDK_REENABLED only when panic is already off.
     *
     * @param isRefreshFromPanicClear true when this refresh followed a panic clear.
     * @param shouldLog false to release the mutex without logging (handler missing).
     */
    fun finishRefresh(isRefreshFromPanicClear: Boolean = false, shouldLog: Boolean = true) {
        if (!refreshInProgress.getAndSet(false) || !shouldLog) return
        if (isRefreshFromPanicClear) {
            if (!panicActive) loggerService?.log(LogLevelEnum.INFO, "SDK_REENABLED", null)
        } else {
            loggerService?.log(LogLevelEnum.INFO, "FORCE_REFRESH_COMPLETED", null)
        }
    }

    /**
     * Reads `sdkMetaInfo.refresh` from a polled CDN snapshot without applying the snapshot.
     * Used when the poll payload is stale by `lastUpdatedOn` but still carries a new refresh token.
     *
     * @param settings Parsed settings from a poll GET.
     * @return Control flags; may request v2-pull when refresh timestamp is fresh.
     */
    @Synchronized
    fun evaluateForceRefreshFromPollMeta(settings: Settings): ControlResult {
        val shouldRefresh = armForceRefreshFromMeta(
            settings.sdkMetaInfo?.refresh?.toString(),
            "settings",
        )
        return ControlResult(panicActive, shouldTriggerForceRefresh = shouldRefresh)
    }

    /**
     * Commits the in-flight refresh timestamp after a successful v2-pull apply.
     * A newer [pendingForceRefreshTimestamp] is kept so a follow-up pull can run.
     */
    @Synchronized
    fun recordForceRefreshApplied() {
        val ts = inFlightForceRefreshTimestamp
        if (ts != null) lastForceRefreshTimestamp = ts
        inFlightForceRefreshTimestamp = null
        val pending = pendingForceRefreshTimestamp
        if (pending == null || isStale(pending, lastForceRefreshTimestamp)) {
            pendingForceRefreshTimestamp = null
        }
    }

    /**
     * Drops the in-flight command so it can be retried.
     * A newer pending timestamp is kept.
     */
    @Synchronized
    fun abandonPendingForceRefresh() {
        val inFlight = inFlightForceRefreshTimestamp
        inFlightForceRefreshTimestamp = null
        if (inFlight == null || pendingForceRefreshTimestamp == inFlight) {
            pendingForceRefreshTimestamp = null
        }
    }

    /** True when a refresh command is armed and not yet committed. */
    @Synchronized
    fun hasPendingForceRefresh(): Boolean = pendingForceRefreshTimestamp != null

    /**
     * Tries to acquire the collector-ping confirmation mutex so only one ping runs at a time.
     *
     * @return false if a confirmation ping is already in flight.
     */
    fun tryStartPanicClearConfirmation(): Boolean = pingConfirmationInProgress.compareAndSet(false, true)

    /** Releases the collector-ping confirmation mutex. */
    fun finishPanicClearConfirmation() {
        pingConfirmationInProgress.set(false)
    }

    /**
     * Init / explicit-fetch path: latch panic and refresh from sdkMetaInfo.
     * A missing panic while already latched requests a collector ping instead of clearing.
     *
     * @param settings Settings snapshot whose sdkMetaInfo drives control.
     * @return Control flags including whether a collector ping is needed.
     */
    private fun applyFromSdkMeta(settings: Settings): ControlResult {
        var needsPing = false
        val wasActive = panicActive
        val panicTs = settings.sdkMetaInfo?.panic?.toString()
        if (panicTs.isNullOrEmpty()) {
            if (wasActive) needsPing = true else panicActive = false
        } else {
            latchIfFreshPanic(panicTs, wasActive)
        }
        val shouldRefresh = armForceRefreshFromMeta(
            settings.sdkMetaInfo?.refresh?.toString(),
            "settings",
        )
        return ControlResult(panicActive, shouldRefresh, needsCollectPingConfirmation = needsPing)
    }

    /**
     * Polling never latches panic. A newer panic timestamp, or a missing panic while
     * already latched, requests a collector ping so DaCDN remains the source of truth.
     * Force refresh is not evaluated here; [evaluateForceRefreshFromPollMeta] handles it.
     *
     * @param settings Polled settings snapshot.
     * @return Control flags; [ControlResult.needsCollectPingConfirmation] may be true.
     */
    private fun evaluatePolling(settings: Settings): ControlResult {
        val panicTs = settings.sdkMetaInfo?.panic?.toString()?.takeIf { it.isNotEmpty() }
        val needsPing = when {
            panicTs != null && !isStale(panicTs, lastPanicTimestamp) -> true
            panicTs == null && panicActive -> true
            else -> false
        }
        return ControlResult(panicActive, needsCollectPingConfirmation = needsPing)
    }

    /**
     * Applies panicMode / forceRefresh from a parsed DaCDN JSON object.
     * Event-echo bodies (`d` / `ev`) without control keys clear panic.
     *
     * @param json Parsed collect or batch response object.
     * @param wasActive Panic state before this apply.
     * @return Control flags after applying the object.
     */
    private fun applyDaCdnObject(json: com.google.gson.JsonObject, wasActive: Boolean): ControlResult {
        val hasPanic = json.has("panicMode")
        val hasRefresh = json.has("forceRefresh")
        if ((json.has("d") || json.has("ev")) && !hasPanic && !hasRefresh) {
            return clearViaResponse(wasActive).also {
                if (!wasActive) {
                    loggerService?.log(LogLevelEnum.DEBUG, "COLLECT_RESPONSE_BATCH_ECHO_IGNORED", null)
                }
            }
        }
        applyPanicFromDaCdn(json, hasPanic, wasActive)
        var shouldRefresh = false
        if (hasRefresh) {
            val refreshTs = parseTimestamp(json.get("forceRefresh"))
            if (armForceRefreshFromMeta(refreshTs, "network response")) {
                shouldRefresh = true
            }
        }
        val fromClear = wasActive && !panicActive
        if (fromClear) shouldRefresh = true
        return ControlResult(panicActive, shouldRefresh, fromClear)
    }

    /**
     * Updates the panic latch from DaCDN `panicMode`.
     * Missing or empty panicMode clears panic; a newer timestamp latches it.
     *
     * @param json Parsed DaCDN response object.
     * @param hasPanic true when the JSON contains a panicMode key.
     * @param wasActive Panic state before this apply.
     */
    private fun applyPanicFromDaCdn(json: com.google.gson.JsonObject, hasPanic: Boolean, wasActive: Boolean) {
        if (!hasPanic) {
            panicActive = false
            if (wasActive) logInfo("PANIC_MODE_CLEARED_VIA_RESPONSE", null)
            return
        }
        val panicTs = parseTimestamp(json.get("panicMode"))
        if (panicTs.isNullOrEmpty()) {
            panicActive = false
            if (wasActive) logInfo("PANIC_MODE_CLEARED_VIA_RESPONSE", null)
        } else {
            latchIfFreshPanic(panicTs, wasActive)
        }
    }

    /**
     * Latches panic when [panicTs] is newer than the last seen timestamp.
     * An older timestamp is a no-op; if panic is off, logs why it was not enabled.
     *
     * @param panicTs Unix-seconds timestamp from settings or DaCDN.
     * @param wasActive Panic state before this apply.
     */
    private fun latchIfFreshPanic(panicTs: String, wasActive: Boolean) {
        if (!isStale(panicTs, lastPanicTimestamp)) {
            latchPanic(panicTs, wasActive)
            return
        }
        if (!wasActive) {
            logInfo("PANIC_TIMESTAMP_STALE", mapOf("panicTs" to panicTs))
        }
    }

    /**
     * Latches panic on and records [panicTs].
     * Invokes [onPanicActivated] only when transitioning from off to on.
     *
     * @param panicTs Unix-seconds timestamp from settings or DaCDN.
     * @param wasActive Panic state before this latch.
     */
    private fun latchPanic(panicTs: String, wasActive: Boolean) {
        panicActive = true
        if (wasActive && lastPanicTimestamp != panicTs) {
            logInfo("PANIC_MODE_TIMESTAMP_UPDATED", mapOf("panicTs" to panicTs))
        }
        lastPanicTimestamp = panicTs
        if (!wasActive) {
            logInfo("PANIC_MODE_ACTIVATED", mapOf("panicTs" to panicTs))
            onPanicActivated?.invoke()
        }
    }

    /**
     * Clears panic from a collect/batch response.
     * When panic was on, requests a force-refresh so SDK_REENABLED can be logged after fetch.
     *
     * @param wasActive Panic state before this clear.
     * @return Control flags; refresh is requested only when panic was on.
     */
    private fun clearViaResponse(wasActive: Boolean): ControlResult {
        panicActive = false
        if (!wasActive) return ControlResult(false)
        logInfo("PANIC_MODE_CLEARED_VIA_RESPONSE", null)
        return ControlResult(false, shouldTriggerForceRefresh = true, isRefreshFromPanicClear = true)
    }

    /**
     * Snapshot identity used for stale checks: [Settings.lastUpdatedOn], else [Settings.version].
     *
     * @param settings Settings snapshot.
     * @return Normalized timestamp string, or null when neither field is present.
     */
    private fun snapshotTimestamp(settings: Settings): String? =
        normalize(settings.lastUpdatedOn) ?: normalize(settings.version)

    /**
     * Converts a timestamp-like value to a comparable string.
     *
     * @param value Number or non-empty string; other types are ignored.
     * @return Normalized string, or null when [value] cannot be used as a timestamp.
     */
    private fun normalize(value: Any?): String? = when (value) {
        null -> null
        is Number -> if (value.toDouble().isFinite()) value.toString() else null
        is String -> value.trim().takeIf { it.isNotEmpty() }
        else -> null
    }

    /**
     * Returns whether [candidate] is older than or equal to [lastSeen]
     * (string compare of unix-seconds timestamps).
     *
     * @param candidate Incoming timestamp.
     * @param lastSeen Last applied timestamp, or null if none.
     * @return true when [candidate] should be ignored.
     */
    private fun isStale(candidate: String, lastSeen: String?): Boolean =
        lastSeen != null && candidate <= lastSeen

    /**
     * Arms v2-pull when [refreshTs] is newer than the last successful refresh and not already pending.
     * Does not advance [lastForceRefreshTimestamp] until [recordForceRefreshApplied].
     */
    private fun armForceRefreshFromMeta(refreshTs: String?, source: String): Boolean {
        if (refreshTs.isNullOrEmpty() || isForceRefreshCommandStale(refreshTs)) return false
        pendingForceRefreshTimestamp = refreshTs
        logInfo(
            "FORCE_REFRESH_TRIGGERED",
            mapOf("source" to source, "refreshTs" to refreshTs),
        )
        return true
    }

    private fun isForceRefreshCommandStale(refreshTs: String): Boolean {
        if (isStale(refreshTs, lastForceRefreshTimestamp)) return true
        if (isStale(refreshTs, pendingForceRefreshTimestamp)) return true
        return false
    }

    /**
     * Parses a JSON primitive timestamp from DaCDN (`panicMode` / `forceRefresh`).
     *
     * @param element JSON number or numeric string.
     * @return Unix-seconds string, or null when missing or invalid.
     */
    private fun parseTimestamp(element: JsonElement?): String? {
        if (element == null || element.isJsonNull) return null
        if (!element.isJsonPrimitive) return null
        val primitive = element.asJsonPrimitive
        if (primitive.isNumber) return primitive.asLong.toString()
        if (primitive.isString) return primitive.asString.trim().takeIf { it.isNotEmpty() }
        return null
    }

    /**
     * Logs an INFO message through [loggerService] when a logger is attached.
     *
     * @param key Log message key from info-messages.json.
     * @param map Placeholder substitutions, or null.
     */
    private fun logInfo(key: String, map: Map<String?, String?>?) {
        loggerService?.log(LogLevelEnum.INFO, key, map)
    }
}
