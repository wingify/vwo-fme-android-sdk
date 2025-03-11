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

import android.content.Context

/**
 * A class for managing the storage and retrieval of gateway response data.
 *
 * This class uses local storage to persist gateway response data and provides methods
 * for getting, saving, and clearing the response. It also manages the expiry time
 * of the response data.
 */
internal class GatewayResponseStore(context: Context) : LocalStorageController(context) {

    /**
     * The key used to store the gateway response data in local storage.
     */
    private val GATEWAY_RESPONSE_KEY = "gatewayResponseKey"

    /**
     * The key used to store the gateway response expiry time in local storage.
     */
    private val GATEWAY_RESPONSE_EXPIRY_KEY = "gatewayResponseExpiry"

    /**
     * Retrieves the gateway response data from local storage.
     *
     * @return The gateway response data as a string.
     */
    fun getGatewayResponse(): String {
        return getString(GATEWAY_RESPONSE_KEY)
    }

    /**
     * Saves the gateway response data to local storage.
     *
     * @param gatewayResponse The gateway response data to be saved.
     */
    fun saveGatewayResponse(gatewayResponse: String) {
        saveString(GATEWAY_RESPONSE_KEY, gatewayResponse)
    }

    /**
     * Clears the gateway response data from local storage.
     */
    fun clearGatewayResponse() {
        clearData(GATEWAY_RESPONSE_KEY)
    }

    /**
     * Retrieves the gateway response expiry time from local storage.
     *
     * @return The gateway response expiry time as a long value.
     */
    fun getGatewayResponseExpiry(): Long {
        return getLong(GATEWAY_RESPONSE_EXPIRY_KEY)
    }

    /**
     * Saves the gateway response expiry time to local storage.
     *
     * @param expiryTime The gateway response expiry time to be saved.
     */
    fun saveGatewayResponseExpiry(expiryTime: Long) {
        saveLong(GATEWAY_RESPONSE_EXPIRY_KEY, expiryTime)
    }

    /**
     * Clears the gateway response expiry time from local storage.
     */
    fun clearGatewayResponseExpiry() {
        clearData(GATEWAY_RESPONSE_EXPIRY_KEY)
    }

    /**
     * Generates a storage key for attribute check based on feature key, user ID, list ID, key DSL, and attribute.
     */
    fun getStorageKeyForAttributeCheck(
        featureKey: String,
        listId: String,
        attribute: String,
        userId: String,
        isCustomVariable: Boolean
    ): String {
        val keyDsl = if (isCustomVariable) "customVariable" else "vwoUserId"
        val storageKey = "${featureKey}_${userId}_${listId}_${keyDsl}_${attribute}"
        return storageKey
    }
}