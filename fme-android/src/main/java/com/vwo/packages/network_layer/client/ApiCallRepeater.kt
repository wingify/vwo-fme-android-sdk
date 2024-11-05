/*
 * Copyright (c) 2024 Wingify Software Pvt. Ltd.
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

package com.vwo.packages.network_layer.client

import com.google.gson.Gson
import com.vwo.providers.StorageProvider
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.services.LoggerService

/**
 * An object responsible for repeating failed API calls.
 *
 * This object provides a mechanism to retry failed API requests that have been
 * stored in a request store. It ensures that only one instance of the repeater
 * is running at a time and allows for execution on a new thread if desired.
 */
object ApiCallRepeater {

    /**
     * A flag indicating whether the repeater is already running.
     */
    private var isAlreadyRunning = false

    /**
     * Starts the API call repeater.
     *
     * @param useNewThread A boolean flag indicating whether to execute the repeater
     * on a new thread. Defaults to `false`.
     * @return `true` if the repeater was started successfully, `false` otherwise.
     */
    fun start(useNewThread: Boolean = false): Boolean {

        if (isAlreadyRunning) return false

        isAlreadyRunning = true

        if (useNewThread)
            Thread { process() }.start()
        else
            process()

        return true
    }

    /**
     * Processes the API call repetition.
     *
     * This method executes all pending requests and resets the `isAlreadyRunning`
     * flag to `false` when finished.
     */
    private fun process() {
        executeAllRequests()
        isAlreadyRunning = false
    }

    /**
     * Executes all pending API requests.
     *
     * This method retrieves pending requests from the request store, iterates
     * through them, and attempts to re-execute them. If a request is successful,
     * it is removed from the store. A delay is introduced between requests to
     * avoid overwhelming the server.
     */
    private fun executeAllRequests() {
        try {
            val requests = StorageProvider.requestStore?.getPendingRequests()
            if (requests.isNullOrEmpty()) return

            for (index in requests.lastIndex downTo 0) {
                //This loop must be from last to first, to avoid IndexOutOfBounds exception
                val request = requests[index]
                val requestModel = Gson().fromJson(request, RequestModel::class.java)
                val response = NetworkManager.post(requestModel)
                if (response?.statusCode == 200) {
                    StorageProvider.requestStore?.removeRequest(index)
                }
                Thread.sleep(100)
            }
        } catch (e: Exception) {
            LoggerService.log(LogLevelEnum.DEBUG, e.toString())
        }
    }
}