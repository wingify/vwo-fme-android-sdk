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
package com.vwo.utils

import com.vwo.enums.CampaignTypeEnum
import com.vwo.enums.StatusEnum
import com.vwo.models.Campaign
import com.vwo.models.Feature
import com.vwo.models.Settings
import com.vwo.models.Variation
import com.vwo.models.user.VWOContext
import com.vwo.packages.decision_maker.DecisionMaker
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.segmentation_evaluator.core.SegmentationManager
import com.vwo.services.CampaignDecisionService
import com.vwo.services.LoggerService.Companion.log
import com.vwo.services.StorageService

object DecisionUtil {
    /**
     * This method is used to evaluate the rule for a given feature and campaign.
     * @param settings  SettingsModel object containing the account settings.
     * @param feature   FeatureModel object containing the feature settings.
     * @param campaign  CampaignModel object containing the campaign settings.
     * @param context  VWOContext object containing the user context.
     * @param evaluatedFeatureMap  Map containing the evaluated feature map.
     * @param megGroupWinnerCampaigns  Map containing the MEG group winner campaigns.
     * @param decision  Map containing the decision object.
     * @return   Map containing the result of the evaluation.
     */
    fun checkWhitelistingAndPreSeg(
        settings: Settings,
        feature: Feature?,
        campaign: Campaign,
        context: VWOContext,
        evaluatedFeatureMap: MutableMap<String?, Any>,
        megGroupWinnerCampaigns: MutableMap<Int?, Int?>?,
        storageService: StorageService,
        decision: MutableMap<String?, Any?>
    ): Map<String, Any> {
        val vwoUserId = UUIDUtils.getUUID(context.id, settings.accountId.toString())
        val campaignId = campaign.id!!

        // If the campaign is of type AB, set the _vwoUserId for variation targeting variables
        if (campaign.type == CampaignTypeEnum.AB.value) {
            // set _vwoUserId for variation targeting variables
            context.variationTargetingVariables = object : HashMap<String?, Any?>() {
                init {
                    putAll(context.variationTargetingVariables)
                    put(
                        "_vwoUserId",
                        if (campaign.getIsUserListEnabled()) vwoUserId else context.id
                    )
                }
            }

            decision["variationTargetingVariables"] =
                context.variationTargetingVariables // for integration

            // check if the campaign satisfies the whitelisting
            if (campaign.getIsForcedVariationEnabled()) {
                val whitelistedVariation = checkCampaignWhitelisting(campaign, context)
                if (whitelistedVariation != null) {
                    return object : HashMap<String?, Any?>() {
                        init {
                            put("preSegmentationResult", true)
                            put("whitelistedObject", whitelistedVariation["variation"])
                        }
                    }
                }
            } else {
                log(LogLevelEnum.INFO, "WHITELISTING_SKIP", object : HashMap<String?, String?>() {
                    init {
                        put("userId", context.id)
                        put("campaignKey", campaign.ruleKey)
                    }
                })
            }
        }

        // set _vwoUserId for custom variables
        context.customVariables = object : HashMap<String?, Any?>() {
            init {
                putAll(context.customVariables)
                put("_vwoUserId", if (campaign.getIsUserListEnabled()) vwoUserId else context.id)
            }
        }


        decision["customVariables"] = context.customVariables // for integration

        // Check if RUle being evaluated is part of Mutually Exclusive Group
        val groupId =
            CampaignUtil.getGroupDetailsIfCampaignPartOfIt(settings, campaignId)["groupId"]
        if (groupId != null && !groupId.isEmpty()) {
            val groupWinnerCampaignId = megGroupWinnerCampaigns!![groupId.toInt()]
            if (groupWinnerCampaignId != null && !groupWinnerCampaignId.toString()
                    .isEmpty() && groupWinnerCampaignId == campaignId
            ) {
                // If the campaign is the winner of the MEG, return true
                return object : HashMap<String, Any>() {
                    init {
                        put("preSegmentationResult", true)
                        remove("whitelistedObject")
                    }
                }
            } else if (groupWinnerCampaignId != null && groupWinnerCampaignId.toString().isNotEmpty()) {
                // If the campaign is not the winner of the MEG, return false
                return object : HashMap<String, Any>() {
                    init {
                        put("preSegmentationResult", false)
                        remove("whitelistedObject")
                    }
                }
            }
        }

        // If Whitelisting is skipped/failed, Check campaign's pre-segmentation
        val isPreSegmentationPassed =
            CampaignDecisionService().getPreSegmentationDecision(campaign, context)

        if (isPreSegmentationPassed && !groupId.isNullOrEmpty()) {
            val variationModel = MegUtil.evaluateGroups(
                settings,
                feature,
                groupId.toInt(),
                evaluatedFeatureMap,
                context,
                storageService
            )
            if (variationModel?.id != null && variationModel.id == campaignId) {
                return object : HashMap<String, Any>() {
                    init {
                        put("preSegmentationResult", true)
                        remove("whitelistedObject")
                    }
                }
            }
            megGroupWinnerCampaigns!![groupId.toInt()] = variationModel?.id?:0
            return object : HashMap<String, Any>() {
                init {
                    put("preSegmentationResult", false)
                    remove("whitelistedObject")
                }
            }
        }
        return object : HashMap<String, Any>() {
            init {
                put("preSegmentationResult", isPreSegmentationPassed)
                remove("whitelistedObject")
            }
        }
    }

