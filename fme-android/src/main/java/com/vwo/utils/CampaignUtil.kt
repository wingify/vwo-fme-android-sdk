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
package com.vwo.utils

import com.vwo.constants.Constants
import com.vwo.enums.CampaignTypeEnum
import com.vwo.models.Campaign
import com.vwo.models.Feature
import com.vwo.models.Rule
import com.vwo.models.Settings
import com.vwo.models.Variation
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.LoggerService.Companion.log
import java.util.Objects
import kotlin.math.ceil
import kotlin.math.min


/**
 * Utility class for campaign operations.
 *
 * This object provides helper methods for working with campaigns, such as retrieving campaign settings,validating campaign data, or performing other campaign-related tasks.
 */
object CampaignUtil {
    /**
     * Sets the variation allocation for a given campaign based on its type.
     * If the campaign type is ROLLOUT or PERSONALIZE, it handles the campaign using `_handleRolloutCampaign`.
     * Otherwise, it assigns range values to each variation in the campaign.
     * @param campaign The campaign for which to set the variation allocation.
     */
    fun setVariationAllocation(campaign: Campaign) {
        // Check if the campaign type is roll out or PERSONALIZE
        if (campaign.type == CampaignTypeEnum.ROLLOUT.value || campaign.type == CampaignTypeEnum.PERSONALIZE.value) {
            handleRolloutCampaign(campaign)
        } else {
            var currentAllocation = 0
            // Iterate over each variation in the campaign
            for (variation in campaign.variations!!) {
                // Assign range values to the variation and update the current allocation
                val stepFactor = assignRangeValues(variation, currentAllocation)
                currentAllocation += stepFactor
                log(
                    LogLevelEnum.INFO,
                    "VARIATION_RANGE_ALLOCATION",
                    object : HashMap<String?, String?>() {
                        init {
                            put("campaignKey", campaign.key)
                            put("variationKey", variation.name)
                            put("variationWeight", variation.weight.toString())
                            put("startRange", variation.startRangeVariation.toString())
                            put("endRange", variation.endRangeVariation.toString())
                        }
                    })
            }
        }
    }

    /**
     * Assigns start and end range values to a variation based on its weight.
     * @param data The variation model to assign range values.
     * @param currentAllocation The current allocation value before this variation.
     * @return The step factor calculated from the variation's weight.
     */
    fun assignRangeValues(data: Variation?, currentAllocation: Int): Int {
        // Calculate the bucket range based on the variation's weight
        val stepFactor = getVariationBucketRange(data!!.weight)

        // Set the start and end range of the variation
        if (stepFactor > 0) {
            data.startRangeVariation = currentAllocation + 1
            data.endRangeVariation = currentAllocation + stepFactor
        } else {
            data.startRangeVariation = -1
            data.endRangeVariation = -1
        }
        return stepFactor
    }

    /**
     * Scales the weights of variations to sum up to 100%.
     * @param variations The list of variations to scale.
     */
    fun scaleVariationWeights(variations: List<Variation>) {
        // Calculate the total weight of all variations
        val totalWeight = variations.sumOf { it.weight }

        // If total weight is zero, assign equal weight to each variation
        if (totalWeight == 0.0) {
            val equalWeight = 100.0 / variations.size
            for (variation in variations) {
                variation.weight = equalWeight
            }
        } else {
            // Scale each variation's weight to make the total 100%
            for (variation in variations) {
                variation.weight = (variation.weight / totalWeight) * 100
            }
        }
    }

    /**
     * Generates a bucketing seed based on user ID, campaign, and optional group ID.
     * @param userId The user ID.
     * @param campaign The campaign object.
     * @param groupId The optional group ID.
     * @return The bucketing seed.
     */
    fun getBucketingSeed(userId: String?, campaign: Campaign?, groupId: Int?): String {
        // Return a seed combining group ID and user ID if group ID is provided
        if (groupId != null) {
            return groupId.toString() + "_" + userId
        }

        // get campaign type
        val campaignType = campaign?.type
        // check if campaign type is rollout or personalize
        val isRolloutOrPersonalize = Objects.equals(campaignType, CampaignTypeEnum.ROLLOUT.value) ||
                Objects.equals(campaignType, CampaignTypeEnum.PERSONALIZE.value)

        // Get salt based on campaign type
        val salt = if (isRolloutOrPersonalize) campaign?.variations?.getOrNull(0)?.salt else campaign?.salt
        // if salt is not null and not empty, use salt else use campaign id
        val bucketKey = if (salt.isNullOrEmpty())
            campaign?.id.toString() + "_" + userId
        else
            salt + "_" + userId
        // Return a seed combining campaign ID and user ID otherwise
        return bucketKey
    }

