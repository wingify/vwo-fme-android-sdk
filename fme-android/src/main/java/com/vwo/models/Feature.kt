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
 * Represents a feature in VWO.
 *
 * This class encapsulates information about a VWO feature, including its key,metrics, status, ID,
 * rules, impact campaign, name, type, linked campaigns, gateway service requirement, and variables.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class Feature {
    @JsonProperty("key")
    var key: String? = null

    @JsonProperty("metrics")
    var metrics: List<Metric>? = null

    @JsonProperty("status")
    var status: String? = null

    @JsonProperty("id")
    var id: Int? = null

    @JsonProperty("rules")
    var rules: List<Rule>? = null

    @JsonProperty("impactCampaign")
    var impactCampaign: ImpactCampaign = ImpactCampaign()

    @JsonProperty("name")
    var name: String? = null

    @JsonProperty("type")
    var type: String? = null

    @JsonProperty("rulesLinkedCampaign")
    var rulesLinkedCampaign: List<Campaign> = ArrayList()

    @JsonProperty("isGatewayServiceRequired")
    var isGatewayServiceRequired: Boolean = false

    @JsonProperty("variables")
    var variables: List<Variable>? = null
}
