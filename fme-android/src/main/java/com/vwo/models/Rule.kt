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

import androidx.annotation.VisibleForTesting
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a rule in VWO.
 *
 * This class encapsulates information about a VWO rule, including its rule key,variation ID,
 * campaign ID, and type.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class Rule {
    @JsonProperty("ruleKey")
    var ruleKey: String? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal set

    @JsonProperty("variationId")
    var variationId: Int? = null

    @JsonProperty("campaignId")
    var campaignId: Int? = null

    @JsonProperty("type")
    var type: String? = null

    /**
     * Sets the rule key.
     *
     * @param ruleKey The unique key of the rule.
     */
    fun setStatus(ruleKey: String?) {
        this.ruleKey = ruleKey
    }
}
