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
package com.vwo.models.request.EventArchQueryParams

import java.util.Calendar

class RequestQueryParams(
    private val en: String,
    private val a: String,
    private val env: String,
    private val visitor_ua: String,
    private val visitor_ip: String,
    private val url: String
) {
    private val eTime = Calendar.getInstance().timeInMillis
    private val random = Math.random()
    private val p = "FS"

    val queryParams: Map<String, String>
        get() {
            val path: MutableMap<String, String> = HashMap()
            path["en"] = en
            path["a"] = a
            path["env"] = env
            path["eTime"] = eTime.toString()
            path["random"] = random.toString()
            path["p"] = p
            path["visitor_ua"] = visitor_ua
            path["visitor_ip"] = visitor_ip
            return path
        }
}
