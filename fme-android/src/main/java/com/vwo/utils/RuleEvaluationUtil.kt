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

import com.vwo.models.Campaign
import com.vwo.models.Feature
import com.vwo.models.Settings
import com.vwo.models.Variation
import com.vwo.models.user.VWOContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.LoggerService.Companion.log
import com.vwo.services.StorageService

/**
 * Utility object for rule evaluation operations.
 *
 * This object provides helper methods for evaluating rules and conditions, such as checking if
 * user attributes match targeting conditions or determining if variations should be applied based
 * on defined rules.
 */
object RuleEvaluationUtil {
    /**
     * This method is used to evaluate the rule for a given feature and campaign.
     * @param settings  SettingsModel object containing the account settings.
     * @param feature   FeatureModel object containing the feature settings.
     * @param campaign  CampaignModel object containing the campaign settings.
     * @param context   VWOContext object containing the user context.
     * @param evaluatedFeatureMap   Map containing the evaluated feature map.
     * @param megGroupWinnerCampaigns  Map containing the MEG group winner campaigns.
     * @param decision  Map containing the decision object.
     * @return
     */
    fun evaluateRule(
        settings: Settings,
        feature: Feature?,
        campaign: Campaign,
        context: VWOContext,
        evaluatedFeatureMap: MutableMap<String, Any>,
        megGroupWinnerCampaigns: MutableMap<Int, String>?,
        storageService: StorageService,
        decision: MutableMap<String, Any>
    ): Map<String, Any> {
        // Perform whitelisting and pre-segmentation checks
        try {
            // Check if the campaign satisfies the whitelisting and pre-segmentation
            val checkResult = DecisionUtil.checkWhitelistingAndPreSeg(
                settings,
                feature,
                campaign,
                context,
                evaluatedFeatureMap,
                megGroupWinnerCampaigns,
                storageService,
                decision
            )

            // Extract the results of the evaluation
            val preSegmentationResult = checkResult["preSegmentationResult"] as Boolean
            val whitelistedObject = checkResult["whitelistedObject"] as Variation?

            // If pre-segmentation is successful and a whitelisted object exists, proceed to send an impression
            val whilistedId = whitelistedObject?.id
            if (preSegmentationResult && whilistedId != null) {
                // Update the decision object with campaign and variation details
                val cmpId = campaign.id?:0
                decision["experimentId"] = cmpId
                decision["experimentKey"] = campaign.key?:""
                decision["experimentVariationId"] = whilistedId

                // Send an impression for the variation shown
                ImpressionUtil.createAndSendImpressionForVariationShown(
                    settings,
                    cmpId,
                    whilistedId,
                    context
                )
            }

            // Return the results of the evaluation
            val result: MutableMap<String, Any> = HashMap()
            result["preSegmentationResult"] = preSegmentationResult
            whitelistedObject?.let { result["whitelistedObject"] = it }
            result["updatedDecision"] = decision
            return result
        } catch (exception: Exception) {
            log(LogLevelEnum.ERROR, "Error occurred while evaluating rule: $exception")
            return HashMap()
        }
    }
}
