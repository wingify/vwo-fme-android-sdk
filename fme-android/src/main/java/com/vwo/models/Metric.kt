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
 * Represents a metric in VWO.
 *
 * This class encapsulates information about a VWO metric, including its various properties such as
 * `mca`, `hashProps`, `identifier`, `id`, and `type`.
 */
class Metric {
    @SerializedName("mca")
    var mca: Int? = null

    @SerializedName("hasProps")
    var hashProps: Boolean? = null

    @SerializedName("identifier")
    var identifier: String? = null

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("type")
    var type: String? = null
}
