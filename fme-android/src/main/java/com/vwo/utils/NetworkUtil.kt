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
package com.vwo.utils

import com.vwo.VWOClient
import com.vwo.constants.Constants
import com.vwo.constants.Constants.defaultString
import com.vwo.enums.HeadersEnum
import com.vwo.enums.UrlEnum
import com.vwo.models.Settings
import com.vwo.models.request.Event
import com.vwo.models.request.EventArchData
import com.vwo.models.request.EventArchPayload
import com.vwo.models.request.EventArchQueryParams.RequestQueryParams
import com.vwo.models.request.EventArchQueryParams.SettingsQueryParams
import com.vwo.models.request.Props
import com.vwo.models.request.visitor.Visitor
import com.vwo.models.user.VWOContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.services.LoggerService.Companion.log
import com.vwo.services.SettingsManager
import com.vwo.services.UrlService.baseUrl
import java.util.Calendar

class NetworkUtil {
    /**
     * Creates the query parameters for the settings API.
     * @param apikey  The sdk key of the account.
     * @param accountId The ID of the account.
     * @return
     */
    fun getSettingsPath(apikey: String?, accountId: Int): MutableMap<String, String> {
        val settingsQueryParams =
            SettingsQueryParams(apikey!!, generateRandom(), accountId.toString())
        return settingsQueryParams.queryParams
    }

