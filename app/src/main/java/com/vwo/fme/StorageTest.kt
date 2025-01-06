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
package com.vwo.fme

import com.vwo.packages.storage.Connector

/**
 * A class for storing and retrieving data from a storage.
 */
class StorageTest : Connector() {
    /**
     * A In-memory mutable map to store the data.
     * The keys are strings representing a combination of feature key and user ID.
     * The values are maps containing the data for each key.
     */
    private val storage: MutableMap<String, Map<String, Any?>> = HashMap()

    /**
     * Stores the data in the storage.
     *
     * @param data A map containing the data to be stored.
     * The map should contain the following keys:
     * - "featureKey": The feature key for the data.
     * - "user": The user ID for the data.
     * - "rolloutKey": The rollout key for the data.
     * - "rolloutVariationId": The rollout variation ID for the data.
     * - "experimentKey": The experimentkey for the data.
     * - "experimentVariationId": The experiment variation ID for the data.
     */
    override fun set(data: Map<String, Any>) {
        val key = data["featureKey"].toString() + "_" + data["userId"]

        // Create a map to store the data
        val value: MutableMap<String, Any?> = HashMap()
        value["rolloutKey"] = data["rolloutKey"]
        value["rolloutVariationId"] = data["rolloutVariationId"]
        value["experimentKey"] = data["experimentKey"]
        value["experimentVariationId"] = data["experimentVariationId"]
        value["rolloutId"] = data["rolloutId"]
        value["experimentId"] = data["experimentId"]

        // Store the value in the storage
        storage[key] = value
    }

    /**
     * Retrieves the data from the storage.
     *
     * @param featureKey The feature key for the data.
     * @param userId The user ID for the data.
     * @return The data if found, or null otherwise.
     */
    override fun get(featureKey: String?, userId: String?): Any? {
        val key = featureKey + "_" + userId

        // Check if the key exists in the storage
        if (storage.containsKey(key)) {
            return storage[key]
        }
        return null
    }
}
