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
 * Represents a variation in VWO.
 *
 * This class encapsulates information about a VWO variation, including its ID, key, name, weight,
 * start range variation, end range variation, variables, variations, and segments.
 */
class Variation {
    @SerializedName("id")
    var id: Int? = null

    @SerializedName("key")
    var key: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("ruleKey")
    var ruleKey: String? = null

    @SerializedName("type")
    var type: String? = null

    @SerializedName("weight")
    var weight: Double = 0.0

    @SerializedName("startRangeVariation")
    var startRangeVariation: Int = 0

    @SerializedName("endRangeVariation")
    var endRangeVariation: Int = 0

    @SerializedName("variables")
    var variables: List<Variable> = ArrayList()

    @SerializedName("variations")
    var variations: List<Variation> = ArrayList()

    @SerializedName("segments")
    var segments: Map<String, Any> = HashMap()

    @SerializedName("salt")
    var salt: String? = null

    @SerializedName("segments_events")
    var segments_events: List<Any>? = null
}
