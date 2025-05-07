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
package com.vwo.models.schemas

import com.vwo.models.*
import kotlin.Boolean

/**
 * Validates the structure and content of VWO settings.
 *
 * This class provides a method to verify if the provided settings object conforms to the expected schema.
 * It checks for the presence and validity of required fields and nested objects within the settings.
 */
class SettingsSchema {

    /**
     * Checks if the provided settings object is valid.
     *
     * @param settings The settings object to validate.
     * @return `true` if the settings are valid, `false` otherwise.
     */
    fun isSettingsValid(settings: Settings?): Boolean {
        if (settings == null) {
            return false
        }

        // Validate SettingsModel fields
        if (settings.version == null || settings.accountId == null) {
            return false
        }

        val campaigns = settings.campaigns ?: return false

        for (campaign in campaigns) {
            if (!isValidCampaign(campaign)) {
                return false
            }
        }

        for (feature in settings.features) {
            if (!isValidFeature(feature)) {
                return false
            }
        }

        return true
    }

    /**
     * Checks if a campaign object is valid.
     *
     * @param campaign The campaign object to validate.
     * @return `true` if the campaign is valid, `false` otherwise.
     */
    private fun isValidCampaign(campaign: Campaign): Boolean {
        if (campaign.id == null || campaign.type == null || campaign.key == null || campaign.status == null || campaign.name == null) {
            return false
        }

        val variations = campaign.variations
        if (variations.isNullOrEmpty()) {
            return false
        }

        for (variation in variations) {
            if (!isValidCampaignVariation(variation)) {
                return false
            }
        }

        return true
    }

    /**
     * Checks if a campaign variation object is valid.
     *
     * @param variation The campaign variation object to validate.
     * @return `true` if the variation is valid,`false` otherwise.
     */
    private fun isValidCampaignVariation(variation: com.vwo.models.Variation): Boolean {
        if (variation.id == null || variation.name == null) {
            return false
        }

        for (variable in variation.variables) {
            if (!isValidVariableObject(variable)) {
                return false
            }
        }

        return true
    }

    /**
     * Checks if a variable object is valid.
     *
     * @param variable The variable object to validate.
     * @return `true` if the variable is valid, `false` otherwise.
     */
    private fun isValidVariableObject(variable: Variable): Boolean {
        return variable.id != null && variable.type != null && variable.key != null && variable.value != null
    }

    /**
     * Checks if a feature object is valid.
     *
     * @param feature The feature object to validate.
     * @return `true` if the feature is valid, `false` otherwise.
     */
    private fun isValidFeature(feature: Feature): Boolean {
        if (feature.id == null || feature.key == null || feature.status == null || feature.name == null || feature.type == null) {
            return false
        }

        val metrics = feature.metrics
        if (metrics.isNullOrEmpty()) {
            return false
        }

        for (metric in metrics) {
            if (!isValidCampaignMetric(metric)) {
                return false
            }
        }

        val rules = feature.rules
        if (rules != null) {
            for (rule in rules) {
                if (!isValidRule(rule)) {
                    return false
                }
            }
        }

        val variables = feature.variables
        if (variables != null) {
            for (variable in variables) {
                if (!isValidVariableObject(variable)) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * Checks if a campaign metric object is valid.
     *
     * @param metric The campaign metric object to validate.
     * @return `true` if the metric is valid, `false` otherwise.
     */
    private fun isValidCampaignMetric(metric: Metric): Boolean {
        return metric.id != null && metric.type != null && metric.identifier != null
    }

    /**
     * Checks if a rule object is valid.
     *
     * @param rule The rule object to validate.
     * @return `true` if the rule is valid, `false` otherwise.
     */
    private fun isValidRule(rule: Rule): Boolean {
        return rule.type != null && rule.ruleKey != null && rule.campaignId != null
    }
}