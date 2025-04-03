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
package com.vwo.packages.network_layer.client

import com.vwo.VWOClient
import com.vwo.constants.Constants.RETRY_DELAY
import com.vwo.constants.Constants.MAX_RETRY_ATTEMPTS
import com.vwo.interfaces.networking.NetworkClientInterface
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.packages.network_layer.models.ResponseModel
import com.vwo.services.LoggerService
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * A network client for making HTTP requests.
 *
 * This class provides functionality for sending HTTP requests and receiving responses.
 */
class NetworkClient : NetworkClientInterface {

    private val logLevel = LogLevelEnum.INFO

    /**
     * Performs a GET request using the provided RequestModel.
     * @param request The model containing request options.
     * @return A ResponseModel with the response data.
     */
    override fun GET(request: RequestModel): ResponseModel {
        return retryWrapper { retryCount: Int, outOf: Int ->
            val responseModel = ResponseModel()
            try {
                val networkOptions = request.options
                val url = URL(constructUrl(networkOptions))

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                //connection.setConnectTimeout(5000);
                connection.connect()

                val statusCode = connection.responseCode
                responseModel.statusCode = statusCode

                val contentType = connection.getHeaderField("Content-Type")

                if (statusCode != 200 || !contentType.contains(request.expectedResponseType)) {
                    val error =
                        "Invalid response. Status Code: " + statusCode + ", Response : " + connection.responseMessage
                    responseModel.error = Exception(error)
                    LoggerService.log(logLevel, "GET: attempt $retryCount/$outOf [${responseModel.statusCode}] $url")
                    return@retryWrapper responseModel
                }

                val `in` = BufferedReader(InputStreamReader(connection.inputStream))
                var inputLine: String?
                val response = StringBuilder()

                while ((`in`.readLine().also { inputLine = it }) != null) {
                    response.append(inputLine)
                }
                `in`.close()

                val responseData = response.toString()
                responseModel.data = responseData

                LoggerService.log(logLevel, "GET: attempt $retryCount/$outOf [$statusCode] $url")
                return@retryWrapper responseModel
            } catch (exception: Exception) {
                responseModel.error = exception
                LoggerService.log(logLevel, "GET: attempt $retryCount/$outOf [404] ${constructUrl(request.options)} $exception")
                return@retryWrapper responseModel
            }
        }
    }

    /**
     * Performs a POST request using the provided RequestModel.
     * @param request The model containing request options.
     * @return A ResponseModel with the response data.
     */
    override fun POST(request: RequestModel): ResponseModel {
        return retryWrapper { retryCount: Int, outOf: Int ->
            val responseModel = ResponseModel()
            try {
                val networkOptions = request.options
                val url = URL(constructUrl(networkOptions))

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                val body = networkOptions["body"]
                val bodyArray = if (body is LinkedHashMap<*, *>) {
                    val jsonBody: String = VWOClient.objectMapper.writeValueAsString(body)
                    jsonBody.toByteArray(StandardCharsets.UTF_8)
                } else if (body is String) {
                    body.toByteArray(StandardCharsets.UTF_8)
                } else {
                    throw IllegalArgumentException("Unsupported body type: " + body?.javaClass?.name)
                }

                // set headers
                for ((key, value) in networkOptions) {
                    if (key == "headers") {
                        val headers = value as Map<String, String>
                        for ((key1, value1) in headers) {
                            connection.setRequestProperty(key1, value1)
                        }
                        if (bodyArray.isNotEmpty())
                            connection.setRequestProperty(
                                "Content-Length",
                                bodyArray.size.toString()
                            )
                    }
                }

                connection.outputStream.use { os ->
                    os.write(bodyArray)
                    os.flush()
                }
                val statusCode = connection.responseCode
                responseModel.statusCode = statusCode

                val `in` = BufferedReader(InputStreamReader(connection.inputStream, "utf-8"))
                val response = StringBuilder()
                var inputLine: String?

                while ((`in`.readLine().also { inputLine = it }) != null) {
                    response.append(inputLine)
                }
                `in`.close()

                val responseData = response.toString()
                responseModel.data = responseData

                if (statusCode != 200) {
                    val error = "Request failed. Status Code: $statusCode, Response: $responseData"
                    responseModel.error = Exception(error)
                }
                LoggerService.log(logLevel, "POST: attempt $retryCount/$outOf [${responseModel.statusCode}] $url")
                return@retryWrapper responseModel
            } catch (exception: Exception) {
                responseModel.error = exception
                responseModel.statusCode = 404
                LoggerService.log(logLevel, "POST: attempt $retryCount/$outOf [404] ${constructUrl(request.options)} $exception")
                return@retryWrapper responseModel
            }
        }
    }

    private fun retryWrapper(requestProcessor: (retryCount: Int, outOf:Int) -> ResponseModel): ResponseModel {
        var countOfRetries = 0
        var retryDelay = RETRY_DELAY // Initial delay (used for retries, not the first attempt)
        var response: ResponseModel

        do {
            response = requestProcessor(countOfRetries+1, MAX_RETRY_ATTEMPTS)
            countOfRetries++

            // If the request failed and we need to retry, apply the delay
            if (response.error != null && countOfRetries < MAX_RETRY_ATTEMPTS) {

                retryDelay *= 2 // Double the delay for the next retry
                Thread.sleep(retryDelay)
            }
        } while (countOfRetries < MAX_RETRY_ATTEMPTS && response.error != null)

        return response
    }

    companion object {
        /**
         * Constructs a URL from network options.
         *
         * This method creates a URL string based on the provided network options, including scheme, hostname, port, and path.
         *
         * @param networkOptions A map containing network options.
         * @return The constructed URL string.
         */
        fun constructUrl(networkOptions: Map<String, Any?>): String {
            var hostname = networkOptions["hostname"] as String?
            val path = networkOptions["path"] as String?
            if (networkOptions["port"] != null && networkOptions["port"].toString().toInt() != 0) {
                hostname += ":" + networkOptions["port"]
            }
            return networkOptions["scheme"].toString()
                .lowercase(Locale.getDefault()) + "://" + hostname + path
        }
    }
}
