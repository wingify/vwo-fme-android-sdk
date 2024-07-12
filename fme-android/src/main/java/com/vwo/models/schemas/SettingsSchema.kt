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
package com.vwo.models.schemas

import com.vwo.models.*
import kotlin.Boolean

class SettingsSchema {
    fun isSettingsValid(settings: Settings?): Boolean {
        if (settings == null) {
            return false
        }

        // Validate SettingsModel fields
        if (settings.getVersion() == null || settings.getAccountId() == null) {
            return false
        }

        if (settings.getCampaigns() == null || settings.getCampaigns().isEmpty()) {
            return false
        }

        for (campaign in settings.getCampaigns()) {
            if (!isValidCampaign(campaign)) {
                return false
            }
        }

        if (settings.getFeatures() != null) {
            for (feature in settings.getFeatures()) {
                if (!isValidFeature(feature)) {
                    return false
                }
            }
        }

        return true
    }

    private fun isValidCampaign(campaign: Campaign): Boolean {
        if (campaign.getId() == null || campaign.getType() == null || campaign.getKey() == null || campaign.getStatus() == null || campaign.getName() == null) {
            return false
        }

        if (campaign.getVariations() == null || campaign.getVariations().isEmpty()) {
            return false
        }

        for (variation in campaign.getVariations()) {
            if (!isValidCampaignVariation(variation)) {
                return false
            }
        }

        return true
    }

    private fun isValidCampaignVariation(variation: com.vwo.models.Variation): Boolean {
        if (variation.getId() == null || variation.getName() == null || String.valueOf(variation.getWeight())
                .isEmpty()
        ) {
            return false
        }

        if (variation.getVariables() != null) {
            for (variable in variation.getVariables()) {
                if (!isValidVariableObject(variable)) {
                    return false
                }
            }
        }

        return true
    }

    private fun isValidVariableObject(variable: com.vwo.models.Variable): Boolean {
        return variable.getId() != null && variable.getType() != null && variable.getKey() != null && variable.getValue() != null
    }

    private fun isValidFeature(feature: com.vwo.models.Feature): Boolean {
        if (feature.getId() == null || feature.getKey() == null || feature.getStatus() == null || feature.getName() == null || feature.getType() == null) {
            return false
        }

        if (feature.getMetrics() == null || feature.getMetrics().isEmpty()) {
            return false
        }

        for (metric in feature.getMetrics()) {
            if (!isValidCampaignMetric(metric)) {
                return false
            }
        }

        if (feature.getRules() != null) {
            for (rule in feature.getRules()) {
                if (!isValidRule(rule)) {
                    return false
                }
            }
        }

        if (feature.getVariables() != null) {
            for (variable in feature.getVariables()) {
                if (!isValidVariableObject(variable)) {
                    return false
                }
            }
        }

        return true
    }

    private fun isValidCampaignMetric(metric: com.vwo.models.Metric): Boolean {
        return metric.getId() != null && metric.getType() != null && metric.getIdentifier() != null
    }

    private fun isValidRule(rule: Rule): Boolean {
        return rule.getType() != null && rule.getRuleKey() != null && rule.getCampaignId() != null
    }
}