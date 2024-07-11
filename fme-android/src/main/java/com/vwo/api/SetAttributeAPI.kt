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

import com.vwo.enums.EventEnum
import com.vwo.models.Settings
import com.vwo.models.user.VWOContext
import com.vwo.utils.NetworkUtil

object SetAttributeAPI {
    /**
     * This method is used to set an attribute for the user.
     * @param settings The settings model containing configuration.
     * @param attributeKey The key of the attribute to set.
     * @param attributeValue The value of the attribute to set.
     * @param context  The user context model containing user-specific data.
     */
    @JvmStatic
    fun setAttribute(
        settings: Settings,
        attributeKey: String,
        attributeValue: String,
        context: VWOContext
    ) {
        createAndSendImpressionForSetAttribute(settings, attributeKey, attributeValue, context)
    }

    /**
     * Creates and sends an impression for a track event.
     * This function constructs the necessary properties and payload for the event
     * and uses the NetworkUtil to send a POST API request.
     *
     * @param settings   The settings model containing configuration.
     * @param attributeKey  The key of the attribute to set.
     * @param attributeValue  The value of the attribute to set.
     * @param context    The user context model containing user-specific data.
     */
    private fun createAndSendImpressionForSetAttribute(
        settings: Settings,
        attributeKey: String,
        attributeValue: String,
        context: VWOContext
    ) {
        // Get base properties for the event
        val properties = NetworkUtil.getEventsBaseProperties(
            settings,
            EventEnum.VWO_SYNC_VISITOR_PROP.value,
            encodeURIComponent(context.userAgent),
            context.ipAddress
        )

        // Construct payload data for tracking the user
        val payload = NetworkUtil.getAttributePayloadData(
            settings,
            context.id,
            EventEnum.VWO_SYNC_VISITOR_PROP.value,
            attributeKey,
            attributeValue
        )

        // Send the constructed properties and payload as a POST request
        NetworkUtil.sendPostApiRequest(properties, payload, context.userAgent, context.ipAddress)
    }
}
