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

import com.fasterxml.jackson.core.JsonProcessingException
import com.vwo.VWOClient
import com.vwo.constants.Constants
import com.vwo.decorators.StorageDecorator
import com.vwo.enums.CampaignTypeEnum
import com.vwo.models.Campaign
import com.vwo.models.Feature
import com.vwo.models.Settings
import com.vwo.models.Storage
import com.vwo.models.Variation
import com.vwo.models.user.VWOContext
import com.vwo.packages.decision_maker.DecisionMaker
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.CampaignDecisionService
import com.vwo.services.LoggerService.Companion.log
import com.vwo.services.StorageService
import com.vwo.utils.CampaignUtil.getBucketingSeed
import com.vwo.utils.CampaignUtil.setCampaignAllocation
import com.vwo.utils.FunctionUtil.cloneObject
import java.util.stream.Collectors


/**
 * Utility object for MEG-related operations.
 *
 * This object provides helper methods for working with MEG.
 */
object MegUtil {
    /**
     * Evaluates groups for a given feature and group ID.
     *
     * @param settings - The settings model.
     * @param feature - The feature model to evaluate.
     * @param groupId - The ID of the group.
     * @param evaluatedFeatureMap - A map containing evaluated features.
     * @param context - The context model.
     * @return The evaluation result.
     */
    fun evaluateGroups(
        settings: Settings,
        feature: Feature?,
        groupId: Int,
        evaluatedFeatureMap: MutableMap<String, Any>,
        context: VWOContext,
        storageService: StorageService
    ): Variation? {
        val featureToSkip: MutableList<String?> = ArrayList()
        val campaignMap: MutableMap<String, MutableList<Campaign>> = HashMap()

        // get all feature keys and all campaignIds from the groupId
        val featureKeysAndGroupCampaignIds = getFeatureKeysFromGroup(settings, groupId)
        val featureKeys = featureKeysAndGroupCampaignIds["featureKeys"] as List<String>?
        val groupCampaignIds:List<String>? = featureKeysAndGroupCampaignIds["groupCampaignIds"] as List<String>?

        for (featureKey in featureKeys!!) {
            val currentFeature = FunctionUtil.getFeatureFromKey(settings, featureKey)

            // check if the feature is already evaluated
            if (currentFeature == null || featureToSkip.contains(featureKey)) {
                continue
            }

            // evaluate the feature rollout rules
            val isRolloutRulePassed = isRolloutRuleForFeaturePassed(
                settings,
                currentFeature,
                evaluatedFeatureMap,
                featureToSkip,
                context,
                storageService
            )
            if (isRolloutRulePassed) {
                for (feature1 in settings.features) {
                    if (feature1.key == featureKey) {
                        for (campaign in feature1.rulesLinkedCampaign) {
                            if (groupCampaignIds!!.contains(campaign.id.toString())
                                || groupCampaignIds.contains(campaign.id.toString()
                                        + "_" + campaign.variations!![0].id)) {

                                campaignMap.getOrPut(featureKey) { mutableListOf() }
                                val campaigns = campaignMap[featureKey]
                                if (campaigns!!.none { it.ruleKey == campaign.ruleKey }) {
                                    campaigns.add(campaign)
                                }
                            }
                        }
                    }
                }
            }
        }

        val eligibleCampaignsMap =
            getEligibleCampaigns(settings, campaignMap, context, storageService)
        val eligibleCampaigns: List<Campaign>? =
            eligibleCampaignsMap["eligibleCampaigns"] as List<Campaign>?
        val eligibleCampaignsWithStorage: List<Campaign>? =
            eligibleCampaignsMap["eligibleCampaignsWithStorage"] as List<Campaign>?

        return findWinnerCampaignAmongEligibleCampaigns(
            settings,
            feature!!.key,
            eligibleCampaigns,
            eligibleCampaignsWithStorage,
            groupId,
            context,
            storageService
        )
    }

