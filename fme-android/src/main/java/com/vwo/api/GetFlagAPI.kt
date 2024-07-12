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
package com.vwo.api

import com.vwo.decorators.StorageDecorator
import com.vwo.models.Feature
import com.vwo.models.Settings
import com.vwo.models.Storage
import com.vwo.packages.logger.enums.LogLevelEnum

object GetFlagAPI {
    /**
     * This method is used to get the flag value for the given feature key.
     * @param featureKey Feature key for which flag value is to be fetched.
     * @param settings Settings object containing the account settings.
     * @param context  VWOContext object containing the user context.
     * @param hookManager  HooksManager object containing the integrations.
     * @return GetFlag object containing the flag value.
     */
    fun getFlag(
        featureKey: String,
        settings: Settings?,
        context: VWOContext?,
        hookManager: HooksManager
    ): GetFlag {
        val getFlag: GetFlag = GetFlag()
        var shouldCheckForExperimentsRules = false

        val passedRulesInformation: MutableMap<String, Any?> = HashMap()
        val evaluatedFeatureMap: MutableMap<String, Any> = HashMap()

        // get feature object from feature key
        val feature: Feature = getFeatureFromKey(settings, featureKey)

        /**
         * Decision object to be sent for the integrations
         */
        val decision: MutableMap<String, Any?> = HashMap()
        decision["featureName"] = if (feature != null) feature.name else null
        decision["featureId"] = if (feature != null) feature.id else null
        decision["featureKey"] = if (feature != null) feature.key else null
        decision["userId"] = if (context != null) context.id else null
        decision["api"] = ApiEnum.GET_FLAG

        val storageService: StorageService = StorageService()
        val storedDataMap: Map<String, Any> =
            StorageDecorator().getFeatureFromStorage(featureKey, context, storageService)

        /**
         * If feature is found in the storage, return the stored variation
         */
        try {
            val storageMapAsString: String =
                VWOClient.objectMapper.writeValueAsString(storedDataMap)
            val storedData: Storage =
                VWOClient.objectMapper.readValue(storageMapAsString, Storage::class.java)
            if (storedData?.experimentVariationId != null && !storedData.experimentVariationId.toString()
                    .isEmpty()
            ) {
                if (storedData.experimentKey != null && !storedData.experimentKey!!.isEmpty()) {
                    val variation: Variation = getVariationFromCampaignKey(
                        settings,
                        storedData.experimentKey,
                        storedData.experimentVariationId
                    )
                    // If variation is found in settings, return the variation
                    if (variation != null) {
                        LoggerService.log(
                            LogLevelEnum.INFO,
                            "STORED_VARIATION_FOUND",
                            object : HashMap<String?, String?>() {
                                init {
                                    put("variationKey", variation.name)
                                    put("userId", context.id)
                                    put("experimentType", "experiment")
                                    put("experimentKey", storedData.experimentKey)
                                }
                            })
                        getFlag.setIsEnabled(true)
                        getFlag.setVariables(variation.variables)
                        return getFlag
                    }
                }
            } else if (storedData?.rolloutKey != null && !storedData.rolloutKey!!.isEmpty() && storedData.rolloutId != null && !storedData.rolloutId.toString()
                    .isEmpty()
            ) {
                val variation: Variation = getVariationFromCampaignKey(
                    settings,
                    storedData.rolloutKey,
                    storedData.rolloutVariationId
                )
                // If variation is found in settings, evaluate experiment rules
                if (variation != null) {
                    LoggerService.log(
                        LogLevelEnum.INFO,
                        "STORED_VARIATION_FOUND",
                        object : HashMap<String?, String?>() {
                            init {
                                put("variationKey", variation.name)
                                put("userId", context.id)
                                put("experimentType", "rollout")
                                put("experimentKey", storedData.rolloutKey)
                            }
                        })

                    LoggerService.log(
                        LogLevelEnum.DEBUG,
                        "EXPERIMENTS_EVALUATION_WHEN_ROLLOUT_PASSED",
                        object : HashMap<String?, String?>() {
                            init {
                                put("userId", context.id)
                            }
                        })

                    getFlag.setIsEnabled(true)
                    shouldCheckForExperimentsRules = true
                    val featureInfo: MutableMap<String, Any?> = HashMap()
                    featureInfo["rolloutId"] = storedData.rolloutId
                    featureInfo["rolloutKey"] = storedData.rolloutKey
                    featureInfo["rolloutVariationId"] = storedData.rolloutVariationId
                    evaluatedFeatureMap[featureKey] = featureInfo
                    passedRulesInformation.putAll(featureInfo)
                }
            }
        } catch (e: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "Error parsing stored data: " + e.message)
        }

