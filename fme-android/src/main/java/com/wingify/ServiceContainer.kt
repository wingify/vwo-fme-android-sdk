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
package com.wingify

import com.wingify.constants.Constants
import com.wingify.interfaces.networking.HttpMethods
import com.wingify.services.HooksManager
import com.wingify.services.LoggerService
import com.wingify.services.SettingsManager
import com.wingify.models.Settings
import com.wingify.models.user.WingifyInitOptions
import com.wingify.packages.network_layer.manager.BatchManager
import com.wingify.packages.network_layer.manager.OnlineBatchUploadManager
import com.wingify.packages.segmentation_evaluator.core.SegmentationManager
import com.wingify.packages.storage.Storage
import com.wingify.utils.UsageStats

/**
 * ServiceContainer manages all services required for VWO SDK operations.
 * This ensures each VWO instance has its own isolated set of services,
 * preventing conflicts when multiple accounts are used.
 */
class ServiceContainer(
    private var settingsManager: SettingsManager?,
    private val options: WingifyInitOptions,
    private var settings: Settings?,
    private var loggerService: LoggerService?
) {
    private val hooksManager: HooksManager = HooksManager(options.integrations)
    private val segmentationManager = SegmentationManager()
    private val batchManager = BatchManager
    val onlineBatchUploadManager = OnlineBatchUploadManager()
    lateinit var usageStats: UsageStats
    internal var storage: Storage? = null

    init {
        // Set the ServiceContainer reference in SettingsManager for logging
        settingsManager?.serviceContainer = this
    }

    /**
     * Returns the LoggerService class for static method access
     * @return LoggerService class
     */
    fun getLoggerService(): LoggerService? {
        return loggerService
    }

    fun setLoggerService(loggerService: LoggerService) {
        this.loggerService = loggerService
    }

    /**
     * Returns the SettingsManager instance
     * @return SettingsManager instance
     */
    fun getSettingsManager(): SettingsManager? {
        return settingsManager
    }

    /**
     * Sets the SettingsManager instance
     * @param settingsManager SettingsManager instance
     */
    fun setSettingsManager(settingsManager: SettingsManager) {
        this.settingsManager = settingsManager
    }

    /**
     * Returns the HooksManager instance
     * @return HooksManager instance
     */
    fun getHooksManager(): HooksManager {
        return hooksManager
    }

    /**
     * Returns the initialization options for this SDK instance.
     */
    fun getInitOptions(): WingifyInitOptions {
        return options
    }

    /**
     * Returns the BatchManager instance (static for now due to existing architecture)
     * @return BatchManager instance
     */
    internal fun getBatchManager(): BatchManager {
        return batchManager
    }

    /**
     * Returns the SegmentationManager instance
     * @return SegmentationManager instance
     */
    fun getSegmentationManager(): SegmentationManager {
        return segmentationManager
    }

    /**
     * Returns the Settings instance
     * @return Settings instance
     */
    fun getSettings(): Settings? {
        return settings
    }

    /**
     * Resolves the request host for the given HTTP method.
     * Valid gateway config overrides new/legacy hosts. On gateway parse failure, falls back to
     * new endpoints (edge/collect) or legacy hosts. POST requests append [Settings.collectionPrefix]
     * when present for both new and legacy modes.
     */
    fun resolveHost(httpMethod: HttpMethods): String {
        if (options.gatewayService.isNotEmpty() && settingsManager?.isGatewayHostValid == true) {
            return settingsManager?.hostname.orEmpty()
        }
        if (options.isWingifySDKActive) {
            val host = when (httpMethod) {
                HttpMethods.GET -> Constants.EDGE_HOST_NAME
                HttpMethods.POST -> Constants.COLLECT_HOST_NAME
            }
            return appendCollectionPrefixForPost(httpMethod, host)
        }
        return resolveLegacyHost(httpMethod)
    }

    /**
     * Resolves the URL scheme for network requests.
     */
    fun resolveScheme(): String {
        if (options.gatewayService.isNotEmpty() && settingsManager?.isGatewayHostValid == true) {
            return settingsManager?.protocol ?: Constants.HTTPS_PROTOCOL
        }
        return Constants.HTTPS_PROTOCOL
    }

    /**
     * Resolves the host for [Constants.EVENT_BATCH_ENDPOINT] uploads.
     * Gateway is intentionally skipped because the batch endpoint is not served on the gateway.
     * Wingify SDK uses [Constants.COLLECT_HOST_NAME]; legacy VWO SDK uses [Constants.HOST_NAME].
     */
    fun resolveBatchUploadHost(): String {
        if (options.isWingifySDKActive) {
            return appendCollectionPrefixForPost(HttpMethods.POST, Constants.COLLECT_HOST_NAME)
        }
        return appendCollectionPrefixForPost(HttpMethods.POST, Constants.HOST_NAME)
    }

    /**
     * Resolves the URL scheme for batch event uploads (always HTTPS; gateway is not used).
     */
    fun resolveBatchUploadScheme(): String = Constants.HTTPS_PROTOCOL

    private fun resolveLegacyHost(httpMethod: HttpMethods): String {
        val host = settingsManager?.hostname.orEmpty()
        return appendCollectionPrefixForPost(httpMethod, host)
    }

    /**
     * Appends [Settings.collectionPrefix] to POST hosts. The combined value is stored in
     * [RequestModel.url] and passed through [NetworkClient.constructUrl] as the authority segment.
     */
    private fun appendCollectionPrefixForPost(httpMethod: HttpMethods, host: String): String {
        if (httpMethod != HttpMethods.POST) {
            return host
        }
        val collectionPrefix = settings?.collectionPrefix
        if (!collectionPrefix.isNullOrEmpty()) {
            return "$host/$collectionPrefix"
        }
        return host
    }

    /**
     * Returns the base URL for POST API requests (legacy alias for [resolveHost]).
     */
    fun getBaseUrl(): String = resolveHost(HttpMethods.POST)

    fun setSettings(settings: Settings) {
        this.settings = settings
    }

    fun getAccountId(): Int {
        return settingsManager?.accountId ?: options?.accountId ?: 0
    }

    fun getSdkKey(): String {
        return settingsManager?.sdkKey ?: options?.sdkKey ?: ""
    }
}
