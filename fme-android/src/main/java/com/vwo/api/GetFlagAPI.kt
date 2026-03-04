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
package com.vwo.api

import com.vwo.ServiceContainer
import com.vwo.VWOClient
import com.vwo.constants.Constants
import com.vwo.constants.Constants.FEATURE_KEY
import com.vwo.decorators.StorageDecorator
import com.vwo.enums.ApiEnum
import com.vwo.enums.CampaignTypeEnum
import com.vwo.enums.DebuggerCategoryEnum
import com.vwo.models.Campaign
import com.vwo.models.Feature
import com.vwo.models.HoldoutGroup
import com.vwo.models.Settings
import com.vwo.models.Storage
import com.vwo.models.Variation
import com.vwo.models.impression.ImpressionPayload
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.decision_maker.DecisionMaker
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.HoldoutGroupService
import com.vwo.services.HooksManager
import com.vwo.services.LoggerService
import com.vwo.services.StorageService
import com.vwo.utils.CampaignUtil.getVariationFromCampaignKey
import com.vwo.utils.DecisionUtil
import com.vwo.utils.FunctionUtil.getAllExperimentRules
import com.vwo.utils.FunctionUtil.getFeatureFromKey
import com.vwo.utils.FunctionUtil.getSpecificRulesBasedOnType
import com.vwo.utils.ImpressionUtil
import com.vwo.utils.RuleEvaluationUtil
import com.vwo.utils.extractDecisionKeys
import com.vwo.utils.sendDebugEventToVWO

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
        settings: Settings,
        context: VWOUserContext,
        serviceContainer: ServiceContainer,
        hookManager: HooksManager,
    ): GetFlag {

        // Store the logs as higher order function and later invoke them when required
        // REASON:  it's a drop in replacement for execution flow change without any side-effects on
        //          the logical flow.
        val logsToBeShownAfterHoldoutLogs = mutableListOf<() -> Unit>()

        val impressionPayload = ImpressionPayload()

        val getFlag = GetFlag(context)
        var shouldCheckForExperimentsRules = false

        val passedRulesInformation: MutableMap<String, Any> = HashMap()
        val evaluatedFeatureMap: MutableMap<String, Any> = HashMap()

        // get feature object from feature key
        val feature: Feature? = getFeatureFromKey(settings, featureKey)

        // Initialize debug event properties
        val debugEventProps = mutableMapOf<String, Any>(
            "an" to ApiEnum.GET_FLAG.value,
            "uuid" to context.getUuid(serviceContainer),
            FEATURE_KEY to featureKey,
            "sId" to context.sessionId
        )

        /**
         * Decision object to be sent for the integrations
         */
        val decision = mutableMapOf<String, Any>().apply {
            feature?.name?.let { put("featureName", it) }
            feature?.id?.let { put("featureId", it) }
            feature?.key?.let { put("featureKey", it) }
            context.id?.let { put(Constants.USER_ID, it) }
            put("api", ApiEnum.GET_FLAG.value)

            // default decisions for holdouts; fallback values
            put(Constants.KEY_DECISION_IS_USER_PART_OF_CAMPAIGN, false)
            put("isPartOfHoldout", false)
            put("holdoutIDs", emptyList<Int>())

            val isAnyHoldoutApplicableForThisFeature =
                settings.holdoutGroups?.any { isHoldoutApplicableToFeature(it, feature?.id) } ?: false

            put("isHoldoutPresent", isAnyHoldoutApplicableForThisFeature)
        }

        val storageService = StorageService(serviceContainer)
        val storedDataMap: Map<String, Any>? =
            StorageDecorator().getFeatureFromStorage(featureKey, context, storageService)

        /**
         * If feature is found in the storage, return the stored variation or holdout decision
         */

        var storedData: Storage? = null

        try {
            val storageMapAsString: String = VWOClient.objectMapper.writeValueAsString(
                obj = storedDataMap?.toMap() ?: emptyMap<String, Any>()
            )
            storedData = VWOClient.objectMapper.readValue(
                json = storageMapAsString, clazz = Storage::class.java
            )

            val isInHoldout = storedData?.holdoutIds?.values.isNullOrEmpty().not()

            // locally saved "in holdout" ids and "not in holdout" ids
            val savedHoldoutIds = storedData?.holdoutIds?.values
            val savedNotInHoldoutIds = storedData?.notInHoldoutIds?.values

            val holdoutIdsFromSettings =
                settings.holdoutGroups
                    ?.filter { isHoldoutApplicableToFeature(it, feature?.id) }
                    ?.mapNotNull { it.id } ?: listOf()

            val onServerButHidNotInLocal = holdoutIdsFromSettings.filter {
                isOnServerButNotInLocal(it, savedHoldoutIds, savedNotInHoldoutIds)
            }

            //
            // get a list of
            // localHidAlsoValidOnServer - ids from local storage that are still on server ( valid ids )
            // _    - ids that are not on server {( meaning they have been removed from server)
            //                             (these needs to be removed because doesn't exist)}
            val localHidAlsoValidOnServer =
                savedHoldoutIds?.filter { isIdPresentOnServer(it, holdoutIdsFromSettings) }?.toSet()
                    ?.toList() // remove if any duplicate ids
                    ?: listOf()

            // just like above, these ids were there on the server too
            val validSavedNotInHoldoutIds =
                savedNotInHoldoutIds?.filter { isIdPresentOnServer(it, holdoutIdsFromSettings) }?.toSet()
                    ?.toList() // remove if any duplicate ids
                    ?: listOf()

            // Only treat stored "in holdout" as valid for this feature if the holdout still
            // applies to this feature (feature id still in holdout's featureIds, or holdout is global).
            // If the holdout was detached from this feature, we must not early-exit as "blocked by holdout".
            val holdoutIdsStillApplicableToThisFeature = settings.holdoutGroups
                ?.filter { isHoldoutApplicableToFeature(it, feature?.id) }
                ?.mapNotNull { it.id } ?: emptyList()
            val localHidAlsoValidOnServerForThisFeature =
                localHidAlsoValidOnServer.filter { isIdPresentOnServer(it, holdoutIdsStillApplicableToThisFeature) }

            if (isInHoldout && localHidAlsoValidOnServerForThisFeature.isNotEmpty()) {
                serviceContainer.getLoggerService()?.log(
                    LogLevelEnum.INFO, "STORED_HOLDOUT_DECISION_FOUND", mapOf(
                        Constants.USER_ID to context.id,
                        "featureKey" to featureKey,
                        "holdoutId" to localHidAlsoValidOnServerForThisFeature.toString()
                    )
                )

                // we might have new holdouts in server
                // we need to evaluate them and send the data to the server
                if (feature != null) {

                    val holdoutGroupService = HoldoutGroupService(DecisionMaker(), serviceContainer)
                    val (_, holdoutImpressions) = holdoutGroupService.getHoldoutsFor(
                        settings = settings,
                        feature = feature,
                        context = context,
                        storageService = storageService
                    )

                    // filter out holdouts that were not part of the holdout
                    val (qualifiedIdsAfterEvaluation, notQualifiedIdsAfterEvaluation) = holdoutImpressions.impressionList.partition { it.variationId == Constants.Holdouts.VARIATION_IS_PART_OF_HOLDOUT }
                        .let { (qualified, notQualified) ->
                            qualified.map { it.campaignId } to notQualified.map { it.campaignId }
                        }

                    ImpressionUtil.createAndSendImpressionForVariationShown(
                        settings = settings,
                        impressionPayload = holdoutImpressions,
                        context = context,
                        serviceContainer = serviceContainer
                    )

                    // these are non duplicate final holdout ids that include
                    // local ids that still apply to this feature (valid for this feature only)
                    val finalInHoldoutIds =
                        (localHidAlsoValidOnServerForThisFeature + qualifiedIdsAfterEvaluation).toSet()
                            .toList()
                    val finalNotInHoldoutIds =
                        (notQualifiedIdsAfterEvaluation + validSavedNotInHoldoutIds).toSet()
                            .toList()

                    storageService.updateDataInStorage(
                        feature = feature, context = context, data = mapOf(
                            Constants.Holdouts.KEY_STORAGE_HOLDOUT_IDS to finalInHoldoutIds,
                            Constants.Holdouts.KEY_STORAGE_NOT_IN_HOLDOUT_IDS to finalNotInHoldoutIds,
                        )
                    )
                }

                getFlag.setIsEnabled(false)
                getFlag.setVariables(emptyList())
                return getFlag
            } else {

                // update local storage: only persist holdout ids that still apply to this feature,
                // so if feature was detached from a holdout we clear it and re-evaluate next time
                storageService.updateDataInStorage(
                    feature = feature, context = context, data = mapOf(
                        Constants.Holdouts.KEY_STORAGE_HOLDOUT_IDS to localHidAlsoValidOnServerForThisFeature,
                        Constants.Holdouts.KEY_STORAGE_NOT_IN_HOLDOUT_IDS to validSavedNotInHoldoutIds,
                    )
                )

            }

            if (storedData != null && storedData.isDecisionExpired()) {
                serviceContainer.getLoggerService()?.log(
                    key = "DECISION_EXPIRED",
                    level = LogLevelEnum.WARN,
                    map = mapOf("featureKey" to featureKey, "id" to "${context.id}")
                )
            } else if (storedData?.experimentVariationId?.toString()?.isNotEmpty() == true) {

                if (storedData.experimentKey != null && storedData.experimentKey!!.isNotEmpty()) {
                    val variation: Variation? = getVariationFromCampaignKey(
                        settings, storedData.experimentKey, storedData.experimentVariationId!!
                    )
                    // If variation is found in settings, return the variation
                    if (variation != null) {
                        serviceContainer.getLoggerService()?.log(
                            LogLevelEnum.INFO,
                            "STORED_VARIATION_FOUND",
                            object : HashMap<String?, String?>() {
                                init {
                                    put(Constants.VARIATION_KEY, variation.name)
                                    put(Constants.USER_ID, context.id)
                                    put(Constants.KEY_EXPERIMENT_TYPE, "experiment")
                                    put(Constants.KEY_EXPERIMENT_KEY, storedData.experimentKey)
                                }
                            })

                        sendNotInHoldoutForNewlyAddedHoldouts(
                            newIds = onServerButHidNotInLocal,
                            feature = feature,
                            settings = settings,
                            context = context,
                            impressionPayload = impressionPayload,
                            storageService = storageService,
                            storedData = storedData,
                            serviceContainer = serviceContainer,
                            shouldUploadImmediately = true
                        )

                        getFlag.setIsEnabled(true)
                        decision[Constants.KEY_DECISION_IS_USER_PART_OF_CAMPAIGN] = true
                        getFlag.setVariables(variation.variables)
                        return getFlag
                    }
                }
            } else if (storedData?.rolloutKey != null && storedData.rolloutKey!!.isNotEmpty() && storedData.rolloutId != null && storedData.rolloutId.toString()
                    .isNotEmpty()
            ) {

                val variation: Variation? = getVariationFromCampaignKey(
                    settings, storedData.rolloutKey, storedData.rolloutVariationId
                )
                // If variation is found in settings, evaluate experiment rules
                if (variation != null) {
                    logsToBeShownAfterHoldoutLogs.add {
                        serviceContainer.getLoggerService()?.log(
                            level = LogLevelEnum.INFO, key = "STORED_VARIATION_FOUND", map = mapOf(
                                Constants.VARIATION_KEY to variation.name,
                                Constants.USER_ID to context.id,
                                Constants.KEY_EXPERIMENT_TYPE to "rollout",
                                Constants.KEY_EXPERIMENT_KEY to storedData.rolloutKey,
                            )
                        )

                        serviceContainer.getLoggerService()?.log(
                            level = LogLevelEnum.DEBUG,
                            key = "EXPERIMENTS_EVALUATION_WHEN_ROLLOUT_PASSED",
                            map = mapOf(
                                Constants.USER_ID to context.id
                            )
                        )
                    }

                    // need to change here
                    sendNotInHoldoutForNewlyAddedHoldouts(
                        newIds = onServerButHidNotInLocal,
                        feature = feature,
                        settings = settings,
                        context = context,
                        impressionPayload = impressionPayload,
                        storageService = storageService,
                        storedData = storedData,
                        serviceContainer = serviceContainer,
                        shouldUploadImmediately = false
                    )

                    getFlag.setIsEnabled(true)
                    decision[Constants.KEY_DECISION_IS_USER_PART_OF_CAMPAIGN] = true
                    getFlag.setVariables(variation.variables)
                    shouldCheckForExperimentsRules = true
                    val featureInfo = mutableMapOf<String, Any>()
                    storedData.rolloutId?.let { featureInfo["rolloutId"] = it }
                    storedData.rolloutKey?.let { featureInfo["rolloutKey"] = it }
                    storedData.rolloutVariationId?.let {
                        featureInfo["rolloutVariationId"] = it
                    }
                    evaluatedFeatureMap[featureKey] = featureInfo

                    passedRulesInformation.putAll(featureInfo)
                }
            }
        } catch (e: Exception) {
            serviceContainer.getLoggerService()
                ?.log(LogLevelEnum.DEBUG, "Error parsing stored data: " + e.message, null)
        }

        /**
         * if feature is not found, return false
         */
        if (feature == null) {
            LoggerService.errorLog(

                key = "FEATURE_NOT_FOUND",
                data = mapOf("featureKey" to featureKey),
                debugData = debugEventProps,
                shouldSendToVWO = true,
                serviceContainer = serviceContainer
            )
            getFlag.setIsEnabled(false)
            return getFlag
        }

        serviceContainer.getSegmentationManager()
            .setContextualData(settings, feature, context, serviceContainer)

        val holdoutGroup = mutableListOf<HoldoutGroup>()
        var holdoutImpressions = ImpressionPayload()

        // we want to only evaluate if getFlag is not true
        // meaning: nothing was found on local storage
        // only evaluate holdouts if we do not find anything in storage
        if (!getFlag.isEnabled()) {

            /**
             * Check if user is in a holdout group for this feature
             * If user is in holdout, exclude them from the feature and return
             */
            val holdoutGroupService = HoldoutGroupService(DecisionMaker(), serviceContainer)
            holdoutGroupService.getHoldoutsFor(
                settings = settings,
                feature = feature,
                context = context,
                storageService = storageService
            ).apply {
                holdoutGroup.clear()
                holdoutGroup.addAll(elements = first)

                holdoutImpressions = second
            }

            // merge impressions for holdout and get flag
            for (index in 0 until holdoutImpressions.size()) {
                val hip = holdoutImpressions.get(index)
                impressionPayload.add(
                    campaignId = hip.campaignId,
                    variationId = hip.variationId,
                    featureId = hip.featureId
                )
            }

            if (holdoutGroup.isNotEmpty()) {

                val qualifiedHoldoutNames = holdoutGroup.joinToString(
                    separator = ",", transform = { "${it.name}" })
                val qualifiedHoldoutIds = holdoutGroup.mapNotNull { it.id }

                decision["holdoutIDs"] = qualifiedHoldoutIds

                serviceContainer.getLoggerService()?.log(
                    LogLevelEnum.INFO, "USER_IN_HOLDOUT_GROUP", mutableMapOf(
                        Constants.USER_ID to "${context.id}",
                        "featureId" to "${feature.id}",
                        "featureKey" to featureKey,
                        "holdoutGroupName" to qualifiedHoldoutNames
                    )
                )

                val holdoutStorageMap = mutableMapOf<String, Any>()
                feature.key?.let { holdoutStorageMap["featureKey"] = it }
                context.id?.let { holdoutStorageMap[Constants.USER_ID] = it }

                val holdoutGroupSet = holdoutGroup.toSet()
                holdoutStorageMap[Constants.Holdouts.KEY_STORAGE_HOLDOUT_IDS] =
                    holdoutGroup.map { it.id }
                holdoutStorageMap[Constants.Holdouts.KEY_STORAGE_NOT_IN_HOLDOUT_IDS] =
                    settings.holdoutGroups
                        // only not selected ids
                        ?.filter { it !in holdoutGroupSet }
                        // applicable for global or targeted to specific feature id
                        ?.filter {
                            val isForThisFeature =
                                (it.featureIds?.contains((feature.id ?: -1)) == true)
                            (it.isGlobal == true) || isForThisFeature
                        }
                        // store only ids as list
                        ?.map { it.id } ?: listOf<String>()
                StorageDecorator().setDataInStorage(holdoutStorageMap, storageService)

                getFlag.setIsEnabled(false)
                getFlag.setVariables(emptyList())

                serviceContainer.getLoggerService()?.log(
                    LogLevelEnum.INFO, "USER_EXCLUDED_DUE_TO_HOLDOUT", mapOf(
                        Constants.USER_ID to "${context.id}",
                        "featureKey" to featureKey,
                        "holdoutId" to qualifiedHoldoutIds.joinToString(prefix = "[", postfix = "]", transform = { "$it" })
                    )
                )

                decision["isEnabled"] = false

                hookManager.set(decision)
                hookManager.execute(hookManager.get())

                ImpressionUtil.createAndSendImpressionForVariationShown(
                    settings = settings,
                    impressionPayload = impressionPayload,
                    context = context,
                    serviceContainer = serviceContainer
                )

                return getFlag
            } else {
                serviceContainer.getLoggerService()?.log(
                    LogLevelEnum.INFO, "USER_NOT_EXCLUDED_DUE_TO_HOLDOUT", mapOf(
                        Constants.USER_ID to "${context.id}", "featureKey" to featureKey
                    )
                )
            }
        }

        /**
         * get all the rollout rules for the feature and evaluate them
         * if any of the rollout rule passes, break the loop and evaluate the traffic
         */
        val rollOutRules: List<Campaign> =
            getSpecificRulesBasedOnType(feature, CampaignTypeEnum.ROLLOUT)
        if (rollOutRules.isNotEmpty() && !getFlag.isEnabled()) {
            val rolloutRulesToEvaluate: MutableList<Campaign> = ArrayList<Campaign>()
            for (rule in rollOutRules) {
                val evaluateRuleResult: Map<String, Any> = RuleEvaluationUtil.evaluateRule(
                    settings,
                    feature,
                    rule,
                    context,
                    evaluatedFeatureMap,
                    HashMap(),
                    storageService,
                    decision,
                    serviceContainer
                )
                val preSegmentationResult = evaluateRuleResult["preSegmentationResult"] as Boolean
                // If pre-segmentation passes, add the rule to the list of rules to evaluate
                if (preSegmentationResult) {
                    rolloutRulesToEvaluate.add(rule)
                    val featureMap: MutableMap<String, Any> = HashMap()

                    rule.id?.let { featureMap["rolloutId"] = it }
                    rule.key?.let { featureMap["rolloutKey"] = it }
                    rule.variations?.getOrNull(0)?.id?.let {
                        featureMap["rolloutVariationId"] = it
                    }

                    evaluatedFeatureMap[featureKey] = featureMap
                    break
                }
            }

            // Evaluate the passed rollout rule traffic and get the variation
            if (rolloutRulesToEvaluate.isNotEmpty()) {
                val passedRolloutCampaign: Campaign = rolloutRulesToEvaluate[0]
                val variation: Variation? = DecisionUtil().evaluateTrafficAndGetVariation(
                    settings, passedRolloutCampaign, context.id, serviceContainer
                )
                if (variation != null) {
                    getFlag.setIsEnabled(true)
                    getFlag.setVariables(variation.variables)
                    shouldCheckForExperimentsRules = true
                    updateIntegrationsDecisionObject(
                        passedRolloutCampaign, variation, passedRulesInformation, decision
                    )

                    impressionPayload.add(
                        campaignId = (passedRolloutCampaign.id ?: 0),
                        variationId = (variation.id ?: 0)
                    )
                }
            }
        } else {
            if (rollOutRules.isEmpty()) {
                serviceContainer.getLoggerService()?.log(
                    LogLevelEnum.DEBUG, "EXPERIMENTS_EVALUATION_WHEN_NO_ROLLOUT_PRESENT", null
                )
            }
            shouldCheckForExperimentsRules = true
        }

        /**
         * If any rollout rule passed pre segmentation and traffic evaluation, check for experiment rules
         * If no rollout rule passed, return false
         */
        if (shouldCheckForExperimentsRules) {
            val experimentRulesToEvaluate: MutableList<Campaign> = ArrayList<Campaign>()
            val experimentRules: List<Campaign> = getAllExperimentRules(feature)
            val megGroupWinnerCampaigns = mutableMapOf<Int, String>()

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
                    decision,
                    serviceContainer
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
                        decision[Constants.KEY_DECISION_IS_USER_PART_OF_CAMPAIGN] = true
                        getFlag.setVariables(whitelistedObject.variables)
                        rule.id?.let { passedRulesInformation["experimentId"] = it }
                        rule.key?.let { passedRulesInformation[Constants.KEY_EXPERIMENT_KEY] = it }
                        whitelistedObject.id?.let {
                            passedRulesInformation["experimentVariationId"] = it
                        }
                    }
                    break
                }
            }

            // Evaluate the passed experiment rule traffic and get the variation
            if (experimentRulesToEvaluate.isNotEmpty()) {
                val campaign: Campaign = experimentRulesToEvaluate[0]
                val variation: Variation? = DecisionUtil().evaluateTrafficAndGetVariation(
                    settings, campaign, context.id, serviceContainer
                )
                if (variation != null) {
                    getFlag.setIsEnabled(true)
                    decision[Constants.KEY_DECISION_IS_USER_PART_OF_CAMPAIGN] = true
                    getFlag.setVariables(variation.variables)
                    updateIntegrationsDecisionObject(
                        campaign, variation, passedRulesInformation, decision
                    )
                    impressionPayload.add(
                        campaignId = (campaign.id ?: 0), variationId = (variation.id ?: 0)
                    )
                }
            }
        }

        val storageMap = mutableMapOf<String, Any>()

        feature.key?.let { storageMap["featureKey"] = it }
        context.id?.let { storageMap[Constants.USER_ID] = it }

        val holdoutGroupSet = holdoutGroup.toSet()
        if (getFlag.isEnabled()) {
            storageMap["context"] = context
            storageMap.putAll(passedRulesInformation)

            val cachedDecisionExpiryTime =
                serviceContainer.getVWOInitOptions().cachedDecisionExpiryTime
            if (cachedDecisionExpiryTime > 0) {
                // Decision expiry: only set when we made a fresh decision (re-evaluated).
                // - When stored decision is valid and not expired (isAlreadyValid): we reuse it and do
                //   not add decisionExpiryTime to the write, so we don't overwrite/rewrite the TTL.
                // - When stored is null or expired (!isAlreadyValid): we re-evaluated, so set new
                //   expiry (now + cachedDecisionExpiryTime).
                val isAlreadyValid = (storedData != null) && !storedData.isDecisionExpired()

                if (!isAlreadyValid) {
                    storageMap["decisionExpiryTime"] =
                        System.currentTimeMillis() + cachedDecisionExpiryTime
                }
            }

            storageMap[Constants.Holdouts.KEY_STORAGE_NOT_IN_HOLDOUT_IDS] = settings.holdoutGroups
                // only not selected ids
                ?.filter { it !in holdoutGroupSet }
                // applicable for global or targeted to specific feature id
                ?.filter {
                    val isForThisFeature = (it.featureIds?.contains((feature.id ?: -1)) == true)
                    (it.isGlobal == true) || isForThisFeature
                }
                // store only ids as list
                ?.map { it.id } ?: listOf<String>()

            StorageDecorator().setDataInStorage(storageMap, storageService)
        } else {

            // holdouts data should be saved even if the getFlag is not enabled
            storageMap[Constants.Holdouts.KEY_STORAGE_NOT_IN_HOLDOUT_IDS] = settings.holdoutGroups
                // only not selected ids
                ?.filter { it !in holdoutGroupSet }
                // applicable for global or targeted to specific feature id
                ?.filter {
                    val isForThisFeature = (it.featureIds?.contains((feature.id ?: -1)) == true)
                    (it.isGlobal == true) || isForThisFeature
                }
                // store only ids as list
                ?.map { it.id } ?: listOf<String>()

        }

        // Execute the integrations
        hookManager.set(decision)
        hookManager.execute(hookManager.get())

        // send debug event, if debugger is enabled
        if (feature.isDebuggerEnabled) {
            debugEventProps["cg"] = DebuggerCategoryEnum.DECISION.key
            debugEventProps["lt"] = com.vwo.enums.LogLevelEnum.INFO.name
            debugEventProps["msg_t"] = Constants.FLAG_DECISION_GIVEN
            debugEventProps["uuid"] = context.getUuid(serviceContainer)
            // Update debug event props with decision keys
            updateDebugEventProps(debugEventProps, decision)
            sendDebugEventToVWO(debugEventProps, serviceContainer)
        }

        /**
         * If the feature has an impact campaign, send an impression for the variation shown
         * If flag enabled - variation 2, else - variation 1
         */
        if (feature.impactCampaign.campaignId != null && feature.impactCampaign.campaignId.toString()
                .isNotEmpty()
        ) {
            serviceContainer.getLoggerService()?.log(
                LogLevelEnum.INFO, "IMPACT_ANALYSIS", mapOf(
                    Constants.USER_ID to context.id,
                    "featureKey" to featureKey,
                    "status" to if (getFlag.isEnabled()) "enabled" else "disabled"
                )
            )
            feature.impactCampaign.campaignId?.let {
                impressionPayload.add(
                    campaignId = it, variationId = (if (getFlag.isEnabled()) 2 else 1)
                )
            }
        }

        if (logsToBeShownAfterHoldoutLogs.isNotEmpty()) logsToBeShownAfterHoldoutLogs.forEach { it.invoke() }

        ImpressionUtil.createAndSendImpressionForVariationShown(
            settings = settings,
            impressionPayload = impressionPayload,
            context = context,
            serviceContainer = serviceContainer
        )

        return getFlag
    }

    private fun sendNotInHoldoutForNewlyAddedHoldouts(
        settings: Settings,
        context: VWOUserContext,
        feature: Feature?,
        newIds: List<Int?>,
        impressionPayload: ImpressionPayload,
        storageService: StorageService,
        storedData: Storage?,
        serviceContainer: ServiceContainer,
        shouldUploadImmediately: Boolean
    ) {

        val validNewIds = newIds.filterNotNull()
        if (validNewIds.isEmpty()) return

        validNewIds.forEach { hid ->
            impressionPayload.add(
                campaignId = hid,
                featureId = feature?.id ?: Constants.IMPRESSION_NO_FEATURE_ID,
                variationId = Constants.Holdouts.VARIATION_NOT_PART_OF_HOLDOUT
            )
        }

        val updatedIds = storedData?.notInHoldoutIds?.values?.toMutableSet() ?: mutableSetOf()
        updatedIds.addAll(validNewIds)

        storageService.updateDataInStorage(
            feature = feature, context = context, data = mapOf(
                Constants.Holdouts.KEY_STORAGE_NOT_IN_HOLDOUT_IDS to updatedIds.toList()
            )
        )

        if (shouldUploadImmediately) {
            ImpressionUtil.createAndSendImpressionForVariationShown(
                settings = settings,
                impressionPayload = impressionPayload,
                context = context,
                serviceContainer = serviceContainer
            )
        }
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
        passedRulesInformation: MutableMap<String, Any>,
        decision: MutableMap<String, Any>
    ) {
        if (campaign.type == CampaignTypeEnum.ROLLOUT.value) {
            passedRulesInformation["rolloutId"] = campaign.id ?: 0
            passedRulesInformation["rolloutKey"] = campaign.key ?: ""
            passedRulesInformation["rolloutVariationId"] = variation.id ?: 0
        } else {
            passedRulesInformation["experimentId"] = campaign.id ?: 0
            passedRulesInformation[Constants.KEY_EXPERIMENT_KEY] = campaign.key ?: ""
            passedRulesInformation["experimentVariationId"] = variation.id ?: 0
        }
        decision.putAll(passedRulesInformation)
    }

    /**
     * Returns true if the holdout id is on server but not present in local storage
     * (neither in "in holdout" nor "not in holdout" saved lists).
     */
    private fun isOnServerButNotInLocal(
        id: Int,
        savedHoldoutIds: List<Int>?,
        savedNotInHoldoutIds: List<Int>?
    ): Boolean =
        id !in (savedHoldoutIds ?: emptyList()) && id !in (savedNotInHoldoutIds ?: emptyList())

    /**
     * Returns true if the holdout id is present in the server's holdout id list.
     */
    private fun isIdPresentOnServer(id: Int, serverHoldoutIds: List<Int>): Boolean =
        id in serverHoldoutIds

    /**
     * Returns true if the holdout is applicable to the feature (global holdout or feature id in holdout's featureIds).
     */
    private fun isHoldoutApplicableToFeature(holdout: HoldoutGroup, featureId: Int?): Boolean =
        holdout.isGlobal == true || (featureId != null && holdout.featureIds?.contains(featureId) == true)

    /**
     * Update debug event props with decision keys
     * @param debugEventProps Debug event props
     * @param decision Decision
     */
    private fun updateDebugEventProps(
        debugEventProps: MutableMap<String, Any>, decision: MutableMap<String, Any>
    ) {
        val decisionKeys = extractDecisionKeys(decision)

        val featureKey = decision["featureKey"] as? String ?: ""
        val rolloutKey = decision["rolloutKey"] as? String
        val rolloutVariationId = decision["rolloutVariationId"]
        val experimentKey = decision[Constants.KEY_EXPERIMENT_KEY] as? String
        val experimentVariationId = decision["experimentVariationId"]

        val featurePrefix = if (featureKey.isNotEmpty()) "${featureKey}_" else ""

        val sb = StringBuilder("Flag decision given for feature:").append(featureKey).append('.')
        if (!rolloutKey.isNullOrEmpty() && rolloutVariationId != null) {
            val rolloutSuffix = rolloutKey.removePrefix(featurePrefix)
            sb.append(" Got rollout:").append(rolloutSuffix).append(" with variation:")
                .append(rolloutVariationId)
        }
        if (!experimentKey.isNullOrEmpty() && experimentVariationId != null) {
            val expSuffix = experimentKey.removePrefix(featurePrefix)
            sb.append(" and experiment:").append(expSuffix).append(" with variation:")
                .append(experimentVariationId)
        }

        debugEventProps["msg"] = sb.toString()
        debugEventProps.putAll(decisionKeys)
    }
}

