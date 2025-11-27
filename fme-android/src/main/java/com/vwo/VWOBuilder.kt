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

import SdkDataManager
import com.vwo.constants.Constants
import com.vwo.enums.ApiEnum
import com.vwo.utils.JsonNode
import com.vwo.utils.*
import com.vwo.providers.StorageProvider
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.manager.BatchManager
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.manager.OnlineBatchUploadManager
import com.vwo.packages.segmentation_evaluator.core.SegmentationManager
import com.vwo.packages.storage.GatewayResponseStore
import com.vwo.packages.storage.MobileDefaultStorage
import com.vwo.packages.storage.Storage
import com.vwo.services.LoggerService
import com.vwo.services.SettingsManager
import com.vwo.utils.DataTypeUtil
import com.vwo.utils.LogMessageUtil.buildMessage
import java.lang.ref.WeakReference
import com.vwo.packages.storage.SettingsStore
import com.vwo.utils.FunctionUtil.getFormattedErrorMessage

/**
 * Builder class for constructing and configuring VWO instances.
 *
 * This class provides a fluent interface for setting up various components of the VWO SDK, such
 * as logging, settings management, storage, network management, and segmentation.
 *
 * @param options Optional initialization options for pre-configuring the builder.
 */
open class VWOBuilder(private val options: VWOInitOptions?) {
    private var vwoClient: VWOClient? = null
    private var settingFileManager: SettingsManager? = null
    private val settings: String? = null
    private var originalSettings: String? = null
    private var isSettingsFetchInProgress = false
    internal var isSettingsValid = false
    internal var settingsFetchTime: Long = 0

    // Set VWOClient instance
    fun setVWOClient(vwoClient: VWOClient?) {
        this.vwoClient = vwoClient
    }

    /**
     * Sets the network manager with the provided client and development mode options.
     * @return The VWOBuilder instance.
     */
    fun setNetworkManager(): VWOBuilder {
        if (this.options != null && options.networkClientInterface != null) {
            NetworkManager.attachClient(options.networkClientInterface)
        } else {
            NetworkManager.attachClient()
        }
        NetworkManager.config?.developmentMode = false
        LoggerService.log(
            LogLevelEnum.DEBUG,
            "SERVICE_INITIALIZED",
            object : HashMap<String?, String>() {
                init {
                    put("service", "Network Layer")
                }
            })
        return this
    }

    /**
     * Sets the segmentation evaluator with the provided segmentation options.
     * @return The instance of this builder.
     */
    fun setSegmentation(): VWOBuilder {
        if (options?.segmentEvaluator != null) {
            SegmentationManager.attachEvaluator(options.segmentEvaluator)
        }
        LoggerService.log(
            LogLevelEnum.DEBUG,
            "SERVICE_INITIALIZED",
            object : HashMap<String?, String>() {
                init {
                    put("service", "Segmentation Evaluator")
                }
            })
        return this
    }

    /**
     * Fetches settings asynchronously, ensuring no parallel fetches.
     * @param forceFetch - Force fetch ignoring cache.
     * @return The fetched settings.
     */
    private fun fetchSettings(forceFetch: Boolean): String? {
        // Check if a fetch operation is already in progress
        if (isSettingsFetchInProgress || settingFileManager == null) {
            // Avoid parallel fetches
            return null // Or throw an exception, or handle as needed
        }

        // Set the flag to indicate that a fetch operation is in progress
        isSettingsFetchInProgress = true

        try {
            // Retrieve the settings synchronously
            val settings = settingFileManager!!.getSettings(forceFetch)
            this.isSettingsValid = this.settingFileManager?.isSettingsValid ?: false
            this.settingsFetchTime = this.settingFileManager?.settingsFetchTime ?: 0

            if (!forceFetch) {
                // Store the original settings
                originalSettings = settings
            }

            // Clear the flag to indicate that the fetch operation is complete
            isSettingsFetchInProgress = false

            // Return the fetched settings
            return settings
        } catch (e: Exception) {
            LoggerService.errorLog(
                "ERROR_FETCHING_SETTINGS",
                mapOf(Constants.ERR to getFormattedErrorMessage(e)),
                mapOf(
                    "an" to if (forceFetch) Constants.POLLING else ApiEnum.INIT.value
                ),
                false
            )
            // Clear the flag to indicate that the fetch operation is complete
            isSettingsFetchInProgress = false

            // Return null or handle the error as needed
            return null
        }
    }

    /**
     * Gets the settings, fetching them if not cached or if forced.
     * @param forceFetch - Force fetch ignoring cache.
     * @return The fetched settings.
     */
    fun getSettings(forceFetch: Boolean): String? {
        return fetchSettings(forceFetch)
    }

    /**
     * Sets the storage connector for the VWO instance.
     * @return  The instance of this builder.
     */
    fun setStorage(): VWOBuilder {
        if (options?.storage != null) {
            if (options.storage is MobileDefaultStorage)
                (options.storage as MobileDefaultStorage).init()

            Storage.instance?.attachConnector(options.storage)
        }
        return this
    }

    /**
     * Sets the settings manager for the VWO instance.
     * @return The instance of this builder.
     */
    fun setSettingsManager(): VWOBuilder {
        if (options == null) {
            return this
        }
        settingFileManager = SettingsManager(options)
        return this
    }

