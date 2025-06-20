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
 * Represents a group of campaigns in VWO.
 *
 * This class encapsulates information about a group of VWO campaigns,including its name, associated campaigns, and settings for experiment type, priority, and weight.
 */
class Groups {
    @SerializedName("name")
    var name: String? = null

    @SerializedName("campaigns")
    var campaigns: List<String>? = null

    // this is where algo, priority, weight go
    @SerializedName("et")
    private var et: Int? = null

    @SerializedName("p")
    var p: MutableList<String>? = ArrayList()

    @SerializedName("wt")
    var wt: Map<String, Double>? = mutableMapOf()

    /**
     * Sets the experiment type for the group.
     *
     * @param et The experiment type.
     */
    // getters and setters
    fun setEt(et: Int) {
        this.et = et
    }

    /**
     * Gets the experiment type for the group.
     *
     * @return The experiment type. Defaults to 1 (random) if not set.
     */
    fun getEt(): Int? {
        // set default to random
        et = if (et == null || et.toString().isEmpty()) 1 else et

        return et
    }
}
