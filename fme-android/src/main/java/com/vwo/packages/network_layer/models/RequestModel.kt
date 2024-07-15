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

class RequestModel(
    var url: String?,
    method: String?,
    var path: String?,
    var query: MutableMap<String, String?>,
    var body: Map<String, Any?>?,
    private var headers: MutableMap<String, String>,
    scheme: String?,
    port: Int
) {
    var method: String? = method ?: "GET"
    var scheme: String? = scheme ?: "http"
    var port: Int = 0
    @JvmField
    var timeout: Int = 0

    init {
        if (port != 0) {
            this.port = port
        }
    }

    fun getHeaders(): Map<String, String> {
        return headers
    }

    fun setHeaders(headers: MutableMap<String, String>) {
        this.headers = headers
    }

    val options: Map<String, Any?>
        get() {
            val queryParams = StringBuilder()
            for (key in query.keys) {
                queryParams.append(key).append('=').append(query[key]).append('&')
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
                val postBody: String = Gson().toJson(body)
                headers["Content-Type"] = "application/json"
                headers["Content-Length"] = postBody.toByteArray().size.toString()
                options["headers"] = headers
                options["body"] = body
            }

            if (path != null) {
                var combinedPath = path
                if (queryParams.length > 0) {
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
