package com.wingify

import SdkDataManager
import com.wingify.WingifyClient
import com.wingify.models.user.WingifyInitOptions
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.constants.Constants
import com.wingify.enums.ApiEnum
import com.wingify.models.Settings
import com.wingify.packages.network_layer.manager.BatchManager
import com.wingify.packages.network_layer.manager.NetworkManager
import com.wingify.packages.segmentation_evaluator.core.SegmentationManager
import com.wingify.packages.storage.GatewayResponseStore
import com.wingify.packages.storage.MobileDefaultStorage
import com.wingify.packages.storage.SettingsStore
import com.wingify.packages.storage.Storage
import com.wingify.providers.ServiceContainerProvider
import com.wingify.providers.StorageProvider
import com.wingify.services.LoggerService
import com.wingify.services.SettingsControlApplyMode
import com.wingify.services.SettingsManager
import com.wingify.utils.DataTypeUtil
import com.wingify.utils.FunctionUtil
import com.wingify.utils.JsonNode
import com.wingify.utils.LogMessageUtil
import java.lang.ref.WeakReference

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
/**
 * Builder class for constructing and configuring VWO instances.
 *
 * This class provides a fluent interface for setting up various components of the VWO SDK, such
 * as logging, settings management, storage, network management, and segmentation.
 *
 * @param options Optional initialization options for pre-configuring the builder.
 */
open class WingifyBuilder(private val options: WingifyInitOptions) {

    private var serviceContainer: ServiceContainer? = null
    private var wingifyClient: WingifyClient? = null
    private var settingFileManager: SettingsManager? = null
    private val settings: String? = null
    private var originalSettings: String? = null
    private var isSettingsFetchInProgress = false
    private var isPullFetchInProgress = false
    internal var isSettingsValid = false
    internal var settingsFetchTime: Long = 0

    // Instance-level services instead of static ones
    private var loggerService: LoggerService? = null
    private var batchManager: BatchManager? = null
    internal var storage = Storage()

    fun setWingifyClient(wingifyClient: WingifyClient?) {
        this.wingifyClient = wingifyClient
    }

    @Deprecated(
        message = "Use setWingifyClient instead",
        replaceWith = ReplaceWith("setWingifyClient(wingifyClient)"),
    )
    fun setVWOClient(wingifyClient: WingifyClient?) {
        setWingifyClient(wingifyClient)
    }

    /**
     * Gets the LoggerService instance
     * @return LoggerService instance
     */
    fun getLoggerService(): LoggerService? {
        return loggerService
    }

    /**
     * Gets the SettingsManager instance
     * @return SettingsManager instance
     */
    fun getSettingsManager(): SettingsManager? {
        return settingFileManager
    }

    /**
     * Gets the BatchManager instance
     * @return BatchManager instance
     */
    internal fun getBatchManager(): Any? {
        return batchManager
    }

