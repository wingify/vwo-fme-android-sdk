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
package com.vwo.packages.storage

/**
 * An abstract class representing a connector for data storage and retrieval.
 *
 * This class defines the basic interface for connectors that interact with data storage mechanisms.
 * Subclasses must implement the `set` and `get` methods to provide concrete implementations for
 * specific data stores.
 */
abstract class Connector {
    /**
     * Sets data for a given key.
     *
     * @param data A map containing the data to be set.
     * @throws Exception if an error occurs during the set operation.
     */
    @Throws(Exception::class)
    abstract fun set(data: Map<String, Any>)

    /**
     * Retrieves data for a given feature key and user ID.
     *
     * @param featureKey The key of the feature for which to retrieve data.
     * @param userId The ID of the user for which to retrieve data.
     * @return The retrieved data or null if no data is found.
     * @throws Exception if an error occurs during the get operation.*/
    @Throws(Exception::class)
    abstract fun get(featureKey: String?, userId: String?): Any?
}