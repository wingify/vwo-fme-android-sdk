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
package com.vwo.enums

/**
 * Enumeration representing different API endpoints.
 *
 * This enum defines constants for API endpoints used in the application,
 * associating each endpoint with its corresponding string value.
 */
enum class ApiEnum(val value: String) {
    INIT("init"),
    ON_INIT("onInit"),
    GET_FLAG("getFlag"),
    TRACK_EVENT("trackEvent"),
    SET_ATTRIBUTE("setAttribute"),
    UPDATE_SETTINGS("updateSettings"),
    GET_USER_DATA("getUserDetails"),
    ATTRIBUTE_CHECK("checkAttribute"),
    GET_ALIAS("getAliasUserId"),
    SET_ALIAS("setAlias")
}
