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

import com.google.gson.Gson
import com.vwo.VWOClient
import com.vwo.constants.Constants
import com.vwo.constants.Constants.RETRY_DELAY
import com.vwo.constants.Constants.MAX_RETRY_ATTEMPTS
import com.vwo.constants.Constants.SETTINGS_ENDPOINT
import com.vwo.enums.ApiEnum
import com.vwo.enums.CampaignTypeEnum
import com.vwo.enums.DebuggerCategoryEnum
import com.vwo.enums.EventEnum
import com.vwo.enums.UrlEnum
import com.vwo.interfaces.networking.NetworkClientInterface
import com.vwo.models.request.EventArchPayload
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.packages.network_layer.models.ResponseModel
import com.vwo.services.LoggerService
import com.vwo.utils.FunctionUtil.getFormattedErrorMessage
import com.vwo.utils.NetworkUtil.Companion.removeNullValues
import com.vwo.utils.sendDebugEventToVWO
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
        return retryWrapper(request) { retryCount: Int, outOf: Int ->
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
        return retryWrapper(request) { retryCount: Int, outOf: Int ->
            val responseModel = ResponseModel()
            var bodyArray: ByteArray? = null
            try {
                val networkOptions = request.options
                val url = URL(constructUrl(networkOptions))

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                val body = networkOptions["body"]
                bodyArray = if (body is LinkedHashMap<*, *>) {
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
                LoggerService.log(logLevel, "POST: attempt $retryCount/$outOf [${responseModel.statusCode}] $url body=${String(bodyArray)}")
                return@retryWrapper responseModel
            } catch (exception: Exception) {
                responseModel.error = exception
                responseModel.statusCode = 404
                LoggerService.log(logLevel, "POST: attempt $retryCount/$outOf [404] ${constructUrl(request.options)} $exception")
                return@retryWrapper responseModel
            }
        }
    }

    private fun retryWrapper(
        request: RequestModel,
        requestProcessor: (retryCount: Int, outOf: Int) -> ResponseModel
    ): ResponseModel {
        var countOfRetries = 0
        var retryDelay = RETRY_DELAY // Initial delay (used for retries, not the first attempt)
        var response: ResponseModel

        do {
            response = requestProcessor(countOfRetries + 1, MAX_RETRY_ATTEMPTS)
            countOfRetries++

            // If the request failed and we need to retry, apply the delay
            if (response.error != null && countOfRetries < MAX_RETRY_ATTEMPTS) {

                // Don't send retry log for the normal attempt, send log when first retry is attempted
                if (countOfRetries > 1) {
                    val data = mapOf(
                        Constants.END_POINT to (request.path ?: ""),
                        Constants.ERR to (response.error ?: ""),
                        "delay" to (retryDelay / 1000),
                        "attempt" to (countOfRetries - 1),
                        "maxRetries" to MAX_RETRY_ATTEMPTS
                    )
                    LoggerService.errorLog(
                        "ATTEMPTING_RETRY_FOR_FAILED_NETWORK_CALL",
                        data,
                        request.getExtraInfo(),
                        false
                    )
                }
                request.lastError = getFormattedErrorMessage(response.error)

                retryDelay *= 2 // Double the delay for the next retry
                Thread.sleep(retryDelay)
            } else if (response.error != null) {
                request.lastError = getFormattedErrorMessage(response.error)
            }
        } while (countOfRetries < MAX_RETRY_ATTEMPTS && response.error != null)

        // Set the total number of attempts made
        response.totalAttempts = countOfRetries - 1
        if (countOfRetries > 1 && !request.eventName.contains(EventEnum.VWO_DEBUGGER_EVENT.value)) {
            val debugEventProps = createNetWorkAndRetryDebugEvent(request, response)
            sendDebugEventToVWO(removeNullValues(debugEventProps))
        }
        if (response.error != null && !request.eventName.contains(EventEnum.VWO_DEBUGGER_EVENT.value)) {

            LoggerService.errorLog(
                "NETWORK_CALL_FAILURE_AFTER_MAX_RETRIES",
                mapOf(
                    "extraData" to (request.path ?: ""),
                    "attempts" to response.totalAttempts.toString(),
                    Constants.ERR to (response.error ?: "")
                ),
                request.getExtraInfo(),
                false
            )
        }

        return response
    }

    private fun createNetWorkAndRetryDebugEvent(
        request: RequestModel,
        response: ResponseModel,
    ): Map<String, Any?> {
        // set category, if call got success then category is retry, otherwise network
        val category = if (response.error == null) DebuggerCategoryEnum.RETRY else DebuggerCategoryEnum.NETWORK

        return try {
            val msgT = if (response.error == null) Constants.NETWORK_CALL_SUCCESS_WITH_RETRIES else Constants.NETWORK_CALL_FAILURE_AFTER_MAX_RETRIES
            val lt = if (response.error == null) LogLevelEnum.INFO.name else com.vwo.enums.LogLevelEnum.ERROR.name

            val payload = if (request.body != null) {
                val gson = Gson()
                val jsonString = gson.toJson(request.body)
                gson.fromJson(jsonString, EventArchPayload::class.java)
            } else
                null

            val recentError = if (response.error != null) response.error else request.lastError
            val msgPlaceholder = mutableMapOf<String?, String?>(
                Constants.END_POINT to request.path.toString(),
                "apiName" to request.path.toString(),
                "extraData" to (request.path ?: ""),
                "attempts" to response.totalAttempts.toString(),
                Constants.ERR to getFormattedErrorMessage(recentError),
            )
            var apiEnum = ApiEnum.INIT
            var extraDataForMessage: String = ""
            if (payload?.d != null) {
                val eventName = payload.d?.event?.name

                if (eventName == EventEnum.VWO_VARIATION_SHOWN.value) {

                    val campaignInfo = request.campaignInfo
                    extraDataForMessage = if (campaignInfo != null) {
                        val type = campaignInfo["campaignType"] as? String
                        val featureName = campaignInfo["featureName"] as? String ?: ""
                        val variationName = campaignInfo["variationName"] as? String ?: ""
                        val campaignKey = campaignInfo["campaignKey"] as? String ?: ""

                        val isRolloutOrPersonalize = (type == CampaignTypeEnum.ROLLOUT.value)
                                || (type == CampaignTypeEnum.PERSONALIZE.value)

                        if (isRolloutOrPersonalize) {
                            "feature: $featureName, rule: $variationName"
                        } else {
                            "feature: $featureName, rule: $campaignKey and variation: $variationName"
                        }
                    } else {
                        ""
                    }
                    apiEnum =  ApiEnum.GET_FLAG
                } else if (eventName != EventEnum.VWO_VARIATION_SHOWN.value) {
                    if (eventName === EventEnum.VWO_SYNC_VISITOR_PROP.value) {
                        apiEnum = ApiEnum.SET_ATTRIBUTE
                        extraDataForMessage = apiEnum.value
                    } else if (eventName !== EventEnum.VWO_DEBUGGER_EVENT.value
                        && eventName !== EventEnum.VWO_INIT_CALLED.value
                    ) {
                        apiEnum = ApiEnum.TRACK_EVENT
                        extraDataForMessage = "event: $eventName"
                    }
                }
            } else if (request.path == UrlEnum.SET_ALIAS.url) {
                apiEnum = ApiEnum.SET_ALIAS
                msgPlaceholder["apiName"] = apiEnum.value
            }
            msgPlaceholder["apiName"] = extraDataForMessage
            msgPlaceholder["extraData"] = extraDataForMessage

            val msg = if (response.error == null) {
                val templateName = "NETWORK_CALL_SUCCESS_WITH_RETRIES"
                LoggerService.getMessage(LogLevelEnum.INFO, templateName, msgPlaceholder)
            } else {
                val templateName = "NETWORK_CALL_FAILURE_AFTER_MAX_RETRIES"
                LoggerService.getMessage(LogLevelEnum.ERROR, templateName, msgPlaceholder)
            }
            val debugEventProps = mutableMapOf(
                "cg" to category.key,
                "tRa" to response.totalAttempts,
                "sc" to response.statusCode,
                Constants.ERR to getFormattedErrorMessage(response.error),
                "uuid" to payload?.d?.visId,
                "eId" to payload?.d?.event?.props?.id,
                "msg_t" to msgT,
                "lt" to lt,
                "msg" to msg
            )
            val variation = payload?.d?.event?.props?.variation
            if (variation != null) {
                debugEventProps["vId"] = variation
            }

            val apiName = getApiName(payload, request.path)
            if (!apiName.isNullOrEmpty()) {
                debugEventProps["an"] = apiName
            }

            val sessionId = payload?.d?.sessionId
            debugEventProps["sId"] = sessionId ?: (System.currentTimeMillis() / 1000)
            debugEventProps
        } catch (err: Exception) {
            mapOf(
                "cg" to (category),
                Constants.ERR to err.toString()
            )
        }
    }

    private fun getApiName(payload: EventArchPayload?, path: String?): String? {
        if (payload == null) {
            return when (path) {
                SETTINGS_ENDPOINT -> ApiEnum.INIT.value
                UrlEnum.GET_USER_DATA.url -> ApiEnum.GET_USER_DATA.value
                UrlEnum.ATTRIBUTE_CHECK.url -> ApiEnum.ATTRIBUTE_CHECK.value
                UrlEnum.GET_ALIAS.url -> ApiEnum.GET_ALIAS.value
                else -> path
            }
        }
        if (path == UrlEnum.SET_ALIAS.url) {
            return ApiEnum.SET_ALIAS.value
        }
        //Code ported from node sdk
        val eventName = payload.d?.event?.name
        val api = if (eventName == EventEnum.VWO_VARIATION_SHOWN.value) {
             ApiEnum.GET_FLAG
        } else {
            if (eventName == EventEnum.VWO_SYNC_VISITOR_PROP.value) {
                ApiEnum.SET_ATTRIBUTE
            } else if (
                eventName != EventEnum.VWO_DEBUGGER_EVENT.value &&
                eventName != EventEnum.VWO_INIT_CALLED.value
            ) {
                ApiEnum.TRACK_EVENT
            } else {
                null
            }
        }
        return api?.value
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
