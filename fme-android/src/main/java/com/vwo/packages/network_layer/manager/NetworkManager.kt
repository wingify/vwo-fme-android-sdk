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
package com.vwo.packages.network_layer.manager

import com.vwo.interfaces.networking.NetworkClientInterface
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.client.NetworkClient
import com.vwo.packages.network_layer.handlers.RequestHandler
import com.vwo.packages.network_layer.models.GlobalRequestModel
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.packages.network_layer.models.ResponseModel
import com.vwo.services.LoggerService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


object NetworkManager {

    var config: GlobalRequestModel? = null
    private var client: NetworkClientInterface? = null

    // Executors.newCachedThreadPool() is a factory method in the Java Executors class
    // that creates a thread pool that can dynamically adjust the number of threads it uses.
    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    fun attachClient(client: NetworkClientInterface?) {
        this.client = client
        this.config = GlobalRequestModel(null, null, null, null) // Initialize with default config
    }

    fun attachClient() {
        this.client = NetworkClient()
        this.config = GlobalRequestModel(null, null, null, null) // Initialize with default config
    }

    private fun createRequest(request: RequestModel): RequestModel? {
        val handler = RequestHandler()
        return this.config?.let { handler.createRequest(request, it) } // Merge and create request
    }

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
     * @param request - The RequestModel containing the URL, headers, and body of the POST request.
     * @return
     */
    fun post(request: RequestModel): ResponseModel? {
        try {
            val networkOptions = createRequest(request)
            return if (networkOptions == null) {
                null
            } else {
                client!!.POST(request)
            }
        } catch (error: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "Error when creating post request, error: $error")
            return null
        }
    }

    /**
     * Asynchronously sends a POST request to the server.
     * @param request - The RequestModel containing the URL, headers, and body of the POST request.
     */
    fun postAsync(request: RequestModel) {
        executorService.submit {
            post(request)
        }
    }
}