    companion object {
        /**
         * Creates the base properties for the event arch APIs.
         * @param setting  The settings model containing configuration.
         * @param eventName  The name of the event.
         * @param visitorUserAgent  The user agent of the user.
         * @param ipAddress  The IP address of the user.
         * @return
         */
        fun getEventsBaseProperties(
            setting: Settings,
            eventName: String,
            visitorUserAgent: String?,
            ipAddress: String?
        ): Map<String, String> {
            val requestQueryParams = RequestQueryParams(
                eventName,
                setting.accountId.toString(),
                setting.sdkKey!!,
                visitorUserAgent!!,
                ipAddress!!,
                generateEventUrl()
            )
            return requestQueryParams.queryParams
        }

        /**
         * Creates the base payload for the event arch APIs.
         * @param settings The settings model containing configuration.
         * @param userId  The ID of the user.
         * @param eventName The name of the event.
         * @param visitorUserAgent The user agent of the user.
         * @param ipAddress The IP address of the user.
         * @return
         */
        fun getEventBasePayload(
            settings: Settings,
            userId: String?,
            eventName: String,
            visitorUserAgent: String?,
            ipAddress: String?
        ): EventArchPayload {
            val uuid = UUIDUtils.getUUID(userId, settings.accountId.toString())
            val eventArchData = EventArchData()
            eventArchData.msgId = generateMsgId(uuid)
            eventArchData.visId = uuid
            eventArchData.sessionId = generateSessionId()
            setOptionalVisitorData(eventArchData, visitorUserAgent, ipAddress)

            val event = createEvent(eventName, settings)
            eventArchData.event = event

            val visitor = createVisitor(settings)
            eventArchData.visitor = visitor

            val eventArchPayload = EventArchPayload()
            eventArchPayload.d = eventArchData
            return eventArchPayload
        }

        /**
         * Sets the optional visitor data for the event arch APIs.
         * @param eventArchData The event model containing the event data.
         * @param visitorUserAgent The user agent of the user.
         * @param ipAddress The IP address of the user.
         */
        private fun setOptionalVisitorData(
            eventArchData: EventArchData,
            visitorUserAgent: String?,
            ipAddress: String?
        ) {
            if (!visitorUserAgent.isNullOrEmpty()) {
                eventArchData.visitor_ua = visitorUserAgent
            }

            if (!ipAddress.isNullOrEmpty()) {
                eventArchData.visitor_ip = ipAddress
            }
        }

        /**
         * Creates the event model for the event arch APIs.
         * @param eventName The name of the event.
         * @param settings The settings model containing configuration.
         * @return The event model.
         */
        private fun createEvent(eventName: String, settings: Settings): Event {
            val event = Event()
            val props = createProps(settings)
            event.props = props
            event.name = eventName
            event.time = Calendar.getInstance().timeInMillis
            return event
        }

        /**
         * Creates the visitor model for the event arch APIs.
         * @param settings The settings model containing configuration.
         * @return The visitor model.
         */
        private fun createProps(settings: Settings): Props {
            val props = Props()
            props.setSdkName(Constants.SDK_NAME)
            props.setSdkVersion(SDKMetaUtil.sdkVersion)
            props.setEnvKey(settings.sdkKey)
            return props
        }

        /**
         * Creates the visitor model for the event arch APIs.
         * @param settings The settings model containing configuration.
         * @return The visitor model.
         */
        private fun createVisitor(settings: Settings): Visitor {
            val visitor = Visitor()
            val visitorProps: MutableMap<String, Any> = HashMap()
            visitorProps[Constants.VWO_FS_ENVIRONMENT] = settings.sdkKey?:defaultString
            visitor.setProps(visitorProps)
            return visitor
        }

        /**
         * Returns the payload data for the track user API.
         * @param settings  The settings model containing configuration.
         * @param userId  The ID of the user.
         * @param eventName  The name of the event.
         * @param campaignId The ID of the campaign.
         * @param variationId  The ID of the variation.
         * @param visitorUserAgent  The user agent of the user.
         * @param ipAddress  The IP address of the user.
         * @return
         */
        fun getTrackUserPayloadData(
            settings: Settings,
            userId: String?,
            eventName: String,
            campaignId: Int,
            variationId: Int,
            visitorUserAgent: String?,
            ipAddress: String?
        ): Map<String, Any?> {
            val properties =
                getEventBasePayload(settings, userId, eventName, visitorUserAgent, ipAddress)
            properties.d!!.event!!.props!!.id = campaignId
            properties.d!!.event!!.props!!.variation = variationId.toString()
            properties.d!!.event!!.props!!.setIsFirst(1)
            log(
                LogLevelEnum.DEBUG,
                "IMPRESSION_FOR_TRACK_USER",
                object : HashMap<String?, String?>() {
                    init {
                        put("accountId", settings.accountId.toString())
                        put("userId", userId)
                        put("campaignId", campaignId.toString())
                    }
                })
            val payload: Map<String, Any?> =
                VWOClient.objectMapper.convertValue(properties, MutableMap::class.java)
            return removeNullValues(payload)
        }

        /**
         * Returns the payload data for the goal API.
         * @param settings  The settings model containing configuration.
         * @param userId  The ID of the user.
         * @param eventName  The name of the event.
         * @param context  The user context model containing user-specific data.
         * @param eventProperties event properties for the event
         * @return  Map containing the payload data.
         */
        fun getTrackGoalPayloadData(
            settings: Settings,
            userId: String?,
            eventName: String,
            context: VWOContext,
            eventProperties: Map<String, Any>?
        ): Map<String, Any?> {
            val properties = getEventBasePayload(
                settings,
                userId,
                eventName,
                context.userAgent,
                context.ipAddress
            )
            properties.d!!.event!!.props!!.setIsCustomEvent(true)
            addCustomEventProperties(properties, eventProperties)
            log(
                LogLevelEnum.DEBUG,
                "IMPRESSION_FOR_TRACK_GOAL",
                object : HashMap<String?, String?>() {
                    init {
                        put("eventName", eventName)
                        put("accountId", settings.accountId.toString())
                        put("userId", userId)
                    }
                })
            val payload: Map<String, Any?> =
                VWOClient.objectMapper.convertValue(properties, MutableMap::class.java)
            return removeNullValues(payload)
        }

        /**
         * Adds custom event properties to the payload.
         * @param properties The payload data for the event.
         * @param eventProperties The custom event properties to add.
         */
        private fun addCustomEventProperties(
            properties: EventArchPayload,
            eventProperties: Map<String, Any>?
        ) {
            if (eventProperties != null) {
                properties.d?.event?.props?.setAdditionalProperties(eventProperties)
            }
        }

        /**
         * Returns the payload data for the attribute API.
         * @param settings  The settings model containing configuration.
         * @param userId  The ID of the user.
         * @param eventName The name of the event.
         * @param attributeKey  The key of the attribute.
         * @param attributeValue The value of the attribute.
         * @return
         */
        fun getAttributePayloadData(
            settings: Settings,
            userId: String?,
            eventName: String,
            attributeKey: String,
            attributeValue: String
        ): Map<String, Any?> {
            val properties = getEventBasePayload(settings, userId, eventName, null, null)
            properties.d?.event?.props?.setIsCustomEvent(true)
            properties.d?.visitor?.props?.set(attributeKey, attributeValue)
            log(
                LogLevelEnum.DEBUG,
                "IMPRESSION_FOR_SYNC_VISITOR_PROP",
                object : HashMap<String?, String?>() {
                    init {
                        put("eventName", eventName)
                        put("accountId", settings.accountId.toString())
                        put("userId", userId)
                    }
                })
            val payload: Map<String, Any?> =
                VWOClient.objectMapper.convertValue(properties, MutableMap::class.java)
            return removeNullValues(payload)
        }

        /**
         * Sends a POST request to the VWO server.
         * @param properties The properties required for the request.
         * @param payload  The payload data for the request.
         * @param userAgent The user agent of the user.
         * @param ipAddress The IP address of the user.
         */
        fun sendPostApiRequest(
            properties: MutableMap<String, String?>,
            payload: Map<String, Any?>?,
            userAgent: String?,
            ipAddress: String?
        ) {
            try {
                NetworkManager.attachClient()
                val headers = createHeaders(userAgent, ipAddress)
                val request = RequestModel(
                    baseUrl,
                    "POST",
                    UrlEnum.EVENTS.url,
                    properties,
                    payload,
                    headers,
                    SettingsManager.instance!!.protocol,
                    SettingsManager.instance!!.port
                )
                NetworkManager.postAsync(request)
            } catch (exception: Exception) {
                log(
                    LogLevelEnum.ERROR,
                    "NETWORK_CALL_FAILED",
                    object : HashMap<String?, String?>() {
                        init {
                            put("method", "POST")
                            put("err", exception.toString())
                        }
                    })
            }
        }

        /**
         * Removes null values from the map. If the value is a map, recursively removes null values from the nested map.
         * @param originalMap The map containing null/non-null values
         * @return  Map containing non-null values.
         */
        fun removeNullValues(originalMap: Map<String, Any?>): Map<String, Any?> {
            val cleanedMap: MutableMap<String, Any?> = LinkedHashMap()

            for (entry in originalMap.entries) {
                var value = entry.value
                if (value is Map<*, *>) {
                    // Recursively remove null values from nested maps
                    value = removeNullValues(value as Map<String, Any?>)
                }
                if (value != null) {
                    cleanedMap[entry.key] = value
                }
            }

            return cleanedMap
        }

        /**
         * Generates the UUID for the user.
         * @return The UUID for the user.
         */
        private fun generateRandom(): String {
            return Math.random().toString()
        }

        /**
         * Generates the URL for the event.
         * @return The URL for the event.
         */
        private fun generateEventUrl(): String {
            return Constants.HTTPS_PROTOCOL + baseUrl + UrlEnum.EVENTS.url
        }

        /**
         * Generates a message ID for the event. The message ID is a combination of the UUID and the current timestamp.
         * @param uuid The UUID of the user.
         * @return The message ID.
         */
        private fun generateMsgId(uuid: String?): String {
            return uuid + "-" + Calendar.getInstance().timeInMillis
        }

        /**
         * Generates a session ID for the event.
         * @return The session ID.
         */
        private fun generateSessionId(): Long {
            return Calendar.getInstance().timeInMillis / 1000
        }

        /**
         * Creates the headers for the request. Adds the user agent and IP address to the headers if they are not null or empty.
         * @param userAgent The user agent of the user.
         * @param ipAddress The IP address of the user.
         * @return Map containing the headers.
         */
        private fun createHeaders(userAgent: String?, ipAddress: String?): MutableMap<String, String> {
            val headers: MutableMap<String, String> = HashMap()
            if (!userAgent.isNullOrEmpty())
                headers[HeadersEnum.USER_AGENT.header] = userAgent
            if (!ipAddress.isNullOrEmpty())
                headers[HeadersEnum.IP.header] = ipAddress
            return headers
        }
    }
}
