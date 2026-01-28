/**
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
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
package com.vwo

import android.content.Context
import com.google.gson.Gson
import com.vwo.api.GetFlagAPI
import com.vwo.api.SetAttributeAPI.setAttribute
import com.vwo.api.TrackEventAPI
import com.vwo.constants.Constants
import com.vwo.constants.Constants.FEATURE_KEY
import com.vwo.enums.ApiEnum
import com.vwo.models.Settings
import com.vwo.models.schemas.SettingsSchema
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.providers.ServiceContainerProvider
import com.vwo.services.LoggerService
import com.vwo.utils.AliasIdentityManager
import com.vwo.utils.DataTypeUtil.getType
import com.vwo.utils.DataTypeUtil.isBoolean
import com.vwo.utils.DataTypeUtil.isNumber
import com.vwo.utils.DataTypeUtil.isString
import com.vwo.utils.FunctionUtil.getFormattedErrorMessage
import com.vwo.utils.GsonUtil
import com.vwo.utils.SettingsUtil
import com.vwo.utils.UserIdUtil

/**
 * Client class for interacting with the VWO SDK.
 *
 * This class handles the core functionality of the VWO SDK,including processing settings, managing
 * configurations, and providing methods for feature flag evaluation, event tracking, and user
 * attribute management.
 *
 * @param settings The initial settings for the VWO client.
 * @param options The initialization options for the VWO client.
 */
open class VWOClient(
    settings: String?,
    val options: VWOInitOptions,
    private val vwoBuilder: VWOBuilder
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
                this.processedSettings?.let {
                    SettingsUtil.processSettings(it)
                }
                // init SDKMetaUtil and set sdkVersion
                //SDKMetaUtil.init()
                val serviceContainer = createServiceContainer()
                serviceContainer?.getLoggerService()
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
            vwoBuilder,
            processedSettings,
            options,
        )
        return serviceContainer
    }

    /**
     * This method is used to update the settings
     * @param newSettings New settings to be updated
     */
    open fun updateSettings(newSettings: String?) {
        try {
            this.processedSettings = gson.fromJson(newSettings, Settings::class.java)
            this.processedSettings?.let { SettingsUtil.processSettings(it) }
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
        }
    }

    /**
     * This method is used to get the flag value for the given feature key
     * @param featureKey Feature key for which the flag value is to be fetched
     * @param context User context
     * @return GetFlag object containing the flag values
     */
    fun getFlag(featureKey: String?, context: VWOUserContext): GetFlag {
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
                    shouldSendToVWO = false,
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
     * This method is used to track the event
     * @param eventName Event name to be tracked
     * @param context User context
     * @param eventProperties event properties to be sent for the event
     * @return Map containing the event name and its status
     */
    private fun track(
        eventName: String,
        context: VWOUserContext?,
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

            // Use effective user ID (either provided userId or generated deviceId)
            val userId = UserIdUtil.getUserId(context, options, serviceContainer)
            require(!userId.isNullOrEmpty()) { "User ID is required. Please provide a user ID or enable device ID in VWOUserContext." }

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
                    shouldSendToVWO = false,
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
        context: VWOUserContext?,
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
    open fun trackEvent(eventName: String, context: VWOUserContext?): Map<String, Boolean> {
        return track(eventName, context, HashMap())
    }


    /**
     * Sets an attribute for a user in the context provided.
     * This method validates the types of the inputs before proceeding with the API call.
     * @param attributes - Map of attribute key and value to be set
     * @param context User context
     */
    open fun setAttribute(immutableAttributes: Map<String, Any>, context: VWOUserContext) {
        val apiName = ApiEnum.SET_ATTRIBUTE.value
        try {
            // Create ServiceContainer for this API call
            val serviceContainer = createServiceContainer()
            if (serviceContainer == null) {
                return
            }
            serviceContainer.getLoggerService()
                ?.log(LogLevelEnum.DEBUG, "API_CALLED", mapOf("apiName" to apiName))

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
            require(!userId.isNullOrEmpty()) { "User ID is required. Please provide a user ID or enable device ID in VWOUserContext." }

            // Update context with effective user ID if it was generated
            if (context.id != userId) {
                context.id = userId
            }

            if (this.processedSettings == null || !SettingsSchema().isSettingsValid(this.processedSettings)) {
                LoggerService.errorLog(
                    key = "INVALID_SETTINGS_SCHEMA", emptyMap(),
                    debugData = mapOf("an" to apiName, "uuid" to context.getUuid(serviceContainer)),
                    shouldSendToVWO = false,
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
                debugData = mapOf("an" to apiName, "uuid" to context.getUuid(serviceContainer = sc2)),
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

    open fun setAlias(context: VWOUserContext, aliasId: String) {

        val serviceContainer = createServiceContainer()

        if (options?.isAliasingEnabled != true) {

            val msgMap = mapOf("key" to "VWOInitOptions.isAliasingEnabled to true.")
            val debugData =
                mutableMapOf("an" to ApiEnum.SET_ALIAS.value, "uuid" to context.getUuid(serviceContainer))
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
                mutableMapOf("an" to ApiEnum.SET_ALIAS.value, "uuid" to context.getUuid(serviceContainer))
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

    companion object {
        val gson: Gson = GsonUtil.gson

        // Gson-based ObjectMapper replacement for API compatibility
        @JvmStatic
        val objectMapper = GsonObjectMapper()
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