    /**
     * Retrieves feature keys associated with a group based on the group ID.
     *
     * @param settings - The settings model.
     * @param groupId - The ID of the group.
     * @return An object containing feature keys and group campaign IDs.
     */
    fun getFeatureKeysFromGroup(settings: Settings, groupId: Int): Map<String, List<Any>?> {
        val groupCampaignIds = CampaignUtil.getCampaignsByGroupId(settings, groupId)
        val featureKeys = CampaignUtil.getFeatureKeysFromCampaignIds(settings, groupCampaignIds)

        val result: MutableMap<String, List<Any>?> = HashMap()
        result["featureKeys"] = featureKeys
        result["groupCampaignIds"] = groupCampaignIds

        return result
    }

    /**
     * Evaluates the feature rollout rules for a given feature.
     *
     * @param settings - The settings model.
     * @param feature - The feature model to evaluate.
     * @param evaluatedFeatureMap - A map containing evaluated features.
     * @param featureToSkip - A list of features to skip during evaluation.
     * @param context - The context model.
     * @return true if the feature passes the rollout rules, false otherwise.
     */
    private fun isRolloutRuleForFeaturePassed(
        settings: Settings, feature: Feature, evaluatedFeatureMap: MutableMap<String, Any>,
        featureToSkip: MutableList<String?>, context: VWOContext,
        storageService: StorageService
    ): Boolean {
        val featureKey = feature.key ?: return false
        if (evaluatedFeatureMap.containsKey(featureKey) &&
            (evaluatedFeatureMap[featureKey] as Map<*, *>?)?.containsKey("rolloutId") == true
        ) {
            return true
        }

        val rollOutRules: List<Campaign> =
            FunctionUtil.getSpecificRulesBasedOnType(feature, CampaignTypeEnum.ROLLOUT)
        if (rollOutRules.isNotEmpty()) {
            var ruleToTestForTraffic: Campaign? = null

            for (rule in rollOutRules) {
                val preSegmentationResult = RuleEvaluationUtil.evaluateRule(
                    settings,
                    feature,
                    rule,
                    context,
                    evaluatedFeatureMap,
                    null,
                    storageService,
                    HashMap()
                )
                if ((preSegmentationResult["preSegmentationResult"] as Boolean?)!!) {
                    ruleToTestForTraffic = rule
                    break
                }
            }

            if (ruleToTestForTraffic != null) {
                val variation: Variation? = DecisionUtil.evaluateTrafficAndGetVariation(
                    settings,
                    ruleToTestForTraffic,
                    context.id
                )
                if (variation != null) {
                    val rollOutInformation: MutableMap<String, Any> = HashMap()
                    variation.id?.let { rollOutInformation["rolloutId"] = it }
                    variation.name?.let { rollOutInformation["rolloutKey"] = it }
                    variation.id?.let { rollOutInformation["rolloutVariationId"] = it }
                    evaluatedFeatureMap[featureKey] = rollOutInformation

                    return true
                }
            }

            // no rollout rule passed
            featureToSkip.add(featureKey)
            return false
        }

        // no rollout rule, evaluate experiments
        log(LogLevelEnum.INFO, "MEG_SKIP_ROLLOUT_EVALUATE_EXPERIMENTS",
            object : HashMap<String?, String?>() {
                init {
                    put("featureKey", featureKey)
                }
            })
        return true
    }

