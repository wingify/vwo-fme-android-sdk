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
package com.vwo.enums

/**
 * Container class for enumeration representing decision types.
 *
 * This class holds an enum called `DecisionTypes` that defines constants for
 * different types of decisions made within the application. Currently, it
 * includes a single decision type for campaign decisions.
 */
class HooksEnum {
    /**
     * Enumeration representing different decision types.
     *
     * This enum defines constants for various types of decisions,
     * currently including only campaign decisions.
     */
    enum class DecisionTypes(val type: String) {
        /**
         * Decision type representing a campaign decision.
         */
        CAMPAIGN_DECISION("CAMPAIGN_DECISION")
    }

    /**
     * Default decision type set to `CAMPAIGN_DECISION`.
     */
    val decisionTypes: DecisionTypes = DecisionTypes.CAMPAIGN_DECISION
}
