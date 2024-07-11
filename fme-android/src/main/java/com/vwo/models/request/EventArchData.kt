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
package com.vwo.models.request

import com.vwo.models.request.visitor.Visitor

class EventArchData {
    var msgId: String? = null
    var visId: String? = null
    var sessionId: Long? = null
    var event: com.vwo.models.request.Event? = null
    var visitor: com.vwo.models.request.visitor.Visitor? = null
    var visitor_ua: String? = null
    var visitor_ip: String? = null
}
