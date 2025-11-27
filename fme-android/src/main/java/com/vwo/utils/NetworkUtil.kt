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
package com.vwo.utils

import com.vwo.VWOClient
import com.vwo.constants.Constants
import com.vwo.constants.Constants.PRODUCT_NAME
import com.vwo.constants.Constants.VWO_FS_ENVIRONMENT
import com.vwo.constants.Constants.defaultString
import com.vwo.enums.EventEnum
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
import com.vwo.models.user.FMEConfig
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.providers.StorageProvider
import com.vwo.services.LoggerService.Companion.log
import com.vwo.services.SettingsManager
import com.vwo.services.UrlService.baseUrl
import java.util.Calendar

/**
 * Provides network-related utility functions.
 *
 * This class offers helper methods for performing network operations, such as making API requests,
 * handling network responses, and managing network connectivity.
 */
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

        return settingsQueryParams.getQueryParams()
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
            eventName: String,
            visitorUserAgent: String?,
            ipAddress: String?,
            isUsageStatsEvent: Boolean = false,
            usageStatsAccountId: Int = 0,
        ): MutableMap<String, String> {

            val requestQueryParams = RequestQueryParams(
                eventName,
                SettingsManager.instance?.accountId.toString(),
                SettingsManager.instance?.sdkKey ?: "",
                visitorUserAgent,
                ipAddress,
                generateEventUrl()
            )
            if (isUsageStatsEvent) {
                requestQueryParams.a = usageStatsAccountId.toString()
            } else {
                requestQueryParams.env = SettingsManager.instance?.sdkKey
            }

            requestQueryParams.queryParams["sn"] = SDKMetaUtil.sdkName
            requestQueryParams.queryParams["sv"] = SDKMetaUtil.sdkVersion
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
            settings: Settings?,
            context: VWOUserContext?,
            userId: String?,
            eventName: String,
            visitorUserAgent: String?,
            ipAddress: String?,
            isUsageStatsEvent: Boolean = false,
            usageStatsAccountId: Int = 0,
            shouldGenerateUUID: Boolean = true,
        ): EventArchPayload {

            val accountId = if (isUsageStatsEvent) {
                usageStatsAccountId
            } else {
                SettingsManager.instance?.accountId
            }

            val uuid = if (shouldGenerateUUID) {
                UUIDUtils.getUUID(userId, accountId.toString())
            } else {
                userId.toString()
            }
            val eventArchData = EventArchData()
            eventArchData.msgId = generateMsgId(uuid)
            eventArchData.visId = uuid
            eventArchData.sessionId = context?.sessionId
            setOptionalVisitorData(eventArchData, visitorUserAgent, ipAddress)

            val event = createEvent(eventName, isUsageStatsEvent)
            eventArchData.event = event
            val visitor = if (isUsageStatsEvent) {
                null
            } else {
                createVisitor(isUsageStatsEvent)
            }
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
        private fun createEvent(eventName: String, isUsageStatsEvent: Boolean = false): Event {
            val event = Event()
            val props = createProps(isUsageStatsEvent)
            event.props = props
            event.name = eventName
            event.time = System.currentTimeMillis()
            return event
        }

        /**
         * Creates the visitor model for the event arch APIs.
         * @param settings The settings model containing configuration.
         * @return The visitor model.
         */
        private fun createProps(isUsageStatsEvent: Boolean = false): Props {
            val props = Props()
            props.setSdkName(SDKMetaUtil.sdkName)
            props.setSdkVersion(SDKMetaUtil.sdkVersion)
            if (!isUsageStatsEvent) {
                props.setEnvKey(SettingsManager.instance?.sdkKey)
            }
            return props
        }

        /**
         * Creates the visitor model for the event arch APIs.
         * @param settings The settings model containing configuration.
         * @return The visitor model.
         */
        private fun createVisitor(isUsageStatsEvent: Boolean = false): Visitor {
            val visitor = Visitor()
            val visitorProps: MutableMap<String, Any> = HashMap()
            if (!isUsageStatsEvent) {
                visitorProps[VWO_FS_ENVIRONMENT] = SettingsManager.instance?.sdkKey ?: defaultString
            }
            visitor.setProps(visitorProps)
            return visitor
        }

        /**
         * Adds custom variables to visitor props based on postSegmentationVariables.
         * @param properties The payload data for the event.
         * @param context The user context containing customVariables and postSegmentationVariables.
         */
        private fun addCustomVariablesToVisitorProps(
            properties: EventArchPayload,
            context: VWOUserContext?
        ) {
            // A temporary map to hold all custom variables and device info to be added.
            val variablesToAdd = mutableMapOf<String, Any>()

            // Check if the context has both post-segmentation keys and custom variables.
            if (context?.postSegmentationVariables != null && context.customVariables.isNotEmpty()) {
                // Iterate through the keys specified for post-segmentation.
                for (key in context.postSegmentationVariables!!) {
                    // If a post-segmentation key exists in the custom variables map,
                    // add it to our temporary map.
                    if (context.customVariables.containsKey(key)) {
                        variablesToAdd[key] = context.customVariables[key]!!
                    }
                }
            }

            // Get the Android application context from the singleton StorageProvider.
            // Use .let to safely execute the block only if the context reference is not null.
            StorageProvider.contextRef.get()?.let {
                // Retrieve all device details using the application context.
                val deviceInfo = DeviceInfo().getAllDeviceDetails(it)
                // Add all gathered device info to our temporary map.
                variablesToAdd.putAll(deviceInfo)
            }

            // Check if there are any variables to add to prevent unnecessary operations.
            if (variablesToAdd.isNotEmpty()) {
                val existingProps = properties.d?.visitor?.props ?: mutableMapOf()

                // Merge the new variables (custom variables + device info) into the existing properties.
                existingProps.putAll(variablesToAdd)

                // Set the updated properties map back into the event payload's visitor object.
                properties.d?.visitor?.setProps(existingProps)
            }
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
            context: VWOUserContext,
            userId: String?,
            eventName: String,
            campaignId: Int,
            variationId: Int,
            visitorUserAgent: String?,
            ipAddress: String?
        ): Map<String, Any> {
            val properties =
                getEventBasePayload(
                    settings,
                    context,
                    userId,
                    eventName,
                    visitorUserAgent,
                    ipAddress
                )
            properties.d!!.event!!.props!!.id = campaignId
            properties.d!!.event!!.props!!.variation = variationId.toString()
            properties.d!!.event!!.props!!.setFirst(1)

            if (eventName == EventEnum.VWO_VARIATION_SHOWN.value) {
                properties.d?.event?.props?.setIsMII(FMEConfig.isMISdkLinked)
                // Add custom variables to visitor props for VWO_VARIATION_SHOWN events
                addCustomVariablesToVisitorProps(properties, context)
            }

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
            val payload: Map<*, *> =
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
            context: VWOUserContext,
            eventProperties: Map<String, Any>
        ): Map<String, Any?> {
            val properties = getEventBasePayload(
                settings,
                context,
                userId,
                eventName,
                StorageProvider.userAgent,
                StorageProvider.ipAddress
            )
            properties.d?.event?.props?.setCustomEvent(true)
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
            val payload: Map<*, *> =
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
         * @param attributeMap - Map of attribute key and value to be set
         * @return
         */
        fun getAttributePayloadData(
            settings: Settings,
            context: VWOUserContext,
            userId: String?,
            eventName: String,
            attributeMap: Map<String, Any>
        ): Map<String, Any> {
            val properties = getEventBasePayload(settings, context, userId, eventName, null, null)
            properties.d?.event?.props?.setCustomEvent(true)
            properties.d?.visitor?.props?.putAll(attributeMap)
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
            val payload: Map<*, *> =
                VWOClient.objectMapper.convertValue(properties, MutableMap::class.java)
            return removeNullValues(payload)
        }

        /**
         * Returns the payload data for the messaging event.
         * @param messageType The type of the message.
         * @param message The content of the message.
         * @param eventName The name of the event.
         * @return
         */
        fun getMessagingEventPayload(
            messageType: String,
            message: String,
            eventName: String
        ): Map<String, Any> {
            val settingsManager = SettingsManager.instance
            val userId = settingsManager?.accountId.toString() + "_" + settingsManager?.sdkKey
            val properties = getEventBasePayload(null, null, userId, eventName, null, null)
            properties.d?.event?.props?.setProduct(PRODUCT_NAME)
            val data: MutableMap<String, Any> = HashMap()
            data["type"] = messageType

            val messageContent: MutableMap<String, Any> = HashMap()
            messageContent["title"] = message
            messageContent["dateTime"] = System.currentTimeMillis()

            data["content"] = messageContent
            properties.d!!.event!!.props!!.setData(data)

            val payload: Map<*, *> = VWOClient.objectMapper.convertValue(
                properties,
                MutableMap::class.java
            )
            return removeNullValues(payload)
        }

        /**
         * Returns the payload data for the SDK init event.
         * @param eventName The name of the event.
         * @param settingsFetchTime Time taken to fetch settings in milliseconds.
         * @param sdkInitTime Time taken to initialize the SDK in milliseconds.
         * @return Map containing the payload data.
         */
        fun getSDKInitEventPayload(
            eventName: String,
            settingsFetchTime: Long? = null,
            sdkInitTime: Long? = null
        ): Map<String, Any> {
            val settingsManager = SettingsManager.instance
            val accountId = settingsManager?.accountId
            val sdkKey = settingsManager?.sdkKey
            if (accountId == null || sdkKey == null)
                return emptyMap()

            val uniqueKey = accountId.toString() + "_" + sdkKey
            val properties = getEventBasePayload(null, null, uniqueKey, eventName, null, null)

            // Set the required fields as specified
            properties.d?.event?.props?.let { props ->

                val map = mapOf(VWO_FS_ENVIRONMENT to sdkKey)
                props.setAdditionalProperties(map)
                props.setProduct(PRODUCT_NAME)

                val data = mutableMapOf<String, Any>(
                    "isSDKInitialized" to true
                )
                settingsFetchTime?.let { data["settingsFetchTime"] = it }
                sdkInitTime?.let { data["sdkInitTime"] = it }

                props.setData(data)
            }

            val payload: Map<*, *> = VWOClient.objectMapper.convertValue(
                properties,
                MutableMap::class.java
            )
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
            settings: Settings,
            properties: MutableMap<String, String>,
            payload: Map<String, Any?>?,
            userAgent: String?,
            ipAddress: String?,
            eventProperties: Map<String, Any?> = emptyMap(),
            campaignInfo: Map<String, Any> = emptyMap()
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
                request.campaignInfo = campaignInfo
                NetworkManager.postAsync(request)
                if (UsageStats.getStats().isNotEmpty())
                    UsageStats.clearUsageStats()
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

        fun sendGatewayEvent(
            queryParams: MutableMap<String, String>?,
            payload: Map<String, Any?>?,
            eventName: String
        ) {
            try {
                NetworkManager.attachClient()
                val headers = createHeaders(null, null)
                val request = RequestModel(
                    baseUrl,
                    "POST",
                    UrlEnum.EVENTS.url,
                    queryParams,
                    payload,
                    headers,
                    SettingsManager.instance!!.protocol,
                    SettingsManager.instance!!.port
                )
                request.eventName = eventName
                NetworkManager.postAsync(request)
            } catch (exception: Exception) {
                // Silently catch any exceptions to prevent disrupting normal flow
                /*log(
                    LogLevelEnum.ERROR,
                    "NETWORK_CALL_FAILED",
                    object : HashMap<String?, String?>() {
                        init {
                            put("method", "POST")
                            put("err", exception.toString())
                        }
                    })*/
            }
        }

        fun sendMessagingEvent(
            properties: MutableMap<String, String>?,
            payload: Map<String, Any?>?,
            eventName: String
        ) {
            try {
                NetworkManager.attachClient()
                val headers = createHeaders(null, null)
                val request = RequestModel(
                    Constants.HOST_NAME,
                    "POST",
                    UrlEnum.EVENTS.url,
                    properties,
                    payload,
                    headers,
                    Constants.HTTPS_PROTOCOL,
                    0
                )
                request.eventName = eventName
                NetworkManager.postAsync(request)
            } catch (exception: Exception) {
                log(
                    LogLevelEnum.DEBUG,
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
         * Constructs the payload for an SDK usage statistics event.
         *
         * This function generates a map representing the data payload that will be sent
         * to track SDK usage. It incorporates essential information such as the
         * event type, account identifiers, and collected usage statistics.
         *
         * @param event The type of SDK usage event being tracked.
         *              This is an enum value from `EventEnum`.
         * @param usageStatsAccountId The account ID specifically designated for tracking usage statistics.
         *                            This might be different from the main VWO account ID.
         * @return A map containing the non-null key-value pairs representing the payload
         *         for the SDK usage statistics event. This map is ready to be serialized
         *         (e.g., to JSON) and sent to the server.
         */
        fun getSDKUsageStatsEventPayload(event: EventEnum, usageStatsAccountId: Int): Map<String, Any> {
            val settingsManager = SettingsManager.instance
            val accountId = settingsManager?.accountId
            val sdkKey = settingsManager?.sdkKey
            val userId = "${accountId}_$sdkKey"

            val properties = getEventBasePayload(
                null,
                null,
                userId,
                event.value,
                null,
                null,
                true,
                usageStatsAccountId
            )

            // Set the required fields
            properties.d?.event?.props?.setProduct(PRODUCT_NAME)

            val usageStats = UsageStats.getStats()
            if (usageStats.isNotEmpty()) {
                properties.d?.event?.props?.setVwoMeta(usageStats)
            }
            val payload: Map<*, *> = VWOClient.objectMapper.convertValue(
                properties,
                MutableMap::class.java
            )
            return removeNullValues(payload)
        }

        /**
         * Removes null values from the map. If the value is a map, recursively removes null values from the nested map.
         * @param originalMap The map containing null/non-null values
         * @return  Map containing non-null values.
         */
        fun removeNullValues(originalMap: Map<*, *>): Map<String, Any> {
            val cleanedMap: MutableMap<String, Any> = mutableMapOf()

            for (entry in originalMap.entries) {
                var value = entry.value
                if (value is Map<*, *>) {
                    // Recursively remove null values from nested maps
                    value = removeNullValues(value)
                }
                if (value != null && entry.key is String) {
                    cleanedMap[entry.key as String] = value
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
         * Returns the payload data for the debugger event.
         * @param eventProps The properties for the debugger event.
         * @return Map containing the payload data.
         */
        fun getDebuggerEventPayload(eventProps: Map<String, Any> = emptyMap()): Map<String, Any> {

            // Compute UUID like Node: if absent, generate using accountId and sdkKey; else use provided
            val accountId = SettingsManager.instance?.accountId
            val sdkKey = SettingsManager.instance?.sdkKey
            val computedUuid = if (!eventProps.containsKey("uuid")) {
                val userKey = "${accountId}_${sdkKey}"
                UUIDUtils.getUUID(userKey, accountId.toString())
            } else {
                eventProps["uuid"].toString()
            }

            val properties = getEventBasePayload(
                null,
                null,
                computedUuid,
                EventEnum.VWO_DEBUGGER_EVENT.value,
                null,
                null,
                false,
                0,
                false
            )

            // Ensure visId explicitly matches computed UUID
            properties.d?.visId = computedUuid

            // Set debugger event specific properties
            properties.d?.event?.props = Props()

            // Set session ID if provided, else generate one
            val sessionId = (eventProps["sId"] as? Long) ?: FMEConfig.generateSessionId()
            properties.d?.sessionId = sessionId
            properties.d?.event?.props?.let { props ->
                val envKey = SettingsManager.instance?.sdkKey?:""
                val map = mapOf(VWO_FS_ENVIRONMENT to envKey)
                props.setAdditionalProperties(map)
                props.setEnvKey(envKey)
            }

            // Add all event properties to the payload
            val vwoMeta: MutableMap<String, Any> = mutableMapOf()
            vwoMeta.putAll(eventProps)

            // Ensure uuid is present in vwoMeta similar to Node implementation
            properties.d?.visId?.let { visId ->
                vwoMeta["uuid"] = visId
            }
            // Mirror sessionId into vwoMeta if not provided in eventProps
            if (!eventProps.containsKey("sId")) {
                properties.d?.sessionId?.let { sId -> vwoMeta["sId"] = sId }
            }

            // Static/meta fields
            vwoMeta["a"] = SettingsManager.instance?.accountId ?: ""
            vwoMeta["product"] = Constants.PRODUCT_NAME
            vwoMeta["sn"] = SDKMetaUtil.sdkName
            vwoMeta["sv"] = SDKMetaUtil.sdkVersion
            vwoMeta["pt"] = Constants.PLATFORM
            // Add eventId generated from sdkKey
            vwoMeta["eventId"] = UUIDUtils.getRandomUUID(SettingsManager.instance?.sdkKey ?: "")

            properties.d?.event?.props?.setVwoMeta(vwoMeta)

            val payload: Map<*, *> = VWOClient.objectMapper.convertValue(
                properties,
                MutableMap::class.java
            )
            return removeNullValues(payload)
        }

        /**
         * Creates the headers for the request. Adds the user agent and IP address to the headers if they are not null or empty.
         * @param userAgent The user agent of the user.
         * @param ipAddress The IP address of the user.
         * @return Map containing the headers.
         */
        fun createHeaders(
            userAgent: String?,
            ipAddress: String?
        ): MutableMap<String, String> {
            val headers: MutableMap<String, String> = HashMap()
            if (!userAgent.isNullOrEmpty())
                headers[HeadersEnum.USER_AGENT.header] = userAgent
            if (!ipAddress.isNullOrEmpty())
                headers[HeadersEnum.IP.header] = ipAddress
            return headers
        }
    }

}
