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
import com.vwo.utils.GsonUtil
import com.vwo.api.GetFlagAPI
import com.vwo.api.SetAttributeAPI.setAttribute
import com.vwo.api.TrackEventAPI
import com.vwo.models.Settings
import com.vwo.models.schemas.SettingsSchema
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOUserContext
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.HooksManager
import com.vwo.services.LoggerService
import com.vwo.services.UrlService
import com.vwo.utils.AliasIdentityManager
import com.vwo.utils.DataTypeUtil.getType
import com.vwo.utils.DataTypeUtil.isBoolean
import com.vwo.utils.DataTypeUtil.isNumber
import com.vwo.utils.DataTypeUtil.isString
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
class VWOClient(settings: String?, options: VWOInitOptions?) {
    internal var processedSettings: Settings? = null
    var settings: String? = null
    private var options: VWOInitOptions? = null
    private var context: Context? = null
    internal var isSettingsValid = false
    internal var settingsFetchTime: Long = 0

    init {
        try {
            this.options = options
            if (settings != null) {
                this.settings = settings
                this.processedSettings = gson.fromJson(settings, Settings::class.java)
                this.processedSettings?.let {
                    SettingsUtil.processSettings(it)
                    // init url version with collection prefix
                    UrlService.init(it.collectionPrefix)
                }
                // init SDKMetaUtil and set sdkVersion
                //SDKMetaUtil.init()
                LoggerService.log(LogLevelEnum.INFO, "CLIENT_INITIALIZED", null)
            }
        } catch (exception: Exception) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "exception occurred while parsing settings " + exception.message
            )
        }
    }

    /**
     * This method is used to update the settings
     * @param newSettings New settings to be updated
     */
    fun updateSettings(newSettings: String?) {
        try {
            this.processedSettings = gson.fromJson(newSettings, Settings::class.java)
            this.processedSettings?.let { SettingsUtil.processSettings(it) }
        } catch (exception: Exception) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "Exception occurred while updating settings " + exception.message
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
        val apiName = "getFlag"
        val getFlag = GetFlag(context)
        try {
            LoggerService.log(
                LogLevelEnum.DEBUG,
                "API_CALLED",
                object : HashMap<String?, String?>() {
                    init {
                        put("apiName", apiName)
                    }
                })
            val hooksManager: HooksManager = HooksManager(options?.integrations)

            // Use effective user ID (either provided userId or generated deviceId)
            val userId = UserIdUtil.getUserId(context, options)
            if (userId.isNullOrEmpty()) {
                getFlag.setIsEnabled(false)
                throw IllegalArgumentException("User ID is required. Please provide a user ID or enable device ID in VWOUserContext.")
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
                return getFlag
            }
            return GetFlagAPI.getFlag(featureKey, procSettings, context, hooksManager)

        } catch (exception: Exception) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "API_THROW_ERROR",
                object : HashMap<String?, String?>() {
                    init {
                        put("apiName", "getFlag")
                        put("err", exception.toString())
                    }
                })
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
        val apiName = "trackEvent"
        val resultMap: MutableMap<String, Boolean> = HashMap()
        try {
            LoggerService.log(
                LogLevelEnum.DEBUG, "API_CALLED", object : HashMap<String?, String?>() {
                    init {
                        put("apiName", apiName)
                    }
                })
            val hooksManager = HooksManager(options?.integrations)
            if (!isString(eventName)) {
                LoggerService.log(
                    LogLevelEnum.ERROR,
                    "API_INVALID_PARAM",
                    object : HashMap<String?, String?>() {
                        init {
                            put("apiName", apiName)
                            put("key", "eventName")
                            put("type", getType(eventName))
                            put("correctType", "String")
                        }
                    })
                throw IllegalArgumentException("TypeError: Event-name should be a string")
            }

            // Use effective user ID (either provided userId or generated deviceId)
            val userId = UserIdUtil.getUserId(context, options)
            require(!userId.isNullOrEmpty()) { "User ID is required. Please provide a user ID or enable device ID in VWOUserContext." }

            // Update context with effective user ID if it was generated
            if (context != null && context.id != userId) {
                context.id = userId
            }

            val pSettings = this.processedSettings
            if (pSettings == null || !SettingsSchema().isSettingsValid(this.processedSettings)) {
                resultMap[eventName] = false
                return resultMap
            }

            val result: Boolean = TrackEventAPI.track(
                pSettings,
                eventName,
                context!!,
                eventProperties,
                hooksManager
            )
            if (result) {
                resultMap[eventName] = true
            } else {
                resultMap[eventName] = false
            }
            return resultMap
        } catch (exception: Exception) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "API_THROW_ERROR",
                object : HashMap<String?, String?>() {
                    init {
                        put("apiName", apiName)
                        put("err", exception.toString())
                    }
                })
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
    fun trackEvent(
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
    fun trackEvent(eventName: String, context: VWOUserContext?): Map<String, Boolean> {
        return track(eventName, context, HashMap())
    }


    /**
     * Sets an attribute for a user in the context provided.
     * This method validates the types of the inputs before proceeding with the API call.
     * @param attributes - Map of attribute key and value to be set
     * @param context User context
     */
    fun setAttribute(immutableAttributes: Map<String, Any>, context: VWOUserContext) {
        val apiName = "setAttribute"
        try {
            LoggerService.log(LogLevelEnum.DEBUG, "API_CALLED", mapOf("apiName" to apiName))

            val attributes = immutableAttributes.toMutableMap()

            if (attributes.isEmpty()) {
                LoggerService.log(
                    LogLevelEnum.WARN, "ATTRIBUTES_NOT_FOUND", mapOf(
                        "apiName" to apiName,
                        "key" to "attributes",
                        "expectedFormat" to "a Map with String keys and String, Number or Boolean value types"
                    )
                )
                throw java.lang.IllegalArgumentException("TypeError: attributeMap should be a non empty map")
            }
            removedUnsupportedValues(attributes, apiName)

            // Use effective user ID (either provided userId or generated deviceId)
            val userId = UserIdUtil.getUserId(context, options)
            require(!userId.isNullOrEmpty()) { "User ID is required. Please provide a user ID or enable device ID in VWOUserContext." }

            // Update context with effective user ID if it was generated
            if (context.id != userId) {
                context.id = userId
            }

            if (this.processedSettings == null || !SettingsSchema().isSettingsValid(this.processedSettings)) {
                return
            }

            setAttribute(processedSettings!!, attributes, context)
        } catch (exception: Exception) {
            LoggerService.log(
                LogLevelEnum.ERROR, "API_THROW_ERROR", mapOf(
                    "apiName" to apiName,
                    "err" to exception.toString()
                )
            )
        }
    }

    private fun removedUnsupportedValues(
        attributes: MutableMap<String, Any>,
        apiName: String
    ) {
        attributes.entries.forEach { entry ->

            if (!isString(entry.value) && !isNumber(entry.value) && !isBoolean(entry.value)) {
                LoggerService.log(
                    LogLevelEnum.ERROR,
                    "API_INVALID_PARAM",
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

    fun setAlias(context: VWOUserContext, aliasId: String) {

        if(options?.isAliasingEnabled != true) {

            val msgMap = mapOf<String?, String?>("key" to "VWOInitOptions.isAliasingEnabled to true.")
            LoggerService.log(LogLevelEnum.ERROR, "ALIAS_NOT_ENABLED", msgMap)
            return
        }

        if (options?.gatewayService?.isEmpty() == true) {

            LoggerService.log(LogLevelEnum.ERROR, "GATEWAY_URL_ERROR", null)
            return
        }

        (context.getIdBasedOnSpecificCondition())?.let { sanitizedId ->

            AliasIdentityManager().setAlias(userId = sanitizedId, aliasId = aliasId)
        } ?: kotlin.run {

            LoggerService.log(LogLevelEnum.ERROR, "API_CONTEXT_INVALID")
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
