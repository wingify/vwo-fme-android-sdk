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
import com.google.gson.annotations.SerializedName

/**
 * Represents a rule in VWO.
 *
 * This class encapsulates information about a VWO rule, including its rule key, variation ID,
 * campaign ID, and type.
 */
class Rule {
    @SerializedName("ruleKey")
    var ruleKey: String? = null
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal set

    @SerializedName("variationId")
    var variationId: Int? = null

    @SerializedName("campaignId")
    var campaignId: Int? = null

    @SerializedName("type")
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
