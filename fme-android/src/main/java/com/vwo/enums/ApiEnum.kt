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
 * Enumeration representing different API endpoints.
 *
 * This enum defines constants for API endpoints used in the application,
 * associating each endpoint with its corresponding string value.
 */
enum class ApiEnum(val value: String) {
    /**
     * API endpoint for retrieving feature flags.
     */
    GET_FLAG("getFlag"),
    /**
     * API endpoint for tracking user events.
     */
    TRACK("track")
}
