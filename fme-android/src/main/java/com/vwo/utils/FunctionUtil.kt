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
import com.vwo.models.Feature
import com.vwo.models.Settings
import java.util.function.Predicate
import java.util.stream.Collectors

object FunctionUtil {
    /**
     * Clones an object using JSON serialization and deserialization.
     * @param obj  The object to clone.
     * @return   The cloned object.
     */
    fun cloneObject(obj: Any?): Any? {
        if (obj == null) {
            return null
        }
        // Use JSON serialization and deserialization to perform a deep clone
        return Gson().fromJson(Gson().toJson(obj), obj.javaClass)
    }

    val currentUnixTimestamp: Long
        /**
         * Retrieves the current Unix timestamp in seconds.
         * @return  The current Unix timestamp in seconds.
         */
        get() =// Convert the current date to Unix timestamp in seconds
            System.currentTimeMillis() / 1000L

    val currentUnixTimestampInMillis: Long
        /**
         * Retrieves the current Unix timestamp in milliseconds.
         * @return  The current Unix timestamp in milliseconds.
         */
        get() =// Return the current Unix timestamp in milliseconds
            System.currentTimeMillis()

    val randomNumber: Double
        /**
         * Retrieves a random number between 0 and 1.
         * @return  A random number between 0 and 1.
         */
        get() =// Use Math.random() to generate a random number between 0 and 1
            Math.random()

    /**
     * Retrieves specific rules based on the type from a feature.
     * @param feature The feature model.
     * @param type The type of the rules to retrieve.
     * @return A list of rules that match the type.
     */
    fun getSpecificRulesBasedOnType(feature: Feature?, type: CampaignTypeEnum?): List<Campaign> {
        if (feature?.rulesLinkedCampaign == null) {
            return emptyList<Campaign>()
        }
        if (type != null) {
            return feature.rulesLinkedCampaign.stream()
                .filter(Predicate<Campaign> { rule: Campaign -> rule.type == type.value })
                .collect(Collectors.toList<Campaign>())
        }
        return feature.rulesLinkedCampaign
    }

    /**
     * Retrieves all AB and Personalize rules from a feature.
     * @param feature The feature model.
     * @return A list of AB and Personalize rules.
     */
    fun getAllExperimentRules(feature: Feature?): List<Campaign> {
        if (feature?.rulesLinkedCampaign == null) {
            return emptyList<Campaign>()
        }
        return feature.rulesLinkedCampaign.stream()
            .filter(Predicate<Campaign> { rule: Campaign -> rule.type == CampaignTypeEnum.AB.value || rule.type == CampaignTypeEnum.PERSONALIZE.value })
            .collect(Collectors.toList<Campaign>())
    }

    /**
     * Retrieves a feature by its key from the settings.
     * @param settings The settings model.
     * @param featureKey The key of the feature to find.
     * @return The feature if found, otherwise null.
     */
    fun getFeatureFromKey(settings: Settings?, featureKey: String): Feature? {
        if (settings?.features == null) {
            return null
        }
        return settings.features!!.stream()
            .filter { feature: Feature -> feature.key == featureKey }
            .findFirst()
            .orElse(null)
    }

    fun doesEventBelongToAnyFeature(eventName: String, settings: Settings): Boolean {
        return settings.features!!.stream()
            .anyMatch { feature: Feature ->
                feature.metrics!!.stream()
                    .anyMatch(Predicate<Metric> { metric: Metric -> metric.identifier == eventName })
            }
    }
}
