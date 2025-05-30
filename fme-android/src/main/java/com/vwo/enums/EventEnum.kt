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
 * Enumeration representing different event types.
 *
 * This enum defines constants for various event types used in the application,
 * particularly those related to VWO (Visual Website Optimizer) functionality.
 * Each event type is associated with a specific string value.
 */
enum class EventEnum(val value: String) {
    /**
     * Event triggered when a variation is shown to the user.
     */
    VWO_VARIATION_SHOWN("vwo_variationShown"),
    /**
     * Event triggered when a user attribute is set.
     */
    VWO_SYNC_VISITOR_PROP("vwo_syncVisitorProp"),

    /** Error log*/
    VWO_ERROR("vwo_log"),

    VWO_RECOMMENDATION_SHOWN("vwo_recommendation_block_shown");
}
