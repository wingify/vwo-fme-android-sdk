/*
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vwo.providers

import com.vwo.ServiceContainer
import com.vwo.VWOBuilder
import com.vwo.models.Settings
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.logger.enums.LogLevelEnum
import java.util.concurrent.ConcurrentHashMap

object ServiceContainerProvider {

    /**
     * Map of ServiceContainer instances keyed by AccountId_SdkKey
     * This allows multiple VWO accounts to maintain their own isolated service contexts
     */
    private val serviceContainers: ConcurrentHashMap<String, ServiceContainer> = ConcurrentHashMap()

    /**
     * Stores a ServiceContainer instance for a specific account
     * @param accountId The account ID (nullable)
     * @param sdkKey The SDK key
     * @param serviceContainer The ServiceContainer instance to store
     */
    fun storeServiceContainer(accountId: Int?, sdkKey: String?, serviceContainer: ServiceContainer) {
        val key = "${accountId ?: 0}_${sdkKey ?: ""}"
        serviceContainers[key] = serviceContainer
    }

    /**
     * Retrieves a ServiceContainer instance for a specific account
     * @param accountId The account ID (nullable)
     * @param sdkKey The SDK key
     * @return The ServiceContainer instance or null if not found
     */
    fun getServiceContainer(accountId: Int?, sdkKey: String?): ServiceContainer? {
        val key = "${accountId ?: 0}_${sdkKey ?: ""}"
        return serviceContainers[key]
    }

    /**
     * Removes a ServiceContainer instance for a specific account
     * @param accountId The account ID (nullable)
     * @param sdkKey The SDK key
     * @return The removed ServiceContainer instance or null if not found
     */
    fun removeServiceContainer(accountId: Int?, sdkKey: String?): ServiceContainer? {
        val key = "${accountId ?: 0}_${sdkKey ?: ""}"
        return serviceContainers.remove(key)
    }

    /**
     * Retrieves a ServiceContainer instance by key directly
     * @param key The key in format "AccountId_SdkKey"
     * @return The ServiceContainer instance or null if not found
     */
    fun getServiceContainerByKey(key: String): ServiceContainer? {
        return serviceContainers[key]
    }

    /**
     * Gets all stored ServiceContainer instances
     * @return Map of all ServiceContainer instances keyed by AccountId_SdkKey
     */
    fun getAllServiceContainers(): Map<String, ServiceContainer> {
        return serviceContainers.toMap()
    }

    /**
     * Clears all stored ServiceContainer instances
     * Useful for cleanup when shutting down the SDK
     */
    fun clearAllServiceContainers() {
        serviceContainers.clear()
    }

    /**
     * Creates or retrieves a ServiceContainer instance with the current settings and options
     * Following Java SDK pattern where ServiceContainer is created per API call
     * @param builder VWOBuilder instance containing services
     * @param settings Settings data (nullable)
     * @param initOptions VWO initialization options
     * @return ServiceContainer instance
     */
    fun createServiceContainer(
        builder: VWOBuilder, 
        settings: Settings?, 
        initOptions: VWOInitOptions
    ): ServiceContainer {
        val settingsManager = builder.getSettingsManager()
        val accountId = settingsManager?.accountId ?: initOptions.accountId ?: 0
        val sdkKey = settingsManager?.sdkKey ?: initOptions.sdkKey ?: ""
        
        // Check for existing ServiceContainer
        val existingServiceContainer = getServiceContainer(accountId, sdkKey)
        
        if (existingServiceContainer != null) {
            // Update existing ServiceContainer with any new non-null values
            updateServiceContainerIfNeeded(
                existingServiceContainer,
                builder,
                settings,
                initOptions
            )
            settingsManager?.serviceContainer = existingServiceContainer
            return existingServiceContainer
        }

        // Create new ServiceContainer
        val serviceContainer = ServiceContainer(
            settingsManager = settingsManager,
            options = initOptions,
            settings = settings,
            loggerService = builder.getLoggerService()
        )

        // Store the ServiceContainer for multi-account support
        storeServiceContainer(accountId, sdkKey, serviceContainer)
        serviceContainer.storage = builder.storage

        return serviceContainer
    }

    /**
     * Updates existing ServiceContainer variables if any value is null and a non-null value is now available
     * @param existingServiceContainer The existing ServiceContainer to potentially update
     * @param vwoBuilder The VWOBuilder instance containing potentially updated values
     * @param settings Settings data (nullable)
     * @param initOptions VWO initialization options (nullable)
     */
    private fun updateServiceContainerIfNeeded(
        existingServiceContainer: ServiceContainer,
        vwoBuilder: VWOBuilder,
        settings: Settings?,
        initOptions: VWOInitOptions?
    ) {
        var hasUpdates = false

        // Check and update Settings if it was null but is now available
        if (existingServiceContainer.getSettings() == null && settings != null) {
            existingServiceContainer.setSettings(settings)
            hasUpdates = true
        }

        // Check if SettingsManager needs serviceContainer reference
        val existingSettingsManager = existingServiceContainer.getSettingsManager()
        val newSettingsManager = vwoBuilder.getSettingsManager()
        
        if (existingSettingsManager == null && newSettingsManager != null) {
            existingServiceContainer.setSettingsManager(newSettingsManager)
            hasUpdates = true
        }

        if (existingSettingsManager?.serviceContainer == null) {
            existingSettingsManager?.serviceContainer = existingServiceContainer
            hasUpdates = true
        }

        // Check LoggerService
        val loggerService = existingServiceContainer.getLoggerService()
        if (loggerService == null && vwoBuilder.getLoggerService() != null) {
            existingServiceContainer.setLoggerService(vwoBuilder.getLoggerService()!!)
            hasUpdates = true
        }

        // Check Storage - handle null or uninitialized storage
        val currentStorage = existingServiceContainer.storage
        if (currentStorage == null || currentStorage.getConnector() == null) {
            existingServiceContainer.storage = vwoBuilder.storage
            hasUpdates = true
        }

        // Log overall update status
        if (hasUpdates) {
            val accountId = existingSettingsManager?.accountId ?: initOptions?.accountId ?: 0
            val sdkKey = existingSettingsManager?.sdkKey ?: initOptions?.sdkKey ?: ""
            
            existingServiceContainer.getLoggerService()?.log(
                LogLevelEnum.INFO,
                "ServiceContainer updated with new values",
                mapOf(
                    "accountId" to accountId.toString(),
                    "sdkKey" to sdkKey
                )
            )
        }
    }

    /**
     * Creates or gets a ServiceContainer for a specific account
     * Convenience method for direct account-based access
     * @param accountId The account ID
     * @param sdkKey The SDK key
     * @param factory Lambda function to create ServiceContainer if not found
     * @return ServiceContainer instance
     */
    fun getOrCreateServiceContainer(
        accountId: Int?,
        sdkKey: String?,
        factory: () -> ServiceContainer
    ): ServiceContainer {
        return getServiceContainer(accountId, sdkKey) ?: run {
            val serviceContainer = factory()
            storeServiceContainer(accountId, sdkKey, serviceContainer)
            serviceContainer
        }
    }
}