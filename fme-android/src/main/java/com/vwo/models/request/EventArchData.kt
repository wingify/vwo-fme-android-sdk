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
package com.vwo.models.request

import com.google.gson.annotations.SerializedName
import com.vwo.models.request.visitor.Visitor

/**
 * Event architecture data
 *
 * @constructor Create empty Event arch data
 */
class EventArchData {

    @SerializedName("msgId")
    var msgId: String? = null

    @SerializedName("visId")
    var visId: String? = null

    @SerializedName("sessionId")
    var sessionId: Long? = null

    @SerializedName("event")
    var event: Event? = null

    @SerializedName("visitor")
    var visitor: Visitor? = null

    @SerializedName("visitor_ua")
    var visitor_ua: String? = null

    @SerializedName("visitor_ip")
    var visitor_ip: String? = null
}
