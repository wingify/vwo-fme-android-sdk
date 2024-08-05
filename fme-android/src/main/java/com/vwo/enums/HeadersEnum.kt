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
package com.vwo.enums

/**
 * Enumeration representing HTTP headers used in API requests.
 *
 * This enum defines constants for specific HTTP headers used for conveying
 * information about the client or request context. Each header is associated
 * with its corresponding string value.
 */
enum class HeadersEnum(val header: String) {
    /**
     * Header representing the user agent of the client.
     */
    USER_AGENT("X-Device-User-Agent"),
    /**
     * Header representing the IP address of the client.
     */
    IP("VWO-X-Forwarded-For")
}
