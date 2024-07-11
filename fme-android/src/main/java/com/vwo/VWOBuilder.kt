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
package com.vwo

import com.fasterxml.jackson.databind.JsonNode
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.storage.Storage
import com.vwo.services.SettingsManager

class VWOBuilder(options: VWOInitOptions?) {
    private var vwoClient: VWOClient? = null
    private val options: VWOInitOptions? = options
    private var settingFileManager: SettingsManager? = null
    private val settings: String? = null
    private var originalSettings: String? = null
    private var isSettingsFetchInProgress = false

    // Set VWOClient instance
    fun setVWOClient(vwoClient: VWOClient?) {
        this.vwoClient = vwoClient
    }

    /**
     * Sets the network manager with the provided client and development mode options.
     * @return The VWOBuilder instance.
     */
    fun setNetworkManager(): VWOBuilder {
        val networkInstance: NetworkManager = NetworkManager.instance
        if (this.options != null && options.networkClientInterface != null) {
            networkInstance.attachClient(options.networkClientInterface)
        } else {
            networkInstance.attachClient()
        }
        networkInstance.config.developmentMode = false
        LoggerService.log(
            LogLevelEnum.DEBUG,
            "SERVICE_INITIALIZED",
            object : HashMap<String?, String?>() {
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
        if (options != null && options.segmentEvaluator != null) {
            SegmentationManager.instance.attachEvaluator(options.segmentEvaluator)
        }
        LoggerService.log(
            LogLevelEnum.DEBUG,
            "SERVICE_INITIALIZED",
            object : HashMap<String?, String?>() {
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
    fun fetchSettings(forceFetch: Boolean?): String? {
        // Check if a fetch operation is already in progress
        if (isSettingsFetchInProgress || settingFileManager == null) {
            // Avoid parallel fetches
            return null // Or throw an exception, or handle as needed
        }

        // Set the flag to indicate that a fetch operation is in progress
        isSettingsFetchInProgress = true

        try {
            // Retrieve the settings synchronously
            val settings = settingFileManager!!.getSettings(forceFetch!!)

            if (!forceFetch) {
                // Store the original settings
                originalSettings = settings
            }

            // Clear the flag to indicate that the fetch operation is complete
            isSettingsFetchInProgress = false

            // Return the fetched settings
            return settings
        } catch (e: Exception) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "SETTINGS_FETCH_ERROR",
                object : HashMap<String?, String?>() {
                    init {
                        put("err", e.toString())
                    }
                })
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
    fun getSettings(forceFetch: Boolean?): String? {
        return fetchSettings(forceFetch)
    }

    /**
     * Sets the storage connector for the VWO instance.
     * @return  The instance of this builder.
     */
    fun setStorage(): VWOBuilder {
        if (options != null && options.storage != null) {
            Storage.instance!!.attachConnector(options.storage)
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
                LoggerService(HashMap<String, Any>())
            } else {
                LoggerService(options.logger)
            }
            LoggerService.log(
                LogLevelEnum.DEBUG,
                "SERVICE_INITIALIZED",
                object : HashMap<String?, String?>() {
                    init {
                        put("service", "Logger")
                    }
                })
        } catch (e: Exception) {
            val message: String =
                buildMessage("Error occurred while initializing Logger : " + e.message, null)
            System.err.println(message)
        }
        return this
    }

    /**
     * Initializes the polling with the provided poll interval.
     * @return The instance of this builder.
     */
    fun initPolling(): VWOBuilder {
        if (options.pollInterval == null) {
            return this
        }

        if (options.pollInterval != null && !DataTypeUtil.isInteger(
                options.pollInterval
            )
        ) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "INIT_OPTIONS_INVALID",
                object : HashMap<String?, String?>() {
                    init {
                        put("key", "pollInterval")
                        put("correctType", "number")
                    }
                })
            return this
        }

        if (options.pollInterval != null && options.pollInterval < 1000) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "INIT_OPTIONS_INVALID",
                object : HashMap<String?, String?>() {
                    init {
                        put("key", "pollInterval")
                        put("correctType", "number")
                    }
                })
            return this
        }

        Thread { this.checkAndPoll() }.start()

        return this
    }

    /**
     * Checks and polls for settings updates at the provided interval.
     */
    private fun checkAndPoll() {
        val pollingInterval: Int = options.pollInterval

        while (true) {
            try {
                val latestSettings = getSettings(true)
                if (originalSettings != null && latestSettings != null) {
                    val latestSettingJsonNode: JsonNode =
                        VWOClient.Companion.objectMapper.readTree(latestSettings)
                    val originalSettingsJsonNode: JsonNode =
                        VWOClient.Companion.objectMapper.readTree(originalSettings)
                    if (!latestSettingJsonNode.equals(originalSettingsJsonNode)) {
                        originalSettings = latestSettings
                        LoggerService.log(LogLevelEnum.INFO, "POLLING_SET_SETTINGS", null)
                        // Update VWOClient settings
                        if (vwoClient != null) {
                            vwoClient!!.updateSettings(originalSettings)
                        }
                    } else {
                        LoggerService.log(LogLevelEnum.INFO, "POLLING_NO_CHANGE_IN_SETTINGS", null)
                    }
                }
                // Sleep for the polling interval
                Thread.sleep(pollingInterval.toLong())
            } catch (e: InterruptedException) {
                LoggerService.log(LogLevelEnum.ERROR, "POLLING_FETCH_SETTINGS_FAILED", null)
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                LoggerService.log(LogLevelEnum.ERROR, "Error is $e")
            }
        }
    }
}
