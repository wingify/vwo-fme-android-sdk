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
package com.vwo.packages.network_layer.models

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Represents a network request model.
 *
 * This class encapsulates the details of a network request, including URL, method,headers, body,
 * and other options. It provides a way to construct and configure network requests before sending
 * them.
 */
class RequestModel(
    var url: String?,
    method: String?,
    var path: String?,
    var query: MutableMap<String, String>?,
    var body: Map<String, Any?>?,
    var headers: MutableMap<String, String>?,
    scheme: String?,
    val port: Int,
    val expectedResponseType: String = "application/json"
) {
    var method: String? = method ?: "GET"
    var scheme: String? = scheme ?: "http"

    @JvmField
    var timeout: Int = 0

    internal var eventName: String = ""
    internal var campaignInfo: Map<String, Any>? = null
    /**
     * A map containing various options for the request.
     */
    val options: Map<String, Any?>
        get() {
            val queryParams = buildQueryString(query)

            val options: MutableMap<String, Any?> = HashMap()
            options["hostname"] = url
            options["agent"] = false

            if (scheme != null) {
                options["scheme"] = scheme
            }
            if (port != 80) {
                options["port"] = port
            }
            options["headers"] = headers

            if (method != null) {
                options["method"] = method
            }

            if (body != null) {
                headers?.set("Content-Type", "application/json")
                options["headers"] = headers
                options["body"] = body
            }

            if (path != null) {
                var combinedPath = path
                if (queryParams.isNotEmpty()) {
                    combinedPath += "?$queryParams"
                }
                options["path"] = combinedPath
            }
            if (timeout > 0) {
                options["timeout"] = timeout
            }

            return options
        }

    internal var lastError: String = ""
    /*set(value) {
        FunctionUtil.getFormattedErrorMessage(value)
    }*/

    /**
     * Retrieves the extra information of the HTTP request.
     * @return A map of key-value pairs representing the extra information.
     */
    fun getExtraInfo(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        options.forEach { (key, value) ->
            if (value != null) {
                result[key] = value
            }
        }
        url?.let { result["url"] = it }
        method?.let { result["method"] = it }
        query?.let { result["query"] = it }
        path?.let { result["path"] = it }
        body?.let { result["body"] = it }
        headers?.let { result["headers"] = it }
        scheme?.let { result["scheme"] = it }
        result["port"] = port
        return result
    }

    private fun buildQueryString(query: Map<String, Any>?): String {
        return query?.entries?.joinToString("&") { (key, value) ->
            val encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.toString())
            val encodedValue =
                URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.toString())
            "$encodedKey=$encodedValue"
        } ?: ""
    }
}