        /**
         * if feature is not found, return false
         */
        if (feature == null) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "FEATURE_NOT_FOUND",
                object : HashMap<String?, String?>() {
                    init {
                        put("featureKey", featureKey)
                    }
                })
            getFlag.setIsEnabled(false)
            return getFlag
        }

        SegmentationManager.instance.setContextualData(settings, feature, context)

        /**
         * get all the rollout rules for the feature and evaluate them
         * if any of the rollout rule passes, break the loop and evaluate the traffic
         */
        val rollOutRules: List<Campaign> =
            getSpecificRulesBasedOnType(feature, CampaignTypeEnum.ROLLOUT)
        if (!rollOutRules.isEmpty() && !getFlag.isEnabled) {
            val rolloutRulesToEvaluate: MutableList<Campaign> = ArrayList<Campaign>()
            for (rule in rollOutRules) {
                val evaluateRuleResult: Map<String, Any> = RuleEvaluationUtil.evaluateRule(
                    settings,
                    feature,
                    rule,
                    context,
                    evaluatedFeatureMap,
                    HashMap<Int, Int>(),
                    storageService,
                    decision
                )
                val preSegmentationResult = evaluateRuleResult["preSegmentationResult"] as Boolean
                // If pre-segmentation passes, add the rule to the list of rules to evaluate
                if (preSegmentationResult) {
                    rolloutRulesToEvaluate.add(rule)
                    val featureMap: MutableMap<String, Any> = HashMap()
                    featureMap["rolloutId"] = rule.id
                    featureMap["rolloutKey"] = rule.key
                    featureMap["rolloutVariationId"] = rule.variations.get(0).id
                    evaluatedFeatureMap[featureKey] = featureMap
                    break
                }
            }

            // Evaluate the passed rollout rule traffic and get the variation
            if (!rolloutRulesToEvaluate.isEmpty()) {
                val passedRolloutCampaign: Campaign = rolloutRulesToEvaluate[0]
                val variation: Variation =
                    evaluateTrafficAndGetVariation(settings, passedRolloutCampaign, context.id)
                if (variation != null) {
                    getFlag.setIsEnabled(true)
                    getFlag.setVariables(variation.variables)
                    shouldCheckForExperimentsRules = true
                    updateIntegrationsDecisionObject(
                        passedRolloutCampaign,
                        variation,
                        passedRulesInformation,
                        decision
                    )
                    createAndSendImpressionForVariationShown(
                        settings,
                        passedRolloutCampaign.id,
                        variation.id,
                        context
                    )
                }
            }
        } else {
            LoggerService.log(
                LogLevelEnum.DEBUG,
                "EXPERIMENTS_EVALUATION_WHEN_NO_ROLLOUT_PRESENT",
                null
            )
            shouldCheckForExperimentsRules = true
        }

        /**
         * If any rollout rule passed pre segmentation and traffic evaluation, check for experiment rules
         * If no rollout rule passed, return false
         */
        if (shouldCheckForExperimentsRules) {
            val experimentRulesToEvaluate: MutableList<Campaign> = ArrayList<Campaign>()
            val experimentRules: List<Campaign> = getAllExperimentRules(feature)
            val megGroupWinnerCampaigns: Map<Int, Int> = HashMap()

            for (rule in experimentRules) {
                // Evaluate the rule here
                val evaluateRuleResult: Map<String, Any> = RuleEvaluationUtil.evaluateRule(
                    settings,
                    feature,
                    rule,
                    context,
                    evaluatedFeatureMap,
                    megGroupWinnerCampaigns,
                    storageService,
                    decision
                )
                val preSegmentationResult = evaluateRuleResult["preSegmentationResult"] as Boolean
                // If pre-segmentation passes, check if the rule has whitelisted variation or not
                if (preSegmentationResult) {
                    val whitelistedObject: Variation? =
                        evaluateRuleResult["whitelistedObject"] as Variation?
                    // If whitelisted object is null, add the rule to the list of rules to evaluate
                    if (whitelistedObject == null) {
                        experimentRulesToEvaluate.add(rule)
                    } else {
                        // If whitelisted object is not null, update the decision object and send an impression
                        getFlag.setIsEnabled(true)
                        getFlag.setVariables(whitelistedObject.variables)
                        passedRulesInformation["experimentId"] = rule.id
                        passedRulesInformation["experimentKey"] = rule.key
                        passedRulesInformation["experimentVariationId"] = whitelistedObject.id
                    }
                    break
                }
            }

            // Evaluate the passed experiment rule traffic and get the variation
            if (!experimentRulesToEvaluate.isEmpty()) {
                val campaign: Campaign = experimentRulesToEvaluate[0]
                val variation: Variation =
                    evaluateTrafficAndGetVariation(settings, campaign, context.id)
                if (variation != null) {
                    getFlag.setIsEnabled(true)
                    getFlag.setVariables(variation.variables)
                    updateIntegrationsDecisionObject(
                        campaign,
                        variation,
                        passedRulesInformation,
                        decision
                    )
                    createAndSendImpressionForVariationShown(
                        settings,
                        campaign.id,
                        variation.id,
                        context
                    )
                }
            }
        }

        if (getFlag.isEnabled) {
            val storageMap: MutableMap<String, Any?> = HashMap()
            storageMap["featureKey"] = feature.key
            storageMap["user"] = context.id
            storageMap.putAll(passedRulesInformation)
            StorageDecorator().setDataInStorage(storageMap, storageService)
        }

        // Execute the integrations
        hookManager.set(decision)
        hookManager.execute(hookManager.get())

        /**
         * If the feature has an impact campaign, send an impression for the variation shown
         * If flag enabled - variation 2, else - variation 1
         */
        if (feature.impactCampaign != null && feature.impactCampaign.campaignId != null && !feature.impactCampaign.campaignId.toString()
                .isEmpty()
        ) {
            LoggerService.log(
                LogLevelEnum.INFO,
                "IMPACT_ANALYSIS",
                object : HashMap<String?, String?>() {
                    init {
                        put("userId", context.id)
                        put("featureKey", featureKey)
                        put("status", if (getFlag.isEnabled) "enabled" else "disabled")
                    }
                })
            createAndSendImpressionForVariationShown(
                settings,
                feature.impactCampaign.campaignId,
                if (getFlag.isEnabled) 2 else 1,
                context
            )
        }
        return getFlag
    }

    /**
     * This method is used to update the integrations decision object with the campaign and variation details.
     * @param campaign  CampaignModel object containing the campaign details.
     * @param variation  VariationModel object containing the variation details.
     * @param passedRulesInformation  Map containing the information of the passed rules.
     * @param decision  Map containing the decision object.
     */
    private fun updateIntegrationsDecisionObject(
        campaign: Campaign,
        variation: Variation,
        passedRulesInformation: MutableMap<String, Any?>,
        decision: MutableMap<String, Any?>
    ) {
        if (campaign.type == CampaignTypeEnum.ROLLOUT.value) {
            passedRulesInformation["rolloutId"] = campaign.id
            passedRulesInformation["rolloutKey"] = campaign.name
            passedRulesInformation["rolloutVariationId"] = variation.id
        } else {
            passedRulesInformation["experimentId"] = campaign.id
            passedRulesInformation["experimentKey"] = campaign.key
            passedRulesInformation["experimentVariationId"] = variation.id
        }
        decision.putAll(passedRulesInformation)
    }
}
