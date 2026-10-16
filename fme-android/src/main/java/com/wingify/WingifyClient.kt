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
package com.wingify

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.vwo.models.user.GetFlag
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.api.GetFlagAPI
import com.wingify.api.GetFlagsAPI
import com.wingify.api.SetAttributeAPI.setAttribute
import com.wingify.api.TrackEventAPI
import com.wingify.constants.Constants
import com.wingify.constants.Constants.FEATURE_KEY
import com.wingify.enums.ApiEnum
import com.wingify.models.HoldoutGroup
import com.wingify.models.Settings
import com.wingify.models.schemas.SettingsSchema
import com.wingify.models.user.FlagCollection
import com.wingify.models.user.WingifyInitOptions
import com.wingify.models.user.WingifyUserContext
import com.wingify.providers.ServiceContainerProvider
import com.wingify.services.LoggerService
import com.wingify.services.SettingsControlApplyMode
import com.wingify.utils.AliasIdentityManager
import com.wingify.utils.DataTypeUtil.getType
import com.wingify.utils.DataTypeUtil.isBoolean
import com.wingify.utils.DataTypeUtil.isNumber
import com.wingify.utils.DataTypeUtil.isString
import com.wingify.utils.EventsUtils
import com.wingify.utils.FunctionUtil.getFormattedErrorMessage
import com.wingify.utils.GsonUtil
import com.wingify.utils.InternalEventSamplingUtil
import com.wingify.utils.NetworkUtil
import com.wingify.utils.SettingsUtil
import com.wingify.utils.UserIdUtil
import java.lang.reflect.Type

/**
 * Client class for interacting with the Wingify SDK.
 *
 * This class handles the core functionality of the SDK, including processing settings, managing
 * configurations, and providing methods for feature flag evaluation, event tracking, and user
 * attribute management.
 *
 * @param settings The initial settings for the client.
 * @param options The initialization options for the client.
 */
