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
 * Represents a holdout group in VWO.
 *
 * This class encapsulates information about a holdout group, including its ID,
 * targeting segments, traffic percentage, global flag, and associated feature IDs.
 * A holdout group is used to exclude a specific percentage of users from new features
 * to measure the cumulative, long-term impact of product changes.
 */
class HoldoutGroup {

    @SerializedName("name")
    var name: String? = null

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("segments")
    var segments: Map<String, Any>? = null

    @SerializedName("percentTraffic")
    var trafficPercent: Int? = null

    @SerializedName("isGlobal")
    var isGlobal: Boolean? = false

    @SerializedName("isGatewayServiceRequired")
    var isGatewayServiceRequired: Boolean? = false

    @SerializedName("featureIds")
    var featureIds: List<Int>? = null

    @SerializedName("metrics")
    var metrics: List<Metrics>? = null

    class Metrics {

        @SerializedName("type")
        var type: String = ""

        @SerializedName("id")
        var id: Int = -1

        @SerializedName("identifier")
        var identifier: String = ""

    }

}