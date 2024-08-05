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
 * Represents a group of campaigns in VWO.
 *
 * This class encapsulates information about a group of VWO campaigns,including its name, associated campaigns, and settings for experiment type, priority, and weight.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class Groups {
    @get:JsonProperty("name")
    @set:JsonProperty("name")
    @JsonProperty("name")
    var name: String? = null

    @get:JsonProperty("campaigns")
    @set:JsonProperty("campaigns")
    @JsonProperty("campaigns")
    var campaigns: List<Int>? = null

    // this is where algo, priority, weight go
    @JsonProperty("et")
    private var et: Int? = null

    @get:JsonProperty("p")
    @set:JsonProperty("p")
    @JsonProperty("p")
    var p: List<Int>? = null

    @get:JsonProperty("wt")
    @set:JsonProperty("wt")
    @JsonProperty("wt")
    var wt: Map<String, Int>? = null

    /**
     * Sets the experiment type for the group.
     *
     * @param et The experiment type.
     */
    // getters and setters
    @JsonProperty("et")
    fun setEt(et: Int) {
        this.et = et
    }

    /**
     * Gets the experiment type for the group.
     *
     * @return The experiment type. Defaults to 1 (random) if not set.
     */
    @JsonProperty("et")
    fun getEt(): Int? {
        // set default to random
        et = if (et == null || et.toString().isEmpty()) 1 else et

        return et
    }
}
