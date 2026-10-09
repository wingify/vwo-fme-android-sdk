/*
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.vwo.utils

import com.wingify.WingifyClient
import com.wingify.models.Campaign
import com.wingify.models.Feature
import com.wingify.models.Groups
import com.wingify.models.Settings
import com.wingify.utils.GsonUtil
import com.wingify.utils.SettingsUtil

/**
 * Builds a 12-flag settings fixture for parallel [com.wingify.api.GetFlagsAPI] stress tests.
 *
 * Composition:
 * - [INDEPENDENT_FLAG_KEYS]: rollout-only flags with no MEG group (parallel-safe).
 * - [SALT_FLAG_KEYS]: A/B flags with distinct campaign salts (parallel-safe).
 * - [MEG_RANDOM_FLAG_KEYS]: two flags sharing MEG group 1 (random algorithm, et=1).
 * - [MEG_ADVANCE_FLAG_KEYS]: two flags sharing MEG group 2 (advance algorithm, et=2).
 */
object GetFlagsStressTestSettingsFactory {

    const val MEG_RANDOM_GROUP_ID = 1
    const val MEG_ADVANCE_GROUP_ID = 2

    val INDEPENDENT_FLAG_KEYS: List<String> =
        (1..6).map { index -> "independent_%02d".format(index) }

    val SALT_FLAG_KEYS: List<String> = listOf("salt_feature1", "salt_feature2")

    val MEG_RANDOM_FLAG_KEYS: List<String> = listOf("meg_random_a", "meg_random_b")

    val MEG_ADVANCE_FLAG_KEYS: List<String> = listOf("meg_advance_a", "meg_advance_b")

    val ALL_FLAG_KEYS: List<String> =
        INDEPENDENT_FLAG_KEYS + SALT_FLAG_KEYS + MEG_RANDOM_FLAG_KEYS + MEG_ADVANCE_FLAG_KEYS

