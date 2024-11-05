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
package com.vwo.packages.network_layer.client

import com.vwo.VWOClient
import com.vwo.interfaces.networking.NetworkClientInterface
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.packages.network_layer.models.ResponseModel
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

    private val maxRetryAttempts = 3

    /**
     * Performs a GET request using the provided RequestModel.
     * @param request The model containing request options.
     * @return A ResponseModel with the response data.
     */
    override fun GET(request: RequestModel): ResponseModel {
        return retryWrapper {
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

                if (statusCode != 200 || !contentType.contains("application/json")) {
                    val error =
                        "Invalid response. Status Code: " + statusCode + ", Response : " + connection.responseMessage
                    responseModel.error = Exception(error)
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

                return@retryWrapper responseModel
            } catch (exception: Exception) {
                responseModel.error = exception
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
        return retryWrapper {
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

                return@retryWrapper responseModel
            } catch (exception: Exception) {
                responseModel.error = exception
                responseModel.statusCode = 404
                return@retryWrapper responseModel
            }
        }
    }

    private fun retryWrapper(requestProcessor: () -> ResponseModel): ResponseModel {
        var countOfRetries = 0
        var response: ResponseModel
        do {
            response = requestProcessor()
            countOfRetries++
        } while (countOfRetries < maxRetryAttempts && response.error != null)

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