    /**
     * Sets the logger for the VWO instance.
     * @return The instance of this builder.
     */
    fun setLogger(): VWOBuilder {
        try {
            if (this.options == null || (options.logger == null) || options.logger.isEmpty()) {
                LoggerService(hashMapOf<String, Any>())
            } else {
                LoggerService(options.logger)
            }
            LoggerService.log(
                LogLevelEnum.DEBUG,
                "SERVICE_INITIALIZED",
                object : HashMap<String?, String>() {
                    init {
                        put("service", "Logger")
                    }
                })
        } catch (e: Exception) {
            val message = buildMessage("Error occurred while initializing Logger : " + e.message, null)
            System.err.println(message)
        }
        return this
    }

    /**
     * Initializes the polling with the provided poll interval.
     * @return The instance of this builder.
     */
    fun initPolling(): VWOBuilder {
        if (options?.pollInterval == null) {
            return this
        }

        if (!DataTypeUtil.isInteger(options.pollInterval)) {
            LoggerService.errorLog(
                "INVALID_POLLING_CONFIGURATION",
                mapOf(
                    "key" to "pollInterval",
                    "correctType" to "number"
                ),
                mapOf("an" to ApiEnum.INIT.value),
            )

            return this
        }

        if ((options.pollInterval ?: 0) < 1000) {
            LoggerService.errorLog(
                "INVALID_POLLING_CONFIGURATION",
                mapOf(
                    "key" to "pollInterval",
                    "correctType" to "number >= 1000"
                ),
                mapOf("an" to ApiEnum.INIT.value),
            )
            return this
        }

        Thread { this.checkAndPoll() }.start()

        return this
    }

    /**
     * Checks and polls for settings updates at the provided interval.
     */
    private fun checkAndPoll() {
        val pollingInterval: Int = options?.pollInterval ?: 1000

        while (true) {
            try {
                // When sdk is initialized, the settings are fetched from VWO.setInstance() and
                // checkAndPoll(). To avoid parallel fetching delay is added first and then settings
                // are fetched.
                // Sleep for the polling interval, then fetch the latest settings
                Thread.sleep(pollingInterval.toLong())

                val latestSettings = getSettings(true)
                val originalSettingsValue = originalSettings // Store in local variable for smart cast
                if (originalSettingsValue != null && latestSettings != null) {
                    val latestSettingJsonNode: JsonNode =
                        VWOClient.objectMapper.readTree(latestSettings)
                    val originalSettingsJsonNode: JsonNode =
                        VWOClient.objectMapper.readTree(originalSettingsValue)
                    if (!latestSettingJsonNode.equals(originalSettingsJsonNode)) {
                        setNewSettings(latestSettings)
                    } else {
                        LoggerService.log(LogLevelEnum.INFO, "POLLING_NO_CHANGE_IN_SETTINGS", null)
                    }
                } else if (latestSettings != null) {
                    setNewSettings(latestSettings)
                }
            } catch (e: InterruptedException) {
                LoggerService.errorLog(
                    "ERROR_FETCHING_SETTINGS_WITH_POLLING",
                    mapOf(Constants.ERR to getFormattedErrorMessage(e)),
                    mapOf("an" to Constants.POLLING)
                )
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                LoggerService.log(LogLevelEnum.ERROR, "Error is $e")
            }
        }
    }

    private fun setNewSettings(latestSettings: String?) {
        originalSettings = latestSettings
        LoggerService.log(LogLevelEnum.INFO, "POLLING_SET_SETTINGS", null)
        // Update VWOClient settings
        vwoClient?.updateSettings(originalSettings)
    }

    /**
     * Sets the shared preferences for the VWO SDK.
     * This function initializes the settings store and request store using the provided context.
     * It also saves the migration version to the settings store.
     *
     * @return This VWOBuilder instance.
     */
    fun setSharePreferences(): VWOBuilder {
        val context = options?.context ?: return this

        StorageProvider.settingsStore = SettingsStore(context)
        StorageProvider.gatewayStore = GatewayResponseStore(context)
        return this
    }

    /**
     * Sets the context for the VWO SDK.
     *
     * This function sets the context reference using a WeakReference to avoid memory leaks.
     *
     * @return This VWOBuilder instance.
     */
    fun setContext(): VWOBuilder {
        val context = options?.context ?: return this
        StorageProvider.contextRef = WeakReference(context)
        StorageProvider.sdkDataManager = SdkDataManager(context)
        BatchManager.sdkDataManager = StorageProvider.sdkDataManager
        return this
    }

    /**
     * Initializes batch manager & necessary values for it.
     */
    fun initBatchManager() {
        OnlineBatchUploadManager.batchMinSize = options?.batchMinSize ?: -1
        OnlineBatchUploadManager.batchUploadTimeInterval = options?.batchUploadTimeInterval ?: -1

        val onlineBatchingAllowed = BatchManager.isOnlineBatchingAllowed()
        val status = if(onlineBatchingAllowed) "enabled" else "disabled"
        if (onlineBatchingAllowed) {
            OnlineBatchUploadManager.startBatchUploader()
        }
        LoggerService.log(
            LogLevelEnum.INFO,
            "ONLINE_BATCH_PROCESSING_STATUS",
            mapOf("status" to status)
        )
    }

}
