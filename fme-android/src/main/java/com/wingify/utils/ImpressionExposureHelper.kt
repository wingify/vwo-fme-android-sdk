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
package com.wingify.utils

import com.vwo.models.user.GetFlag
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.ServiceContainer
import com.wingify.WingifyClient
import com.wingify.constants.Constants
import com.wingify.models.Feature
import com.wingify.models.Settings
import com.wingify.models.Storage
import com.wingify.models.impression.ImpressionPayload
import com.wingify.models.schemas.SettingsSchema
import com.wingify.models.user.WingifyUserContext
import com.wingify.services.StorageService
import com.wingify.utils.FunctionUtil.getFeatureFromKey

/**
 * Owns send vs defer vs expose for `vwo_variationShown` impressions.
 *
 * Keep this small: no Strategy/Proxy hierarchy. All getFlag-path variationShown sites
 * route through [record]; [expose] sends the stash unioned with a storage rebuild.
 */
object ImpressionExposureHelper {

    /**
     * Dependencies needed to enqueue deferred impressions later via [GetFlag.exposeResult].
     */
    data class ExposeDeps(
        val settings: Settings,
        val featureKey: String,
        val context: WingifyUserContext,
        val serviceContainer: ServiceContainer,
        val isEnabled: Boolean,
    )

    /**
     * Records impressions built during evaluation: enqueue now, or merge into [getFlag]'s stash.
     *
     * @return `true` when a non-empty payload was actually enqueued to the batch DB.
     */
    fun record(
        settings: Settings,
        impressionPayload: ImpressionPayload,
        context: WingifyUserContext,
        serviceContainer: ServiceContainer,
        shouldSend: Boolean,
        getFlag: GetFlag,
        featureKey: String,
        feature: Feature?,
    ): Boolean {
        getFlag.attachExposeDeps(
            ExposeDeps(
                settings = settings,
                featureKey = featureKey,
                context = context,
                serviceContainer = serviceContainer,
                isEnabled = getFlag.isEnabled(),
            )
        )

        if (impressionPayload.hasNoValidData()) {
            return false
        }

        if (shouldSend) {
            if (!send(settings, impressionPayload, context, serviceContainer)) {
                return false
            }
            getFlag.markImpressionSent()
            persistVariationShownSent(
                feature = feature,
                context = context,
                serviceContainer = serviceContainer,
                sent = true,
            )
            return true
        }

        getFlag.mergePendingImpressions(impressionPayload)
        persistVariationShownSent(
            feature = feature,
            context = context,
            serviceContainer = serviceContainer,
            sent = false,
        )
        serviceContainer.getLoggerService()?.log(
            LogLevelEnum.INFO,
            "IMPRESSION_DEFERRED",
            mapOf(
                Constants.USER_ID to context.id,
                "featureKey" to featureKey,
                "count" to impressionPayload.size().toString(),
            ),
        )
        return false
    }

    /**
     * Exposes a deferred decision using only the feature key and user context.
     *
     * Reads the sticky decision left by a `fetchFlag`/`fetchFlags` in an earlier process, then
     * delegates to [expose]. `isEnabled` comes from the stored decision: a rollout or experiment
     * variation means enabled; holdout-only storage stays disabled.
     *
     * @return `true` when an impression was enqueued.
     */
    fun exposeByKey(
        featureKey: String,
        context: WingifyUserContext,
        serviceContainer: ServiceContainer,
    ): Boolean {
        // Resolve the id the same way getFlag did, so a device-id fallback reaches the same
        // storage record. The caller's context is left untouched.
        val userId = UserIdUtil.getUserId(context, serviceContainer.getInitOptions(), serviceContainer)
            ?: return false
        val evalContext = context.clone().apply { id = userId }

        val settings = serviceContainer.getSettings()
        if (settings == null || !SettingsSchema().isSettingsValid(settings)) return false

        val stored = readStorageLenient(
            StorageService(serviceContainer).getDataInStorage(featureKey, evalContext)
        ) ?: return false

        val isEnabled =
            stored.experimentVariationId != null || stored.rolloutVariationId != null
        val getFlag = GetFlag(evalContext).apply {
            setIsEnabled(isEnabled)
            attachExposeDeps(
                ExposeDeps(
                    settings = settings,
                    featureKey = featureKey,
                    context = evalContext,
                    serviceContainer = serviceContainer,
                    isEnabled = isEnabled,
                )
            )
        }
        return expose(getFlag)
    }

