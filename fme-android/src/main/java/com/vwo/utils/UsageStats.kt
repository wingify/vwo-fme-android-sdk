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

import android.content.Context
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.logger.core.LogManager
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.storage.MobileDefaultStorage
import com.vwo.providers.StorageProvider
import com.vwo.services.LoggerService
import com.vwo.services.SettingsManager
import java.lang.ref.WeakReference

private const val KEY_INTEGRATIONS = "ig"
private const val KEY_EVENT_BATCHING = "eb"
private const val KEY_ONLINE_BATCHING_SIZE = "obs"
private const val KEY_ONLINE_BATCHING_TIME = "obt"
private const val KEY_OFFLINE_BATCHING = "ebo"
private const val KEY_CUSTOM_LOGGER = "cl"
private const val KEY_LOG_LEVEL = "ll"
private const val KEY_STORAGE_SERVICE = "ss"
private const val KEY_GATEWAY_SERVICE = "gs"
private const val KEY_POLL_INTERVAL = "pi"
private const val KEY_APP_VERSION = "av" //Available if application's context is provided
private const val KEY_OS_VERSION = "osv"
private const val KEY_PLATFORM = "p"
private const val KEY_LANGUAGE_VERSION = "lv"
private const val KEY_CACHED_SETTINGS_EXPIRY = "cse"
private const val KEY_ACCOUNT_ID = "a"
private const val KEY_ENVIRONMENT = "env"

object UsageStats {

    private val stats = mutableMapOf<String, Any>()
    private val context: WeakReference<Context> = StorageProvider.contextRef

    private val featureEnabledValue = 1
    private val featureDisabledValue = 0
    private lateinit var initOptions: VWOInitOptions

    private fun collectStats() {

        stats[KEY_ACCOUNT_ID] = SettingsManager.instance?.accountId ?: 0
        stats[KEY_ENVIRONMENT] = SettingsManager.instance?.sdkKey ?: ""

        // Set integration flag if present
        if (initOptions.integrations != null) {
            stats[KEY_INTEGRATIONS] = featureEnabledValue
        }

        // Set event batching flags if online batching is enabled
        if (initOptions.batchMinSize > 0 || initOptions.batchUploadTimeInterval > 0) {
            stats[KEY_EVENT_BATCHING] = featureEnabledValue
            if (initOptions.batchMinSize > 0) {
                stats[KEY_ONLINE_BATCHING_SIZE] = featureEnabledValue
            }
            if (initOptions.batchUploadTimeInterval > 0) {
                stats[KEY_ONLINE_BATCHING_TIME] = featureEnabledValue
            }
        }
        if (context.get() != null) {
            stats[KEY_OFFLINE_BATCHING] = featureEnabledValue
        }

        // Set custom logger flag if present
        if (initOptions.logger.isNotEmpty()) {
            // Set log transport flag if logger is configured
            if (initOptions.logger.containsKey("transports")) {
                stats[KEY_CUSTOM_LOGGER] = featureEnabledValue
            }
        }

        // Set log level if present
        LogManager.instance?.level?.position?.let { stats[KEY_LOG_LEVEL] = it }

        // Set storage service flag
        if (initOptions.storage !is MobileDefaultStorage) // Client has provided their own storage service
            stats[KEY_STORAGE_SERVICE] = featureEnabledValue
        else if (context.get() != null) // MobileDefaultStorage is used in this case
            stats[KEY_STORAGE_SERVICE] = featureEnabledValue

        // Set polling interval if present
        initOptions.pollInterval?.let {
            stats[KEY_POLL_INTERVAL] = it
        }

        // Set Gateway service if present
        if (initOptions.gatewayService.containsKey("url")) {
            stats[KEY_GATEWAY_SERVICE] = featureEnabledValue
        }

        // App version
        context.get()?.let { stats[KEY_APP_VERSION] = SDKInfoUtils.getAppVersion(it) }

        // OS info
        stats[KEY_OS_VERSION] = SDKInfoUtils.getOSVersion()
        stats[KEY_PLATFORM] = SDKInfoUtils.getPlatform()

        // Set cache settings expiry if present
        if (initOptions.cachedSettingsExpiryTime > 0) {
            stats[KEY_CACHED_SETTINGS_EXPIRY] = featureEnabledValue
        }

        // If it is native sdk, gather and send language version,
        // otherwise it will be sent in _vwo_meta by react/Flutter sdks
        if(SDKMetaUtil.sdkName.contains("android", ignoreCase = true))
            stats[KEY_LANGUAGE_VERSION] = SDKInfoUtils.getLanguageVersion()

        // Add meta keys from init options
        initOptions._vwo_meta.forEach { (key, value) ->
            if (value == featureDisabledValue)
                stats.remove(key)
            else
                stats[key] = value.toString()
        }
    }
    
    fun collectStats(initOptions: VWOInitOptions): Map<String, Any> {
        this@UsageStats.initOptions = initOptions

        if (!initOptions.isUsageStatsDisabled) {
            collectStats()
        }
        LoggerService.log(LogLevelEnum.INFO, "Usage stats: $stats")
        return stats.toMap()
    }

    fun getStats(): MutableMap<String, Any> {
        return stats
    }

    /**
     * Clears the usage statistics data.
     */
    fun clearUsageStats() {
        stats.clear()
    }
}