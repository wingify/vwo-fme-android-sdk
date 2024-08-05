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
package com.vwo.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a variation in VWO.
 *
 * This class encapsulates information about a VWO variation, including its ID, key, name, weight,
 * start range variation, end range variation, variables, variations, and segments.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class Variation {
    @JsonProperty("id")
    var id: Int? = null

    @JsonProperty("key")
    var key: String? = null

    @JsonProperty("name")
    var name: String? = null

    @JsonProperty("weight")
    var weight: Double = 0.0

    @JsonProperty("startRangeVariation")
    var startRangeVariation: Int = 0

    @JsonProperty("endRangeVariation")
    var endRangeVariation: Int = 0

    @JsonProperty("variables")
    var variables: List<Variable> = ArrayList()

    @JsonProperty("variations")
    var variations: List<Variation> = ArrayList()

    @JsonProperty("segments")
    var segments: Map<String, Any> = HashMap()
}
