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

import com.google.gson.Gson
import com.vwo.models.Rule
import com.vwo.models.Settings
import com.vwo.packages.logger.enums.LogLevelEnum
import java.util.Objects
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.regex.Pattern
import java.util.stream.Collectors

object SettingsUtil {
    /**
     * Processes the settings file and modifies it as required.
     * This method is called before the settings are used by the SDK.
     * It sets the variation allocation for each campaign.
     * It adds linked campaigns to each feature in the settings based on rules.
     * It adds isGatewayServiceRequired flag to each feature in the settings based on pre segmentation.
     * @param settings - The settings file to modify.
     */
    fun processSettings(settings: Settings) {
        try {
            val campaigns: List<Campaign>? = settings.campaigns

            for (i in campaigns!!.indices) {
                val campaign: Campaign = campaigns[i]
                CampaignUtil.setVariationAllocation(campaign)
                campaigns.set(i, campaign)
            }
            addLinkedCampaignsToSettings(settings)
            addIsGatewayServiceRequiredFlag(settings)
        } catch (exception: Exception) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "Exception occurred while processing settings " + exception.message
            )
        }
    }

    /**
     * Adds linked campaigns to each feature in the settings based on rules.
     * @param settings  - The settings file to modify.
     */
    private fun addLinkedCampaignsToSettings(settings: Settings) {
        // Create a map for quick access to campaigns by ID

        val campaignMap: Map<Int?, Campaign?> = settings.campaigns!!.stream()
            .collect(
                Collectors.toMap<Campaign, Int?, Campaign?>(
                    Campaign::id,
                    Function<Campaign, Campaign?> { campaign: Campaign? -> campaign })
            )

        // Loop over all features
        for (feature in settings.features!!) {
            val rulesLinkedCampaignModel: List<Campaign> = feature.rules!!.stream()
                .map<Campaign>(Function<Rule, Campaign> { rule: Rule ->
                    val originalCampaign: Campaign = campaignMap[rule.campaignId] ?: return@map null
                    originalCampaign.ruleKey = rule.ruleKey
                    val campaign: Campaign = Campaign()
                    campaign.setModelFromDictionary(originalCampaign)

                    // If a variationId is specified, find and add the variation
                    if (rule.variationId != null) {
                        campaign.variations.stream()
                            .filter(Predicate<Variation> { v: Variation -> v.id == rule.variationId })
                            .findFirst().ifPresent(Consumer<Variation> { variation: Variation ->
                                campaign.variations = listOf<Variation>(variation)
                            })
                    }
                    campaign
                })
                .filter(Predicate<Campaign> { obj: Campaign? -> Objects.nonNull(obj) })
                .collect<List<Campaign>, Any>(Collectors.toList<Campaign>())

            // Assign the linked campaigns to the feature
            feature.rulesLinkedCampaign = rulesLinkedCampaignModel
        }
    }

    /**
     * Adds isGatewayServiceRequired flag to each feature in the settings based on pre segmentation.
     * @param settings  - The settings file to modify.
     */
    private fun addIsGatewayServiceRequiredFlag(settings: Settings) {
        // Updated pattern without using lookbehind
        val patternString =
            "\\b(country|region|city|os|device_type|browser_string|ua)\\b|\"custom_variable\"\\s*:\\s*\\{\\s*\"name\"\\s*:\\s*\"inlist\\([^)]*\\)\""
        val pattern = Pattern.compile(patternString)

        for (feature in settings.features!!) {
            val rules: List<Campaign> = feature.rulesLinkedCampaign
            for (rule in rules) {
                var segments: Any =
                    if (rule.type == CampaignTypeEnum.ROLLOUT.value || rule.type == CampaignTypeEnum.PERSONALIZE.value) {
                        rule.variations.get(0).segments
                    } else {
                        rule.segments
                    }
                if (segments != null) {
                    val jsonSegments: String = Gson().toJson(segments)
                    val matcher = pattern.matcher(jsonSegments)
                    var foundMatch = false

                    while (matcher.find()) {
                        val match = matcher.group()
                        if (match.matches("\\b(country|region|city|os|device_type|browser_string|ua)\\b".toRegex())) {
                            // Check if within "custom_variable" block
                            if (!isWithinCustomVariable(matcher.start(), jsonSegments)) {
                                foundMatch = true
                                break
                            }
                        } else {
                            foundMatch = true
                            break
                        }
                    }

                    if (foundMatch) {
                        feature.setIsGatewayServiceRequired(true)
                        break
                    }
                }
            }
        }
    }

    // Helper method to check if a match is within "custom_variable"
    private fun isWithinCustomVariable(startIndex: Int, input: String): Boolean {
        val index = input.lastIndexOf("\"custom_variable\"", startIndex)
        if (index == -1) return false

        val closingBracketIndex = input.indexOf("}", index)
        return closingBracketIndex != -1 && startIndex < closingBracketIndex
    }
}
