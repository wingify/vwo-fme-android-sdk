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
package com.wingify.packages.storage

/**
 * Abstract connector for SDK data storage and retrieval.
 *
 * Subclasses implement [set] and [get] for a specific storage backend.
 */
abstract class Connector {
    /**
     * Sets data for a given key.
     */
    @Throws(Exception::class)
    abstract fun set(data: Map<String, Any>)

    /**
     * Retrieves data for a given feature key and user ID.
     */
    @Throws(Exception::class)
    abstract fun get(featureKey: String?, userId: String?): Any?
}
