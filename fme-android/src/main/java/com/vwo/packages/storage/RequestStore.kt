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
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.LoggerService.Companion.log

/**
 * A class for managing pending API requests.
 *
 * This class uses local storage to persist pending requests and provides methods
 * for adding, retrieving, removing, and clearing requests. It utilizes Gson
 * for JSON serialization and deserialization.
 */
internal class RequestStore(context: Context) : LocalStorageController(context) {

    /**
     * The key used to store pending requests in local storage.
     */
    private val KEY = "PendingRequests"

    /**
     * A mutable list to store pending requests.
     */
    private val pendingRequests = mutableListOf<String>()

    /**
     * Retrieves the list of pending requests.
     *
     * This method retrieves pending requests from local storage and populates
     * the `pendingRequests` list. If the list is already populated, it is
     * returned directly. If there are no pending requests in local storage,
     * an empty list is returned.
     * @return The list of pending requests.
     */
    fun getPendingRequests(): List<String> {
        if (pendingRequests.isNotEmpty()) return pendingRequests

        val stringArray = getString(KEY)
        if (stringArray.isEmpty()) return pendingRequests
        try {
            val myType = object : TypeToken<List<String>>() {}.type
            val requests = Gson().fromJson<List<String>>(stringArray, myType)
            pendingRequests.addAll(requests)
        } catch (e: JsonSyntaxException) {
            val map = mutableMapOf<String?, String?>()
            map["err"] = e.message
            log(LogLevelEnum.ERROR, "PARSING_ERROR", map)
        }
        return pendingRequests
    }

    /**
     * Adds a request to the list of pending requests.
     *
     * @param request The request to be added.
     */
    fun addRequest(request: String) {
        pendingRequests.add(request)
        saveAllRequests()
    }

    /**
     * Saves all pending requests to local storage.
     *
     * This method serializes the `pendingRequests`list to JSON using Gson
     * and saves it to local storage using the `KEY`.
     */
    private fun saveAllRequests() {
        saveString(key = KEY, Gson().toJson(pendingRequests))
    }

    /**
     * Clears all pending requests from local storage.
     */
    fun clearSettings() {
        clearData(KEY)
    }

    /**
     * Removes a request from the list of pending requests.
     *
     * @param index The index of the request to be removed.
     */
    fun removeRequest(index: Int) {
        pendingRequests.removeAt(index)
        saveAllRequests()
    }
}