/**
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
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
package com.wingify.packages.network_layer.manager

import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.ServiceContainer
import com.wingify.constants.Constants.EVENT_BATCH_ENDPOINT
import com.wingify.enums.EventEnum
import com.wingify.enums.UrlEnum
import com.wingify.interfaces.networking.NetworkClientInterface
import com.wingify.packages.network_layer.client.NetworkClient
import com.wingify.packages.network_layer.handlers.RequestHandler
import com.wingify.packages.network_layer.models.GlobalRequestModel
import com.wingify.packages.network_layer.models.RequestModel
import com.wingify.packages.network_layer.models.ResponseModel
import com.wingify.providers.StorageProvider
import com.wingify.services.PeriodicDataUploader
import com.wingify.utils.GsonUtil
import com.wingify.utils.NetworkUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
     * Does not replace an already-attached custom client.
     */
    fun attachClient() {
        if (this.client == null) {
            this.client = NetworkClient()
        }
        if (this.config == null) {
            this.config = GlobalRequestModel(null, null, null, null)
        }
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
     * @param serviceContainer SDK service container for the network client and logging.
     * @return The ResponseModel containing the response data or null if an error occurs.
     */
    fun get(request: RequestModel, serviceContainer: ServiceContainer): ResponseModel? {
        try {
            val networkOptions = createRequest(request)
            return if (networkOptions == null) {
                null
            } else {
                client?.GET(request, serviceContainer)
            }
        } catch (error: Exception) {
            serviceContainer.getLoggerService()
                ?.log(LogLevelEnum.ERROR, "Error when creating get request, error: $error", null)
            return null
        }
    }

    /**
     * Synchronously sends a POST request to the server.
     * Collect and batch 200 responses are inspected for DaCDN panic/refresh control.
     *
     * @param request The RequestModel containing the URL, headers, and body of the POST request.
     * @param serviceContainer SDK service container for the client and panic control, or null.
     * @return The ResponseModel containing the response data or null if an error occurs.
     */
    fun post(request: RequestModel, serviceContainer: ServiceContainer?): ResponseModel? {
        try {
            val networkOptions = createRequest(request)
            return if (networkOptions == null) {
                null
            } else {
                val response = client?.POST(request, serviceContainer)
                applyDaCdnIfCollectOrBatch(request, response, serviceContainer)
                response
            }
        } catch (error: Exception) {
            val isNonReportableApi = request.path?.contains(EVENT_BATCH_ENDPOINT) == true
                    || request.path?.contains(EventEnum.VWO_ERROR.value) == true

            val logLevel = if (isNonReportableApi) LogLevelEnum.DEBUG else LogLevelEnum.ERROR
            serviceContainer?.getLoggerService()
                ?.log(logLevel, "Error when creating post request, error: $error", null)
            return null
        }
    }

    /**
     * Asynchronously sends a POST request to the server.
     * Skips the send while panic is latched. Online batching queues; otherwise POSTs immediately.
     *
     * @param request The RequestModel containing the URL, headers, and body of the POST request.
     * @param serviceContainer SDK service container for batching, panic, and logging.
     */
    fun postAsync(request: RequestModel, serviceContainer: ServiceContainer) {
        executorService.submit {
            if (serviceContainer.panicManager.isPanicActive()) {
                return@submit
            }
            val onlineBatchUploadManager = serviceContainer.onlineBatchUploadManager
            if (serviceContainer.getBatchManager()
                    .isOnlineBatchingAllowed(onlineBatchUploadManager)
            ) { // Online batching
                addToBatch(request, serviceContainer)
                val accountId = serviceContainer.getAccountId().toLong()
                val sdkKey = serviceContainer.getSdkKey()
                val count = StorageProvider.sdkDataManager?.getEventCount(accountId, sdkKey) ?: 0
                if (onlineBatchUploadManager.batchMinSize > 0 && count >= onlineBatchUploadManager.batchMinSize) {
                    CoroutineScope(Dispatchers.IO).launch {
                        serviceContainer.getBatchManager()
                            .start("Batch size count", serviceContainer)
                    }
                }
                StorageProvider.contextRef.get()?.let { PeriodicDataUploader().enqueue(it) }
            } else { // Offline batching
                val response = post(request, serviceContainer)
                if (StorageProvider.sdkDataManager == null) return@submit
                if (response == null || response.statusCode != 200) {
                    addToBatch(request, serviceContainer)
                    StorageProvider.contextRef.get()?.let { PeriodicDataUploader().enqueue(it) }
                }
            }
        }
    }

    /**
     * Applies DaCDN panic/refresh control from collect (`/events`) and batch 200 responses.
     * Other endpoints are ignored so impression traffic does not drive control.
     *
     * @param request The request whose path is checked for collect or batch.
     * @param response The HTTP response, or null on failure.
     * @param serviceContainer Container whose panic manager should receive control, or null.
     */
    private fun applyDaCdnIfCollectOrBatch(
        request: RequestModel,
        response: ResponseModel?,
        serviceContainer: ServiceContainer?,
    ) {
        if (serviceContainer == null || response?.statusCode != 200) return
        val path = request.path.orEmpty()
        if (!path.contains(UrlEnum.EVENTS.url) && !path.contains(EVENT_BATCH_ENDPOINT)) return
        NetworkUtil.applyDaCdnControlFromNetworkResponse(
            response.data,
            serviceContainer,
            response.statusCode
        )
    }

    /**
     * Adds a request to the offline batch for later uploading.
     *
     * This function serializes the request body using Gson and stores it in the database along
     * with the SDK key and account ID.
     * The stored request will be included in the next batch upload when the device is online.
     *
     * @param request The request to be added to the batch.
     * @param serviceContainer The service container containing the SDK key and account ID.
     */
    fun addToBatch(
        request: RequestModel,
        serviceContainer: ServiceContainer
    ) {
        val requestString = GsonUtil.gson.toJson(request.body).toString()
        StorageProvider.sdkDataManager?.saveSdkData(
            sdkKey = serviceContainer.getSdkKey(),
            accountId = serviceContainer.getAccountId(),
            payload = requestString
        )
    }
}
