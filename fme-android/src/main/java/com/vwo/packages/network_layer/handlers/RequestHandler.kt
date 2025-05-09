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
package com.vwo.packages.network_layer.handlers

import com.vwo.packages.network_layer.models.GlobalRequestModel
import com.vwo.packages.network_layer.models.RequestModel

/**
 * Handles the creation and modification of network requests.
 *
 * This class is responsible for creating and updating request models by merging properties from a
 * base request and a global configuration.
 */
class RequestHandler {
    /**
     * Creates a new request by merging properties from a base request and a configuration model.
     * If both the request URL and the base URL from the configuration are missing, it returns null.
     * Otherwise, it merges the properties from the configuration into the request if they are not already set.
     *
     * @param request The initial request model.
     * @param config The global request configuration model.
     * @return The merged request model or null if both URLs are missing.
     */
    fun createRequest(request: RequestModel, config: GlobalRequestModel): RequestModel? {
        // Check if both the request URL and the configuration base URL are missing
        if (config.baseUrl.isNullOrEmpty() && request.url.isNullOrEmpty()) {
            return null // Return null if no URL is specified
        }

        // Set the request URL, defaulting to the configuration base URL if not set
        if (request.url.isNullOrEmpty()) {
            request.url = config.baseUrl
        }

        // Set the request timeout, defaulting to the configuration timeout if not set
        if (request.timeout == -1) {
            request.timeout = config.timeout
        }

        // Set the request body, defaulting to the configuration body if not set
        if (request.body == null) {
            request.body = config.body
        }

        // Set the request headers, defaulting to the configuration headers if not set
        if (request.headers == null) {
            request.headers = config.headers?: mutableMapOf()
        }

        // Initialize request query parameters, defaulting to an empty map if not set
        var requestQueryParams = request.query
        if (requestQueryParams == null) {
            requestQueryParams = HashMap()
        }

        // Initialize configuration query parameters, defaulting to an empty map if not set
        var configQueryParams = config.query
        if (configQueryParams == null) {
            configQueryParams = HashMap()
        }

        // Merge configuration query parameters into the request query parameters if they don't exist
        for ((key, value) in configQueryParams) {
            (value as? String)?.let {
                if (!requestQueryParams.containsKey(key)) {
                    requestQueryParams[key] = it
                }
            }
        }


        // Set the merged query parameters back to the request
        request.query = requestQueryParams

        return request // Return the modified request
    }
}