    /**
     * Retrieves a variation by its ID within a specific campaign identified by its key.
     * @param settings The settings model containing all campaigns.
     * @param campaignKey The key of the campaign.
     * @param variationId The ID of the variation to retrieve.
     * @return The found variation model or null if not found.
     */
    fun getVariationFromCampaignKey(
        settings: Settings,
        campaignKey: String?,
        variationId: Int?
    ): Variation? {
        // Find the campaign by its key
        val campaign = settings.campaigns?.firstOrNull { it.key == campaignKey }

        return campaign?.variations?.firstOrNull { it.id == variationId }
    }


    /**
     * Sets the allocation ranges for a list of campaigns.
     * @param campaigns The list of campaigns to set allocations for.
     */
    fun setCampaignAllocation(campaigns: List<Variation>) {
        var currentAllocation = 0
        for (campaign in campaigns) {
            // Assign range values to each campaign and update the current allocation
            val stepFactor = assignRangeValuesMEG(campaign, currentAllocation)
            currentAllocation += stepFactor
        }
    }

    /**
     * Determines if a campaign is part of a group.
     * @param settings The settings model containing group associations.
     * @param campaignId The ID of the campaign to check.
     * @return An object containing the group ID and name if the campaign is part of a group, otherwise an empty object.
     */
    fun getGroupDetailsIfCampaignPartOfIt(
        settings: Settings,
        campaignId: Int,
        variationId: Int
    ): MutableMap<String, String> {
        // If variationId is null, that means that campaign is testing campaign
        // If variationId is not null, that means that campaign is personalization campaign and we need to append variationId to campaignId using _
        // then check if the current campaign is part of any group
        val groupDetails: MutableMap<String, String> = HashMap()
        var campaignToCheck = campaignId.toString()
        if (variationId != -1) {
            campaignToCheck = campaignToCheck + "_" + variationId
        }
        val campaignGroups = settings.campaignGroups
        if (campaignGroups!= null && campaignGroups.containsKey(campaignToCheck)) {
            val groupId = campaignGroups[campaignToCheck]
            val groupName = settings.groups?.get(groupId.toString())?.name
            groupDetails["groupId"] = groupId.toString()
            groupDetails["groupName"] = groupName?:""
            return groupDetails
        }
        return groupDetails
    }

    /**
     * Finds all groups associated with a feature specified by its key.
     * @param settings The settings model containing all features and groups.
     * @param featureKey The key of the feature to find groups for.
     * @return An array of groups associated with the feature.
     */
    fun findGroupsFeaturePartOf(
        settings: Settings,
        featureKey: String?
    ): MutableList<MutableMap<String, String>> {
        // Initialize an array to store all rules for the given feature to fetch campaignId and variationId later
        val ruleArrayList: MutableList<Rule> = ArrayList()
        for (feature in settings.features) {
            if (feature.key.equals(featureKey)) {
                feature.rules?.forEach { rule ->
                    // Add rule to the array if it's not already present
                    if (!ruleArrayList.contains(rule)) {
                        ruleArrayList.add(rule)
                    }
                }
            }
        }

        // Initialize an array to store all groups associated with the feature
        val groups: MutableList<MutableMap<String, String>> = ArrayList()
        // Iterate over each rule to find the group details
        for (rule in ruleArrayList) {
            val variationId = if (rule.type == CampaignTypeEnum.PERSONALIZE.value)
                rule.variationId?:-1
            else
                -1
            val group: MutableMap<String, String> = getGroupDetailsIfCampaignPartOfIt(
                settings,
                rule.campaignId!!,
                variationId
            )
            // Add group to the array if it's not already present
            if (group.isNotEmpty() && groups.none { it["groupId"] == group["groupId"] }) {
                groups.add(group)
            }
        }
        return groups
    }

    /**
     * Retrieves campaigns by a specific group ID.
     * @param settings The settings model containing all groups.
     * @param groupId The ID of the group.
     * @return An array of campaigns associated with the specified group ID.
     */
    fun getCampaignsByGroupId(settings: Settings, groupId: Int): List<String> {
        // find the group
        val group = settings.groups?.get(groupId.toString())
        return group?.campaigns?: emptyList()
    }

