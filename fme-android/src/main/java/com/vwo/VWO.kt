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

import android.os.Build
import com.vwo.constants.Constants.PLATFORM
import com.vwo.constants.Constants.SDK_NAME
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.logger.transports.ConsoleTransport
import com.vwo.providers.StorageProvider
import com.vwo.sdk.fme.BuildConfig
import com.vwo.utils.EventsUtils
import com.vwo.utils.SDKMetaUtil
import com.vwo.utils.UsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

/**
 *  VWO (Visual Website Optimizer) is a powerful A/B testing and experimentation platform.
 *
 * This class provides instances for interacting with the VWO SDK. Each init() call creates
 * a new VWO instance with its own isolated services, allowing multiple accounts to be used
 * simultaneously without conflicts.
 */
class VWO private constructor(settings: String?, options: VWOInitOptions, vwoBuilder: VWOBuilder) :
    VWOClient(settings, options, vwoBuilder) {

    companion object {

        /**
         * Tracks the current initialization state of the VWO SDK per account.
         *
         * This ConcurrentHashMap ensures thread-safe access across multiple threads and prevents
         * concurrent initialization attempts for each account. The state transitions through:
         * - NOT_INITIALIZED: SDK hasn't been initialized or initialization failed
         * - INITIALIZING: SDK initialization is currently in progress
         * - INITIALIZED: SDK has been successfully initialized and is ready for use
         *
         * Key format: "accountId_sdkKey"
         */
        private val accountStates: ConcurrentHashMap<String, SDKState> = ConcurrentHashMap()

        /**
         * Cache of VWO instances per account to avoid re-initialization.
         * Key format: "accountId_sdkKey"
         */
        private val vwoInstances: ConcurrentHashMap<String, VWO> = ConcurrentHashMap()

        /**
         * Generates a unique key for an account based on accountId and sdkKey.
         * @param options VWO initialization options
         * @return Account key in format "accountId_sdkKey"
         */
        private fun getAccountKey(options: VWOInitOptions): String {
            return "${options.accountId ?: 0}_${options.sdkKey ?: ""}"
        }

        /**
         * Gets the current state for a specific account.
         * @param accountKey The account key
         * @return Current SDKState for the account
         */
        private fun getAccountState(accountKey: String): SDKState {
            return accountStates[accountKey] ?: SDKState.NOT_INITIALIZED
        }

        /**
         * Sets the state for a specific account.
         * @param accountKey The account key
         * @param state The new state
         */
        private fun setAccountState(accountKey: String, state: SDKState) {
            accountStates[accountKey] = state
        }

        /**
         * Gets an existing VWO instance for an account if available.
         * @param accountKey The account key
         * @return Existing VWO instance or null
         */
        private fun getExistingInstance(accountKey: String): VWO? {
            return vwoInstances[accountKey]
        }

        /**
         * Caches a VWO instance for an account.
         * @param accountKey The account key
         * @param instance The VWO instance to cache
         */
        private fun cacheInstance(accountKey: String, instance: VWO) {
            vwoInstances[accountKey] = instance
        }

        /**
         * Creates a new VWO instance with the provided options.
         * Each call creates a separate instance with isolated services.
         * @param options - Configuration options for setting up VWO.
         * @return A new VWO instance.
         */
        private fun setInstance(options: VWOInitOptions): VWO {
            val vwoBuilder: VWOBuilder = options.vwoBuilder ?: VWOBuilder(options)
            vwoBuilder.setLogger() // Sets up logging for debugging and monitoring.
                .setContext()
                .setSharePreferences()// Sets up local storage for saving settings
                .setSettingsManager() // Sets the settings manager for configuration management.
                .setStorage() // Configures storage for data persistence.
                .setNetworkManager() // Configures network management for API communication.
                .setSegmentation() // Sets up segmentation for targeted functionality.
                .initPolling() // Initializes the polling mechanism for fetching settings.

            SDKMetaUtil.sdkName = options.sdkName
            SDKMetaUtil.sdkVersion = options.sdkVersion
            StorageProvider.userAgent =
                "VWO FME $PLATFORM ${BuildConfig.SDK_VERSION} ($PLATFORM/${Build.VERSION.RELEASE})"
            val serviceContainer = vwoBuilder.createServiceContainer(null, options)
            val settings = vwoBuilder.getSettings(false)

            // Create new VWO instance instead of reusing singleton
            val vwoInstance = VWO(settings, options, vwoBuilder)
            vwoInstance.processedSettings?.let { serviceContainer.setSettings(it) }
            vwoInstance.isSettingsValid = vwoBuilder.isSettingsValid
            vwoInstance.settingsFetchTime = vwoBuilder.settingsFetchTime
            vwoBuilder.setVWOClient(vwoInstance)
            val usageStats = UsageStats()
            usageStats.collectStats(options, serviceContainer)
            serviceContainer.usageStats = usageStats

            return vwoInstance
        }

        @JvmStatic
        fun init(options: VWOInitOptions, initListener: IVwoInitCallback) {
            val accountKey = getAccountKey(options)
            val currentState = getAccountState(accountKey)

            val initialLogger = ConsoleTransport(LogLevelEnum.INFO)

            if (currentState == SDKState.INITIALIZING) {
                // This specific account is already initializing
                initialLogger.log(LogLevelEnum.INFO, "Account $accountKey is already initializing")
                return
            }

            if (currentState == SDKState.INITIALIZED) {
                // Return existing cached instance for this account
                val existingInstance = getExistingInstance(accountKey)
                if (existingInstance != null) {
                    initialLogger.log(
                        LogLevelEnum.INFO,
                        "Account $accountKey has already been initialized"
                    )
                    initListener.vwoInitSuccess(
                        existingInstance,
                        "VWO already initialized for this account"
                    )
                    return
                } else {
                    // Instance was somehow lost, reset state and continue with initialization
                    setAccountState(accountKey, SDKState.NOT_INITIALIZED)
                }
            }

            setAccountState(accountKey, SDKState.INITIALIZING)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (options.sdkKey.isNullOrEmpty()) {
                        setAccountState(accountKey, SDKState.NOT_INITIALIZED)
                        val message =
                            "SDK key is required to initialize VWO. Please provide the sdkKey in " +
                                    "the options."
                        initListener.vwoInitFailed(message)
                        return@launch
                    }

                    if (options.accountId == null || options.accountId == 0) {
                        setAccountState(accountKey, SDKState.NOT_INITIALIZED)
                        val message =
                            "Account ID is required to initialize VWO. Please provide the " +
                                    "accountId in the options."
                        initListener.vwoInitFailed(message)
                        return@launch
                    }

                    var vwo: VWO
                    val sdkInitTime = measureTimeMillis {
                        // Create new instance for each init call (like Java SDK)
                        vwo = setInstance(options)
                    }

                    val serviceContainer = vwo.createServiceContainer()
                    if (options.sdkName == SDK_NAME) // Don't call sendSdkInitEvent for hybrid SDKs
                        serviceContainer.let { vwo.sendSdkInitEvent(sdkInitTime, it) }

                    serviceContainer.let { vwo.sendUsageStats(it) }

                    // Cache the instance and mark as initialized for this account
                    cacheInstance(accountKey, vwo)
                    setAccountState(accountKey, SDKState.INITIALIZED)
                    initListener.vwoInitSuccess(vwo, "VWO initialized successfully")

                    serviceContainer.getBatchManager().let {
                        it.initBatchManager(serviceContainer) // Initialize batch manager
                        it.sdkDataManager = StorageProvider.sdkDataManager
                        it.start(it.initName, null) //Upload all events on init
                    }
                } catch (exception: Exception) {
                    // Reset state on any initialization failure
                    setAccountState(accountKey, SDKState.NOT_INITIALIZED)
                    val errorMessage =
                        "VWO initialization failed for account $accountKey: ${exception.message}"
                    initListener.vwoInitFailed(errorMessage)
                }
            }
        }

        /**
         * Gets an existing VWO instance for the specified account.
         * @param accountId The account ID
         * @param sdkKey The SDK key
         * @return Existing VWO instance or null if not initialized
         */
        @JvmStatic
        fun getInstance(accountId: Int?, sdkKey: String?): VWO? {
            val accountKey = "${accountId ?: 0}_${sdkKey ?: ""}"
            return getExistingInstance(accountKey)
        }

        /**
         * Clears the cached instance for a specific account.
         * This will force re-initialization on next init() call.
         * @param accountId The account ID
         * @param sdkKey The SDK key
         */
        @JvmStatic
        fun clearInstance(accountId: Int?, sdkKey: String?) {
            val accountKey = "${accountId ?: 0}_${sdkKey ?: ""}"
            vwoInstances.remove(accountKey)
            setAccountState(accountKey, SDKState.NOT_INITIALIZED)
        }

        /**
         * Clears all cached instances and resets all account states.
         * This will force re-initialization for all accounts on next init() calls.
         */
        @JvmStatic
        fun clearAllInstances() {
            vwoInstances.clear()
            accountStates.clear()
        }
    }

    /**
     * Sends an SDK initialization event.
     *
     * This function checks if the VWO instance is valid, if its settings have been processed,
     * and critically, if the SDK has not been marked as initialized previously in the current
     * session or from cached settings. If all conditions are true, it proceeds to send
     * an "SDK initialized" tracking event, including the time it took for settings to be fetched
     * and the time it took for the SDK to complete its initialization process.
     *
     * This helps in tracking the initial setup performance and ensuring that the
     * initialization event is sent only once per effective SDK start.
     *
     * @param sdkInitTime The timestamp (in milliseconds) marking the completion of the SDK's initialization process.
     */
    fun sendSdkInitEvent(sdkInitTime: Long, serviceContainer: ServiceContainer? = null) {

        val serviceContainer = serviceContainer ?: createServiceContainer()

        val wasInitializedEarlier = this.processedSettings?.sdkMetaInfo?.wasInitializedEarlier

        if (this.isSettingsValid && wasInitializedEarlier != true) {
            EventsUtils().sendSdkInitEvent(this.settingsFetchTime, sdkInitTime, serviceContainer)
        }
    }

    /**
     * Sends SDK usage statistics.
     *
     * This function retrieves the usage statistics account ID from settings.
     * If the account ID is found, it triggers an event to send SDK usage statistics.
     * This helps in understanding how the SDK is being utilized.
     * If the `usageStatsAccountId` is not available in the settings, the function will return early
     * and no event will be sent.
     */
    fun sendUsageStats(serviceContainer: ServiceContainer) {
        // Get usage stats account id from settings
        val usageStatsAccountId = this.processedSettings?.usageStatsAccountId ?: return
        EventsUtils().sendSDKUsageStatsEvent(usageStatsAccountId, serviceContainer)
    }

    /**
     * This method is used to update the settings
     * @param newSettings New settings to be updated
     */
    override fun updateSettings(newSettings: String?) {
        super.updateSettings(newSettings)
    }

    /**
     * This method is used to get the flag value for the given feature key
     * @param featureKey Feature key for which the flag value is to be fetched
     * @param context User context
     * @param listener IVwoListener Callback when getFlag operation completes
     * @return GetFlag object containing the flag values
     */
    fun getFlag(featureKey: String, context: VWOUserContext, listener: IVwoListener) {
        thread(start = true) {
            try {
                val flag = super.getFlag(featureKey, context)
                if (flag != null)
                    listener.onSuccess(flag)
                else
                    listener.onFailure("Error getting flag!")
            } catch (e: Exception) {
                listener.onFailure(e.message ?: e.toString())
            }
        }
    }

    /**
     * Overloaded function if event properties need to be passed
     * calls track method to track the event
     * @param eventName Event name to be tracked
     * @param context User context
     * @param eventProperties event properties to be sent for the event
     * @return Map containing the event name and its status
     */
    override fun trackEvent(
        eventName: String,
        context: VWOUserContext?,
        eventProperties: Map<String, Any>
    ): Map<String, Boolean> {
        return super.trackEvent(eventName, context, eventProperties)
    }

    /**
     * Overloaded function for no event properties
     * calls track method to track the event
     * @param eventName Event name to be tracked
     * @param context User context
     * @return Map containing the event name and its status
     */
    override fun trackEvent(eventName: String, context: VWOUserContext?): Map<String, Boolean> {
        return super.trackEvent(eventName, context)
    }

    /**
     * Sets an attribute for a user in the context provided.
     * This method validates the types of the inputs before proceeding with the API call.
     * @param attributes - Map of attribute key and value to be set
     * @param context User context
     */
    override fun setAttribute(attributes: Map<String, Any>, context: VWOUserContext) {
        super.setAttribute(attributes, context)
    }

    /**
     * Set alias of [aliasId] to the qualified id in [context].
     *
     * @param context - Context that will be used for [getFlag]
     * @param aliasId  - The actual user id, maybe after login
     */
    override fun setAlias(context: VWOUserContext, aliasId: String) {
        super.setAlias(context, aliasId)
    }

}

