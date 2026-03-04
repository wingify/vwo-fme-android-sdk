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
package com.vwo.services

import com.vwo.ServiceContainer
import com.vwo.VWOClient
import com.vwo.constants.Constants.Holdouts.VARIATION_IS_PART_OF_HOLDOUT
import com.vwo.constants.Constants.Holdouts.VARIATION_NOT_PART_OF_HOLDOUT
import com.vwo.constants.Constants.IMPRESSION_NO_FEATURE_ID
import com.vwo.decorators.StorageDecorator
import com.vwo.models.Feature
import com.vwo.models.HoldoutGroup
import com.vwo.models.Settings
import com.vwo.models.Storage
import com.vwo.models.impression.ImpressionPayload
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.decision_maker.DecisionMaker
import com.vwo.packages.logger.enums.LogLevelEnum

/**
 * Service for evaluating holdout group targeting and bucketing logic.
 *
 * This service handles the core business logic for determining if a user should be
 * excluded from features due to holdout group membership. It evaluates segmentation
 * rules and traffic percentage bucketing to determine holdout status.
 */
class HoldoutGroupService(
    private val decisionMaker: DecisionMaker,
    private val serviceContainer: ServiceContainer?
) {

    /**
     * Checks if a user should be excluded from a specific feature due to holdout group membership.
     *
     * This method evaluates all holdout groups to determine if the user should be excluded:
     * 1. Checks if holdout group applies to the feature (global or selective)
     * 2. Evaluates segmentation targeting
     * 3. Evaluates traffic percentage bucketing
     *
     * Note: Storage check for holdout decisions is handled in GetFlagAPI. This method only
     * evaluates holdout eligibility without checking or storing results internally.
     *
     * @param settings The settings containing holdout groups configuration
     * @param featureId The ID of the feature being evaluated
     * @param context The user context containing user information
     * @return HoldoutGroup? Returns the holdout group if user is in holdout, null otherwise
     */
    fun getHoldoutsFor(
        settings: Settings,
        feature: Feature,
        context: VWOUserContext,
        storageService: StorageService,
    ): Pair<List<HoldoutGroup>, ImpressionPayload> {

        val featureId = feature.id

        // We must check if user was already evaluated for holdouts
        val storage = getLocalStorageDataForThisFeature(
            feature = feature,
            context = context,
            storageService = storageService
        )

        val inHoldout = storage.holdoutIds?.values ?: listOf()

        val notInHoldout = storage.notInHoldoutIds?.values ?: listOf()

        val alreadyEvaluatedHoldoutIds = (inHoldout + notInHoldout)

        if (inHoldout.isNotEmpty()) {
            serviceContainer?.getLoggerService()?.log(
                LogLevelEnum.DEBUG,
                "HOLDOUT_GROUP_FOUND_IN_STORAGE",
                mapOf(
                    "userId" to "${context.id}",
                    "holdoutName" to inHoldout.toString(),
                    "featureKey" to "${feature.key}"
                )
            )
        }

        // If no holdout groups exist, user is not in holdout
        val holdoutGroups = settings.holdoutGroups
        if (holdoutGroups.isNullOrEmpty()) {
            serviceContainer?.getLoggerService()
                ?.log(LogLevelEnum.DEBUG, "HOLDOUT_NOT_CONFIGURED", mapOf())
            return Pair(emptyList(), ImpressionPayload())
        }

        // If no featureId provided, can only check global holdouts
        if (featureId == null) {
            serviceContainer?.getLoggerService()
                ?.log(LogLevelEnum.ERROR, "HOLDOUT_FEATURE_ID_NULL", mapOf())
        }

        val qualifiedHoldoutGroups = mutableListOf<HoldoutGroup>()
        val impressions = ImpressionPayload()

        for (holdoutGroup in holdoutGroups) {

            if (alreadyEvaluatedHoldoutIds.contains(holdoutGroup.id)) {
                // holdout was already evaluated for this {user + feature}
                serviceContainer?.getLoggerService()?.log(
                    level = LogLevelEnum.DEBUG,
                    key = "HOLDOUT_SKIP_EVALUATION",
                    map = mapOf(
                        "holdoutName" to "${holdoutGroup.name}",
                        "reason" to "user ${context.id} was already evaluated for feature with id: $featureId; SKIP decision making altogether.",
                    )
                )
                continue
            }

            if (!doesHoldoutApplyToFeature(holdoutGroup, featureId)) {
                continue
            }

            // Evaluate segmentation targeting
            val passesSegmentation = evaluateHoldoutSegmentation(holdoutGroup, context)
            if (!passesSegmentation) {
                serviceContainer?.getLoggerService()?.log(
                    LogLevelEnum.DEBUG,
                    "HOLDOUT_SEGMENTATION_FAIL",
                    mutableMapOf(
                        "userId" to "${context.id}",
                        "holdoutGroupName" to "${holdoutGroup.name}"
                    )
                )
                serviceContainer?.getLoggerService()?.log(
                    LogLevelEnum.INFO,
                    "SEGMENTATION_FAILED_HOLDOUT",
                    mapOf(
                        "holdoutId" to "${holdoutGroup.id}",
                        "userId" to "${context.id}"
                    )
                )
                holdoutGroup.id?.let { holdoutId ->
                    impressions.add(
                        campaignId = holdoutId,
                        variationId = VARIATION_NOT_PART_OF_HOLDOUT,
                        featureId = featureId ?: IMPRESSION_NO_FEATURE_ID
                    )
                }
                continue
            }

            serviceContainer?.getLoggerService()?.log(
                LogLevelEnum.INFO,
                "SEGMENTATION_PASSED_HOLDOUT",
                mapOf(
                    "holdoutId" to "${holdoutGroup.id}",
                    "userId" to "${context.id}"
                )
            )

            // Evaluate traffic percentage bucketing
            val shouldExcludeUser = shouldExcludeUserFromFeature(
                holdoutGroup = holdoutGroup,
                userId = context.id,
                accountId = settings.accountId,
                featureId = featureId
            )

            if (shouldExcludeUser) {

                serviceContainer?.getLoggerService()?.log(
                    LogLevelEnum.INFO, "USER_IN_HOLDOUT_GROUP", mapOf(
                        "userId" to "${context.id}",
                        "featureId" to "$featureId",
                        "holdoutGroupName" to "${holdoutGroup.name}",
                        "featureKey" to "$featureId"
                    )
                )

                qualifiedHoldoutGroups.add(holdoutGroup)

                holdoutGroup.id?.let { holdoutId ->
                    impressions.add(
                        campaignId = holdoutId,
                        variationId = VARIATION_IS_PART_OF_HOLDOUT,
                        featureId = featureId ?: IMPRESSION_NO_FEATURE_ID
                    )
                }
            } else {
                holdoutGroup.id?.let { holdoutId ->
                    impressions.add(
                        campaignId = holdoutId,
                        variationId = VARIATION_NOT_PART_OF_HOLDOUT,
                        featureId = featureId ?: IMPRESSION_NO_FEATURE_ID
                    )
                }
            }

        }

        return Pair(qualifiedHoldoutGroups, impressions)
    }

    private fun getLocalStorageDataForThisFeature(
        feature: Feature,
        context: VWOUserContext,
        storageService: StorageService
    ): Storage {
        val key = "${feature.key}"
        val storedDataMap: Map<String, Any>? =
            StorageDecorator().getFeatureFromStorage(featureKey = key, context, storageService)
        val storageMapAsString: String = VWOClient.objectMapper.writeValueAsString(
            obj = storedDataMap?.toMap() ?: emptyMap<String, Any>()
        )
        return VWOClient.objectMapper.readValue(
            json = storageMapAsString,
            clazz = Storage::class.java
        )
    }

    /**
     * Determines if a holdout group applies to the given feature.
     *
     * A holdout group applies to a feature if:
     * - It's a global holdout (applies to all features), OR
     * - It's a selective holdout and the featureId is in the holdout's featureIds list
     *
     * @param holdoutGroup The holdout group to check
     * @param featureId The feature ID to check against
     * @return true if the holdout applies to the feature, false otherwise
     */
    private fun doesHoldoutApplyToFeature(holdoutGroup: HoldoutGroup, featureId: Int?): Boolean {
        // Global holdouts apply to all features
        if (holdoutGroup.isGlobal == true) {
            return true
        }

        // For selective holdouts, check if featureId is in the list
        if (featureId != null) {
            val featureIds = holdoutGroup.featureIds
            if (!featureIds.isNullOrEmpty() && featureIds.contains(featureId)) {
                return true
            }
        }

        return false
    }

    /**
     * Evaluates segmentation targeting for a holdout group.
     *
     * If segments exist, this uses the existing SegmentationManager to validate
     * if the user passes the targeting rules. If no segments exist, returns true.
     *
     * @param holdoutGroup The holdout group with segments to evaluate
     * @param context The user context
     * @param settings The settings for contextual data
     * @return true if user passes segmentation (or no segments), false otherwise
     */
    private fun evaluateHoldoutSegmentation(
        holdoutGroup: HoldoutGroup,
        context: VWOUserContext,
    ): Boolean {
        val segments = holdoutGroup.segments

        // If no segments, user passes (holdout applies to all users after bucketing)
        if (segments.isNullOrEmpty()) {
            serviceContainer?.getLoggerService()?.log(
                LogLevelEnum.INFO,
                "HOLDOUT_SEGMENTATION_SKIP",
                mapOf(
                    "holdoutId" to "${holdoutGroup.id}",
                    "userId" to "${context.id}"
                )
            )
            return true
        }

        // Evaluate segments using existing segmentation logic
        val segmentationResult = serviceContainer?.getSegmentationManager()?.validateSegmentation(
            segments,
            context.customVariables
        )

        return (segmentationResult ?: false)
    }

    /**
     * Determines if a user should be excluded based on traffic percentage bucketing.
     *
     * Uses DecisionMaker with bucket key `{accountId}_{holdoutGroupId}_{userId}` for consistent assignment.
     * This matches the Node SDK format to ensure cross-platform consistency.
     * Traffic percentage (1-10) directly maps to bucket value threshold (1-100 scale).
     *
     * @param holdoutGroup The holdout group with traffic percentage
     * @param userId The user ID
     * @param accountId The account ID for bucket key generation
     * @return true if user should be excluded, false otherwise
     */
    private fun shouldExcludeUserFromFeature(
        holdoutGroup: HoldoutGroup,
        userId: String?,
        accountId: Int?,
        featureId: Int?,
    ): Boolean {
        if (userId == null || holdoutGroup.id == null || holdoutGroup.trafficPercent == null || accountId == null) {
            return false
        }

        // Match Node SDK format: ${accountId}_${holdoutId}_${userId}
        val bucketKey = "${accountId}_${holdoutGroup.id}_$userId"
        val bucketValue = decisionMaker.getBucketValueForUser(bucketKey)
        val trafficAllocation = holdoutGroup.trafficPercent ?: 0

        val isInHoldout = bucketValue != 0 && bucketValue <= trafficAllocation

        if (isInHoldout) {
            serviceContainer?.getLoggerService()?.log(
                LogLevelEnum.INFO,
                "HOLDOUT_SHOULD_EXCLUDE_USER",
                mapOf(
                    "userId" to userId,
                    "bucketValue" to "$bucketValue",
                    "holdoutGroupName" to "${holdoutGroup.name}",
                    "featureId" to "$featureId",
                    "percentTraffic" to "$trafficAllocation",
                    "isInHoldout" to "$isInHoldout",
                )
            )
        } else {
            serviceContainer?.getLoggerService()?.log(
                LogLevelEnum.DEBUG,
                "HOLDOUT_SHOULD_NOT_EXCLUDE_USER",
                mapOf(
                    "userId" to userId,
                    "holdoutGroupName" to "${holdoutGroup.name}",
                )
            )
        }

        return isInHoldout
    }

}