    /**
     * Enqueues the deferred variationShown impression: in-memory stash unioned with storage.
     *
     * Idempotent: no-ops when already sent (in-memory or durable legacy-safe storage).
     *
     * @return `true` when an impression was enqueued.
     */
    fun expose(getFlag: GetFlag): Boolean {
        if (getFlag.isImpressionAlreadySent()) {
            logExpose(getFlag, "IMPRESSION_EXPOSE_ALREADY_SENT")
            return false
        }

        val deps = getFlag.getExposeDeps()
        if (deps == null) {
            logExpose(getFlag, "IMPRESSION_EXPOSE_NOTHING")
            return false
        }

        return deps.serviceContainer.withFlagLock(deps.featureKey, deps.context.id) {
            // Re-check after acquiring lock: a concurrent getFlag/expose may have sent already.
            if (getFlag.isImpressionAlreadySent()) {
                logExpose(getFlag, "IMPRESSION_EXPOSE_ALREADY_SENT")
                return@withFlagLock false
            }

            val storageService = StorageService(deps.serviceContainer)
            val rawStored = storageService.getDataInStorage(deps.featureKey, deps.context)
            val stored = readStorage(rawStored)
            // A sent sticky only settles the decision it was written for. A later fetch can
            // stash new rows (new holdouts) that were never dispatched, so send those.
            if (isAlreadySent(rawStored, stored) && !getFlag.hasPendingImpressions()) {
                getFlag.markImpressionSent()
                logExpose(getFlag, "IMPRESSION_EXPOSE_ALREADY_SENT")
                return@withFlagLock false
            }

            // Stash can be a partial later getFlag (impact or new holdouts only).
            // Union with storage so the original rollout/experiment is not dropped.
            // add() dedupes on campaign id + variation id; stash wins so its feature id is kept.
            val payload = getFlag.consumePendingImpressions()
            val fromStorage = rebuildPayload(deps, stored ?: readStorageLenient(rawStored))
            val stashSize = payload.size()
            for (index in 0 until fromStorage.size()) {
                val row = fromStorage.get(index)
                payload.add(
                    campaignId = row.campaignId,
                    variationId = row.variationId,
                    featureId = row.featureId,
                )
            }
            val rebuilt = payload.size() > stashSize

            if (!send(deps.settings, payload, deps.context, deps.serviceContainer)) {
                logExpose(getFlag, "IMPRESSION_EXPOSE_NOTHING")
                return@withFlagLock false
            }
            getFlag.markImpressionSent()

            val feature = getFeatureFromKey(deps.settings, deps.featureKey)
            persistVariationShownSent(
                feature = feature,
                context = deps.context,
                serviceContainer = deps.serviceContainer,
                sent = true,
            )

            logExpose(
                getFlag,
                if (rebuilt) "IMPRESSION_EXPOSE_REBUILD" else "IMPRESSION_EXPOSE_DEFERRED",
            )
            true
        }
    }