    fun build(): Settings {
        val reader = DummySettingsReader()
        val basicRollout = parseSettings(reader.settingsMap["BASIC_ROLLOUT_SETTINGS"]!!)
        val saltSettings = parseSettings(reader.settingsMap["SETTINGS_WITH_DIFFERENT_SALT"]!!)
        val megRandomTemplate = parseSettings(reader.settingsMap["MEG_CAMPAIGN_RANDOM_ALGO_SETTINGS"]!!)
        val megAdvanceTemplate = parseSettings(reader.settingsMap["MEG_CAMPAIGN_ADVANCE_ALGO_SETTINGS"]!!)

        val features = mutableListOf<Feature>()
        val allCampaigns = mutableListOf<Campaign>()
        val groups = mutableMapOf<String, Groups>()
        val campaignGroups = mutableMapOf<String, Int>()

        var featureId = 1
        // Campaign IDs must not overlap across features; MEG uses campaignGroups keyed by id string.
        var nextCampaignId = 100

        INDEPENDENT_FLAG_KEYS.forEach { featureKey ->
            val cloned = cloneBasicRolloutFeature(
                template = basicRollout,
                featureKey = featureKey,
                featureId = featureId++,
                rolloutCampaignId = nextCampaignId++,
            )
            features.add(cloned.feature)
            allCampaigns.addAll(cloned.campaigns)
        }

        SALT_FLAG_KEYS.forEachIndexed { index, featureKey ->
            val templateFeature = saltSettings.features[index]
            val campaignIdBase = nextCampaignId
            val cloned = cloneFeatureWithCampaigns(
                templateSettings = saltSettings,
                templateFeature = templateFeature,
                featureKey = featureKey,
                featureId = featureId++,
                campaignIdBase = campaignIdBase,
                templateFeatureKey = templateFeature.key!!,
            )
            features.add(cloned.feature)
            allCampaigns.addAll(cloned.campaigns)
            nextCampaignId = maxCampaignId(cloned.campaigns) + 1
        }

        val megRandomGroupCampaignKeys = mutableListOf<String>()
        MEG_RANDOM_FLAG_KEYS.forEach { featureKey ->
            val campaignIdBase = nextCampaignId
            val cloned = cloneMegFeature(
                templateSettings = megRandomTemplate,
                featureKey = featureKey,
                featureId = featureId++,
                campaignIdBase = campaignIdBase,
                templateFeatureKey = megRandomTemplate.features[0].key!!,
                templateGroupId = MEG_RANDOM_GROUP_ID,
            )
            features.add(cloned.feature)
            allCampaigns.addAll(cloned.campaigns)
            megRandomGroupCampaignKeys.addAll(cloned.groupCampaignKeys)
            cloned.groupCampaignKeys.forEach { campaignGroups[it] = MEG_RANDOM_GROUP_ID }
            nextCampaignId = maxCampaignId(cloned.campaigns) + 1
        }
        groups[MEG_RANDOM_GROUP_ID.toString()] = Groups().apply {
            name = "MEG Random Stress Group"
            this.campaigns = megRandomGroupCampaignKeys.distinct()
        }

        val megAdvanceGroupCampaignKeys = mutableListOf<String>()
        val megAdvancePriority = mutableListOf<String>()
        val megAdvanceWeights = mutableMapOf<String, Double>()
        MEG_ADVANCE_FLAG_KEYS.forEach { featureKey ->
            val campaignIdBase = nextCampaignId
            val cloned = cloneMegFeature(
                templateSettings = megAdvanceTemplate,
                featureKey = featureKey,
                featureId = featureId++,
                campaignIdBase = campaignIdBase,
                templateFeatureKey = megAdvanceTemplate.features[0].key!!,
                templateGroupId = 1,
            )
            features.add(cloned.feature)
            allCampaigns.addAll(cloned.campaigns)
            megAdvanceGroupCampaignKeys.addAll(cloned.groupCampaignKeys)
            cloned.groupCampaignKeys.forEach { campaignGroups[it] = MEG_ADVANCE_GROUP_ID }
            nextCampaignId = maxCampaignId(cloned.campaigns) + 1

            val templateGroup = megAdvanceTemplate.groups?.get("1")
            templateGroup?.p?.forEach { priorityKey ->
                val remapped = remapGroupCampaignKey(priorityKey, cloned.campaignIdMapping)
                megAdvancePriority.add(remapped)
            }
            templateGroup?.wt?.forEach { (weightKey, weightValue) ->
                val remapped = remapGroupCampaignKey(weightKey, cloned.campaignIdMapping)
                megAdvanceWeights[remapped] = weightValue
            }
        }
        groups[MEG_ADVANCE_GROUP_ID.toString()] = Groups().apply {
            name = "MEG Advance Stress Group"
            setEt(2)
            this.campaigns = megAdvanceGroupCampaignKeys.distinct()
            p = megAdvancePriority.distinct().toMutableList()
            wt = megAdvanceWeights
        }

        return Settings().apply {
            accountId = 123456
            sdkKey = "getflags-stress-test-key"
            version = 1
            this.features = features
            this.campaigns = allCampaigns
            this.groups = groups
            this.campaignGroups = campaignGroups
        }.also { SettingsUtil.processSettings(it) }
    }

    private fun parseSettings(json: String): Settings {
        return GsonUtil.gson.fromJson(json, Settings::class.java)
    }

    private fun maxCampaignId(campaigns: List<Campaign>): Int {
        return campaigns.maxOf { it.id ?: 0 }
    }

    private inline fun <reified T> deepClone(value: T): T {
        val json = WingifyClient.objectMapper.writeValueAsString(value as Any)
        return WingifyClient.objectMapper.readValue(json, T::class.java)
    }

    private data class ClonedFeatureBundle(
        val feature: Feature,
        val campaigns: List<Campaign>,
    )

    private data class ClonedMegFeatureBundle(
        val feature: Feature,
        val campaigns: List<Campaign>,
        val groupCampaignKeys: List<String>,
        val campaignIdMapping: Map<Int, Int>,
    )

