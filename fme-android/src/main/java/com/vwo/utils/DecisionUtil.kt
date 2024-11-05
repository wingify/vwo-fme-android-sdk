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

import com.vwo.VWOClient
import com.vwo.constants.Constants
import com.vwo.decorators.StorageDecorator
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
import com.vwo.utils.CampaignUtil.getGroupDetailsIfCampaignPartOfIt
import com.vwo.utils.MegUtil.evaluateGroups
import com.vwo.utils.UUIDUtils.getUUID
import com.vwo.models.Storage


/**
 * Utility object for decision-making operations.
 *
 * This object provides helper methods for making decisions based on various factors, such as user
 * eligibility for campaigns, feature variations, or other decision points within the application.
 */
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
        evaluatedFeatureMap: MutableMap<String, Any>,
        megGroupWinnerCampaigns: MutableMap<Int, String>?,
        storageService: StorageService?,
        decision: MutableMap<String, Any>
    ): MutableMap<String, Any?> {
        val vwoUserId = getUUID(context.id, settings.accountId.toString())
        val campaignId = campaign.id!!

        // If the campaign is of type AB, set the _vwoUserId for variation targeting variables
        if (campaign.type == CampaignTypeEnum.AB.value) {
            // set _vwoUserId for variation targeting variables
            context.variationTargetingVariables = object : HashMap<String, Any>() {
                init {
                    putAll(context.variationTargetingVariables)
                    val id = if (campaign.isUserListEnabled == true) vwoUserId else context.id
                    id?.let { put("_vwoUserId", it) }
                }
            }

            decision["variationTargetingVariables"] =
                context.variationTargetingVariables // for integration

            // check if the campaign satisfies the whitelisting
            if (campaign.isForcedVariationEnabled == true) {
                val whitelistedVariation = checkCampaignWhitelisting(campaign, context)
                if (whitelistedVariation != null) {
                    return object : HashMap<String, Any?>() {
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
        context.customVariables = object : HashMap<String, Any>() {
            init {
                putAll(context.customVariables)
                val id = if (campaign.isUserListEnabled == true) vwoUserId else context.id
                id?.let { put("_vwoUserId", it) }
            }
        }


        decision["customVariables"] = context.customVariables // for integration

        // Check if RUle being evaluated is part of Mutually Exclusive Group
        val id = if (campaign.type == CampaignTypeEnum.PERSONALIZE.value) campaign.variations?.get(0)?.id?:-1 else -1
            val groupId = getGroupDetailsIfCampaignPartOfIt(
                settings,
                campaign.id!!,
                id
            )["groupId"]
        if (groupId != null && !groupId.isEmpty()) {
            // check if the group is already evaluated for the user
            val groupWinnerCampaignId = megGroupWinnerCampaigns?.get(groupId.toInt())
            if (groupWinnerCampaignId != null && !groupWinnerCampaignId.isEmpty()) {
                if (campaign.type == CampaignTypeEnum.AB.value) {
                    if (groupWinnerCampaignId == campaignId.toString()) {
                        // If the campaign is the winner of the MEG, return true
                        return object : HashMap<String, Any?>() {
                            init {
                                put("preSegmentationResult", true)
                                put("whitelistedObject", null)
                            }
                        }
                    }
                } else if (campaign.type == CampaignTypeEnum.PERSONALIZE.value) {
                    // if personalise then check if the reqeusted variation is the winner
                    if (groupWinnerCampaignId == campaign.id.toString() + "_" + campaign.variations!![0].id) {
                        // If the campaign is the winner of the MEG, return true
                        return object : HashMap<String, Any?>() {
                            init {
                                put("preSegmentationResult", true)
                                put("whitelistedObject", null)
                            }
                        }
                    }
                }
                // If the campaign is not the winner of the MEG, return false
                return object : HashMap<String, Any?>() {
                    init {
                        put("preSegmentationResult", false)
                        put("whitelistedObject", null)
                    }
                }
            } else {
                // check in storage if the group is already evaluated for the user
                val storedDataMap = StorageDecorator().getFeatureFromStorage(
                    Constants.VWO_META_MEG_KEY + groupId, context,
                    storageService!!
                )
                try {
                    val storageMapAsString =
                        VWOClient.objectMapper.writeValueAsString(storedDataMap)
                    val storedData: Storage? = VWOClient.objectMapper.readValue(
                        storageMapAsString,
                        Storage::class.java
                    )
                    if (storedData != null && storedData.experimentId != null && storedData.experimentKey != null) {
                        log(
                            LogLevelEnum.INFO,
                            "MEG_CAMPAIGN_FOUND_IN_STORAGE",
                            object : HashMap<String?, String?>() {
                                init {
                                    put("campaignKey", storedData.experimentKey)
                                    put("userId", context.id)
                                }
                            })
                        if (storedData.experimentId === campaignId) {
                            if (campaign.type == CampaignTypeEnum.PERSONALIZE.value) {
                                // if personalise then check if the reqeusted variation is the winner
                                if (storedData.experimentVariationId==campaign.variations!![0].id) {
                                    return object : HashMap<String, Any?>() {
                                        init {
                                            put("preSegmentationResult", true)
                                            put("whitelistedObject", null)
                                        }
                                    }
                                } else {
                                    // store the campaign in local cache, so that it can be used later without looking into user storage again
                                    megGroupWinnerCampaigns?.set(groupId.toInt(),
                                        "${storedData.experimentId}_${storedData.experimentVariationId}"
                                    )
                                    return object : HashMap<String, Any?>() {
                                        init {
                                            put("preSegmentationResult", false)
                                            put("whitelistedObject", null)
                                        }
                                    }
                                }
                            } else {
                                // return the campaign if the called campaignId matches
                                return object : HashMap<String, Any?>() {
                                    init {
                                        put("preSegmentationResult", true)
                                        put("whitelistedObject", null)
                                    }
                                }
                            }
                        }
                        // if experimentId is not -1 then campaign is personalise campaign, store the details and return
                        if (storedData.experimentVariationId !== -1) {
                            megGroupWinnerCampaigns?.set(groupId.toInt(),
                                "${storedData.experimentId}_${storedData.experimentVariationId}"
                            )
                        } else {
                            // else store the campaignId only and return
                            megGroupWinnerCampaigns?.set(groupId.toInt(),
                                java.lang.String.valueOf(storedData.experimentId)
                            )
                        }
                        return object : HashMap<String, Any?>() {
                            init {
                                put("preSegmentationResult", false)
                                put("whitelistedObject", null)
                            }
                        }
                    }
                } catch (e: Exception) {
                    log(
                        LogLevelEnum.ERROR,
                        "STORED_DATA_ERROR",
                        object : HashMap<String?, String?>() {
                            init {
                                put("err", e.toString())
                            }
                        })
                }
            }
        }

        // If Whitelisting is skipped/failed, Check campaign's pre-segmentation
        val isPreSegmentationPassed =
            CampaignDecisionService().getPreSegmentationDecision(campaign, context)

        if (isPreSegmentationPassed && groupId != null && groupId.isNotEmpty()) {
            val variationModel = evaluateGroups(
                settings,
                feature,
                groupId.toInt(),
                evaluatedFeatureMap,
                context,
                storageService!!
            )
            // this condition would be true only when the current campaignId match with group winner campaignId
            // for personalise campaign, all personalise variations have same campaignId, so we check for campaignId_variationId
            if (variationModel?.id != null && variationModel.id == campaignId) {
                // if campaign is AB then return true
                if (variationModel.type== CampaignTypeEnum.AB.value) {
                    return object : HashMap<String, Any?>() {
                        init {
                            put("preSegmentationResult", true)
                            put("whitelistedObject", null)
                        }
                    }
                } else {
                    // if personalise then check if the requested variation is the winner
                    if (variationModel.variations[0].id == campaign.variations!![0].id) {
                        return object : HashMap<String, Any?>() {
                            init {
                                put("preSegmentationResult", true)
                                put("whitelistedObject", null)
                            }
                        }
                    } else {
                        // store the campaign in local cache, so that it can be used later
                        megGroupWinnerCampaigns?.set(groupId.toInt(),
                            variationModel.id.toString() + "_" + variationModel.variations[0].id
                        )
                        return object : HashMap<String, Any?>() {
                            init {
                                put("preSegmentationResult", false)
                                put("whitelistedObject", null)
                            }
                        }
                    }
                }
            } else if (variationModel?.id != null) { // when there is a winner but not the current campaign
                if (variationModel.type==CampaignTypeEnum.AB.value) {
                    // if campaign is AB then store only the campaignId
                    megGroupWinnerCampaigns?.set(groupId.toInt(), variationModel.id.toString())
                } else {
                    // if campaign is personalise then store the campaignId_variationId
                    megGroupWinnerCampaigns?.set(groupId.toInt(),
                        variationModel.id.toString() + "_" + variationModel.variations[0].id
                    )
                }
                return object : HashMap<String, Any?>() {
                    init {
                        put("preSegmentationResult", false)
                        put("whitelistedObject", null)
                    }
                }
            }
            // store -1 if no winner found, so that we don't evaluate the group again as the result would be the same for the current getFlag call
            megGroupWinnerCampaigns?.set(groupId.toInt(), "-1")
            return object : HashMap<String, Any?>() {
                init {
                    put("preSegmentationResult", false)
                    put("whitelistedObject", null)
                }
            }
        }
        return object : HashMap<String, Any?>() {
            init {
                put("preSegmentationResult", isPreSegmentationPassed)
                put("whitelistedObject", null)
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
                        put(
                            "campaignKey",
                            if (campaign.type.equals(CampaignTypeEnum.AB.value))
                                campaign.key
                            else
                                campaign.name + "_" + campaign.ruleKey
                        )
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
                put(
                    "campaignKey",
                    if (campaign.type.equals(CampaignTypeEnum.AB.value))
                        campaign.key
                    else
                        campaign.name + "_" + campaign.ruleKey
                )
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
            if (variation.segments.isEmpty()) {
                log(LogLevelEnum.INFO, "WHITELISTING_SKIP", object : HashMap<String?, String?>() {
                    init {
                        put("userId", context.id)
                        put(
                            "campaignKey",
                            if (campaign.type.equals(CampaignTypeEnum.AB.value))
                                campaign.key
                            else
                                campaign.name + "_" + campaign.ruleKey
                        )
                        put(
                            "variation",
                            if (variation.name?.isNotEmpty() == true) "for variation: " + variation.name else ""
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
