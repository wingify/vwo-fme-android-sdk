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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a campaign in VWO.
 *
 * This class encapsulates information about a VWO campaign, including its ID,segments, status,
 * traffic allocation, variations, and other related data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class Campaign {
    @JsonProperty("isAlwaysCheckSegment")
    var isAlwaysCheckSegment: Boolean? = false

    @JsonProperty("isUserListEnabled")
    var isUserListEnabled: Boolean? = false

    @get:JsonProperty("id")
    @set:JsonProperty("id")
    @JsonProperty("id")
    var id: Int? = null

    @get:JsonProperty("segments")
    @set:JsonProperty("segments")
    @JsonProperty("segments")
    var segments: Map<String, Any>? = null

    @get:JsonProperty("segments_events")
    @set:JsonProperty("segments_events")
    @JsonProperty("segments_events")
    var segments_events: List<Any>? = null

    @JsonProperty("ruleKey")
    var ruleKey: String? = null

    @get:JsonProperty("status")
    @set:JsonProperty("status")
    @JsonProperty("status")
    var status: String? = null

    @get:JsonProperty("percentTraffic")
    @set:JsonProperty("percentTraffic")
    @JsonProperty("percentTraffic")
    var percentTraffic: Int? = null

    @get:JsonProperty("key")
    @set:JsonProperty("key")
    @JsonProperty("key")
    var key: String? = null

    @get:JsonProperty("type")
    @set:JsonProperty("type")
    @JsonProperty("type")
    var type: String? = null

    @get:JsonProperty("name")
    @set:JsonProperty("name")
    @JsonProperty("name")
    var name: String? = null

    @get:JsonProperty("isForcedVariationEnabled")
    @set:JsonProperty("isForcedVariationEnabled")
    @JsonProperty("isForcedVariationEnabled")
    var isForcedVariationEnabled: Boolean? = false

    @get:JsonProperty("variations")
    @set:JsonProperty("variations")
    @JsonProperty("variations")
    var variations: List<com.vwo.models.Variation>? = null

    @JsonProperty("startRangeVariation")
    var startRangeVariation: Int = 0

    @JsonProperty("endRangeVariation")
    var endRangeVariation: Int = 0

    @get:JsonProperty("variables")
    @set:JsonProperty("variables")
    @JsonProperty("variables")
    var variables: List<com.vwo.models.Variable>? = null

    @JsonProperty("weight")
    var weight: Double = 0.0

    var isEventsDsl: Boolean = false

    /**
     * Sets the properties of this campaign from another campaign object.
     *
     * @param model The campaign object to copy properties from.
     */
    fun setModelFromDictionary(model: Campaign) {
        if (model.id != null) {
            this.id = model.id
        }
        if (model.segments != null) {
            this.segments = model.segments
        }
        if (model.status != null) {
            this.status = model.status
        }
        if (model.percentTraffic != null) {
            this.percentTraffic = model.percentTraffic
        }
        if (model.key != null) {
            this.key = model.key
        }
        if (model.type != null) {
            this.type = model.type
        }
        if (model.name != null) {
            this.name = model.name
        }
        if (model.isForcedVariationEnabled != null) {
            this.isForcedVariationEnabled = model.isForcedVariationEnabled
        }
        if (model.variations != null) {
            this.variations = model.variations
        }
        if (model.variables != null) {
            this.variables = model.variables
        }
        if (model.ruleKey != null) {
            this.ruleKey = model.ruleKey
        }

        if (model.isAlwaysCheckSegment != null) {
            this.isAlwaysCheckSegment = model.isAlwaysCheckSegment
        }

        if (model.isUserListEnabled != null) {
            this.isUserListEnabled = model.isUserListEnabled
        }

        if (model.weight != 0.0) {
            this.weight = model.weight
        }

        if (model.startRangeVariation != 0) {
            this.startRangeVariation = model.startRangeVariation
        }

        if (model.endRangeVariation != 0) {
            this.endRangeVariation = model.endRangeVariation
        }

        if (model.segments_events != null) {
            this.segments_events = model.segments_events
        }
    }
}

