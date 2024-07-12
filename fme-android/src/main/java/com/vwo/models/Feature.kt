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

@JsonInclude(JsonInclude.Include.NON_NULL)
class Feature {
    @JsonProperty("key")
    var key: String? = null

    @JsonProperty("metrics")
    var metrics: List<com.vwo.models.Metric>? = null

    @JsonProperty("status")
    var status: String? = null

    @JsonProperty("id")
    var id: Int? = null

    @JsonProperty("rules")
    var rules: List<com.vwo.models.Rule>? = null

    @JsonProperty("impactCampaign")
    var impactCampaign: com.vwo.models.ImpactCampaign = com.vwo.models.ImpactCampaign()

    @JsonProperty("name")
    var name: String? = null

    @JsonProperty("type")
    var type: String? = null

    @JsonProperty("rulesLinkedCampaign")
    var rulesLinkedCampaign: List<com.vwo.models.Campaign> = ArrayList()

    @JsonProperty("isGatewayServiceRequired")
    var isGatewayServiceRequired: Boolean = false

    @JsonProperty("variables")
    var variables: List<com.vwo.models.Variable>? = null
}
