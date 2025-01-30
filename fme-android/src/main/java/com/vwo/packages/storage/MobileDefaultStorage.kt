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
package com.vwo.packages.storage

import com.vwo.providers.StorageProvider.contextRef
import com.vwo.utils.toMap
import org.json.JSONObject

/**
 * A class that provides a default implementation for storing and retrieving data* using the device's local storage.
 *
 * This class implements the `Connector` interface and uses a `DefaultStorage`
 * instance to interact with the local storage. It provides methods for setting
 * and getting data associated with feature keys and user IDs.
 */
class MobileDefaultStorage :Connector() {

    /**
     * The instance of `DefaultStorage` used for local storage operations.
     */
    private var storage: DefaultStorage? = null

    /**
     * Initializes the `MobileDefaultStorage` instance.
     *
     * This method retrieves the application context and initializes the
     * `storage` instance using the context.
     */
    fun init() {
        val context = contextRef.get()
        context?.let { storage = DefaultStorage(context) }
    }

    /**
     * Stores data associated with a feature key and userID.
     *
     * @param data A map containing the data to be stored. The map should
     * contain the following keys:
     * - "featureKey": The feature key for the data.
     * - "user": The user ID for the data.
     * - "rolloutKey": The rollout key for the data (optional).
     * - "rolloutVariationId": The rollout variation ID for the data (optional).
     * - "experimentKey": The experiment key for the data (optional).
     * - "experimentVariationId": The experiment variation ID for the data (optional).
     * - "rolloutId": The rollout ID for the data (optional).
     * - "experimentId": The experiment ID for the data (optional).
     */
    override fun set(data: Map<String, Any>) {
        if (storage == null) return

        val key = data["featureKey"].toString() + "_" + data["userId"]

        // Create a map to store the data
        val value: MutableMap<String, Any?> = HashMap()
        data["rolloutKey"]?.let { value["rolloutKey"] = it }
        data["rolloutVariationId"]?.let { value["rolloutVariationId"] = it }
        data["experimentKey"]?.let { value["experimentKey"] = it }
        data["experimentVariationId"]?.let { value["experimentVariationId"] = it }

        data["rolloutId"]?.let { value["rolloutId"] = it }
        data["experimentId"]?.let { value["experimentId"] = it }

        val jsonValue = JSONObject(value)
        // Store the value in the storage
        storage?.saveFeatureKey(key, jsonValue.toString())
    }

    /**
     * Retrieves data associated with a feature key and user ID.
     *
     * @param featureKey The feature key for the data.
     * @param userId The user ID for the data.
     * @return The data if found, or null otherwise.
     */
    override fun get(featureKey: String?, userId: String?): Any? {
        val key = featureKey + "_" + userId

        val stringValue = storage?.getFeatureKey(key)
        // Check if the key exists in the storage
        if (stringValue.isNullOrEmpty()) {
            return null
        }
        return JSONObject(stringValue).toMap()
    }
}
