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
package com.vwo.packages.network_layer.models

import com.google.gson.Gson

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
    val port: Int
) {
    var method: String? = method ?: "GET"
    var scheme: String? = scheme ?: "http"

    @JvmField
    var timeout: Int = 0

    /**
     * A map containing various options for the request.
     */
    val options: Map<String, Any?>
        get() {
            val queryParams = StringBuilder()
            query?.let {
                for ((key, value) in it) {
                    queryParams.append(key).append('=').append(value).append('&')
                }
            }

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
                    combinedPath += "?" + queryParams.substring(0, queryParams.length - 1)
                }
                options["path"] = combinedPath
            }
            if (timeout > 0) {
                options["timeout"] = timeout
            }

            return options
        }
}
