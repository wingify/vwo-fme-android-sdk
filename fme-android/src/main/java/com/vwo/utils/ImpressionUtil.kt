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

import com.vwo.enums.EventEnum
import com.vwo.models.Settings
import com.vwo.models.user.VWOContext
import com.vwo.providers.StorageProvider
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**
 * Utility object for impression-related operations.
 *
 * This object provides helper methods for managing and tracking impressions, such as recording
 * impression events, calculating impression counts, or handling impression-related data.
 */
object ImpressionUtil {
    /**
     * Creates and sends an impression for a variation shown event.
     * This function constructs the necessary properties and payload for the event
     * and uses the NetworkUtil to send a POST API request.
     *
     * @param settings   The settings model containing configuration.
     * @param campaignId The ID of the campaign.
     * @param variationId The ID of the variation shown to the user.
     * @param context    The user context model containing user-specific data.
     */
    fun createAndSendImpressionForVariationShown(
        settings: Settings,
        campaignId: Int,
        variationId: Int,
        context: VWOContext
    ) {
        // Get base properties for the event
        val properties: MutableMap<String, String> = NetworkUtil.getEventsBaseProperties(
            EventEnum.VWO_VARIATION_SHOWN.value,
            encodeURIComponent(StorageProvider.userAgent),
            StorageProvider.ipAddress
        )

        // Construct payload data for tracking the user
        val payload: Map<String, Any> = NetworkUtil.getTrackUserPayloadData(
            settings,
            context,
            context.id,
            EventEnum.VWO_VARIATION_SHOWN.value,
            campaignId,
            variationId,
            StorageProvider.userAgent,
            StorageProvider.ipAddress
        )

        // Send the constructed properties and payload as a POST request
        NetworkUtil.sendPostApiRequest(
            settings,
            properties,
            payload,
            StorageProvider.userAgent,
            StorageProvider.ipAddress
        )
    }

    /**
     * Encodes the query parameters to ensure they are URL-safe
     * @param value The query parameters to encode
     * @return The encoded query parameters
     */
    fun encodeURIComponent(value: String): String {
        try {
            return URLEncoder.encode(value, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }
}
