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
package com.vwo.utils

import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.services.SettingsManager
import com.vwo.services.UrlService.baseUrl
import java.net.URLEncoder

/**
 * Utility object for gateway service operations.
 *
 * This object provides helper methods for interacting with gateway services, such as sending
 * requests, handling responses, or managing connections.
 */
object GatewayServiceUtil {
    /**
     * Fetches data from the gateway service
     * @param queryParams The query parameters to send with the request
     * @param endpoint The endpoint to send the request to
     * @return The response data from the gateway service
     */
    fun getFromGatewayService(
        queryParams: MutableMap<String, String>,
        endpoint: String,
        expectedResponseType: String = "application/json"
    ): String? {
        var responseString: String? = null
        try {
            val request = RequestModel(
                baseUrl,
                "GET",
                endpoint,
                queryParams,
                null,
                null,
                SettingsManager.instance?.protocol,
                SettingsManager.instance?.port ?: 0,
                expectedResponseType,
            )
            val response = NetworkManager.get(request)

            responseString = response?.data
        } catch (e: Exception) {
            responseString = null
        }
        return responseString
    }

    /**
     * Encodes the query parameters to ensure they are URL-safe
     * @param queryParams The query parameters to encode
     * @return The encoded query parameters
     */
    fun getQueryParams(queryParams: Map<String, String?>): MutableMap<String, String> {
        val encodedParams: MutableMap<String, String> = HashMap()

        for ((key, value) in queryParams) {
            // Encode the parameter value to ensure it is URL-safe
            val encodedValue = URLEncoder.encode(value)
            // Add the encoded parameter to the result map
            encodedParams[key] = encodedValue
        }

        return encodedParams
    }
}