    /**
     * Retrieves eligible campaigns based on the provided campaign map and context.
     *
     * @param settings - The settings model.
     * @param campaignMap - A map containing feature keys and corresponding campaigns.
     * @param context - The context model.
     * @return An object containing eligible campaigns, campaigns with storage, and ineligible campaigns.
     */
    private fun getEligibleCampaigns(
        settings: Settings, campaignMap: Map<String, MutableList<Campaign>>,
        context: VWOContext, storageService: StorageService
    ): Map<String, Any> {
        val eligibleCampaigns: MutableList<Campaign> = ArrayList<Campaign>()
        val eligibleCampaignsWithStorage: MutableList<Campaign> = ArrayList<Campaign>()
        val inEligibleCampaigns: MutableList<Campaign> = ArrayList<Campaign>()

        for ((featureKey, campaigns) in campaignMap) {
            for (campaign in campaigns) {
                val storedDataMap: Map<String, Any>? =
                    StorageDecorator().getFeatureFromStorage(featureKey, context, storageService)
                try {
                    val storageMapAsString: String =
                        VWOClient.objectMapper.writeValueAsString(storedDataMap)
                    val storedData: Storage? = VWOClient.objectMapper.readValue(storageMapAsString, Storage::class.java)
                    if (storedData?.experimentVariationId != null
                        && storedData.experimentVariationId.toString().isNotEmpty()) {

                        if (!storedData.experimentKey.isNullOrEmpty() && storedData.experimentKey == campaign.key) {
                            val variation: Variation? = CampaignUtil.getVariationFromCampaignKey(
                                settings,
                                storedData.experimentKey,
                                storedData.experimentVariationId!!
                            )
                            if (variation != null) {
                                log(
                                    LogLevelEnum.INFO,
                                    "MEG_CAMPAIGN_FOUND_IN_STORAGE",
                                    object : HashMap<String?, String?>() {
                                        init {
                                            put("campaignKey", storedData.experimentKey)
                                            put("userId", context.id)
                                        }
                                    })
                                if (eligibleCampaignsWithStorage.none { c -> c.key == campaign.key }) {
                                    eligibleCampaignsWithStorage.add(campaign)
                                }
                                continue
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
                // Check if user is eligible for the campaign
                if (CampaignDecisionService().getPreSegmentationDecision(campaign, context) &&
                    CampaignDecisionService().isUserPartOfCampaign(context.id, campaign)
                ) {
                    log(
                        LogLevelEnum.INFO,
                        "MEG_CAMPAIGN_ELIGIBLE",
                        object : HashMap<String?, String?>() {
                            init {
                                put(
                                    "campaignKey",
                                    if (campaign.type == CampaignTypeEnum.AB.value) campaign.key else campaign.name + "_" + campaign.ruleKey
                                )
                                put("userId", context.id)
                            }
                        })
                    eligibleCampaigns.add(campaign)
                    continue
                }

                inEligibleCampaigns.add(campaign)
            }
        }

        val result: MutableMap<String, Any> = HashMap()
        result["eligibleCampaigns"] = eligibleCampaigns
        result["eligibleCampaignsWithStorage"] = eligibleCampaignsWithStorage
        result["inEligibleCampaigns"] = inEligibleCampaigns

        return result
    }

    /**
     * Evaluates the eligible campaigns and determines the winner campaign.
     *
     * @param settings - The settings model.
     * @param featureKey - The key of the feature.
     * @param eligibleCampaigns - A list of eligible campaigns.
     * @param eligibleCampaignsWithStorage - A list of eligible campaigns with storage.
     * @param groupId - The ID of the group.
     * @param context - The context model.
     * @param storageService - The storage service.
     * @return The winner campaign.
     */
    private fun findWinnerCampaignAmongEligibleCampaigns(
        settings: Settings, featureKey: String?,
        eligibleCampaigns: List<Campaign>?,
        eligibleCampaignsWithStorage: List<Campaign>?,
        groupId: Int, context: VWOContext, storageService: StorageService
    ): Variation? {
        val campaignIds = CampaignUtil.getCampaignIdsFromFeatureKey(settings, featureKey)
        var winnerCampaign: Variation? = null
        try {
            val group = settings.groups!![groupId.toString()]
            val megAlgoNumber = if (group != null && !group.getEt().toString().isEmpty()
            ) group.getEt()!! else Constants.RANDOM_ALGO
            if (eligibleCampaignsWithStorage!!.size == 1) {
                try {
                    val campaignModel: String = VWOClient.objectMapper.writeValueAsString(
                        eligibleCampaignsWithStorage[0]
                    )
                    winnerCampaign =
                        VWOClient.objectMapper.readValue(campaignModel, Variation::class.java)
                } catch (e: JsonProcessingException) {
                    throw RuntimeException(e)
                }
                val finalWinnerCampaign: Variation? = winnerCampaign
                val map = mutableMapOf<String, String>().apply {
                    put(
                        "campaignKey",
                        (if (finalWinnerCampaign?.type.equals(CampaignTypeEnum.AB.value)) finalWinnerCampaign!!.key else finalWinnerCampaign!!.name + "_" + finalWinnerCampaign.ruleKey)!!
                    )
                    put("groupId", groupId.toString())
                    context.id?.let { put("userId", it) }
                }
                log(
                    LogLevelEnum.INFO,
                    "MEG_WINNER_CAMPAIGN",
                    map as Map<String?, String?>
                )
            } else if (eligibleCampaignsWithStorage.size > 1 && megAlgoNumber == Constants.RANDOM_ALGO) {
                winnerCampaign = normalizeWeightsAndFindWinningCampaign(
                    eligibleCampaignsWithStorage,
                    context,
                    campaignIds,
                    groupId,
                    storageService
                )
            } else if (eligibleCampaignsWithStorage.size > 1) {
                winnerCampaign = getCampaignUsingAdvancedAlgo(
                    settings,
                    eligibleCampaignsWithStorage,
                    context,
                    campaignIds,
                    groupId,
                    storageService
                )
            }

            if (eligibleCampaignsWithStorage.isEmpty()) {
                if (eligibleCampaigns!!.size == 1) {
                    try {
                        val campaignModel: String = VWOClient.objectMapper.writeValueAsString(
                            eligibleCampaigns[0]
                        )
                        winnerCampaign =
                            VWOClient.objectMapper.readValue(campaignModel, Variation::class.java)
                    } catch (e: JsonProcessingException) {
                        throw RuntimeException(e)
                    }
                    val finalWinnerCampaign1: Variation = winnerCampaign
                    log(
                        LogLevelEnum.INFO,
                        "MEG_WINNER_CAMPAIGN",
                        object : HashMap<String?, String?>() {
                            init {
                                put("campaignKey",
                                    if (finalWinnerCampaign1.type.equals(CampaignTypeEnum.AB.value))
                                        finalWinnerCampaign1.key
                                    else
                                        finalWinnerCampaign1.name + "_" + finalWinnerCampaign1.ruleKey
                                )
                                put("groupId", groupId.toString())
                                put("userId", context.id)
                                put("algo", "")
                            }
                        })
                } else if (eligibleCampaigns.size > 1 && megAlgoNumber == Constants.RANDOM_ALGO) {
                    winnerCampaign = normalizeWeightsAndFindWinningCampaign(
                        eligibleCampaigns,
                        context,
                        campaignIds,
                        groupId,
                        storageService
                    )
                } else if (eligibleCampaigns.size > 1) {
                    winnerCampaign = getCampaignUsingAdvancedAlgo(
                        settings,
                        eligibleCampaigns,
                        context,
                        campaignIds,
                        groupId,
                        storageService
                    )
                }
            }
        } catch (exception: Exception) {
            log(LogLevelEnum.ERROR,
                "MEG: error inside findWinnerCampaignAmongEligibleCampaigns$exception")
        }
        return winnerCampaign
    }

    /**
     * Normalizes the weights of shortlisted campaigns and determines the winning campaign using random allocation.
     *
     * @param shortlistedCampaigns - A list of shortlisted campaigns.
     * @param context - The context model.
     * @param calledCampaignIds - A list of campaign IDs that have been called.
     * @param groupId - The ID of the group.
     * @return The winning campaign or null if none is found.
     */
    private fun normalizeWeightsAndFindWinningCampaign(
        shortlistedCampaigns: List<Campaign>?,
        context: VWOContext, calledCampaignIds: List<Int?>?, groupId: Int, storageService:StorageService
    ): Variation? {
        try {
            shortlistedCampaigns?.forEach{ campaign: Campaign ->
                campaign.weight = Math.round(100.0 / shortlistedCampaigns.size) * 10000 / 10000.0
            }

            val variations: List<Variation> = shortlistedCampaigns?.mapNotNull { campaign ->
                try {
                    val campaignModel = VWOClient.objectMapper.writeValueAsString(campaign)
                    VWOClient.objectMapper.readValue(campaignModel, Variation::class.java)
                } catch (e: JsonProcessingException) {
                    null // Optionally log the error or handle it as needed
                }
            } ?: emptyList()


            CampaignUtil.setCampaignAllocation(variations)
            val winnerVariation: Variation? = CampaignDecisionService().getVariation(
                variations,
                DecisionMaker().calculateBucketValue(
                    CampaignUtil.getBucketingSeed(context.id, null, groupId)
                )
            )

            if (winnerVariation != null) {
            log(
                LogLevelEnum.INFO,
                "MEG_WINNER_CAMPAIGN",
                object : HashMap<String?, String?>() {
                    init {
                        put("campaignKey", if(winnerVariation.type==CampaignTypeEnum.AB.value) winnerVariation.key else winnerVariation.name + "_" + winnerVariation.ruleKey)
                        put("groupId", groupId.toString())
                        put("userId", context.id)
                        put("algo", "using random algorithm")
                    }
                })

                val storageMap: MutableMap<String, Any> = java.util.HashMap()
                storageMap["featureKey"] = Constants.VWO_META_MEG_KEY + groupId
                storageMap["userId"] = context.id?:0
                storageMap["experimentId"] = winnerVariation.id?:0
                storageMap["experimentKey"] = winnerVariation.key?:""
                storageMap["experimentVariationId"] = if (winnerVariation.type.equals(CampaignTypeEnum.PERSONALIZE.value)) winnerVariation.variations[0].id?:-1 else -1
                StorageDecorator().setDataInStorage(storageMap, storageService)

                if (calledCampaignIds!!.contains(winnerVariation.id)) {
                    return winnerVariation
                }
            } else {
                log(LogLevelEnum.INFO, "No winner campaign found for MEG group: $groupId")
            }
        } catch (exception: Exception) {
            log(
                LogLevelEnum.ERROR,
                "MEG: error inside normalizeWeightsAndFindWinningCampaign"
            )
        }
        return null
    }

    /**
     * Advanced algorithm to find the winning campaign based on priority order and weighted random distribution.
     *
     * @param settings - The settings model.
     * @param shortlistedCampaigns - A list of shortlisted campaigns.
     * @param context - The context model.
     * @param calledCampaignIds - A list of campaign IDs that have been called.
     * @param groupId - The ID of the group.
     * @param storageService - The storage service.
     * @return The winning campaign or null if none is found.
     */
    private fun getCampaignUsingAdvancedAlgo(
        settings: Settings,
        shortlistedCampaigns: List<Campaign>,
        context: VWOContext,
        calledCampaignIds: List<Int?>,
        groupId: Int,
        storageService: StorageService
    ): Variation? {
        var winnerCampaign: Variation? = null
        var found = false
        try {
            val group = settings.groups!![groupId.toString()]
            val priorityOrder: List<String>? = if (group != null && !group.p!!.isEmpty()
            ) group.p else java.util.ArrayList()
            val wt = if (group != null && !group.wt!!.isEmpty()
            ) group.wt else java.util.HashMap()
            for (integer in priorityOrder!!) {
                for (shortlistedCampaign in shortlistedCampaigns) {
                    if (shortlistedCampaign.id.toString() == integer) {
                        val campaignModel = VWOClient.objectMapper.writeValueAsString(
                            cloneObject(shortlistedCampaign)
                        )
                        winnerCampaign = VWOClient.objectMapper.readValue(
                            campaignModel,
                            Variation::class.java
                        )
                        found = true
                        break
                    } else if ((shortlistedCampaign.id.toString() + "_" + shortlistedCampaign.variations!![0].id) == integer) {
                        val campaignModel = VWOClient.objectMapper.writeValueAsString(
                            cloneObject(shortlistedCampaign)
                        )
                        winnerCampaign = VWOClient.objectMapper.readValue(
                            campaignModel,
                            Variation::class.java
                        )
                        found = true
                        break
                    }
                }
                if (found) break
            }

            if (winnerCampaign == null) {
                val participatingCampaignList: MutableList<Campaign?> = java.util.ArrayList()
                for (campaign in shortlistedCampaigns) {
                    val campaignId = campaign.id!!
                    if (wt!!.containsKey(campaignId.toString())) {
                        val clonedCampaign = cloneObject(campaign) as Campaign?
                        clonedCampaign!!.weight = wt!![campaignId.toString()]!!
                        participatingCampaignList.add(clonedCampaign)
                    } else if (wt!!.containsKey(campaignId.toString() + "_" + campaign.variations!![0].id)) {
                        val clonedCampaign = cloneObject(campaign) as Campaign?
                        clonedCampaign!!.weight =
                            wt!![campaignId.toString() + "_" + campaign.variations!![0].id]!!
                        participatingCampaignList.add(clonedCampaign)
                    }
                }

                val variations = participatingCampaignList.map { campaign: Campaign? ->
                        try {
                            val campaignModel = VWOClient.objectMapper.writeValueAsString(campaign)
                            return@map VWOClient.objectMapper.readValue<Variation>(
                                campaignModel,
                                Variation::class.java
                            )
                        } catch (e: JsonProcessingException) {
                            throw java.lang.RuntimeException(e)
                        }
                    }

                setCampaignAllocation(variations)
                winnerCampaign = CampaignDecisionService().getVariation(
                    variations,
                    DecisionMaker().calculateBucketValue(
                        getBucketingSeed(
                            context.id,
                            null,
                            groupId
                        )
                    )
                )
            }

            val finalWinnerCampaign = winnerCampaign


            if (winnerCampaign != null) {
                log(
                    LogLevelEnum.INFO,
                    "MEG_WINNER_CAMPAIGN",
                    object : java.util.HashMap<String?, String?>() {
                        init {
                            put(
                                "campaignKey",
                                if (finalWinnerCampaign!!.type == CampaignTypeEnum.AB.value) finalWinnerCampaign!!.key else finalWinnerCampaign!!.name + "_" + finalWinnerCampaign!!.ruleKey
                            )
                            put("groupId", groupId.toString())
                            put("userId", context.id)
                            put("algo", "using advanced algorithm")
                        }
                    })

                val storageMap: MutableMap<String, Any> = java.util.HashMap()
                storageMap["featureKey"] = Constants.VWO_META_MEG_KEY + groupId
                storageMap["userId"] = context.id?:0
                storageMap["experimentId"] = winnerCampaign.id?:0
                storageMap["experimentKey"] = winnerCampaign.key?:""
                storageMap["experimentVariationId"] =
                    if (winnerCampaign.type == CampaignTypeEnum.PERSONALIZE.value) winnerCampaign.variations[0].id?:-1 else -1
                StorageDecorator().setDataInStorage(storageMap, storageService)

                if (calledCampaignIds.contains(winnerCampaign.id)) {
                    return winnerCampaign
                }
            } else {
                log(
                    LogLevelEnum.INFO,
                    "No winner campaign found for MEG group: $groupId"
                )
            }
        } catch (exception: java.lang.Exception) {
            log(
                LogLevelEnum.ERROR,
                "MEG: error inside getCampaignUsingAdvancedAlgo " + exception.message
            )
        }

        return null
    }

    /**
     * Converts the weight map to a map of integers.
     * @param wt - The weight map.
     * @return The converted map.
     */
    private fun convertWtToMap(wt: Map<String, Int>?): Map<Int, Int> {
        val wtToReturn: MutableMap<Int, Int> = HashMap()
        if (wt == null) return wtToReturn
        for ((key, value) in wt) {
            wtToReturn[key.toInt()] = value
        }
        return wtToReturn
    }
}
