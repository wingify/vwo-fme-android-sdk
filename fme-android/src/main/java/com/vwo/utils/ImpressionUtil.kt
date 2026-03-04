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

import com.vwo.ServiceContainer
import com.vwo.enums.EventEnum
import com.vwo.enums.UrlEnum
import com.vwo.models.Settings
import com.vwo.models.impression.ImpressionPayload
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.network_layer.manager.BatchManager
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.providers.StorageProvider.ipAddress
import com.vwo.providers.StorageProvider.userAgent
import com.vwo.utils.CampaignUtil.getCampaignKeyFromCampaignId
import com.vwo.utils.CampaignUtil.getCampaignTypeFromCampaignId
import com.vwo.utils.CampaignUtil.getVariationNameFromCampaignIdAndVariationId
import com.vwo.utils.NetworkUtil.Companion.createHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
     * Creates and sends impressions for multiple variation shown events.
     * This is the main implementation that handles multiple campaigns and variations
     * in a single batch operation. It constructs the necessary properties and payload
     * for each event and adds them to the batch queue for processing.
     *
     * @param settings   The settings model containing configuration.
     * @param campaignIds The list of campaign IDs.
     * @param variationIds The list of variation IDs shown to the user.
     * @param context    The user context model containing user-specific data.
     */
    fun createAndSendImpressionForVariationShown(
        settings: Settings,
        impressionPayload: ImpressionPayload,
        context: VWOUserContext,
        serviceContainer: ServiceContainer
    ) {

        if (impressionPayload.hasNoValidData()) return

        // Get base properties for the event
        val properties: MutableMap<String, String> = NetworkUtil.getEventsBaseProperties(
            eventName = EventEnum.VWO_VARIATION_SHOWN.value,
            visitorUserAgent = encodeURIComponent(value = userAgent),
            ipAddress = ipAddress,
            serviceContainer = serviceContainer
        )

        // Construct payload data for tracking the user
        for (index in 0 until impressionPayload.size()) {
            val impression = impressionPayload.get(index)

            val campaignId = impression.campaignId
            val variationId = impression.variationId
            val featureId = impression.featureId

            val payload = NetworkUtil.getTrackUserPayloadData(
                settings = settings,
                context = context,
                userId = context.id,
                eventName = EventEnum.VWO_VARIATION_SHOWN.value,
                campaignId = campaignId,
                variationId = variationId,
                visitorUserAgent = userAgent,
                ipAddress = ipAddress,
                featureId = featureId,
                serviceContainer = serviceContainer
            )

            val headers = createHeaders(userAgent, ipAddress)
            val request = RequestModel(
                serviceContainer.getBaseUrl(),
                "POST",
                UrlEnum.EVENTS.url,
                properties,
                payload,
                headers,
                serviceContainer.getSettingsManager()!!.protocol,
                serviceContainer.getSettingsManager()!!.port,
            )

            val campaignKeyWithFeatureName = getCampaignKeyFromCampaignId(settings, campaignId)
            val variationName =
                getVariationNameFromCampaignIdAndVariationId(settings, campaignId, variationId)
            val featureName = campaignKeyWithFeatureName?.split('_')?.getOrNull(0)
            val campaignKey = campaignKeyWithFeatureName?.split('_')?.getOrNull(1)
            val campaignType = getCampaignTypeFromCampaignId(settings, campaignId)

            request.campaignInfo = mapOf<String, Any>(
                "campaignKey" to (campaignKey ?: ""),
                "variationName" to (variationName ?: ""),
                "featureName" to (featureName ?: ""),
                "campaignType" to (campaignType ?: "")
            )
            NetworkManager.addToBatch(request, serviceContainer)
        }

        if (serviceContainer.onlineBatchUploadManager.isBatchingDisabled()) {
            // if batching is disabled then send all data immediately
            CoroutineScope(Dispatchers.IO).launch {
                BatchManager.start("Send GetFlag Impression", serviceContainer)
            }
        }
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