    /**
     * Rebuilds a variationShown payload from sticky storage, plus impact when
     * getFlag would have reached the impact block (stored rollout/experiment).
     * Holdout-only storage never reaches that block, so impact is omitted.
     */
    fun rebuildPayload(deps: ExposeDeps, stored: Storage?): ImpressionPayload {
        val payload = ImpressionPayload()
        val feature = getFeatureFromKey(deps.settings, deps.featureKey)
        val featureId = feature?.id ?: Constants.IMPRESSION_NO_FEATURE_ID

        if (stored != null) {
            val experimentId = stored.experimentId
            val experimentVariationId = stored.experimentVariationId
            if (experimentId != null && experimentVariationId != null) {
                payload.add(campaignId = experimentId, variationId = experimentVariationId)
            }

            val rolloutId = stored.rolloutId
            val rolloutVariationId = stored.rolloutVariationId
            if (rolloutId != null && rolloutVariationId != null) {
                payload.add(campaignId = rolloutId, variationId = rolloutVariationId)
            }

            stored.holdoutIds?.values?.forEach { holdoutId ->
                payload.add(
                    campaignId = holdoutId,
                    variationId = Constants.Holdouts.VARIATION_IS_PART_OF_HOLDOUT,
                    featureId = featureId,
                )
            }
        }

        val isHoldoutOnly = stored != null
            && !stored.holdoutIds?.values.isNullOrEmpty()
            && stored.rolloutVariationId == null
            && stored.experimentVariationId == null
        if (!isHoldoutOnly) {
            val impactId = feature?.impactCampaign?.campaignId
            if (impactId != null && impactId.toString().isNotEmpty()) {
                payload.add(
                    campaignId = impactId,
                    variationId = if (deps.isEnabled) 2 else 1,
                )
            }
        }

        return payload
    }

    /**
     * Whether variationShown was already sent for this stored decision.
     *
     * An explicit boolean on the raw map wins. A missing key falls back to
     * [Storage.isVariationShownAlreadySent]: a legacy decision with no key counts as sent.
     */
    private fun isAlreadySent(raw: Map<String, Any>?, stored: Storage?): Boolean {
        return when (raw?.get(Constants.KEY_VARIATION_SHOWN_SENT) as? Boolean) {
            true -> true
            false -> false
            null -> stored?.isVariationShownAlreadySent() ?: hasDecisionInRaw(raw)
        }
    }

    /** Whether raw storage holds an experiment, rollout, or holdout decision. */
    private fun hasDecisionInRaw(raw: Map<String, Any>?): Boolean {
        if (raw == null) return false
        if (raw["experimentVariationId"] != null) return true
        if (raw["rolloutVariationId"] != null) return true
        val holdouts = raw[Constants.Holdouts.KEY_STORAGE_HOLDOUT_IDS]
        return holdouts is Collection<*> && holdouts.isNotEmpty()
    }

