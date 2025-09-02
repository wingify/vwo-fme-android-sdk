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
package com.vwo.interfaces.networking

/**
 * Enumeration representing HTTP request methods used in network operations.
 * 
 * This enum provides type-safe HTTP verb constants for making network requests
 * in the VWO SDK. Each enum value contains the string representation of the
 * HTTP method and overrides the toString() method to return the appropriate
 * HTTP verb string.
 * 
 * @property value The string representation of the HTTP method
 */
enum class HTTPVerb(val value: String) {
    /**
     * HTTP GET method.
     * 
     * Used for retrieving data from the server. GET requests should be idempotent
     * and safe, meaning they should not have side effects on the server state.
     */
    GET("GET") {
        override fun toString(): String {
            return value
        }
    },
    
    /**
     * HTTP POST method.
     * 
     * Used for submitting data to the server. POST requests are typically used
     * for creating new resources or submitting form data that may change server state.
     */
    POST("POST") {
        override fun toString(): String {
            return value
        }
    }
}