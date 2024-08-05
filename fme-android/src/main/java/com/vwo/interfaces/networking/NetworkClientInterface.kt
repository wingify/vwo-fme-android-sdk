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
package com.vwo.interfaces.networking

import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.packages.network_layer.models.ResponseModel

/**
 * Interface for a network client that performs HTTP requests.
 *
 * This interface provides methods for sending GET and POST requests to a server.
 */
interface NetworkClientInterface {
    /**
     * Sends a GET request to the server.
     * @param request - The RequestModel containing the URL and parameters for the GET request.
     * @return A ResponseModel containing the response data.
     */
    fun GET(request: RequestModel): ResponseModel

    /**
     * Sends a POST request to the server.
     * @param request - The RequestModel containing the URL, headers, and body of the POST request.
     * @return A ResponseModel containing the response data.
     */
    fun POST(request: RequestModel): ResponseModel
}
