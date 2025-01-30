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
package com.vwo.packages.network_layer.manager

import com.google.gson.Gson
import com.vwo.interfaces.networking.NetworkClientInterface
import com.vwo.models.Settings
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.client.NetworkClient
import com.vwo.packages.network_layer.handlers.RequestHandler
import com.vwo.packages.network_layer.models.GlobalRequestModel
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.packages.network_layer.models.ResponseModel
import com.vwo.providers.StorageProvider
import com.vwo.services.LoggerService
import com.vwo.services.PeriodicDataUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * Manages network requests and client configuration.
 *
 * This object provides methods for attaching a network client, configuring global request settings,
 * and sending synchronous and asynchronous network requests (GET and POST).
 */
object NetworkManager {

    var config: GlobalRequestModel? = null
    var client: NetworkClientInterface? = null

    // Executors.newCachedThreadPool() is a factory method in the Java Executors class
    // that creates a thread pool that can dynamically adjust the number of threads it uses.
    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    /**
     * Attaches a network client with a custom configuration.
     *
     * @param client The network client to attach.
     */
    fun attachClient(client: NetworkClientInterface?) {
        this.client = client
        this.config = GlobalRequestModel(null, null, null, null) // Initialize with default config
    }

    /**
     * Attaches a default network client with a default configuration.
     */
    fun attachClient() {
        this.client = NetworkClient()
        this.config = GlobalRequestModel(null, null, null, null) // Initialize with default config
    }

    /**
     * Creates a request model by merging the provided request with the global configuration.
     *
     * @param request The request model to merge.
     * @return The merged request model or null if no URL is specified.
     */
    private fun createRequest(request: RequestModel): RequestModel? {
        val handler = RequestHandler()
        return this.config?.let { handler.createRequest(request, it) } // Merge and create request
    }

    /**
     * Synchronously sends a GET request to the server.
     *
     * @param request The RequestModel containing the URL, headers, and query parameters of the GET request.
     * @return The ResponseModel containing the response data or null if an error occurs.
     */
    fun get(request: RequestModel): ResponseModel? {
        try {
            val networkOptions = createRequest(request)
            return if (networkOptions == null) {
                null
            } else {
                client?.GET(request)
            }
        } catch (error: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "Error when creating get request, error: $error")
            return null
        }
    }

    /**
     * Synchronously sends a POST request to the server.
     *
     * @param request The RequestModel containing the URL, headers, and body of the POST request.
     * @return The ResponseModel containing the response data or null if an error occurs.
     */
    fun post(request: RequestModel): ResponseModel? {
        try {
            val networkOptions = createRequest(request)
            return if (networkOptions == null) {
                null
            } else {
                client?.POST(request)
            }
        } catch (error: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "Error when creating post request, error: $error")
            return null
        }
    }

    /**
     * Asynchronously sends a POST request to the server.
     *
     * @param request The RequestModel containing the URL, headers, and body of the POST request.
     */
    fun postAsync(settings: Settings, request: RequestModel) {
        executorService.submit {
            if (BatchManager.isOnlineBatchingAllowed()) { // Online batching
                addToBatch(request, settings)
                val count = StorageProvider.sdkDataManager?.getEntryCount() ?: 0
                if (OnlineBatchUploadManager.batchMinSize > 0 && count >= OnlineBatchUploadManager.batchMinSize) {
                    CoroutineScope(Dispatchers.IO).launch {
                        BatchManager.start("Batch size count")
                    }
                }
                StorageProvider.contextRef.get()?.let { PeriodicDataUploader().enqueue(it) }
            } else { // Offline batching
                val response = post(request)
                if (StorageProvider.sdkDataManager == null) return@submit

                if (response == null || response.statusCode != 200) {
                    addToBatch(request, settings)
                    StorageProvider.contextRef.get()?.let { PeriodicDataUploader().enqueue(it) }
                }
            }
        }
    }

    /**
     * Adds a request to the offline batch for later uploading.
     *
     * This function serializes the request body using Gson and stores it in the database along
     * with the SDK key and account ID.
     * The stored request will be included in the next batch upload when the device is online.
     *
     * @param request The request to be added to the batch.
     * @param settings The settings containing the SDK key and account ID.
     */
    private fun addToBatch(
        request: RequestModel,
        settings: Settings
    ) {
        val requestString = Gson().toJson(request.body).toString()
        StorageProvider.sdkDataManager?.saveSdkData(
            sdkKey = settings.sdkKey,
            accountId = settings.accountId,
            payload = requestString
        )
    }
}
