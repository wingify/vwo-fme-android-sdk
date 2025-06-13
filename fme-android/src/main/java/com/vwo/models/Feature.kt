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
 * Represents a feature in VWO.
 *
 * This class encapsulates information about a VWO feature, including its key,metrics, status, ID,
 * rules, impact campaign, name, type, linked campaigns, gateway service requirement, and variables.
 */
class Feature {
    @SerializedName("key")
    var key: String? = null

    @SerializedName("metrics")
    var metrics: List<Metric>? = null

    @SerializedName("status")
    var status: String? = null

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("rules")
    var rules: List<Rule>? = null

    @SerializedName("impactCampaign")
    var impactCampaign: ImpactCampaign = ImpactCampaign()

    @SerializedName("name")
    var name: String? = null

    @SerializedName("type")
    var type: String? = null

    @SerializedName("rulesLinkedCampaign")
    var rulesLinkedCampaign: List<Campaign> = ArrayList()

    @SerializedName("isGatewayServiceRequired")
    var isGatewayServiceRequired: Boolean = false

    @SerializedName("variables")
    var variables: List<Variable>? = null
}
