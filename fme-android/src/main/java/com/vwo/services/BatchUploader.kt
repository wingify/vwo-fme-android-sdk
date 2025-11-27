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

package com.vwo.services

import com.vwo.constants.Constants
import com.vwo.models.request.EventArchQueryParams.EventBatchQueryParams
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel

/**
 * A class responsible for uploading event batches to theserver.
 *
 * This class handles the process of constructing and sending event batch requests to the server.
 * It uses the `NetworkManager` to make the network requests and logs any errors that occur.
 */
internal class BatchUploader {

    /**
     * The network timeout for requests, in milliseconds.
     */
    private val networkTimeout = Constants.SETTINGS_TIMEOUT.toInt()
    /**
     * The hostname of the server to which requests are sent.
     */
    private val hostname = Constants.HOST_NAME
    /**
     * The protocol used for requests (e.g., "https").
     */
    private val protocol: String = Constants.HTTPS_PROTOCOL
    /**
     * The port number used for requests.
     */
    private val port = 0

    /**
     * Uploads an event batch to the server.
     * This function constructs an event batch request and sends it to the server using the `NetworkManager`.
     * It logs any errors that occur during the upload process.
     *
     * @param accountId The account ID associated with the event batch.
     * @param sdkKey The SDK key associated with the event batch.
     * @param payload The list of event data to be uploaded.
     * @return `true` if the event batch was uploaded successfully, `false` otherwise.
     */
    fun uploadBatch(accountId: Long, sdkKey: String, payload: List<MutableMap<*, *>>): Boolean {
        val body = mutableMapOf("ev" to payload)

        val options = EventBatchQueryParams(sdkKey = sdkKey, accountId = accountId.toString()).queryParams
        val header = mutableMapOf("Authorization" to sdkKey)
        val request = RequestModel(
            hostname,
            "POST",
            Constants.EVENT_BATCH_ENDPOINT,
            options,
            body,
            header,
            protocol,
            port
        )
        request.timeout = networkTimeout
        val response = NetworkManager.post(request)
        if (response?.statusCode != 200) {
            LoggerService.log(
                LogLevelEnum.ERROR, "BATCH_UPLOAD_ERROR",
                mutableMapOf(Constants.ERR to response?.error.toString())
            )
            return false
        }
        return true
    }
}