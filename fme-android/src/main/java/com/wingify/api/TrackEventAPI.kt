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
package com.wingify.api

import com.wingify.ServiceContainer
import com.wingify.constants.Constants
import com.wingify.enums.ApiEnum
import com.wingify.models.Settings
import com.wingify.models.user.WingifyUserContext
import com.wingify.providers.StorageProvider
import com.wingify.services.HooksManager
import com.wingify.services.LoggerService
import com.wingify.utils.FunctionUtil.doesEventBelongToAnyFeature
import com.wingify.utils.FunctionUtil.doesEventBelongToAnyHoldout
import com.wingify.utils.FunctionUtil.getFormattedErrorMessage
import com.wingify.utils.ImpressionUtil.encodeURIComponent
import com.wingify.utils.NetworkUtil

object TrackEventAPI {
    /**
     * This method is used to track an event for the user.
     * @param settings The settings model containing configuration.
     * @param eventName The name of the event to track.
     * @param context The user context model containing user-specific data.
     * @param eventProperties event properties for the event
     * @param hooksManager The hooks manager instance.
     * @return Boolean indicating if the event was successfully tracked.
     */
    fun track(
        settings: Settings,
        eventName: String,
        context: WingifyUserContext,
        eventProperties: Map<String, Any>,
        hooksManager: HooksManager,
        serviceContainer: ServiceContainer
    ): Boolean {
        try {
            if (doesEventBelongToAnyFeature(eventName, settings) || doesEventBelongToAnyHoldout(eventName, settings)) {
                createAndSendImpressionForTrack(settings, eventName, context, eventProperties,serviceContainer)
                val objectToReturn: MutableMap<String, Any> = HashMap()
                objectToReturn["eventName"] = eventName
                objectToReturn["api"] = ApiEnum.TRACK_EVENT.value
                hooksManager.set(objectToReturn)
                hooksManager.execute(hooksManager.get())
                return true
            } else {
                // Log an error if the event does not exist
                LoggerService.errorLog(
                    key = "EVENT_NOT_FOUND",
                    data = mapOf("eventName" to eventName),
                    debugData = mapOf(
                        "an" to ApiEnum.TRACK_EVENT.value,
                        "uuid" to context.getUuid(serviceContainer),
                        "sId" to context.sessionId
                    ),
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                return false
            }
        } catch (e: Exception) {
            LoggerService.errorLog(
                key = "EXECUTION_FAILED",
                data = mapOf(
                    "apiName" to ApiEnum.TRACK_EVENT.value,
                    Constants.ERR to getFormattedErrorMessage(e)
                ),
                debugData = mapOf(
                    "an" to ApiEnum.TRACK_EVENT.value,
                    "uuid" to context.getUuid(serviceContainer),
                    "eventName" to eventName,
                    "sId" to context.sessionId
                ),
                shouldSendToVWO = true,
                serviceContainer = serviceContainer
            )
            return false
        }
    }

    /**
     * Creates and sends an impression for a track event.
     * This function constructs the necessary properties and payload for the event
     * and uses the NetworkUtil to send a POST API request.
     *
     * @param settings   The settings model containing configuration.
     * @param eventName  The name of the event to track.
     * @param context    The user context model containing user-specific data.
     * @param eventProperties event properties for the event
     */
    fun createAndSendImpressionForTrack(
        settings: Settings,
        eventName: String,
        context: WingifyUserContext,
        eventProperties: Map<String, Any>,
        serviceContainer: ServiceContainer
    ) {
        // Get base properties for the event
        val properties = NetworkUtil.getEventsBaseProperties(
            eventName,
            encodeURIComponent(StorageProvider.userAgent),
            StorageProvider.ipAddress,
            serviceContainer
        )

        // Construct payload data for tracking the user
        val payload = NetworkUtil.getTrackGoalPayloadData(
            settings,
            context.id,
            eventName,
            context,
            eventProperties,
            serviceContainer
        )

        // Send the constructed properties and payload as a POST request
        NetworkUtil.sendPostApiRequest(
            properties = properties,
            payload = payload,
            userAgent = StorageProvider.userAgent,
            ipAddress = StorageProvider.ipAddress,
            serviceContainer = serviceContainer
        )
    }
}
