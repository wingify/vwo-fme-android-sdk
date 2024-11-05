/*
 * Copyright (c) 2024 Wingify Software Pvt. Ltd.
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

package com.vwo.packages.storage

import android.content.Context

/**
 * A default implementation of the `LocalStorageController` interface.
 * This class provides methods for storing and retrieving data from the device's
 * local storage using SharedPreferences. It uses a default storage key to store
 * the data and provides methods for accessing and managing feature keys.
 */
internal class DefaultStorage(context: Context) : LocalStorageController(context) {

    /**
     * The default storage key used to store the data.
     */
    private val DEFAULT_STORAGE_STORE_KEY = "Storage"

    /**
     * Retrieves the feature key from the storage.
     *
     * @param featureKey The key used to identify the feature.
     * @return The feature key if found, or an empty string otherwise.
     */
    fun getFeatureKey(featureKey: String): String {
        return getString(getKey(featureKey))
    }

    /**
     * Saves the feature key to the storage.
     *
     * @param featureKey The key used to identify the feature.
     * @param feature The feature value to be stored.
     */
    fun saveFeatureKey(featureKey: String, feature: String) {
        saveString(getKey(featureKey), feature)
    }

    /**
     * Clears the feature key from the storage.
     *
     * @param featureKey The key used to identify the feature.
     */
    fun clearFeatureKey(featureKey: String) {
        clearData(getKey(featureKey))
    }

    /**
     * Generates the storage key for a given feature key.
     *
     * @param featureKey The key used to identify the feature.
     * @return The storage key.
     */
    private fun getKey(featureKey: String): String {
        return DEFAULT_STORAGE_STORE_KEY + featureKey
    }
}