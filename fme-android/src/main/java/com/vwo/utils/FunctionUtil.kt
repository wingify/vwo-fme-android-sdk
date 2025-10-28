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

import com.google.gson.Gson
import com.vwo.enums.CampaignTypeEnum
import com.vwo.models.Campaign
import com.vwo.models.Feature
import com.vwo.models.Metric
import com.vwo.models.Settings
import java.util.function.Predicate
import java.util.stream.Collectors

/**
 * Utility object for functional operations.
 *
 * This object provides helper methods for performing functional operations, such as applying
 * functions to collections, transforming data, or working with higher-order functions.
 */
object FunctionUtil {
    /**
     * Clones an object using JSON serialization and deserialization.
     * @param obj  The object to clone.
     * @return   The cloned object.
     */
    fun cloneObject(obj: Any): Any {
        // Use JSON serialization and deserialization to perform a deep clone
        return GsonUtil.gson.fromJson(GsonUtil.gson.toJson(obj), obj.javaClass)
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
        return feature?.rulesLinkedCampaign?.let { rules ->
            type?.let { t ->
                rules.filter { it.type == t.value }
            } ?: rules
        } ?: emptyList()
    }

    /**
     * Retrieves all AB and Personalize rules from a feature.
     * @param feature The feature model.
     * @return A list of AB and Personalize rules.
     */
    fun getAllExperimentRules(feature: Feature?): List<Campaign> {
        return feature?.rulesLinkedCampaign?.filter {
            it.type == CampaignTypeEnum.AB.value || it.type == CampaignTypeEnum.PERSONALIZE.value
        } ?: emptyList()
    }

    /**
     * Retrieves a feature by its key from the settings.
     * @param settings The settings model.
     * @param featureKey The key of the feature to find.
     * @return The feature if found, otherwise null.
     */
    fun getFeatureFromKey(settings: Settings?, featureKey: String): Feature? {
        return settings?.features?.firstOrNull { it.key == featureKey }
    }

    /**
     * Checks if an event belongs to any feature in the settings.
     *
     * @param eventName The name of the event to check.
     * @param settings The settings containing the features and their associated metrics.
     * @return `true` if the event belongs to any feature, `false` otherwise.
     */
    fun doesEventBelongToAnyFeature(eventName: String, settings: Settings): Boolean {
        return settings.features.any { feature ->
            feature.metrics?.any { metric ->
                metric.identifier == eventName
            } ?: false
        }
    }
}
