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
package com.vwo.models.request.EventArchQueryParams

/**
 * Represents query parameters for event batch requests.*
 * This class encapsulates the query parameters required for making event batch requests.
 * It provides a convenient way to construct and access these parameters.
 *
 * @property sdkKey The SDK key.
 * @property accountId The account ID.
 */
class EventBatchQueryParams(private val sdkKey: String, private val accountId: String) {
    /**
     * A map containing the query parameters.
     *
     * This map stores the query parameters as key-value pairs.
     * The keys are the parameter names, and the values are the corresponding parameter values.
     *
     * The following parameters are included:
     * - `i`: The SDK key.
     * - `env`: The SDK key (used for environment identification).
     * - `a`: The account ID.
     */
    val queryParams = mutableMapOf<String, String>().apply {
        this["i"] = sdkKey
        this["env"] = sdkKey
        this["a"] = accountId
    }
}
