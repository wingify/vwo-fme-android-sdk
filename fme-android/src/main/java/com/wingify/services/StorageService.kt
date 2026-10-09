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
package com.wingify.services

import com.wingify.ServiceContainer
import com.wingify.constants.Constants
import com.wingify.enums.ApiEnum
import com.wingify.models.Feature
import com.wingify.models.user.WingifyUserContext
import com.wingify.packages.storage.Connector
import com.wingify.utils.KeyedLockUtil

/**
 * Provides data storage and retrieval services.
 *
 * This class is responsible for managing the storage and retrieval of data used by the application,
 * such as user preferences, application state, or other persistent information.
 */
class StorageService(private val serviceContainer: ServiceContainer? = null) {

    /**
     * Retrieves data from storage based on the feature key and user ID.
     * @param featureKey The key to identify the feature data.
     * @param context The context model containing at least an ID.
     * @return The data retrieved or an error/storage status enum.
     */
    fun getDataInStorage(featureKey: String?, context: WingifyUserContext): Map<String, Any>? {
        val storageInstance = serviceContainer?.storage?.getConnector() ?: return null
        try {
            return (storageInstance as Connector).get(featureKey, context.id) as? Map<String, Any>
        } catch (e: Exception) {
            LoggerService.errorLog(
                key = "ERROR_READING_STORED_DATA_IN_STORAGE",
                data = mapOf(Constants.ERR to e.toString()),
                debugData = mapOf(
                    "an" to ApiEnum.GET_FLAG.value,
                    "uuid" to context.getUuid(serviceContainer = serviceContainer)
                ),
                shouldSendToVWO = true,
                serviceContainer = serviceContainer
            )
            return null
        }
    }

    /**
     * Stores data in the storage.
     * @param data The data to be stored as a map.
     * @return true if data is successfully stored, otherwise false.
     */
    fun setDataInStorage(data: Map<String, Any>): Boolean {
        val featureKey = data["featureKey"]?.toString() ?: return false
        return KeyedLockUtil.withLock(featureKey) {
            val storageInstance =
                serviceContainer?.storage?.getConnector() ?: return@withLock false
            try {
                (storageInstance as Connector).set(data)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun updateDataInStorage(
        feature: Feature?,
        context: WingifyUserContext,
        data: Map<String, Any>
    ): Boolean {
        if (feature == null) return false
        val featureKey = feature.key ?: return false

        return KeyedLockUtil.withLock(featureKey) {
            val existingData =
                getDataInStorage(featureKey = featureKey, context = context)?.toMutableMap()
                    ?: return@withLock false

            existingData["featureKey"] = featureKey
            existingData["userId"] = "${context.id}"

            data.keys.forEach { key ->
                val value = data[key]
                if (value != null) existingData[key] = value
            }

            val storageInstance =
                serviceContainer?.storage?.getConnector() ?: return@withLock false
            try {
                (storageInstance as Connector).set(existingData)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