    private fun cloneBasicRolloutFeature(
        template: Settings,
        featureKey: String,
        featureId: Int,
        rolloutCampaignId: Int,
    ): ClonedFeatureBundle {
        val templateFeature = template.features[0]
        val templateCampaign = template.campaigns!![0]

        val feature = deepClone(templateFeature).apply {
            key = featureKey
            id = featureId
            name = featureKey
            rules?.forEach { it.campaignId = rolloutCampaignId }
        }

        val campaign = deepClone(templateCampaign).apply {
            id = rolloutCampaignId
            key = "${featureKey}_rolloutRule1"
            name = "${featureKey}_rolloutRule1"
        }

        return ClonedFeatureBundle(feature, listOf(campaign))
    }

    private fun cloneFeatureWithCampaigns(
        templateSettings: Settings,
        templateFeature: Feature,
        featureKey: String,
        featureId: Int,
        campaignIdBase: Int,
        templateFeatureKey: String,
    ): ClonedFeatureBundle {
        val campaignIdMapping = templateFeature.rules!!.associate { rule ->
            rule.campaignId!! to (campaignIdBase + rule.campaignId!! - 1)
        }

        val feature = deepClone(templateFeature).apply {
            key = featureKey
            id = featureId
            name = featureKey
            rules?.forEach { rule ->
                rule.campaignId = campaignIdMapping[rule.campaignId!!]
            }
        }

        val campaigns = templateSettings.campaigns!!
            .filter { campaign -> campaign.id in campaignIdMapping.keys }
            .map { campaign ->
                deepClone(campaign).apply {
                    id = campaignIdMapping[campaign.id!!]
                    key = campaign.key!!.replace(templateFeatureKey, featureKey)
                    name = campaign.name!!.replace(templateFeatureKey, featureKey)
                }
            }

        return ClonedFeatureBundle(feature, campaigns)
    }

    private fun cloneMegFeature(
        templateSettings: Settings,
        featureKey: String,
        featureId: Int,
        campaignIdBase: Int,
        templateFeatureKey: String,
        templateGroupId: Int,
    ): ClonedMegFeatureBundle {
        val templateFeature = templateSettings.features[0]
        val campaignIdMapping = templateFeature.rules!!.associate { rule ->
            rule.campaignId!! to (campaignIdBase + rule.campaignId!! - 1)
        }

        val feature = deepClone(templateFeature).apply {
            key = featureKey
            id = featureId
            name = featureKey
            rules?.forEach { rule ->
                rule.campaignId = campaignIdMapping[rule.campaignId!!]
            }
        }

        val campaigns = templateSettings.campaigns!!
            .filter { campaign -> campaign.id in campaignIdMapping.keys }
            .map { campaign ->
                deepClone(campaign).apply {
                    id = campaignIdMapping[campaign.id!!]
                    key = campaign.key!!.replace(templateFeatureKey, featureKey)
                    name = campaign.name!!.replace(templateFeatureKey, featureKey)
                }
            }

        val groupCampaignKeys = templateSettings.campaignGroups
            ?.filterValues { it == templateGroupId }
            ?.keys
            ?.map { key -> remapGroupCampaignKey(key, campaignIdMapping) }
            .orEmpty()

        return ClonedMegFeatureBundle(
            feature = feature,
            campaigns = campaigns,
            groupCampaignKeys = groupCampaignKeys,
            campaignIdMapping = campaignIdMapping,
        )
    }

    private fun remapGroupCampaignKey(
        templateKey: String,
        campaignIdMapping: Map<Int, Int>,
    ): String {
        val parts = templateKey.split("_")
        val oldCampaignId = parts[0].toInt()
        val newCampaignId = campaignIdMapping[oldCampaignId]
            ?: error("Missing campaign mapping for MEG group key $templateKey")
        return if (parts.size > 1) {
            "${newCampaignId}_${parts[1]}"
        } else {
            newCampaignId.toString()
        }
    }
}