open class WingifyClient(
    settings: String?,
    val options: WingifyInitOptions,
    private val wingifyBuilder: WingifyBuilder
) {
    internal var processedSettings: Settings? = null
    var settings: String? = null
    private var context: Context? = null
    internal var isSettingsValid = false
    internal var settingsFetchTime: Long = 0

    init {
        try {
            if (settings != null) {
                this.settings = settings
                this.processedSettings = gson.fromJson(settings, Settings::class.java)
                // init SDKMetaUtil and set sdkVersion
                //SDKMetaUtil.init()
                val serviceContainer = createServiceContainer()
                this.processedSettings?.let {
                    SettingsUtil.processSettings(it, serviceContainer)
                }
                // Register handler so NetworkUtil can trigger refresh without a circular reference.
                serviceContainer.setForceRefreshHandler {
                    val fetched = wingifyBuilder.getSettings(true, Constants.SETTINGS_PULL_ENDPOINT)
                    if (fetched == null) return@setForceRefreshHandler false
                    val applied = updateSettings(fetched, SettingsControlApplyMode.POST_PING_FETCH)
                    if (applied) wingifyBuilder.syncOriginalSettings(fetched)
                    applied
                }
                handleSettingsControlFlags(SettingsControlApplyMode.APPLY)
                serviceContainer.getLoggerService()
                    ?.log(LogLevelEnum.INFO, "CLIENT_INITIALIZED", null)
            }
        } catch (exception: Exception) {
            val serviceContainer = createServiceContainer()
            serviceContainer?.getLoggerService()?.log(
                LogLevelEnum.ERROR,
                "exception occurred while parsing settings " + exception.message,
                null
            )
        }
    }

    /**
     * Creates a ServiceContainer instance with the current settings and options
     * Following Java SDK pattern where ServiceContainer is created per API call
     * @return ServiceContainer instance
     */
    fun createServiceContainer(): ServiceContainer {
        val serviceContainer = ServiceContainerProvider.createServiceContainer(
            wingifyBuilder,
            processedSettings,
            options,
        )
        return serviceContainer
    }

    /**
     * This method is used to update the settings.
     * Stale or duplicate snapshots are skipped. After a successful parse, panic and
     * force-refresh flags are evaluated according to [controlApplyMode].
     *
     * @param newSettings New settings JSON to be applied.
     * @param controlApplyMode How this snapshot was obtained (init, polling, or post-ping fetch).
     * @return true when the snapshot was parsed and applied.
     */
    open fun updateSettings(
        newSettings: String?,
        controlApplyMode: SettingsControlApplyMode = SettingsControlApplyMode.APPLY,
    ): Boolean {
        try {
            val parsed = gson.fromJson(newSettings, Settings::class.java) ?: return false
            val sc = createServiceContainer()
            if (!sc.panicManager.shouldApplySettingsSnapshot(parsed)) {
                sc.getLoggerService()?.log(
                    LogLevelEnum.INFO,
                    "SETTINGS_UPDATE_SKIPPED_STALE",
                    mapOf("timestamp" to (parsed.lastUpdatedOn ?: parsed.version)?.toString())
                )
                return false
            }
            this.processedSettings = parsed
            this.processedSettings?.let { SettingsUtil.processSettings(it) }
            handleSettingsControlFlags(controlApplyMode)
            return true
        } catch (exception: Exception) {
            val serviceContainer = createServiceContainer()
            LoggerService.errorLog(
                key = "INVALID_SETTINGS_SCHEMA",
                data = mapOf(
                    "apiName" to ApiEnum.UPDATE_SETTINGS.value,
                    "isViaWebhook" to false,
                    Constants.ERR to getFormattedErrorMessage(exception)
                ),
                debugData = mapOf("an" to ApiEnum.UPDATE_SETTINGS.value),
                shouldSendToVWO = true,
                serviceContainer = serviceContainer
            )
            return false
        }
    }

    /**
     * Evaluates panic-mode and force-refresh control flags from the current settings.
     * The [ServiceContainer.triggerForceRefresh] guard prevents concurrent refreshes.
     *
     * @param mode How the current settings snapshot was obtained.
     */
    private fun handleSettingsControlFlags(
        mode: SettingsControlApplyMode = SettingsControlApplyMode.APPLY,
    ) {
        val settings = processedSettings ?: return
        val sc = createServiceContainer()
        val result = sc.panicManager.applySettingsControl(settings, mode)
        if (result.needsCollectPingConfirmation) {
            confirmControlViaCollectPing(sc)
        }
        if (result.shouldTriggerForceRefresh) {
            sc.triggerForceRefresh(result.isRefreshFromPanicClear)
        }
    }

    /**
     * Confirms panic with a control ping. DaCDN response is the source of truth.
     * Fail-safe: keep the current latch when the ping fails.
     *
     * @param serviceContainer Container whose panic manager and network stack to use.
     */
    private fun confirmControlViaCollectPing(serviceContainer: ServiceContainer) {
        if (!serviceContainer.panicManager.tryStartPanicClearConfirmation()) return
        val context = WingifyUserContext().apply {
            id = "${serviceContainer.getAccountId()}_${serviceContainer.getSdkKey()}"
        }
        serviceContainer.executeOnCoroutine {
            try {
                NetworkUtil.sendPanicHeartbeat(context, serviceContainer, launchAsync = false)
            } finally {
                serviceContainer.panicManager.finishPanicClearConfirmation()
            }
        }
    }

    /**
     * Blocks public API work while panic is latched.
     * Fires a collector ping so DaCDN can push updated control. Does not send `vwo_sdkDebug`.
     *
     * @param apiName API name used in the blocked-by-panic log.
     * @param serviceContainer Container whose panic manager and heartbeat to use.
     * @param context User context for the heartbeat visitor id; omitted when null.
     * @return true when the caller should abort (panic is active).
     */
    private fun logAndBlockIfPanicActive(
        apiName: String,
        serviceContainer: ServiceContainer,
        context: WingifyUserContext? = null,
    ): Boolean {
        if (!serviceContainer.panicManager.isPanicActive()) return false
        val placeholders: Map<String?, String?> = mapOf("apiName" to apiName)
        serviceContainer.getLoggerService()?.log(
            LogLevelEnum.INFO,
            "API_BLOCKED_BY_PANIC_MODE",
            placeholders
        )
        if (context != null) {
            NetworkUtil.sendPanicHeartbeat(context, serviceContainer)
        }
        return true
    }

    /**
     * This method is used to get the flag value for the given feature key.
     * Returns a disabled flag while panic is latched and fires a collector heartbeat.
     *
     * @param featureKey Feature key for which the flag value is to be fetched
     * @param context User context
     * @return GetFlag object containing the flag values
     */
    fun getFlag(featureKey: String?, context: WingifyUserContext): GetFlag {
        val apiName = ApiEnum.GET_FLAG.value
        val getFlag = GetFlag(context)
        // Create ServiceContainer for this API call
        val serviceContainer = createServiceContainer()
        if (serviceContainer == null) {
            getFlag.setIsEnabled(false)
            return getFlag
        }
        try {
            serviceContainer.getLoggerService()?.log(
                LogLevelEnum.DEBUG,
                "API_CALLED",
                object : HashMap<String?, String>() {
                    init {
                        put("apiName", apiName)
                    }
                })

            // Panic mode: block before alias/gateway identity work or flag evaluation.
            if (logAndBlockIfPanicActive(apiName, serviceContainer, context)) {
                serviceContainer.getLoggerService()?.log(
                    LogLevelEnum.DEBUG,
                    "PANIC_MODE_ENABLED",
                    mapOf("userId" to context.id, "featureKey" to (featureKey ?: ""))
                )
                getFlag.setIsEnabled(false)
                return getFlag
            }

            // Use effective user ID (either provided userId or generated deviceId)
            val userId = UserIdUtil.getUserId(context, options, serviceContainer)
            if (userId.isNullOrEmpty()) {
                getFlag.setIsEnabled(false)
                LoggerService.errorLog(
                    key = "API_CONTEXT_INVALID",
                    data = emptyMap(),
                    debugData = mapOf(
                        "an" to ApiEnum.GET_FLAG.value,
                        FEATURE_KEY to (featureKey ?: "")
                    ),
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                throw IllegalArgumentException("User ID is required")
            }

            // Update context with effective user ID if it was generated
            if (context.id != userId) {
                context.id = userId
            }
            if (featureKey.isNullOrEmpty()) {
                getFlag.setIsEnabled(false)
                throw IllegalArgumentException("Feature Key is required")
            }

            val procSettings = this.processedSettings
            if (procSettings == null || !SettingsSchema().isSettingsValid(procSettings)) {
                getFlag.setIsEnabled(false)
                LoggerService.errorLog(
                    key = "INVALID_SETTINGS_SCHEMA",
                    data = emptyMap(),
                    debugData = mapOf(
                        "an" to ApiEnum.GET_FLAG.value,
                        "uuid" to context.getUuid(serviceContainer),
                        FEATURE_KEY to featureKey
                    ),
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                return getFlag
            }

            return GetFlagAPI.getFlag(
                featureKey,
                procSettings,
                context,
                serviceContainer,
                serviceContainer.getHooksManager()
            )

        } catch (exception: Exception) {
            LoggerService.errorLog(
                key = "EXECUTION_FAILED",
                data = mapOf(
                    "apiName" to apiName,
                    Constants.ERR to getFormattedErrorMessage(exception)
                ),
                debugData = mapOf(
                    "an" to ApiEnum.GET_FLAG.value,
                    "uuid" to context.getUuid(serviceContainer),
                    FEATURE_KEY to (featureKey ?: "")
                ),
                shouldSendToVWO = true,
                serviceContainer = serviceContainer
            )

            getFlag.setIsEnabled(false)
            return getFlag
        }
    }

    /**
     * Evaluates multiple feature flags in parallel and returns a [FlagCollection].
     * Returns disabled flags while panic is latched and fires a collector heartbeat.
     *
     * @param flagNames Feature keys to evaluate, or null/empty to evaluate all flags from settings.
     * @param context User context (resolved once; each flag gets an isolated copy for evaluation).
     * @return [FlagCollection] keyed by feature key.
     */
    fun getFlags(flagNames: Array<String>?, context: WingifyUserContext): FlagCollection {
        val apiName = ApiEnum.GET_FLAGS.value
        val serviceContainer = createServiceContainer()
        if (serviceContainer == null) {
            return FlagCollection.createDisabled(flagNames, context)
        }
        try {
            serviceContainer.getLoggerService()?.log(
                LogLevelEnum.DEBUG,
                "API_CALLED",
                mapOf("apiName" to apiName)
            )

            if (logAndBlockIfPanicActive(apiName, serviceContainer, context)) {
                return FlagCollection.createDisabled(flagNames, context)
            }

            val userId = UserIdUtil.getUserId(context, options, serviceContainer)
            if (userId.isNullOrEmpty()) {
                LoggerService.errorLog(
                    key = "API_CONTEXT_INVALID",
                    data = emptyMap(),
                    debugData = mapOf("an" to ApiEnum.GET_FLAGS.value),
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                throw IllegalArgumentException("User ID is required")
            }

            if (context.id != userId) {
                context.id = userId
            }

            val procSettings = this.processedSettings
            if (procSettings == null || !isSettingsValid) {
                LoggerService.errorLog(
                    key = "INVALID_SETTINGS_SCHEMA",
                    data = emptyMap(),
                    debugData = mapOf(
                        "an" to ApiEnum.GET_FLAGS.value,
                        "uuid" to context.getUuid(serviceContainer),
                    ),
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                return FlagCollection.createDisabled(flagNames, context)
            }

            return GetFlagsAPI.getFlags(
                flagNames,
                procSettings,
                context,
                serviceContainer,
            )
        } catch (exception: Exception) {
            LoggerService.errorLog(
                key = "EXECUTION_FAILED",
                data = mapOf(
                    "apiName" to apiName,
                    Constants.ERR to getFormattedErrorMessage(exception)
                ),
                debugData = mapOf(
                    "an" to ApiEnum.GET_FLAGS.value,
                    "uuid" to context.getUuid(serviceContainer),
                ),
                shouldSendToVWO = true,
                serviceContainer = serviceContainer
            )
            return FlagCollection.createDisabled(flagNames, context)
        }
    }

    /**
     * This method is used to track the event.
     * Returns false for the event while panic is latched and fires a collector heartbeat.
     *
     * @param eventName Event name to be tracked
     * @param context User context
     * @param eventProperties event properties to be sent for the event
     * @return Map containing the event name and its status
     */
    private fun track(
        eventName: String,
        context: WingifyUserContext?,
        eventProperties: Map<String, Any>
    ): Map<String, Boolean> {
        val apiName = ApiEnum.TRACK_EVENT.value
        val resultMap: MutableMap<String, Boolean> = HashMap()
        try {
            // Create ServiceContainer for this API call
            val serviceContainer = createServiceContainer()
            if (serviceContainer == null) {
                resultMap[eventName] = false
                return resultMap
            }
            serviceContainer.getLoggerService()?.log(
                LogLevelEnum.DEBUG, "API_CALLED", object : HashMap<String?, String?>() {
                    init {
                        put("apiName", apiName)
                    }
                })
            val debugData = mutableMapOf("an" to apiName)
            context?.getUuid(serviceContainer)?.let { debugData["uuid"] = it }

            if (!isString(eventName)) {
                LoggerService.errorLog(
                    key = "INVALID_PARAM",
                    data = mapOf(
                        "apiName" to apiName,
                        "key" to "eventName",
                        "type" to getType(eventName),
                        "correctType" to "String"
                    ),
                    debugData = debugData,
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                throw IllegalArgumentException("TypeError: Event-name should be a string")
            }

            if (logAndBlockIfPanicActive(apiName, serviceContainer, context)) {
                resultMap[eventName] = false
                return resultMap
            }

            // Use effective user ID (either provided userId or generated deviceId)
            val userId = UserIdUtil.getUserId(context, options, serviceContainer)
            require(!userId.isNullOrEmpty()) { "User ID is required. Please provide a user ID or enable device ID in the user context." }

            // Update context with effective user ID if it was generated
            if (context != null && context.id != userId) {
                context.id = userId
            }

            val pSettings = this.processedSettings
            if (pSettings == null || !SettingsSchema().isSettingsValid(this.processedSettings)) {
                resultMap[eventName] = false
                LoggerService.errorLog(
                    key = "INVALID_SETTINGS_SCHEMA",
                    data = emptyMap(),
                    debugData = mapOf("an" to apiName),
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                return resultMap
            }

            val result: Boolean = TrackEventAPI.track(
                pSettings,
                eventName,
                context!!,
                eventProperties,
                serviceContainer.getHooksManager(),
                serviceContainer,
            )
            if (result) {
                resultMap[eventName] = true
            } else {
                resultMap[eventName] = false
            }
            return resultMap
        } catch (exception: Exception) {

            val sc = createServiceContainer()

            val debugData = mutableMapOf("an" to apiName)
            context?.getUuid(serviceContainer = sc)?.let { debugData["uuid"] = it }
            LoggerService.errorLog(
                key = "EXECUTION_FAILED",
                data = mapOf(
                    "apiName" to apiName,
                    Constants.ERR to getFormattedErrorMessage(exception),
                ),
                debugData = debugData,
                shouldSendToVWO = true,
                serviceContainer = sc
            )
            resultMap[eventName] = false
            return resultMap
        }
    }

    /**
     * Overloaded function if event properties need to be passed
     * calls track method to track the event
     * @param eventName Event name to be tracked
     * @param context User context
     * @param eventProperties event properties to be sent for the event
     * @return Map containing the event name and its status
     */
    open fun trackEvent(
        eventName: String,
        context: WingifyUserContext?,
        eventProperties: Map<String, Any>
    ): Map<String, Boolean> {
        return track(eventName, context, eventProperties)
    }

    /**
     * Overloaded function for no event properties
     * calls track method to track the event
     * @param eventName Event name to be tracked
     * @param context User context
     * @return Map containing the event name and its status
     */
    open fun trackEvent(eventName: String, context: WingifyUserContext?): Map<String, Boolean> {
        return track(eventName, context, HashMap())
    }

    /**
     * Sets an attribute for a user in the context provided.
     * While panic is latched, fires a collector ping, then returns.
     * @param attributes - Map of attribute key and value to be set
     * @param context User context
     */
    open fun setAttribute(immutableAttributes: Map<String, Any>, context: WingifyUserContext) {
        val apiName = ApiEnum.SET_ATTRIBUTE.value
        try {
            // Create ServiceContainer for this API call
            val serviceContainer = createServiceContainer()
            if (serviceContainer == null) {
                return
            }
            serviceContainer.getLoggerService()
                ?.log(LogLevelEnum.DEBUG, "API_CALLED", mapOf("apiName" to apiName))

            if (logAndBlockIfPanicActive(apiName, serviceContainer, context)) {
                return
            }

            val attributes = immutableAttributes.toMutableMap()

            if (attributes.isEmpty()) {
                LoggerService.errorLog(
                    key = "ATTRIBUTES_NOT_FOUND",
                    data = mapOf(
                        "apiName" to apiName,
                        "key" to "attributes",
                        "expectedFormat" to "a Map with String keys and String, Number or Boolean value types"
                    ),
                    debugData = mapOf(
                        "an" to apiName,
                        "uuid" to context.getUuid(serviceContainer)
                    ),
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                throw java.lang.IllegalArgumentException("TypeError: attributeMap should be a non empty map")
            }
            removedUnsupportedValues(attributes, apiName, serviceContainer)

            // Use effective user ID (either provided userId or generated deviceId)
            val userId = UserIdUtil.getUserId(context, options, serviceContainer)
            require(!userId.isNullOrEmpty()) { "User ID is required. Please provide a user ID or enable device ID in the user context." }

            // Update context with effective user ID if it was generated
            if (context.id != userId) {
                context.id = userId
            }

            if (this.processedSettings == null || !SettingsSchema().isSettingsValid(this.processedSettings)) {
                LoggerService.errorLog(
                    key = "INVALID_SETTINGS_SCHEMA", emptyMap(),
                    debugData = mapOf("an" to apiName, "uuid" to context.getUuid(serviceContainer)),
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                return
            }

            setAttribute(processedSettings!!, attributes, context, serviceContainer)
        } catch (exception: Exception) {
            val sc2 = createServiceContainer()
            LoggerService.errorLog(
                key = "EXECUTION_FAILED",
                data = mapOf("apiName" to apiName, Constants.ERR to exception.toString()),
                debugData = mapOf(
                    "an" to apiName,
                    "uuid" to context.getUuid(serviceContainer = sc2)
                ),
                shouldSendToVWO = true,
                serviceContainer = sc2
            )
        }
    }

    private fun removedUnsupportedValues(
        attributes: MutableMap<String, Any>,
        apiName: String,
        serviceContainer: ServiceContainer
    ) {
        attributes.entries.forEach { entry ->

            if (!isString(entry.value) && !isNumber(entry.value) && !isBoolean(entry.value)) {
                serviceContainer.getLoggerService()?.log(
                    LogLevelEnum.ERROR,
                    "INVALID_PARAM",
                    mapOf(
                        "apiName" to apiName,
                        "key" to "attribute value",
                        "type" to getType(entry.value),
                        "correctType" to "String, Number, Boolean"
                    )
                )
                throw java.lang.IllegalArgumentException("TypeError: attributeMap should values of type String, Number, Boolean")
            }
        }
    }

    open fun setAlias(context: WingifyUserContext, aliasId: String) {

        val serviceContainer = createServiceContainer()

        if (logAndBlockIfPanicActive(ApiEnum.SET_ALIAS.value, serviceContainer, context)) {
            return
        }

        if (options?.isAliasingEnabled != true) {

            val msgMap = mapOf("key" to "WingifyInitOptions.isAliasingEnabled to true.")
            val debugData =
                mutableMapOf(
                    "an" to ApiEnum.SET_ALIAS.value,
                    "uuid" to context.getUuid(serviceContainer)
                )
            LoggerService.errorLog(
                key = "ALIAS_NOT_ENABLED",
                data = msgMap,
                debugData = debugData,
                shouldSendToVWO = false,
                serviceContainer = serviceContainer
            )
            return
        }

        if (options?.gatewayService?.isEmpty() == true) {
            val debugData =
                mutableMapOf(
                    "an" to ApiEnum.SET_ALIAS.value,
                    "uuid" to context.getUuid(serviceContainer)
                )
            LoggerService.errorLog(
                key = "INVALID_GATEWAY_URL",
                data = null,
                debugData = debugData,
                shouldSendToVWO = true,
                serviceContainer = serviceContainer
            )
            return
        }

        (context.getIdBasedOnSpecificCondition())?.let { sanitizedId ->

            AliasIdentityManager(serviceContainer = serviceContainer).setAlias(
                userId = sanitizedId,
                aliasId = aliasId
            )
        } ?: kotlin.run {
            val debugData = mutableMapOf(
                "an" to ApiEnum.SET_ALIAS.value,
                "uuid" to context.getUuid(serviceContainer)
            )

            LoggerService.errorLog(
                key = "API_CONTEXT_INVALID",
                data = null,
                debugData = debugData,
                shouldSendToVWO = true,
                serviceContainer = serviceContainer
            )
        }
    }

    /**
     * Sends the SDK init event (`vwo_initCalled`) when settings are valid and this
     * is not a repeat init (`wasInitializedEarlier`). No-op while panic is latched.
     *
     * @param serviceContainer Optional container; a new one is created when null.
     */
    fun sendSdkInitEvent(serviceContainer: ServiceContainer? = null) {
        val container = serviceContainer ?: createServiceContainer()
        if (logAndBlockIfPanicActive(ApiEnum.INIT.value, container)) {
            return
        }
        val wasInitializedEarlier = processedSettings?.sdkMetaInfo?.wasInitializedEarlier

        if (isSettingsValid && wasInitializedEarlier != true) {
            EventsUtils().sendSdkInitEvent(container)
        }
    }

    /**
     * Sends SDK usage statistics (`vwo_sdkUsageStats`).
     *
     * Sends only when `usageStatsAccountId` is present in settings and the event
     * passes the usage-stats check. When `alwaysApplySampling.client` is true,
     * sampling uses `sampling.usage.client` (defaults to 1% when missing);
     * otherwise the event is always sent.
     *
     * @param sdkInitTime Time taken to initialize the SDK in milliseconds.
     * @param serviceContainer SDK service container for sampling, panic, and the usage event.
     */
    fun sendUsageStats(sdkInitTime: Long, serviceContainer: ServiceContainer) {
        if (logAndBlockIfPanicActive("usageStats", serviceContainer)) {
            return
        }
        val usageStatsAccountId = processedSettings?.usageStatsAccountId ?: return
        if (!InternalEventSamplingUtil.isUsageStatsEventQualified(processedSettings)) {
            return
        }
        EventsUtils().sendSDKUsageStatsEvent(
            usageStatsAccountId,
            serviceContainer,
            settingsFetchTime,
            sdkInitTime,
        )
    }

    companion object {
        val gson: Gson = GsonUtil.gson

        // Gson-based ObjectMapper replacement for API compatibility
        @JvmStatic
        val objectMapper = GsonObjectMapper()
    }

    class HoldoutGroupAdapter : JsonDeserializer<List<HoldoutGroup>> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): List<HoldoutGroup> {
            // If the server sends [], it's a JsonArray
            if (json.isJsonArray) {
                val list = mutableListOf<HoldoutGroup>()
                json.asJsonArray.forEach {
                    list.add(context.deserialize(it, HoldoutGroup::class.java))
                }
                return list
            }
            // If the server sends {} (empty object), return an empty list
            return emptyList()
        }
    }

    // Companion class for ObjectMapper replacement
    class GsonObjectMapper {
        fun <T> readValue(json: String, clazz: Class<T>): T {
            return gson.fromJson(json, clazz)
        }

        fun writeValueAsString(obj: Any): String {
            return gson.toJson(obj)
        }

        // For JsonNode equivalent, we'll use JsonElement
        fun readTree(json: String): com.google.gson.JsonElement {
            return gson.fromJson(json, com.google.gson.JsonElement::class.java)
        }

        // For type conversion (equivalent to convertValue)
        fun <T> convertValue(obj: Any, clazz: Class<T>): T {
            val json = gson.toJson(obj)
            return gson.fromJson(json, clazz)
        }

        // For creating array nodes (basic implementation)
        fun createArrayNode(): com.google.gson.JsonArray {
            return com.google.gson.JsonArray()
        }

        // For creating object nodes
        fun createObjectNode(): com.google.gson.JsonObject {
            return com.google.gson.JsonObject()
        }

        // For tree conversion
        fun valueToTree(obj: Any): com.google.gson.JsonElement {
            return gson.toJsonTree(obj)
        }
    }
}
