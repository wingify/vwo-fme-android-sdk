/**
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
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
package com.wingify.utils

import com.wingify.ServiceContainer
import com.wingify.WingifyClient
import com.wingify.constants.Constants
import com.wingify.decorators.StorageDecorator
import com.wingify.enums.CampaignTypeEnum
import com.wingify.enums.StatusEnum
import com.wingify.models.Campaign
import com.wingify.models.Feature
import com.wingify.models.Settings
import com.wingify.models.Variation
import com.wingify.models.user.WingifyUserContext
import com.wingify.packages.decision_maker.DecisionMaker
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.services.CampaignDecisionService
import com.wingify.services.StorageService
import com.wingify.models.Storage


/**
 * Utility object for decision-making operations.
 *
 * This object provides helper methods for making decisions based on various factors, such as user
 * eligibility for campaigns, feature variations, or other decision points within the application.
 */
class DecisionUtil {
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
        context: WingifyUserContext,
        evaluatedFeatureMap: MutableMap<String, Any>,
        megGroupWinnerCampaigns: MutableMap<Int, String>?,
        storageService: StorageService?,
        decision: MutableMap<String, Any>,
        serviceContainer: ServiceContainer
    ): MutableMap<String, Any?> {
        val vwoUserId = UUIDUtils.getUUID(context.id, settings.accountId.toString())
        val campaignId = campaign.id!!

        // Force/whitelist for Testing (AB), Rollout, and Personalize when enabled
        if (isForceWhitelistingEligible(campaign)) {
            // set _vwoUserId for variation targeting variables
            // Rollout / Personalize force lists store UUIDUtils.getUUID hashes;
            // Testing (AB) keeps plain ids unless isUserListEnabled.
            context.variationTargetingVariables = object : HashMap<String, Any>() {
                init {
                    putAll(context.variationTargetingVariables)
                    val id = forceMatchUserId(campaign, context.id, vwoUserId)
                    id?.let { put("_vwoUserId", it) }
                }
            }

            decision["variationTargetingVariables"] =
                context.variationTargetingVariables // for integration

            // check if the campaign satisfies the whitelisting
            if (campaign.isForcedVariationEnabled == true) {
                if (isRolloutForceOff(campaign, context, serviceContainer)) {
                    logWhitelistingStatus(
                        campaign,
                        context,
                        serviceContainer,
                        StatusEnum.PASSED,
                        messageKey = "WHITELISTING_FORCED_OFF"
                    )
                    return mutableMapOf(
                        "preSegmentationResult" to false,
                        "whitelistedObject" to null,
                    )
                }
                val whitelistedVariation =
                    checkCampaignWhitelisting(campaign, context, serviceContainer)
                if (whitelistedVariation != null) {
                    return object : HashMap<String, Any?>() {
                        init {
                            put("preSegmentationResult", true)
                            put("whitelistedObject", whitelistedVariation["variation"])
                        }
                    }
                }
            } else {
                serviceContainer.getLoggerService()?.log(
                    LogLevelEnum.INFO,
                    "WHITELISTING_SKIP",
                    object : HashMap<String?, String?>() {
                        init {
                            put("userId", context.id)
                            put("ruleType", forceRuleTypeLabel(campaign))
                            put("campaignKey", forceCampaignKey(campaign))
                            put("variation", "")
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
        val id =
            if (campaign.type == CampaignTypeEnum.PERSONALIZE.value) campaign.variations?.get(0)?.id
                ?: -1 else -1
        val groupId = CampaignUtil.getGroupDetailsIfCampaignPartOfIt(
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
                    val storageMapAsString: String = WingifyClient.objectMapper.writeValueAsString(
                        storedDataMap ?: emptyMap<String, Any>()
                    )
                    val storedData: Storage? = WingifyClient.objectMapper.readValue(
                        storageMapAsString,
                        Storage::class.java
                    )
                    if (storedData != null && storedData.isDecisionExpired()) {
                        serviceContainer.getLoggerService()?.log(
                            level = LogLevelEnum.WARN,
                            key = "MEG_DECISION_EXPIRED",
                            map = mapOf(
                                "groupId" to groupId,
                                "id" to "${context.id}"
                            )
                        )
                    } else if (storedData != null && storedData.experimentId != null && storedData.experimentKey != null) {
                        serviceContainer.getLoggerService()?.log(
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
                                if (storedData.experimentVariationId == campaign.variations!![0].id) {
                                    return object : HashMap<String, Any?>() {
                                        init {
                                            put("preSegmentationResult", true)
                                            put("whitelistedObject", null)
                                        }
                                    }
                                } else {
                                    // store the campaign in local cache, so that it can be used later without looking into user storage again
                                    megGroupWinnerCampaigns?.set(
                                        groupId.toInt(),
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
                            megGroupWinnerCampaigns?.set(
                                groupId.toInt(),
                                "${storedData.experimentId}_${storedData.experimentVariationId}"
                            )
                        } else {
                            // else store the campaignId only and return
                            megGroupWinnerCampaigns?.set(
                                groupId.toInt(),
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
                    serviceContainer.getLoggerService()?.log(
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
            CampaignDecisionService(serviceContainer).getPreSegmentationDecision(campaign, context)

        if (isPreSegmentationPassed && groupId != null && groupId.isNotEmpty()) {
            val variationModel = MegUtil().evaluateGroups(
                settings,
                feature,
                groupId.toInt(),
                evaluatedFeatureMap,
                context,
                storageService!!,
                serviceContainer
            )
            // this condition would be true only when the current campaignId match with group winner campaignId
            // for personalise campaign, all personalise variations have same campaignId, so we check for campaignId_variationId
            if (variationModel?.id != null && variationModel.id == campaignId) {
                // if campaign is AB then return true
                if (variationModel.type == CampaignTypeEnum.AB.value) {
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
                        megGroupWinnerCampaigns?.set(
                            groupId.toInt(),
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
                if (variationModel.type == CampaignTypeEnum.AB.value) {
                    // if campaign is AB then store only the campaignId
                    megGroupWinnerCampaigns?.set(groupId.toInt(), variationModel.id.toString())
                } else {
                    // if campaign is personalise then store the campaignId_variationId
                    megGroupWinnerCampaigns?.set(
                        groupId.toInt(),
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
        serviceContainer: ServiceContainer
    ): Variation? {
        return evaluateTrafficAndGetVariation(settings, campaign, null, serviceContainer)
    }

    /**
     * This method is used to evaluate the traffic for a given campaign and get the variation with custom bucketing support.
     * @param settings  SettingsModel object containing the account settings.
     * @param campaign  CampaignModel object containing the campaign settings.
     * @param userId   String containing the user ID.
     * @param context  WingifyUserContext object for custom bucketing seed resolution.
     * @return  VariationModel object containing the variation details.
     */
    fun evaluateTrafficAndGetVariation(
        settings: Settings,
        campaign: Campaign,
        context: WingifyUserContext?,
        serviceContainer: ServiceContainer
    ): Variation? {
        // Get the variation allotted to the user

        val userId = context?.id

        val variation = CampaignDecisionService(serviceContainer).getVariationAllotted(
            userId,
            settings.accountId.toString(),
            campaign,
            context
        )

        // Resolve bucketing ID for logging purposes
        val bucketingId = BucketingIdResolver.resolve(
            userId,
            context
        )
        val logUserId = BucketingIdResolver.formatUserIdForLogging(userId, bucketingId)

        if (variation == null) {
            serviceContainer.getLoggerService()?.log(
                LogLevelEnum.INFO,
                "USER_CAMPAIGN_BUCKET_INFO",
                object : HashMap<String?, String?>() {
                    init {
                        put("userId", logUserId)
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

        serviceContainer.getLoggerService()?.log(
            LogLevelEnum.INFO,
            "USER_CAMPAIGN_BUCKET_INFO",
            object : HashMap<String?, String?>() {
                init {
                    put("userId", logUserId)
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
        context: WingifyUserContext,
        serviceContainer: ServiceContainer
    ): Map<String, Any?>? {
        val whitelistingResult = evaluateWhitelisting(campaign, context, serviceContainer)
        val status = if (whitelistingResult != null) StatusEnum.PASSED else StatusEnum.FAILED
        val variationName =
            if (whitelistingResult != null) whitelistingResult["variationName"] as String? else null
        val variationString =
            if (!variationName.isNullOrEmpty()) "for variation: $variationName" else ""
        logWhitelistingStatus(campaign, context, serviceContainer, status, variationString)
        return whitelistingResult
    }

    private fun logWhitelistingStatus(
        campaign: Campaign,
        context: WingifyUserContext,
        serviceContainer: ServiceContainer,
        status: StatusEnum,
        variationString: String = "",
        messageKey: String = "WHITELISTING_STATUS"
    ) {
        serviceContainer.getLoggerService()
            ?.log(LogLevelEnum.INFO, messageKey, object : HashMap<String?, String?>() {
                init {
                    put("userId", context.id)
                    put("ruleType", forceRuleTypeLabel(campaign))
                    put("campaignKey", forceCampaignKey(campaign))
                    put("status", status.status)
                    put("variationString", variationString)
                }
            })
    }

    /**
     * Log label for force/whitelist messages (parity with Testing "experiment").
     */
    private fun forceRuleTypeLabel(campaign: Campaign): String {
        return when (campaign.type) {
            CampaignTypeEnum.ROLLOUT.value -> "rollout"
            CampaignTypeEnum.PERSONALIZE.value -> "personalize"
            else -> "experiment"
        }
    }

    /**
     * Identifier substituted into `{campaignKey}` on `WHITELISTING_SKIP`,
     * `WHITELISTING_STATUS`, and `WHITELISTING_FORCED_OFF`.
     *
     * Testing (AB) uses [Campaign.key]. Rollout / Personalize use `{name}_{ruleKey}`
     * when both are set (same shape as [com.wingify.services.CampaignDecisionService]
     * segmentation logs); otherwise [Campaign.key], then [Campaign.ruleKey], then
     * [Campaign.name].
     *
     * @param campaign Campaign being logged.
     * @return Log identifier; may be null when Testing [Campaign.key] is unset.
     */
    private fun forceCampaignKey(campaign: Campaign): String? {
        return if (campaign.type == CampaignTypeEnum.AB.value) {
            campaign.key
        } else {
            val name = campaign.name ?: ""
            val ruleKey = campaign.ruleKey ?: ""
            if (name.isNotEmpty() && ruleKey.isNotEmpty()) {
                name + "_" + ruleKey
            } else {
                campaign.key ?: ruleKey.ifEmpty { name }
            }
        }
    }

    /**
     * Whether this campaign type supports force/whitelisting evaluation.
     */
    private fun isForceWhitelistingEligible(campaign: Campaign): Boolean {
        val type = campaign.type
        return type == CampaignTypeEnum.AB.value ||
            type == CampaignTypeEnum.ROLLOUT.value ||
            type == CampaignTypeEnum.PERSONALIZE.value
    }

    /**
     * User id stored as `_vwoUserId` in [WingifyUserContext.variationTargetingVariables]
     * so force-list `user` operands can match.
     *
     * Rollout / Personalize always use [vwoUserId] because
     * [Variation.whitelistedSegments] stores [UUIDUtils.getUUID] hashes.
     * Testing (AB) uses [vwoUserId] only when [Campaign.isUserListEnabled] is true;
     * otherwise the raw [userId] in [Variation.segments].
     *
     * @param campaign Campaign whose type and [Campaign.isUserListEnabled] pick the id form.
     * @param userId Raw [WingifyUserContext.id].
     * @param vwoUserId [UUIDUtils.getUUID] of [userId] and the account id.
     * @return Value for `_vwoUserId`, or null when [userId] is null on the Testing raw-id path.
     */
    private fun forceMatchUserId(
        campaign: Campaign,
        userId: String?,
        vwoUserId: String
    ): String? {
        val type = campaign.type
        if (type == CampaignTypeEnum.ROLLOUT.value ||
            type == CampaignTypeEnum.PERSONALIZE.value
        ) {
            return vwoUserId
        }
        return if (campaign.isUserListEnabled == true) vwoUserId else userId
    }

    /**
     * Evaluate whitelisting for a campaign
     * @param campaign  Campaign object
     * @param context  Context object containing user information
     * @return  Whitelisted variation or null if not whitelisted
     */
    private fun evaluateWhitelisting(
        campaign: Campaign,
        context: WingifyUserContext,
        serviceContainer: ServiceContainer
    ): Map<String, Any?>? {
        // Rollout / Personalize: variations[0].whitelistedSegments → that variation
        if (campaign.type == CampaignTypeEnum.ROLLOUT.value ||
            campaign.type == CampaignTypeEnum.PERSONALIZE.value
        ) {
            return evaluateVariationWhitelistedSegments(campaign, context, serviceContainer)
        }

        // Testing (AB): variation-level segments (unchanged)
        val targetedVariations: MutableList<Variation> = ArrayList()

        for (variation in campaign.variations!!) {
            if (variation.segments.isEmpty()) {
                serviceContainer.getLoggerService()?.log(
                    LogLevelEnum.INFO,
                    "WHITELISTING_SKIP",
                    object : HashMap<String?, String?>() {
                        init {
                            put("userId", context.id)
                            put("ruleType", forceRuleTypeLabel(campaign))
                            put("campaignKey", forceCampaignKey(campaign))
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
                val segmentationResult =
                    serviceContainer.getSegmentationManager().validateSegmentation(
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
            // Use custom bucketing seed if enabled
            val bucketingId = BucketingIdResolver.resolve(
                context.id,
                context
            )
            whitelistedVariation = CampaignDecisionService(serviceContainer).getVariation(
                targetedVariations,
                DecisionMaker().calculateBucketValue(
                    CampaignUtil.getBucketingSeed(
                        bucketingId,
                        campaign,
                        null
                    )
                )
            )
        } else if (targetedVariations.size == 1) {
            whitelistedVariation = targetedVariations[0]
        }

        return whitelistingResultMap(whitelistedVariation)
    }

    /**
     * Force Off (`not` list) for Rollout only. Personalize has no Off list.
     * Same [com.wingify.packages.segmentation_evaluator] path as Force On,
     * on the inner operand of `not`.
     */
    private fun isRolloutForceOff(
        campaign: Campaign,
        context: WingifyUserContext,
        serviceContainer: ServiceContainer
    ): Boolean {
        if (campaign.type != CampaignTypeEnum.ROLLOUT.value) {
            return false
        }
        val operand = forceOffOperand(campaign.variations?.getOrNull(0)?.whitelistedSegments)
            ?: return false
        return serviceContainer.getSegmentationManager().validateSegmentation(
            operand,
            context.variationTargetingVariables
        )
    }

    /**
     * Unwraps the Force Off operand from [Variation.whitelistedSegments].
     *
     * Backend encodes Force Off as a `not` node. That node is either the root of
     * [segments] (Off list only) or nested under `and` when Force On and Force Off
     * are both present:
     *
     * - Off only: `{ "not": { "or": [{ "user": "..." }] } }`
     * - On + Off: `{ "and": [ { "or": [...] }, { "not": { "or": [...] } } ] }`
     *
     * Returns the inner operand of `not` so [isRolloutForceOff] can run the same
     * [com.wingify.packages.segmentation_evaluator] path as Force On. A match
     * means the user is on the Off list.
     *
     * @param segments [Variation.whitelistedSegments], or null if unset.
     * @return Inner map of the first `not` node, or null when there is no Off list.
     */
    @Suppress("UNCHECKED_CAST")
    private fun forceOffOperand(segments: Map<String, Any>?): Map<String, Any>? {
        if (segments == null) return null
        (segments["not"] as? Map<String, Any>)?.let { return it }
        val andList = segments["and"] as? List<*> ?: return null
        for (item in andList) {
            val map = item as? Map<*, *> ?: continue
            (map["not"] as? Map<String, Any>)?.let { return it }
        }
        return null
    }

    /**
     * Force On for Rollout / Personalize using [Variation.whitelistedSegments]
     * and existing [com.wingify.packages.segmentation_evaluator] validation.
     */
    private fun evaluateVariationWhitelistedSegments(
        campaign: Campaign,
        context: WingifyUserContext,
        serviceContainer: ServiceContainer
    ): Map<String, Any?>? {
        val variation = campaign.variations?.getOrNull(0) ?: return null
        val whitelistSegments = variation.whitelistedSegments
        if (whitelistSegments.isNullOrEmpty()) {
            serviceContainer.getLoggerService()?.log(
                LogLevelEnum.INFO,
                "WHITELISTING_SKIP",
                object : HashMap<String?, String?>() {
                    init {
                        put("userId", context.id)
                        put("ruleType", forceRuleTypeLabel(campaign))
                        put("campaignKey", forceCampaignKey(campaign))
                        put("variation", "")
                    }
                })
            return null
        }

        val segmentationResult =
            serviceContainer.getSegmentationManager().validateSegmentation(
                whitelistSegments,
                context.variationTargetingVariables
            )
        if (!segmentationResult) {
            return null
        }

        return whitelistingResultMap(FunctionUtil.cloneObject(variation) as Variation)
    }

    private fun whitelistingResultMap(whitelistedVariation: Variation?): Map<String, Any?>? {
        if (whitelistedVariation == null) {
            return null
        }
        val map: MutableMap<String, Any?> = HashMap()
        map["variation"] = whitelistedVariation
        map["variationName"] = whitelistedVariation.name
        map["variationId"] = whitelistedVariation.id
        return map
    }
}
