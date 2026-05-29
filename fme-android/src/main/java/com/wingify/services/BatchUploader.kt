/*
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
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

package com.wingify.services

import com.wingify.ServiceContainer
import com.wingify.constants.Constants
import com.wingify.interfaces.networking.HttpMethods
import com.wingify.models.request.EventArchQueryParams.EventBatchQueryParams
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.packages.network_layer.manager.NetworkManager
import com.wingify.packages.network_layer.models.RequestModel

/**
 * A class responsible for uploading event batches to the server.
 *
 * This class handles the process of constructing and sending event batch requests to the server.
 * It uses the `NetworkManager` to make the network requests and logs any errors that occur.
 */
internal object BatchUploader {

    /**
     * The network timeout for requests, in milliseconds.
     */
    private const val NETWORK_TIMEOUT = Constants.SETTINGS_TIMEOUT.toInt()

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
    fun uploadBatch(accountId: Long, sdkKey: String, payload: List<MutableMap<*, *>>, serviceContainer: ServiceContainer?): Boolean {
        val body = mutableMapOf("ev" to payload)

        val options = EventBatchQueryParams(sdkKey = sdkKey, accountId = accountId.toString()).queryParams
        val header = mutableMapOf("Authorization" to sdkKey)
        val request = RequestModel(
            url = serviceContainer?.resolveBatchUploadHost() ?: Constants.HOST_NAME,
            method = HttpMethods.POST.value,
            path = Constants.EVENT_BATCH_ENDPOINT,
            query = options,
            body = body,
            headers = header,
            scheme = serviceContainer?.resolveBatchUploadScheme() ?: Constants.HTTPS_PROTOCOL,
            port = 0
        )
        request.timeout = NETWORK_TIMEOUT
        val response = NetworkManager.post(request, serviceContainer)
        if (response?.statusCode != 200) {
            serviceContainer?.getLoggerService()?.log(
                LogLevelEnum.INFO, "BATCH_UPLOAD_ERROR",
                mutableMapOf("err" to response?.error.toString())
            )
            return false
        }
        return true
    }

    private fun getFinalUrl(serviceContainer: ServiceContainer?): String {
        val collectionPrefix = serviceContainer?.getSettings()?.collectionPrefix
        if (!collectionPrefix.isNullOrEmpty()) {
            return "${Constants.HOST_NAME}/$collectionPrefix"
        }
        return Constants.HOST_NAME
    }
}