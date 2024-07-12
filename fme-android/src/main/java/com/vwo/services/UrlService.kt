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
package com.vwo.services

object UrlService {
    private var collectionPrefix: String? = null

    /**
     * Initializes the UrlService with the collectionPrefix and gatewayService
     * @param collectionPrefix  collectionPrefix to be used in the URL
     */
    @JvmStatic
    fun init(collectionPrefix: String?) {
        if (collectionPrefix != null && !collectionPrefix.isEmpty()) {
            UrlService.collectionPrefix = collectionPrefix
        }
    }

    @JvmStatic
    val baseUrl: String?
        /**
         * Returns the base URL for the API requests
         */
        get() {
            val baseUrl: String = SettingsManager.Companion.getInstance().hostname

            if (SettingsManager.Companion.getInstance().isGatewayServiceProvided) {
                return baseUrl
            }

            // Construct URL with collectionPrefix if it exists
            if (collectionPrefix != null && !collectionPrefix!!.isEmpty()) {
                return baseUrl + "/" + collectionPrefix
            }

            return baseUrl
        }
}