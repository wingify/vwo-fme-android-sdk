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
package com.vwo.interfaces.storage

import com.vwo.models.Variation
import com.vwo.models.user.VWOUserContext
import com.vwo.services.StorageService

/**
 * Interface for decorating storage operations.
 *
 * This interface defines methods for interacting with a storage service, providing
 * a layer of abstraction for storing and retrieving data related to features.
 */
interface IStorageDecorator {
    /**
     * Sets data in storage.
     * @param data The data to be stored.
     * @param storageService The storage service instance.
     * @return The stored VariationModel.
     */
    fun setDataInStorage(data: Map<String, Any>, storageService: StorageService): Variation?

    /**
     * Retrieves a feature from storage.
     * @param featureKey The key of the feature to retrieve.
     * @param context The context model.
     * @param storageService The storage service instance.
     * @return The retrieved feature or relevant status.
     */
    fun getFeatureFromStorage(
        featureKey: String,
        context: VWOUserContext,
        storageService: StorageService
    ): Map<String, Any>?
}

