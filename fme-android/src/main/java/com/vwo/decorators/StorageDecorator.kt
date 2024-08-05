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
package com.vwo.decorators

import com.vwo.interfaces.storage.IStorageDecorator
import com.vwo.models.Variation
import com.vwo.models.user.VWOContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.LoggerService.Companion.log
import com.vwo.services.StorageService

/**
 * Decorator for interacting with the storage service.
 *
 * This class provides methods for retrieving and storing data related to feature* experiments
 * and variations. It ensures data integrity by validating the input before storing it and logs
 * errors if any inconsistencies are found.
 */
class StorageDecorator : IStorageDecorator {

    /**
     * Retrieves feature data from storage for the given feature key and user context.
     *
     * @param featureKey The key of the feature to retrieve.
     * @param context The user context for which to retrieve the data.
     * @param storageService The storage service instance to use.
     * @return The feature data as a Map, or null if not found.
     */
    override fun getFeatureFromStorage(
        featureKey: String,
        context: VWOContext,
        storageService: StorageService
    ): Map<String, Any>? {
        return storageService.getDataInStorage(featureKey, context)
    }

    /**
     * Stores the provided data in the storage service.
     *
     * This method validates the data to ensure it contains the necessary information for
     * identifying the feature, user, and variation. If the data is invalid, it logs an error and
     * returns null. Otherwise, it stores the data and returns a new Variation object.
     *
     * @param data The data to store, containing feature key, user ID, and variation details.
     * @param storageService The storage service instance to use.
     * @return A new Variation object, or null if the data is invalid.
     */
    override fun setDataInStorage(
        data: Map<String, Any>,
        storageService: StorageService
    ): Variation? {
        val featureKey = data["featureKey"] as String?
        val userId = data["user"]?.toString()

        if (featureKey.isNullOrEmpty()) {
            log(LogLevelEnum.ERROR, "STORING_DATA_ERROR", object : HashMap<String?, String?>() {
                init {
                    put("key", "featureKey")
                }
            })
            return null
        }

        if (userId.isNullOrEmpty()) {
            log(LogLevelEnum.ERROR, "STORING_DATA_ERROR", object : HashMap<String?, String?>() {
                init {
                    put("key", "Context or Context.id")
                }
            })
            return null
        }

        val rolloutKey = data["rolloutKey"] as String?
        val experimentKey = data["experimentKey"] as String?
        val rolloutVariationId = data["rolloutVariationId"] as Int?
        val experimentVariationId = data["experimentVariationId"] as Int?

        if (rolloutKey != null && !rolloutKey.isEmpty() && experimentKey == null && rolloutVariationId == null) {
            log(LogLevelEnum.ERROR, "STORING_DATA_ERROR", object : HashMap<String?, String?>() {
                init {
                    put("key", "Variation:(rolloutKey, experimentKey or rolloutVariationId)")
                }
            })
            return null
        }

        if (!experimentKey.isNullOrEmpty() && experimentVariationId == null) {
            log(LogLevelEnum.ERROR, "STORING_DATA_ERROR", object : HashMap<String?, String?>() {
                init {
                    put("key", "Variation:(experimentKey or rolloutVariationId)")
                }
            })
            return null
        }

        storageService.setDataInStorage(data)

        return Variation() // Assuming you need to return a new VariationModel instance.
    }
}
