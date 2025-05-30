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
package com.vwo.models.request.EventArchQueryParams

import java.util.Calendar

/**
 * Represents query parameters for API requests.
 *
 * This class constructs and provides access to a map of query parameters used in APIrequests.
 * It includes parameters for environment, account ID, visitor information, and other metadata.
 *
 * @param en The event name.
 * @param a The account ID.
 * @param env The environment name.
 * @param visitor_ua The visitor's user agent.* @param visitor_ip The visitor's IP address.
 * @param url The requested URL.
 */
class RequestQueryParams(
    private val en: String,
    private val a: String,
    private val env: String,
    private val visitor_ua: String?,
    private val visitor_ip: String?,
    private val url: String
) {
    private val eTime = Calendar.getInstance().timeInMillis
    private val random = Math.random()
    private val p = "FS"

    /**
     * A map containing the query parameters.
     * This map is lazily initialized.
     */
    val queryParams: MutableMap<String, String> by lazy {
        val path = mutableMapOf<String, String>()
        path["en"] = en
        path["a"] = a
        path["env"] = env
        path["eTime"] = eTime.toString()
        path["random"] = random.toString()
        path["p"] = p
        visitor_ua?.let { path["visitor_ua"] = it }
        visitor_ip?.let { path["visitor_ip"] = it }
        path
    }
}
