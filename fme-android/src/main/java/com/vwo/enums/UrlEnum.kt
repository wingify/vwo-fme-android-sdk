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
 * Enumeration representing API endpoint URLs.
 *
 * This enum defines constants for various API endpoint URLs used in the application.
 * Each URL is associated with a specific string value representing the relative path of the endpoint.
 */
enum class UrlEnum(val url: String) {
    /**
     * URL for the events endpoint.
     */
    EVENTS("/events/t"),
    /**
     * URL for checking attributes.
     */
    ATTRIBUTE_CHECK("/check-attribute"),
    /**
     * URL for retrieving user data.
     */
    GET_USER_DATA("/get-user-details")
}
