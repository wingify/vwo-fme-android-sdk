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
package com.vwo.services

import com.vwo.constants.Constants
import com.vwo.enums.CampaignTypeEnum
import com.vwo.models.Campaign
import com.vwo.models.Variation
import com.vwo.models.user.VWOContext
import com.vwo.packages.decision_maker.DecisionMaker
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.segmentation_evaluator.core.SegmentationManager

class CampaignDecisionService {
    /**
     * This method is used to check if the user is part of the campaign.
     * @param userId  User ID for which the check is to be performed.
     * @param campaign CampaignModel object containing the campaign settings.
     * @return  boolean value indicating if the user is part of the campaign.
     */
    fun isUserPartOfCampaign(userId: String?, campaign: Campaign?): Boolean {
        if (campaign == null || userId == null) {
            return false
        }
        // Check if the campaign is of type ROLLOUT or PERSONALIZE
        // If yes, set the traffic allocation to the weight of the first variation
        val trafficAllocation =
            if (campaign.type == CampaignTypeEnum.ROLLOUT.value || campaign.type == CampaignTypeEnum.PERSONALIZE.value) {
                campaign.variations!![0].weight
            } else {
                // If the campaign is of type AB, set the traffic allocation to the percent traffic of the campaign
                campaign.percentTraffic!!.toDouble()
            }

        // Get the bucket value assigned to the user
        val valueAssignedToUser =
            DecisionMaker().getBucketValueForUser(campaign.id.toString() + "_" + userId)
        val isUserPart = valueAssignedToUser != 0 && valueAssignedToUser <= trafficAllocation

        LoggerService.log(
            LogLevelEnum.INFO,
            "USER_PART_OF_CAMPAIGN",
            object : HashMap<String?, String?>() {
                init {
                    put("userId", userId)
                    put("campaignKey", campaign.ruleKey)
                    put("notPart", if (isUserPart) "" else "not")
                }
            })
        return isUserPart
    }

    /**
     * This method is used to get the variation for the user based on the bucket value.
     * @param variations  List of VariationModel objects containing the variations.
     * @param bucketValue  Bucket value assigned to the user.
     * @return  VariationModel object containing the variation for the user.
     */
    fun getVariation(variations: List<Variation>, bucketValue: Int): Variation? {
        for (variation in variations) {
            if (bucketValue >= variation.startRangeVariation && bucketValue <= variation.endRangeVariation) {
                return variation
            }
        }
        return null
    }

    /**
     * This method is used to check if the bucket value falls in the range of the variation.
     * @param variation  VariationModel object containing the variation settings.
     * @param bucketValue  Bucket value assigned to the user.
     * @return  VariationModel object containing the variation if the bucket value falls in the range, otherwise null.
     */
    fun checkInRange(variation: Variation, bucketValue: Int): Variation? {
        if (bucketValue >= variation.startRangeVariation && bucketValue <= variation.endRangeVariation) {
            return variation
        }
        return null
    }

    /**
     * This method is used to bucket the user to a variation based on the bucket value.
     * @param userId  User ID for which the bucketing is to be performed.
     * @param accountId  Account ID for which the bucketing is to be performed.
     * @param campaign  CampaignModel object containing the campaign settings.
     * @return  VariationModel object containing the variation allotted to the user.
     */
    fun bucketUserToVariation(userId: String?, accountId: String, campaign: Campaign?): Variation? {
        if (campaign == null || userId == null) {
            return null
        }

        val multiplier = if (campaign.percentTraffic != 0) 1 else 0
        val percentTraffic = campaign.percentTraffic!!
        val hashValue =
            DecisionMaker().generateHashValue(campaign.id.toString() + "_" + accountId + "_" + userId)
        val bucketValue =
            DecisionMaker().generateBucketValue(hashValue, Constants.MAX_TRAFFIC_VALUE, multiplier)

        LoggerService.log(
            LogLevelEnum.DEBUG,
            "USER_BUCKET_TO_VARIATION",
            object : HashMap<String?, String?>() {
                init {
                    put("userId", userId)
                    put("campaignKey", campaign.ruleKey)
                    put("percentTraffic", percentTraffic.toString())
                    put("bucketValue", bucketValue.toString())
                    put("hashValue", hashValue.toString())
                }
            })

        return campaign.variations?.let { getVariation(it, bucketValue) }
    }

    /**
     * This method is used to analyze the pre-segmentation decision for the user in the campaign.
     * @param campaign  CampaignModel object containing the campaign settings.
     * @param context  VWOContext object containing the user context.
     * @return  boolean value indicating if the user passes the pre-segmentation.
     */
    fun getPreSegmentationDecision(campaign: Campaign, context: VWOContext): Boolean {
        val campaignType = campaign.type

        val segments =
            if (campaignType == CampaignTypeEnum.ROLLOUT.value || campaignType == CampaignTypeEnum.PERSONALIZE.value) {
                campaign.variations!![0].segments
            } else if (campaignType == CampaignTypeEnum.AB.value) {
                campaign.segments
            } else {
                emptyMap()
            }

        if (segments!!.isEmpty()) {
            LoggerService.log(
                LogLevelEnum.INFO,
                "SEGMENTATION_SKIP",
                object : HashMap<String?, String?>() {
                    init {
                        put("userId", context.id)
                        put("campaignKey", campaign.ruleKey)
                    }
                })
            return true
        } else {
            val preSegmentationResult = SegmentationManager.validateSegmentation(
                segments, context.customVariables
            )
            LoggerService.log(
                LogLevelEnum.INFO,
                "SEGMENTATION_STATUS",
                object : HashMap<String?, String?>() {
                    init {
                        put("userId", context.id)
                        put("campaignKey", campaign.ruleKey)
                        put("status", if (preSegmentationResult) "passed" else "failed")
                    }
                })
            return preSegmentationResult
        }
    }

    /**
     * This method is used to get the variation allotted to the user in the campaign.
     * @param userId  User ID for which the variation is to be allotted.
     * @param accountId  Account ID for which the variation is to be allotted.
     * @param campaign  CampaignModel object containing the campaign settings.
     * @return  VariationModel object containing the variation allotted to the user.
     */
    fun getVariationAllotted(userId: String?, accountId: String, campaign: Campaign): Variation? {
        val isUserPart = isUserPartOfCampaign(userId, campaign)
        return if (campaign.type == CampaignTypeEnum.ROLLOUT.value || campaign.type == CampaignTypeEnum.PERSONALIZE.value) {
            if (isUserPart) campaign.variations!![0] else null
        } else {
            if (isUserPart) bucketUserToVariation(userId, accountId, campaign) else null
        }
    }
}