    /**
     * This method is used to evaluate the traffic for a given campaign and get the variation.
     * @param settings  SettingsModel object containing the account settings.
     * @param campaign  CampaignModel object containing the campaign settings.
     * @param userId   String containing the user ID.
     * @return  VariationModel object containing the variation details.
     */
    fun evaluateTrafficAndGetVariation(
        settings: Settings,
        campaign: Campaign,
        userId: String?
    ): Variation? {
        // Get the variation allotted to the user

        val variation = CampaignDecisionService().getVariationAllotted(
            userId,
            settings.accountId.toString(),
            campaign
        )
        if (variation == null) {
            log(
                LogLevelEnum.INFO,
                "USER_CAMPAIGN_BUCKET_INFO",
                object : HashMap<String?, String?>() {
                    init {
                        put("userId", userId)
                        put("campaignKey", campaign.ruleKey)
                        put("status", "did not get any variation")
                    }
                })
            return null
        }

        log(LogLevelEnum.INFO, "USER_CAMPAIGN_BUCKET_INFO", object : HashMap<String?, String?>() {
            init {
                put("userId", userId)
                put("campaignKey", campaign.ruleKey)
                put("status", "got variation: " + variation.name)
            }
        })
        return variation
    }

    /**
     * Check for whitelisting
     * @param campaign   Campaign object
     * @param context  Context object containing user information
     * @return   Whitelisted variation or null if not whitelisted
     */
    private fun checkCampaignWhitelisting(
        campaign: Campaign,
        context: VWOContext
    ): Map<String, Any?>? {
        val whitelistingResult = evaluateWhitelisting(campaign, context)
        val status = if (whitelistingResult != null) StatusEnum.PASSED else StatusEnum.FAILED
        val variationString =
            if (whitelistingResult != null) whitelistingResult["variationName"] as String? else ""
        log(LogLevelEnum.INFO, "WHITELISTING_STATUS", object : HashMap<String?, String?>() {
            init {
                put("userId", context.id)
                put("campaignKey", campaign.ruleKey)
                put("status", status.status)
                put("variationString", variationString)
            }
        })
        return whitelistingResult
    }

    /**
     * Evaluate whitelisting for a campaign
     * @param campaign  Campaign object
     * @param context  Context object containing user information
     * @return  Whitelisted variation or null if not whitelisted
     */
    private fun evaluateWhitelisting(campaign: Campaign, context: VWOContext): Map<String, Any?>? {
        val targetedVariations: MutableList<Variation> = ArrayList()

        for (variation in campaign.variations!!) {
            if (variation.segments != null && variation.segments.isEmpty()) {
                log(LogLevelEnum.INFO, "WHITELISTING_SKIP", object : HashMap<String?, String?>() {
                    init {
                        put("userId", context.id)
                        put("campaignKey", campaign.ruleKey)
                        put(
                            "variation",
                            if (!variation.name!!.isEmpty()) "for variation: " + variation.name else ""
                        )
                    }
                })
                continue
            }

            // Check for segmentation and evaluate
            if (variation.segments != null) {
                val segmentationResult = SegmentationManager.validateSegmentation(
                    variation.segments,
                    context.variationTargetingVariables
                )

                if (segmentationResult) {
                    targetedVariations.add(FunctionUtil.cloneObject(variation) as Variation)
                }
            }
        }

        var whitelistedVariation: Variation? = null

        if (targetedVariations.size > 1) {
            CampaignUtil.scaleVariationWeights(targetedVariations)
            var currentAllocation = 0
            var stepFactor: Int
            for (variation in targetedVariations) {
                stepFactor = CampaignUtil.assignRangeValues(variation, currentAllocation)
                currentAllocation += stepFactor
            }
            whitelistedVariation = CampaignDecisionService().getVariation(
                targetedVariations,
                DecisionMaker().calculateBucketValue(
                    CampaignUtil.getBucketingSeed(
                        context.id,
                        campaign,
                        null
                    )
                )
            )
        } else if (targetedVariations.size == 1) {
            whitelistedVariation = targetedVariations[0]
        }

        if (whitelistedVariation != null) {
            val map: MutableMap<String, Any?> = HashMap()
            map["variation"] = whitelistedVariation
            map["variationName"] = whitelistedVariation.name
            map["variationId"] = whitelistedVariation.id
            return map
        }

        return null
    }
}