    /**
     * Retrieves feature keys from a list of campaign IDs.
     * @param settings The settings model containing all features.
     * @param campaignIdWithVariation An array of campaign IDs.
     * @return An array of feature keys associated with the provided campaign IDs.
     */
    fun getFeatureKeysFromCampaignIds(
        settings: Settings,
        campaignIdWithVariation: List<String>
    ): MutableList<String> {
        val featureKeys: MutableList<String> = ArrayList()
        for (campaign in campaignIdWithVariation) {
            // split key with _ to separate campaignId and variationId
            val campaignIdVariationId = campaign.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val campaignId = campaignIdVariationId[0].toInt()
            val variationId =
                if ((campaignIdVariationId.size > 1)) campaignIdVariationId[1].toInt() else null
            // Iterate over each feature to find the feature key
            for (feature in settings.features) {
                // Break if feature key is already added
                if (featureKeys.contains(feature.key)) {
                    continue
                }
                feature.rules?.forEach { rule ->
                    if (rule.campaignId == campaignId) {
                        // Check if variationId is provided and matches the rule's variationId
                        if (variationId != null) {
                            // Add feature key if variationId matches
                            if (rule.variationId== variationId) {
                                feature.key?.let { featureKeys.add(it) }
                            }
                        } else {
                            // Add feature key if no variationId is provided
                            feature.key?.let { featureKeys.add(it) }
                        }
                    }
                }
            }
        }
        return featureKeys
    }

    /**
     * Retrieves campaign IDs from a specific feature key.
     * @param settings The settings model containing all features.
     * @param featureKey The key of the feature.
     * @return An array of campaign IDs associated with the specified feature key.
     */
    fun getCampaignIdsFromFeatureKey(settings: Settings, featureKey: String?): List<Int?> {
        val campaignIds: MutableList<Int?> = ArrayList()
        for (feature in settings.features) {
            if (feature.key == featureKey) {
                feature.rules?.forEach { rule: Rule -> campaignIds.add(rule.campaignId) }
            }
        }
        return campaignIds
    }

    /**
     * Assigns range values to a campaign based on its weight.
     * @param data The campaign data containing weight.
     * @param currentAllocation The current allocation value before this campaign.
     * @return The step factor calculated from the campaign's weight.
     */
    fun assignRangeValuesMEG(data: Variation, currentAllocation: Int): Int {
        val stepFactor = getVariationBucketRange(data.weight)

        if (stepFactor > 0) {
            data.startRangeVariation = currentAllocation + 1
            data.endRangeVariation = currentAllocation + stepFactor
        } else {
            data.startRangeVariation = -1
            data.endRangeVariation = -1
        }
        return stepFactor
    }

    /**
     * Retrieves the rule type using a campaign ID from a specific feature.
     * @param feature The feature containing rules.
     * @param campaignId The campaign ID to find the rule type for.
     * @return The rule type if found, otherwise an empty string.
     */
    fun getRuleTypeUsingCampaignIdFromFeature(feature: Feature, campaignId: Int): String {
        return feature.rules?.filter { rule -> rule.campaignId == campaignId }
            ?.map { it.type }
            ?.firstOrNull() ?: ""
    }

    /**
     * Calculates the bucket range for a variation based on its weight.
     * @param variationWeight The weight of the variation.
     * @return The calculated bucket range.
     */
    private fun getVariationBucketRange(variationWeight: Double): Int {
        if (variationWeight <= 0) {
            return 0
        }
        val startRange = ceil(variationWeight * 100).toInt()
        return min(startRange.toDouble(), Constants.MAX_TRAFFIC_VALUE.toDouble())
            .toInt()
    }

    /**
     * Handles the rollout campaign by setting start and end ranges for all variations.
     * @param campaign The campaign to handle.
     */
    private fun handleRolloutCampaign(campaign: Campaign) {
        // Set start and end ranges for all variations in the campaign
        for (variation in campaign.variations!!) {
            val endRange = (variation.weight * 100).toInt()
            variation.startRangeVariation = 1
            variation.endRangeVariation = endRange
            log(
                LogLevelEnum.INFO,
                "VARIATION_RANGE_ALLOCATION",
                object : HashMap<String?, String?>() {
                    init {
                        put("campaignKey", campaign.key)
                        put("variationKey", variation.name)
                        put("variationWeight", variation.weight.toString())
                        put("startRange", variation.startRangeVariation.toString())
                        put("endRange", variation.endRangeVariation.toString())
                    }
                })
        }
    }
}
