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
package com.vwo.api

import com.vwo.enums.ApiEnum
import com.vwo.models.Settings
import com.vwo.models.user.VWOContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.HooksManager
import com.vwo.services.LoggerService.Companion.log
import com.vwo.utils.FunctionUtil.doesEventBelongToAnyFeature
import com.vwo.utils.ImpressionUtil.encodeURIComponent
import com.vwo.utils.NetworkUtil

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
        context: VWOContext,
        eventProperties: Map<String, Any>,
        hooksManager: HooksManager
    ): Boolean {
        try {
            if (doesEventBelongToAnyFeature(eventName, settings)) {
                createAndSendImpressionForTrack(settings, eventName, context, eventProperties)
                val objectToReturn: MutableMap<String, Any> = HashMap()
                objectToReturn["eventName"] = eventName
                objectToReturn["api"] = ApiEnum.TRACK.value
                hooksManager.set(objectToReturn)
                hooksManager.execute(hooksManager.get())
                return true
            } else {
                log(LogLevelEnum.ERROR, "EVENT_NOT_FOUND", object : HashMap<String?, String?>() {
                    init {
                        put("eventName", eventName)
                    }
                })
                return false
            }
        } catch (e: Exception) {
            log(LogLevelEnum.ERROR, "Error in tracking event: $eventName Error: $e")
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
    private fun createAndSendImpressionForTrack(
        settings: Settings,
        eventName: String,
        context: VWOContext,
        eventProperties: Map<String, Any>
    ) {
        // Get base properties for the event
        val properties = NetworkUtil.getEventsBaseProperties(
            settings,
            eventName,
            encodeURIComponent(context.userAgent),
            context.ipAddress
        )

        // Construct payload data for tracking the user
        val payload = NetworkUtil.getTrackGoalPayloadData(
            settings,
            context.id,
            eventName,
            context,
            eventProperties
        )

        // Send the constructed properties and payload as a POST request
        NetworkUtil.sendPostApiRequest(properties, payload, context.userAgent, context.ipAddress)
    }
}