    /**
     * Sets the network manager with the provided client and development mode options.
     * @return The WingifyBuilder instance.
     */
    fun setNetworkManager(): WingifyBuilder {
        if (this.options != null && options.networkClientInterface != null) {
            NetworkManager.attachClient(options.networkClientInterface)
        } else {
            NetworkManager.attachClient()
        }
        NetworkManager.config?.developmentMode = false
        loggerService?.log(
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
    fun setSegmentation(): WingifyBuilder {
        if (options?.segmentEvaluator != null) {
            SegmentationManager().attachEvaluator(options.segmentEvaluator)
        }
        loggerService?.log(
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
     * Fetches settings. Poll (`v2-settings`) and panic pull (`v2-pull`) use separate in-flight flags.
     * @param forceFetch - Force fetch ignoring cache.
     * @param endpoint Path to GET (`v2-settings` or panic `v2-pull`).
     * @return The fetched settings.
     */
    private fun fetchSettings(
        forceFetch: Boolean,
        endpoint: String = Constants.SETTINGS_ENDPOINT,
    ): String? {
        if (settingFileManager == null) return null
        val isPull = endpoint == Constants.SETTINGS_PULL_ENDPOINT
        if (isPull) {
            if (isPullFetchInProgress) return null
            isPullFetchInProgress = true
        } else if (isSettingsFetchInProgress) {
            return null
        } else {
            isSettingsFetchInProgress = true
        }

        try {
            val settings = settingFileManager!!.getSettings(forceFetch, endpoint)
            this.isSettingsValid = this.settingFileManager?.isSettingsValid ?: false
            this.settingsFetchTime = this.settingFileManager?.settingsFetchTime ?: 0

            if (!forceFetch) {
                originalSettings = settings
            }
            return settings
        } catch (e: Exception) {
            LoggerService.Companion.errorLog(
                "ERROR_FETCHING_SETTINGS",
                mapOf(Constants.ERR to FunctionUtil.getFormattedErrorMessage(e)),
                mapOf(
                    "an" to when {
                        isPull -> ApiEnum.UPDATE_SETTINGS.value
                        forceFetch -> Constants.POLLING
                        else -> ApiEnum.INIT.value
                    }
                ),
                false,
                serviceContainer
            )
            return null
        } finally {
            if (isPull) {
                isPullFetchInProgress = false
            } else {
                isSettingsFetchInProgress = false
            }
        }
    }

    /**
     * Gets the settings, fetching them if not cached or if forced.
     * @param forceFetch - Force fetch ignoring cache.
     * @param endpoint Path to GET. Panic force-refresh uses [Constants.SETTINGS_PULL_ENDPOINT].
     * @return The fetched settings.
     */
    fun getSettings(
        forceFetch: Boolean,
        endpoint: String = Constants.SETTINGS_ENDPOINT,
    ): String? {
        return fetchSettings(forceFetch, endpoint)
    }

    /**
     * Sets the storage connector for the VWO instance.
     * @return  The instance of this builder.
     */
    fun setStorage(): WingifyBuilder {
        val connector = options.storage ?: MobileDefaultStorage(options).also {
            options.storage = it
        }
        if (connector is MobileDefaultStorage) {
            connector.init()
        }

        storage.attachConnector(connector)
        return this
    }

    /**
     * Sets the settings manager for the VWO instance.
     * @return The instance of this builder.
     */
    fun setSettingsManager(): WingifyBuilder {
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
    fun setLogger(): WingifyBuilder {
        try {
            val serviceContainer = createServiceContainer(null, options)
            if (this.options == null || (options.logger == null) || options.logger.isEmpty()) {
                this.loggerService = LoggerService(hashMapOf<String, Any>(), serviceContainer)
            } else {
                this.loggerService = LoggerService(options.logger, serviceContainer)
            }
            // Use static LoggerService for now due to existing architecture
            loggerService?.log(
                level = LogLevelEnum.DEBUG,
                key = "SERVICE_INITIALIZED",
                map = mapOf(
                    "service" to "Logger"
                )
            )
            validateIntegrationCallbackConfig()
            this.serviceContainer = serviceContainer
        } catch (e: Exception) {
            val message =
                LogMessageUtil.buildMessage(
                    "Error occurred while initializing Logger : " + e.message,
                    null
                )
            System.err.println(message)
        }
        return this
    }

    /**
     * Logs an error when [WingifyInitOptions.shouldTriggerIntegrationCallbackAlways] is enabled
     * without an [WingifyInitOptions.integrations] callback — the flag has no effect otherwise.
     */
    private fun validateIntegrationCallbackConfig() {
        if (options.shouldTriggerIntegrationCallbackAlways && options.integrations == null) {
            loggerService?.log(
                level = LogLevelEnum.ERROR,
                key = "INTEGRATION_CALLBACK_MISSING",
                map = null
            )
        }
    }

    /**
     * Initializes the polling with the provided poll interval.
     * @return The instance of this builder.
     */
    fun initPolling(): WingifyBuilder {
        if (options?.pollInterval == null) {
            return this
        }

        if (!DataTypeUtil.isInteger(options.pollInterval)) {
            LoggerService.Companion.errorLog(
                "INVALID_POLLING_CONFIGURATION",
                mapOf(
                    "key" to "pollInterval",
                    "correctType" to "number"
                ),
                mapOf("an" to ApiEnum.INIT.value),
                true,
                serviceContainer
            )

            return this
        }

        if ((options.pollInterval ?: 0) < 1000) {
            LoggerService.Companion.errorLog(
                key = "INVALID_POLLING_CONFIGURATION",
                data = mapOf(
                    "key" to "pollInterval",
                    "correctType" to "number >= 1000"
                ),
                debugData = mapOf("an" to ApiEnum.INIT.value),
                shouldSendToVWO = true,
                serviceContainer = serviceContainer
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
                maybeTriggerForceRefreshFromPoll(latestSettings)
                // Store in local variable for smart cast
                val originalSettingsValue = originalSettings
                if (originalSettingsValue != null && latestSettings != null) {
                    val latestSettingJsonNode: JsonNode =
                        WingifyClient.objectMapper.readTree(latestSettings)
                    val originalSettingsJsonNode: JsonNode =
                        WingifyClient.objectMapper.readTree(originalSettingsValue)
                    if (!latestSettingJsonNode.equals(originalSettingsJsonNode)) {
                        if (shouldSkipStalePollingSettings(latestSettings)) {
                            loggerService?.log(
                                LogLevelEnum.INFO,
                                "POLLING_SETTINGS_STALE",
                                mapOf("timestamp" to pollingSnapshotId(latestSettings))
                            )
                        } else {
                            setNewSettings(latestSettings)
                        }
                    } else {
                        loggerService?.log(LogLevelEnum.INFO, "POLLING_NO_CHANGE_IN_SETTINGS", null)
                    }
                } else if (latestSettings != null) {
                    setNewSettings(latestSettings)
                }
            } catch (e: InterruptedException) {
                LoggerService.Companion.errorLog(
                    key = "ERROR_FETCHING_SETTINGS_WITH_POLLING",
                    data = mapOf(Constants.ERR to FunctionUtil.getFormattedErrorMessage(e)),
                    debugData = mapOf("an" to Constants.POLLING),
                    shouldSendToVWO = true,
                    serviceContainer = serviceContainer
                )
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                loggerService?.log(LogLevelEnum.ERROR, "Error is $e")
            }
        }
    }

    /**
     * Applies a polled settings snapshot through [WingifyClient.updateSettings] in POLLING mode.
     *
     * @param latestSettings Latest settings JSON from polling, or null.
     */
    private fun setNewSettings(latestSettings: String?) {
        originalSettings = latestSettings
        loggerService?.log(LogLevelEnum.INFO, "POLLING_SET_SETTINGS", null)
        wingifyClient?.updateSettings(originalSettings, SettingsControlApplyMode.POLLING)
    }

    /**
     * Updates the polling baseline after a force-refresh fetch.
     *
     * @param settings Settings JSON that should become the next polling comparison baseline.
     */
    fun syncOriginalSettings(settings: String?) {
        originalSettings = settings
    }

    /**
     * Triggers v2-pull when a poll response carries a fresh `sdkMetaInfo.refresh`, even if the
     * snapshot id is older than the last applied settings (e.g. after a prior v2-pull).
     */
    private fun maybeTriggerForceRefreshFromPoll(latestSettings: String?) {
        if (latestSettings.isNullOrEmpty()) return
        val parsed = try {
            WingifyClient.gson.fromJson(latestSettings, Settings::class.java)
        } catch (_: Exception) {
            return
        } ?: return
        val container = serviceContainer ?: return
        val result = container.panicManager.evaluateForceRefreshFromPollMeta(parsed)
        if (result.shouldTriggerForceRefresh) {
            container.triggerForceRefresh()
        }
    }

    /**
     * Returns true when the polled snapshot is older than or equal to the last applied settings.
     * Unparseable payloads are not skipped so polling can still attempt an update.
     *
     * @param latestSettings Polled settings JSON.
     * @return true when [WingifyClient.updateSettings] should be skipped.
     */
    private fun shouldSkipStalePollingSettings(latestSettings: String): Boolean {
        val parsed = try {
            WingifyClient.gson.fromJson(latestSettings, Settings::class.java)
        } catch (_: Exception) {
            return false
        } ?: return false
        val panicManager = serviceContainer?.panicManager ?: return false
        return !panicManager.shouldApplySettingsSnapshot(parsed)
    }

    /**
     * Snapshot id used in polling logs: [Settings.lastUpdatedOn], else [Settings.version].
     *
     * @param latestSettings Polled settings JSON.
     * @return Timestamp string, or empty when parsing fails.
     */
    private fun pollingSnapshotId(latestSettings: String): String {
        return try {
            val parsed = WingifyClient.gson.fromJson(latestSettings, Settings::class.java)
            (parsed?.lastUpdatedOn ?: parsed?.version)?.toString().orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Sets the shared preferences for the VWO SDK.
     * This function initializes the settings store and request store using the provided context.
     * It also saves the migration version to the settings store.
     *
     * @return This WingifyBuilder instance.
     */
    fun setSharePreferences(): WingifyBuilder {
        val context = options.context ?: return this

        if (StorageProvider.settingsStore == null)
            StorageProvider.settingsStore = SettingsStore(context)

        if (StorageProvider.gatewayStore == null)
            StorageProvider.gatewayStore = GatewayResponseStore(context)
        return this
    }

    /**
     * Sets the context for the VWO SDK.
     *
     * This function sets the context reference using a WeakReference to avoid memory leaks.
     *
     * @return This WingifyBuilder instance.
     */
    fun setContext(): WingifyBuilder {
        val context = options.context ?: return this
        StorageProvider.contextRef = WeakReference(context)
        StorageProvider.sdkDataManager = SdkDataManager(context)
        return this
    }

    /**
     * Creates a ServiceContainer instance with the current settings and options
     * Following Java SDK pattern where ServiceContainer is created per API call
     * @return ServiceContainer instance
     */
    fun createServiceContainer(
        processedSettings: Settings?,
        options: WingifyInitOptions
    ): ServiceContainer {
        val serviceContainer =
            ServiceContainerProvider.createServiceContainer(this, processedSettings, options)
        return serviceContainer
    }
}