    /** Parses raw storage into [Storage], or null when the map is absent or unreadable. */
    private fun readStorage(raw: Map<String, Any>?): Storage? {
        if (raw == null) return null
        return try {
            val json = WingifyClient.objectMapper.writeValueAsString(raw.toMap())
            WingifyClient.objectMapper.readValue(json, Storage::class.java)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Best-effort Storage when Gson cannot parse holdout list wrappers; still carries ids for rebuild.
     */
    private fun readStorageLenient(raw: Map<String, Any>?): Storage? {
        readStorage(raw)?.let { return it }
        if (raw == null) return null
        return Storage().apply {
            featureKey = raw["featureKey"] as? String
            user = raw["userId"]?.toString() ?: raw["user"]?.toString()
            rolloutId = (raw["rolloutId"] as? Number)?.toInt()
            rolloutKey = raw["rolloutKey"] as? String
            rolloutVariationId = (raw["rolloutVariationId"] as? Number)?.toInt()
            experimentId = (raw["experimentId"] as? Number)?.toInt()
            experimentKey = raw["experimentKey"] as? String
            experimentVariationId = (raw["experimentVariationId"] as? Number)?.toInt()
            when (val sent = raw[Constants.KEY_VARIATION_SHOWN_SENT]) {
                is Boolean -> variationShownSent = sent
            }
            val holdoutList = raw[Constants.Holdouts.KEY_STORAGE_HOLDOUT_IDS] as? Collection<*>
            if (!holdoutList.isNullOrEmpty()) {
                holdoutIds = Storage.JsonArrayWrapper().apply {
                    values = holdoutList.mapNotNull { (it as? Number)?.toInt() }
                }
            }
        }
    }

    /**
     * Sends [impressionPayload], dropping user+campaign+variation rows already sent
     * this session or in storage. MEG sibling stash and the sibling's own record
     * can both queue the same campaign; only the first send goes out.
     */
    internal fun send(
        settings: Settings,
        impressionPayload: ImpressionPayload,
        context: WingifyUserContext,
        serviceContainer: ServiceContainer,
    ): Boolean {
        val unsent = ImpressionPayload()
        val userId = context.id ?: ""
        val storageService = StorageService(serviceContainer)
        for (index in 0 until impressionPayload.size()) {
            val row = impressionPayload.get(index)
            val key = "$userId:${row.campaignId}:${row.variationId}"
            if (!serviceContainer.sentVariationShownKeys.add(key)) continue
            if (isSentInStorage(storageService, context, row.campaignId, row.variationId)) continue
            unsent.add(row.campaignId, row.variationId, row.featureId)
        }
        if (unsent.hasNoValidData()) return false
        ImpressionUtil.createAndSendImpressionForVariationShown(
            settings = settings,
            impressionPayload = unsent,
            context = context,
            serviceContainer = serviceContainer,
        )
        for (index in 0 until unsent.size()) {
            val row = unsent.get(index)
            persistCampaignSent(storageService, context, row.campaignId, row.variationId)
        }
        return true
    }

    /** Storage key for one campaign and variation send, separate from the feature decision. */
    private fun variationShownStorageKey(campaignId: Int, variationId: Int): String {
        return "${Constants.VWO_META_VARIATION_SHOWN_KEY}${campaignId}_$variationId"
    }

    /** Whether this user, campaign, and variation was already marked sent in storage. */
    private fun isSentInStorage(
        storageService: StorageService,
        context: WingifyUserContext,
        campaignId: Int,
        variationId: Int,
    ): Boolean {
        val stored = storageService.getDataInStorage(
            variationShownStorageKey(campaignId, variationId),
            context,
        )
        return stored?.get(Constants.KEY_VARIATION_SHOWN_SENT) == true
    }

    /** Persists that this user, campaign, and variation was sent, so a later send can skip it. */
    private fun persistCampaignSent(
        storageService: StorageService,
        context: WingifyUserContext,
        campaignId: Int,
        variationId: Int,
    ) {
        storageService.setDataInStorage(
            mapOf(
                "featureKey" to variationShownStorageKey(campaignId, variationId),
                Constants.USER_ID to (context.id ?: ""),
                Constants.KEY_VARIATION_SHOWN_SENT to true,
            )
        )
    }

    /**
     * Writes `variationShownSent` on the feature decision. No-op when [feature] is null.
     * Never downgrades an already-sent sticky (fetchFlags after exposeResult); a legacy
     * decision with no key counts as sent.
     */
    private fun persistVariationShownSent(
        feature: Feature?,
        context: WingifyUserContext,
        serviceContainer: ServiceContainer,
        sent: Boolean,
    ) {
        if (feature == null) return
        val storageService = StorageService(serviceContainer)
        if (!sent) {
            val alreadySent = storageService.getDataInStorage(feature.key, context)
                ?.let { readStorage(it)?.isVariationShownAlreadySent() } == true
            if (alreadySent) return
        }
        storageService.updateDataInStorage(
            feature = feature,
            context = context,
            data = mapOf(Constants.KEY_VARIATION_SHOWN_SENT to sent),
        )
    }

    /** Logs an expose outcome for this flag. No-op when expose dependencies were never attached. */
    private fun logExpose(getFlag: GetFlag, key: String) {
        val deps = getFlag.getExposeDeps() ?: return
        deps.serviceContainer.getLoggerService()?.log(
            LogLevelEnum.INFO,
            key,
            mapOf(
                Constants.USER_ID to deps.context.id,
                "featureKey" to deps.featureKey,
            ),
        )
    }
}
