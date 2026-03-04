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
package com.vwo.models.impression

import com.vwo.constants.Constants.IMPRESSION_NO_FEATURE_ID

/**
 * Represents a single impression event for tracking campaign-variation pairs.
 *
 * This data class encapsulates the relationship between a campaign and its variation
 * that was shown to a user, serving as a lightweight container for impression tracking
 * in VWO's analytics system. Multiple instances of this class are collected and sent
 * as a batch via [com.vwo.models.impression.ImpressionPayload] to record which
 * variations were displayed to users.
 *
 * The purpose of tracking impressions is to enable:
 * - Analytics and reporting on campaign performance
 * - A/B testing data collection
 * - User behavior analysis
 * - Conversion tracking and optimization
 *
 * Instances of this class are typically created during feature flag evaluation
 * when variations are shown to users as part of experiments or rollouts.
 *
 * @property campaignId The unique identifier for the campaign to which this impression belongs.
 *                      Must be a positive integer when valid; 0 indicates an invalid/unset state.
 * @property variationId The unique identifier for the specific variation that was shown to the user.
 *                       Must be a positive integer when valid; 0 indicates an invalid/unset state.
 *
 * @see com.vwo.models.impression.ImpressionPayload for collecting multiple impressions
 * @see com.vwo.utils.ImpressionUtil for operations on impressions
 */
data class Impression(
    val campaignId: Int,
    val variationId: Int,
    val featureId: Int = IMPRESSION_NO_FEATURE_ID
)