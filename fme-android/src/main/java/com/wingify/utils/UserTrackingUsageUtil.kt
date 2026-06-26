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
package com.wingify.utils

import com.wingify.ServiceContainer
import com.wingify.enums.EventEnum
import com.wingify.interfaces.networking.HttpMethods
import com.wingify.enums.UrlEnum
import com.wingify.models.Feature
import com.wingify.models.Settings
import com.vwo.models.user.GetFlag
import com.wingify.models.user.WingifyUserContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.packages.network_layer.manager.BatchManager
import com.wingify.packages.network_layer.manager.NetworkManager
import com.wingify.packages.network_layer.models.RequestModel
import com.wingify.providers.StorageProvider.ipAddress
import com.wingify.providers.StorageProvider.userAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility for tracking user SDK usage.
 *
 * When a [getFlag][com.wingify.api.GetFlagAPI] evaluation completes without a `variationShown`
 * impression, the SDK emits a dedicated [EventEnum.VWO_FE_TRACK_USAGE] event so DaCDN can
 * record the user. If a variationShown event was already sent for the evaluation, user
 * tracking is handled through that path instead and no separate user-tracked event is required.
 * Duplicate tracking for the same user is deduplicated server-side.
 *
 * @see isUsageTrackingEnabled
 * @see sendTrackingUsage
 */
object UserTrackingUsageUtil {

    /**
     * Returns whether user tracking is enabled for this account.
     *
     * @param settings Parsed SDK settings from DaCDN.
     * @return `true` when user tracking is enabled for the account.
     */
    fun isUsageTrackingEnabled(settings: Settings): Boolean = (settings.isUsageTracked == true)

    /**
     * Sends a user-tracked event when user tracking is enabled and no variationShown was
     * dispatched for this evaluation, then returns the flag result.
     *
     * @param settings SDK settings for the account.
     * @param context User context for the evaluation.
     * @param featureKey Feature key being evaluated.
     * @param feature Resolved feature model, or `null` when the feature was not found.
     * @param serviceContainer SDK service container for network and logging.
     * @param variationShownSent `true` when a variationShown impression was already dispatched
     *   for this evaluation; user tracking is handled via that event instead.
     * @param getFlag The [GetFlag] result of the evaluation.
     * @return The provided [getFlag] instance.
     */
    fun evaluateFlagAndTrackUsage(
        settings: Settings,
        context: WingifyUserContext,
        featureKey: String,
        feature: Feature?,
        serviceContainer: ServiceContainer,
        variationShownSent: Boolean,
        getFlag: GetFlag,
    ): GetFlag {
        if (isUsageTrackingEnabled(settings) && !variationShownSent) {
            sendTrackingUsage(
                settings = settings,
                context = context,
                featureKey = featureKey,
                feature = feature,
                serviceContainer = serviceContainer,
            )
        }
        return getFlag
    }

    /**
     * Queues a [EventEnum.VWO_FE_TRACK_USAGE] event for upload to DaCDN.
     *
     * The request is added to the standard event batch pipeline. When batching is disabled,
     * an immediate upload is triggered on a background coroutine.
     *
     * @param settings SDK settings for the account.
     * @param context User context for the evaluation.
     * @param featureKey Feature key being evaluated.
     * @param feature Resolved feature model, or `null` when the feature was not found.
     * @param serviceContainer SDK service container for network, batching, and logging.
     */
    fun sendTrackingUsage(
        settings: Settings,
        context: WingifyUserContext,
        featureKey: String,
        feature: Feature?,
        serviceContainer: ServiceContainer,
    ) {
        val properties = NetworkUtil.getEventsBaseProperties(
            eventName = EventEnum.VWO_FE_TRACK_USAGE.value,
            visitorUserAgent = ImpressionUtil.encodeURIComponent(userAgent),
            ipAddress = ipAddress,
            serviceContainer = serviceContainer,
        )

        val payload = NetworkUtil.getUserTrackingPayloadData(
            settings = settings,
            context = context,
            userId = context.id,
            featureKey = featureKey,
            featureId = feature?.id,
            serviceContainer = serviceContainer,
        )

        val headers = NetworkUtil.createHeaders(userAgent, ipAddress)
        val request = RequestModel(
            serviceContainer.resolveHost(HttpMethods.POST),
            HttpMethods.POST.value,
            UrlEnum.EVENTS.url,
            properties,
            payload,
            headers,
            serviceContainer.resolveScheme(),
            serviceContainer.getSettingsManager()!!.port,
        )

        request.campaignInfo = mapOf(
            "featureName" to (feature?.name ?: ""),
            "featureKey" to featureKey,
        )

        NetworkManager.addToBatch(request, serviceContainer)

        serviceContainer.getLoggerService()?.log(
            LogLevelEnum.DEBUG,
            "USER_TRACKED",
            mapOf(
                "accountId" to settings.accountId.toString(),
                "userId" to context.id,
                "featureKey" to featureKey,
            ),
        )

        if (serviceContainer.onlineBatchUploadManager.isBatchingDisabled()) {
            CoroutineScope(Dispatchers.IO).launch {
                BatchManager.start("User Tracking Util", serviceContainer)
            }
        }
    }
}
