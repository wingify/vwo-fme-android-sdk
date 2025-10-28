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

/**
 * Represents a network response model.
 *
 * This class encapsulates the details of a network response, including status code, headers,
 * response data, and any errors encountered.
 */
class ResponseModel {
    /**
     * The HTTP status code of the response.
     */
    @JvmField
    var statusCode: Int = 0

    /**
     * Error information, if any.
     */
    @JvmField
    var error: Any? = null

    /**
     * The response headers.
     */
    var headers: Map<String, String>? = null

    /**
     * The response data as a string.
     */
    @JvmField
    var data: String? = null
}