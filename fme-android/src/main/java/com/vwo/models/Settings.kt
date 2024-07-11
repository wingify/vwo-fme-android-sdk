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

@JsonInclude(JsonInclude.Include.NON_NULL)
class Settings {
    @JsonProperty("features")
    var features: List<com.vwo.models.Feature>? = null

    @JsonProperty("accountId")
    var accountId: Int? = null

    @JsonProperty("groups")
    var groups: Map<String, com.vwo.models.Groups>? = null

    @JsonProperty("campaignGroups")
    var campaignGroups: Map<String, Int>? = null

    @JsonProperty("isNBv2")
    var isNBv2: Boolean = false

    @JsonProperty("campaigns")
    var campaigns: List<Campaign>? = null

    @JsonProperty("isNB")
    var isNB: Boolean = false

    @JsonProperty("sdkKey")
    var sdkKey: String? = null

    @JsonProperty("version")
    var version: Int? = null

    @JsonProperty("collectionPrefix")
    var collectionPrefix: String? = null
}
