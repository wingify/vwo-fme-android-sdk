/**
 * Copyright 2024 Wingify Software Pvt. Ltd.
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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.vwo.models.Settings
import com.vwo.packages.logger.enums.LogLevelEnum

open class VWOClient(settings: String?, options: VWOInitOptions?) {
    private var processedSettings: Settings? = null
    var settings: String? = null
    private var options: VWOInitOptions? = null

    init {
        try {
            this.options = options
            if (settings == null) {
                return
            }
            this.settings = settings
            this.processedSettings = objectMapper.readValue(settings, Settings::class.java)
            SettingsUtil.processSettings(this.processedSettings)
            // init url version with collection prefix
            UrlService.init(processedSettings!!.collectionPrefix)
            // init SDKMetaUtil and set sdkVersion
            SDKMetaUtil.init()
            LoggerService.log(LogLevelEnum.INFO, "CLIENT_INITIALIZED", null)
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
            this.processedSettings = objectMapper.readValue(newSettings, Settings::class.java)
            SettingsUtil.processSettings(this.processedSettings)
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
    fun getFlag(featureKey: String?, context: VWOContext?): GetFlag {
        val apiName = "getFlag"
        val getFlag: GetFlag = GetFlag()
        try {
            LoggerService.log(
                LogLevelEnum.DEBUG,
                "API_CALLED",
                object : HashMap<String?, String?>() {
                    init {
                        put("apiName", apiName)
                    }
                })
            val hooksManager: HooksManager = HooksManager(options.integrations)
            if (context == null || context.id == null || context.id.isEmpty()) {
                getFlag.setIsEnabled(false)
                throw IllegalArgumentException("User ID is required")
            }

            if (featureKey == null || featureKey.isEmpty()) {
                getFlag.setIsEnabled(false)
                throw IllegalArgumentException("Feature Key is required")
            }

            if (this.processedSettings == null || !SettingsSchema().isSettingsValid(this.processedSettings)) {
                LoggerService.log(LogLevelEnum.ERROR, "SETTINGS_SCHEMA_INVALID", null)
                getFlag.setIsEnabled(false)
                return getFlag
            }

            return GetFlagAPI.getFlag(featureKey, this.processedSettings, context, hooksManager)
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
        context: VWOContext?,
        eventProperties: Map<String, *>
    ): Map<String, Boolean> {
        val apiName = "trackEvent"
        val resultMap: MutableMap<String, Boolean> = HashMap()
        try {
            LoggerService.log(
                LogLevelEnum.DEBUG,
                "API_CALLED",
                object : HashMap<String?, String?>() {
                    init {
                        put("apiName", apiName)
                    }
                })
            val hooksManager: HooksManager = HooksManager(options.integrations)
            if (!DataTypeUtil.isString(eventName)) {
                LoggerService.log(
                    LogLevelEnum.ERROR,
                    "API_INVALID_PARAM",
                    object : HashMap<String?, String?>() {
                        init {
                            put("apiName", apiName)
                            put("key", "eventName")
                            put("type", DataTypeUtil.getType(eventName))
                            put("correctType", "String")
                        }
                    })
                throw IllegalArgumentException("TypeError: Event-name should be a string")
            }

            require(!(context == null || context.id == null || context.id.isEmpty())) { "User ID is required" }

            if (this.processedSettings == null || !SettingsSchema().isSettingsValid(this.processedSettings)) {
                LoggerService.log(LogLevelEnum.ERROR, "SETTINGS_SCHEMA_INVALID", null)
                resultMap[eventName] = false
                return resultMap
            }

            val result: Boolean = TrackEventAPI.track(
                this.processedSettings,
                eventName,
                context,
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
        context: VWOContext?,
        eventProperties: Map<String, *>
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
    fun trackEvent(eventName: String, context: VWOContext?): Map<String, Boolean> {
        return track(eventName, context, HashMap<String, Any>())
    }


    /**
     * Sets an attribute for a user in the context provided.
     * This method validates the types of the inputs before proceeding with the API call.
     * @param attributeKey - The key of the attribute to set.
     * @param attributeValue - The value of the attribute to set.
     * @param context User context
     */
    fun setAttribute(attributeKey: String?, attributeValue: String?, context: VWOContext?) {
        val apiName = "setAttribute"
        try {
            LoggerService.log(
                LogLevelEnum.DEBUG,
                "API_CALLED",
                object : HashMap<String?, String?>() {
                    init {
                        put("apiName", apiName)
                    }
                })
            if (!DataTypeUtil.isString(attributeKey)) {
                LoggerService.log(
                    LogLevelEnum.ERROR,
                    "API_INVALID_PARAM",
                    object : HashMap<String?, String?>() {
                        init {
                            put("apiName", apiName)
                            put("key", "eventName")
                            put("type", DataTypeUtil.getType(attributeKey))
                            put("correctType", "String")
                        }
                    })
                throw IllegalArgumentException("TypeError: attributeKey should be a string")
            }

            if (!DataTypeUtil.isString(attributeValue)) {
                LoggerService.log(
                    LogLevelEnum.ERROR,
                    "API_INVALID_PARAM",
                    object : HashMap<String?, String?>() {
                        init {
                            put("apiName", apiName)
                            put("key", "eventName")
                            put("type", DataTypeUtil.getType(attributeValue))
                            put("correctType", "String")
                        }
                    })
                throw IllegalArgumentException("TypeError: attributeValue should be a string")
            }

            require(!(context == null || context.id == null || context.id.isEmpty())) { "User ID is required" }

            if (this.processedSettings == null || !SettingsSchema().isSettingsValid(this.processedSettings)) {
                LoggerService.log(LogLevelEnum.ERROR, "SETTINGS_SCHEMA_INVALID", null)
                return
            }

            SetAttributeAPI.setAttribute(
                this.processedSettings,
                attributeKey,
                attributeValue,
                context
            )
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
        }
    }

    companion object {
        var objectMapper: ObjectMapper = object : ObjectMapper() {
            init {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }
}
