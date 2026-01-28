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
package com.vwo

import com.vwo.services.HooksManager
import com.vwo.services.LoggerService
import com.vwo.services.SettingsManager
import com.vwo.models.Settings
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.network_layer.manager.BatchManager
import com.vwo.packages.network_layer.manager.OnlineBatchUploadManager
import com.vwo.packages.segmentation_evaluator.core.SegmentationManager
import com.vwo.packages.storage.Storage
import com.vwo.utils.UsageStats

/**
 * ServiceContainer manages all services required for VWO SDK operations.
 * This ensures each VWO instance has its own isolated set of services,
 * preventing conflicts when multiple accounts are used.
 */
class ServiceContainer(
    private var settingsManager: SettingsManager?,
    private val options: VWOInitOptions,
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
     * Returns the VWOInitOptions instance
     * @return VWOInitOptions instance
     */
    fun getVWOInitOptions(): VWOInitOptions {
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
     * Returns the base URL for the API requests
     */
    fun getBaseUrl(): String {
        val baseUrl = this.settingsManager?.hostname?:""

        // Check if gateway service is provided
        val isGatewayServiceProvided = this.options.gatewayService.isNotEmpty()
        if (isGatewayServiceProvided) {
            return baseUrl
        }

        val collectionPrefix = this.settings?.collectionPrefix
        if (!collectionPrefix.isNullOrEmpty()) {
            return "$baseUrl/$collectionPrefix"
        }

        return baseUrl
    }

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
