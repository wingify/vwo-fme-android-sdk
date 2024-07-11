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

class NetworkClient : NetworkClientInterface {
    /**
     * Performs a GET request using the provided RequestModel.
     * @param requestModel The model containing request options.
     * @return A ResponseModel with the response data.
     */
    override fun GET(requestModel: RequestModel?): ResponseModel? {
        val responseModel = ResponseModel()
        try {
            val networkOptions = requestModel.getOptions()
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
                return responseModel
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

            return responseModel
        } catch (exception: Exception) {
            responseModel.error = exception
            return responseModel
        }
    }

    /**
     * Performs a POST request using the provided RequestModel.
     * @param request The model containing request options.
     * @return A ResponseModel with the response data.
     */
    override fun POST(request: RequestModel?): ResponseModel? {
        val responseModel = ResponseModel()
        try {
            val networkOptions = request.getOptions()
            val url = URL(constructUrl(networkOptions))

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            // set headers
            for ((key, value) in networkOptions!!) {
                if (key == "headers") {
                    val headers = value as Map<String, String>
                    for ((key1, value1) in headers) {
                        connection.setRequestProperty(key1, value1)
                    }
                }
            }

            connection.outputStream.use { os ->
                val body = networkOptions!!["body"]
                if (body is LinkedHashMap<*, *>) {
                    // Convert LinkedHashMap to JSON string
                    val jsonBody: String = VWOClient.objectMapper.writeValueAsString(body)
                    val input = jsonBody.toByteArray(StandardCharsets.UTF_8)
                    os.write(input, 0, input.size)
                } else if (body is String) {
                    val input = (body as String).toByteArray(StandardCharsets.UTF_8)
                    os.write(input, 0, input.size)
                } else {
                    throw IllegalArgumentException("Unsupported body type: " + body!!.javaClass.name)
                }
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

            return responseModel
        } catch (exception: Exception) {
            responseModel.error = exception
            return responseModel
        }
    }

    companion object {
        fun constructUrl(networkOptions: Map<String?, Any?>?): String {
            var hostname = networkOptions!!["hostname"] as String?
            val path = networkOptions["path"] as String?
            if (networkOptions["port"] != null && networkOptions["port"].toString().toInt() != 0) {
                hostname += ":" + networkOptions["port"]
            }
            return networkOptions["scheme"].toString()
                .lowercase(Locale.getDefault()) + "://" + hostname + path
        }
    }
}
