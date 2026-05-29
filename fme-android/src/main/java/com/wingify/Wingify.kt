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

import android.os.Build
import com.wingify.constants.Constants.PLATFORM
import com.wingify.constants.Constants.SDK_NAME
import com.wingify.interfaces.IWingifyInitCallback
import com.wingify.interfaces.IWingifyListener
import com.wingify.models.user.GetFlag
import com.wingify.models.user.WingifyInitOptions
import com.wingify.models.user.WingifyUserContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.packages.logger.transports.ConsoleTransport
import com.wingify.providers.StorageProvider
import com.vwo.sdk.fme.BuildConfig
import com.wingify.SDKState
import com.wingify.ServiceContainer
import com.wingify.utils.SDKMetaUtil
import com.wingify.utils.UsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

/**
 * Public entry point for the Wingify Feature Management and Experimentation SDK.
 *
 * Each [init] call creates an isolated client instance per account. Legacy integrations should
 * continue using [com.vwo.VWO].
 */
open class Wingify protected constructor(
    settings: String?,
    options: WingifyInitOptions,
    wingifyBuilder: WingifyBuilder,
) : WingifyClient(settings, options, wingifyBuilder) {

    /**
     * Evaluates a feature flag asynchronously.
     */
    fun getFlag(featureKey: String, context: WingifyUserContext, listener: IWingifyListener) {
        thread(start = true) {
            try {
                val flag = GetFlag.wrap(super.getFlag(featureKey, context))
                listener.onSuccess(flag)
            } catch (e: Exception) {
                listener.onFailure(e.message ?: e.toString())
            }
        }
    }

    /**
     * Evaluates a feature flag synchronously.
     */
    fun getFlag(featureKey: String, context: WingifyUserContext): GetFlag {
        return GetFlag.wrap(super.getFlag(featureKey, context))
    }

    companion object {
        private val accountStates: ConcurrentHashMap<String, SDKState> = ConcurrentHashMap()
        private val clientInstances: ConcurrentHashMap<String, WingifyClient> = ConcurrentHashMap()

        private fun getAccountKey(options: WingifyInitOptions): String {
            return "${options.accountId ?: 0}_${options.sdkKey ?: ""}"
        }

        private fun getAccountState(accountKey: String): SDKState {
            return accountStates[accountKey] ?: SDKState.NOT_INITIALIZED
        }

        private fun setAccountState(accountKey: String, state: SDKState) {
            accountStates[accountKey] = state
        }

        private fun getExistingInstance(accountKey: String): WingifyClient? {
            return clientInstances[accountKey]
        }

        private fun cacheInstance(accountKey: String, instance: WingifyClient) {
            clientInstances[accountKey] = instance
        }

        internal fun setInstance(
            options: WingifyInitOptions,
            factory: (String?, WingifyInitOptions, WingifyBuilder) -> WingifyClient = ::Wingify,
        ): WingifyClient {
            val builder: WingifyBuilder = options.wingifyBuilder ?: WingifyBuilder(options)
            builder.setLogger()
                .setContext()
                .setSharePreferences()
                .setSettingsManager()
                .setStorage()
                .setNetworkManager()
                .setSegmentation()
                .initPolling()

            SDKMetaUtil.configureFromInitOptions(options)
            StorageProvider.userAgent =
                "VWO FME $PLATFORM ${BuildConfig.SDK_VERSION} ($PLATFORM/${Build.VERSION.RELEASE})"
            val serviceContainer = builder.createServiceContainer(null, options)
            val settings = builder.getSettings(false)

            val wingifyInstance = factory(settings, options, builder)
            wingifyInstance.processedSettings?.let { serviceContainer.setSettings(it) }
            wingifyInstance.isSettingsValid = builder.isSettingsValid
            wingifyInstance.settingsFetchTime = builder.settingsFetchTime
            builder.setWingifyClient(wingifyInstance)
            val usageStats = UsageStats()
            usageStats.collectStats(options, serviceContainer)
            serviceContainer.usageStats = usageStats

            return wingifyInstance
        }

        internal fun initWithFactory(
            options: WingifyInitOptions,
            factory: (String?, WingifyInitOptions, WingifyBuilder) -> WingifyClient,
            onSuccess: (WingifyClient, String) -> Unit,
            onFailure: (String) -> Unit,
        ) {
            val accountKey = getAccountKey(options)
            val currentState = getAccountState(accountKey)
            val initialLogger = ConsoleTransport(LogLevelEnum.INFO)

            if (currentState == SDKState.INITIALIZING) {
                initialLogger.log(LogLevelEnum.INFO, "Account $accountKey is already initializing")
                return
            }

            if (currentState == SDKState.INITIALIZED) {
                val existingInstance = getExistingInstance(accountKey)
                if (existingInstance != null) {
                    initialLogger.log(
                        LogLevelEnum.INFO,
                        "Account $accountKey has already been initialized",
                    )
                    onSuccess(
                        existingInstance,
                        "Wingify already initialized for this account",
                    )
                    return
                } else {
                    setAccountState(accountKey, SDKState.NOT_INITIALIZED)
                }
            }

            setAccountState(accountKey, SDKState.INITIALIZING)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (options.sdkKey.isNullOrEmpty()) {
                        setAccountState(accountKey, SDKState.NOT_INITIALIZED)
                        onFailure(
                            "SDK key is required to initialize Wingify. Please provide the sdkKey in the options.",
                        )
                        return@launch
                    }

                    if (options.accountId == null || options.accountId == 0) {
                        setAccountState(accountKey, SDKState.NOT_INITIALIZED)
                        onFailure(
                            "Account ID is required to initialize Wingify. Please provide the accountId in the options.",
                        )
                        return@launch
                    }

                    lateinit var client: WingifyClient
                    val sdkInitTime = measureTimeMillis {
                        client = setInstance(options, factory)
                    }

                    val serviceContainer = client.createServiceContainer()
                    if (options.sdkName == SDK_NAME) {
                        client.sendSdkInitEvent(sdkInitTime, serviceContainer)
                    }
                    client.sendUsageStats(serviceContainer)

                    cacheInstance(accountKey, client)
                    setAccountState(accountKey, SDKState.INITIALIZED)
                    onSuccess(client, "Wingify initialized successfully")

                    serviceContainer.getBatchManager().let {
                        it.initBatchManager(serviceContainer)
                        it.sdkDataManager = StorageProvider.sdkDataManager
                        it.start(it.initName, null)
                    }
                } catch (exception: Exception) {
                    setAccountState(accountKey, SDKState.NOT_INITIALIZED)
                    onFailure(
                        "Wingify initialization failed for account $accountKey: ${exception.message}",
                    )
                }
            }
        }

        /**
         * Initializes the Wingify SDK for the account defined in [options].
         */
        @JvmStatic
        fun init(options: WingifyInitOptions, initListener: IWingifyInitCallback) {
            initWithFactory(options, ::Wingify, onSuccess = { client, message ->
                initListener.wingifyInitSuccess(client as Wingify, message)
            }, onFailure = initListener::wingifyInitFailed)
        }

        /**
         * Returns a cached client instance for the account, or null if not initialized.
         */
        internal fun getClientInstance(accountId: Int?, sdkKey: String?): WingifyClient? {
            val accountKey = "${accountId ?: 0}_${sdkKey ?: ""}"
            return getExistingInstance(accountKey)
        }

        /**
         * Returns a cached Wingify instance for the account, or null if not initialized.
         */
        @JvmStatic
        fun getInstance(accountId: Int?, sdkKey: String?): Wingify? {
            return getClientInstance(accountId, sdkKey) as? Wingify
        }

        /**
         * Clears the cached instance for an account so the next [init] runs fresh setup.
         */
        @JvmStatic
        fun clearInstance(accountId: Int?, sdkKey: String?) {
            val accountKey = "${accountId ?: 0}_${sdkKey ?: ""}"
            clientInstances.remove(accountKey)
            setAccountState(accountKey, SDKState.NOT_INITIALIZED)
        }

        /**
         * Clears all cached instances and resets initialization state.
         */
        @JvmStatic
        fun clearAllInstances() {
            clientInstances.clear()
            accountStates.clear()
        }
    }
}
