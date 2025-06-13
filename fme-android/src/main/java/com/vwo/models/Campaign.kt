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
 * Represents a campaign in VWO.
 *
 * This class encapsulates information about a VWO campaign, including its ID,segments, status,
 * traffic allocation, variations, and other related data.
 */
class Campaign {
    @SerializedName("isAlwaysCheckSegment")
    var isAlwaysCheckSegment: Boolean? = false

    @SerializedName("isUserListEnabled")
    var isUserListEnabled: Boolean? = false

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("segments")
    var segments: Map<String, Any>? = null

    @SerializedName("segments_events")
    var segments_events: List<Any>? = null

    @SerializedName("ruleKey")
    var ruleKey: String? = null

    @SerializedName("salt")
    var salt: String? = null

    @SerializedName("status")
    var status: String? = null

    @SerializedName("percentTraffic")
    var percentTraffic: Int? = null

    @SerializedName("key")
    var key: String? = null

    @SerializedName("type")
    var type: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("isForcedVariationEnabled")
    var isForcedVariationEnabled: Boolean? = false

    @SerializedName("variations")
    var variations: List<com.vwo.models.Variation>? = null

    @SerializedName("startRangeVariation")
    var startRangeVariation: Int = 0

    @SerializedName("endRangeVariation")
    var endRangeVariation: Int = 0

    @SerializedName("variables")
    var variables: List<com.vwo.models.Variable>? = null

    @SerializedName("weight")
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

        if (model.salt != null) {
            this.salt = model.salt
        }
    }
}

