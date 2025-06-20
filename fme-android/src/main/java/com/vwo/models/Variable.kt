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
package com.vwo.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a variable in VWO.
 *
 * This class encapsulates information about a VWO variable, including its value,type, key, and ID.
 */
class Variable {
    @SerializedName("value")
    var value: Any? = null

    @SerializedName("type")
    var type: String? = null

    @SerializedName("key")
    var key: String? = null

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("displayConfiguration")
    var displayConfiguration: Any? = null
}
