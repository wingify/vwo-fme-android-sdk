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
package com.vwo.packages.segmentation_evaluator.evaluators

import com.fasterxml.jackson.databind.JsonNode
import com.vwo.VWOClient
import com.vwo.decorators.StorageDecorator
import com.vwo.models.Feature
import com.vwo.models.Settings
import com.vwo.models.Storage
import com.vwo.models.user.VWOContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.segmentation_evaluator.enums.SegmentOperatorValueEnum
import com.vwo.packages.segmentation_evaluator.utils.SegmentUtil
import com.vwo.services.LoggerService
import com.vwo.services.StorageService

/**
 * Evaluates user segments for feature targeting.
 *
 * This class is responsible for determining if a user qualifies for a specific feature based on
 * defined segment criteria and user attributes.
 */
class SegmentEvaluator {
    var context: VWOContext? = null
    var settings: Settings? = null
    var feature: Feature? = null

    /**
     * Validates if the segmentation defined in the DSL is applicable based on the provided properties.
     * @param dsl The domain-specific language defining the segmentation rules.
     * @param properties The properties against which the DSL rules are evaluated.
     * @return A boolean indicating if the segmentation is valid.
     */
    fun isSegmentationValid(dsl: JsonNode, properties: Map<String, Any>): Boolean {
        val entry: Map.Entry<String, JsonNode> = SegmentUtil.getKeyValue(dsl)
        val operator = entry.key
        val subDsl: JsonNode = entry.value

        // Evaluate based on the type of segmentation operator
        val operatorEnum: SegmentOperatorValueEnum =
            SegmentOperatorValueEnum.fromValue(operator)

        when (operatorEnum) {
            SegmentOperatorValueEnum.NOT -> {
                val result = isSegmentationValid(subDsl, properties)
                return !result
            }

            SegmentOperatorValueEnum.AND -> return every(subDsl, properties)
            SegmentOperatorValueEnum.OR -> return some(subDsl, properties)
            SegmentOperatorValueEnum.CUSTOM_VARIABLE -> return SegmentOperandEvaluator().evaluateCustomVariableDSL(
                subDsl,
                properties
            )

            SegmentOperatorValueEnum.USER -> return SegmentOperandEvaluator().evaluateUserDSL(
                subDsl.toString(),
                properties
            )

            SegmentOperatorValueEnum.UA -> return SegmentOperandEvaluator().evaluateUserAgentDSL(
                subDsl.toString(),
                context
            )

            else -> return false
        }
    }

    /**
     * Evaluates if any of the DSL nodes are valid using the OR logic.
     * @param dslNodes Array of DSL nodes to evaluate.
     * @param customVariables Custom variables provided for evaluation.
     * @return A boolean indicating if any of the nodes are valid.
     */
    fun some(dslNodes: JsonNode, customVariables: Map<String, Any>): Boolean {
        val uaParserMap: MutableMap<String, MutableList<String>> = HashMap()
        var keyCount = 0 // Initialize count of keys encountered
        var isUaParser = false

        for (dsl in dslNodes) {
            val fieldNames: Iterator<String> = dsl.fieldNames()
            while (fieldNames.hasNext()) {
                val key = fieldNames.next()
                // Check for user agent related keys
                val keyEnum: SegmentOperatorValueEnum =
                    SegmentOperatorValueEnum.Companion.fromValue(key)
                if (keyEnum == SegmentOperatorValueEnum.OPERATING_SYSTEM || keyEnum == SegmentOperatorValueEnum.BROWSER_AGENT || keyEnum == SegmentOperatorValueEnum.DEVICE_TYPE || keyEnum == SegmentOperatorValueEnum.DEVICE) {
                    isUaParser = true
                    val value: JsonNode = dsl.get(key)

                    if (!uaParserMap.containsKey(key)) {
                        uaParserMap[key] = ArrayList()
                    }

                    // Ensure value is treated as an array of strings
                    if (value.isArray()) {
                        for (`val` in value) {
                            if (`val`.isTextual()) {
                                uaParserMap[key]!!.add(`val`.asText())
                            }
                        }
                    } else if (value.isTextual()) {
                        uaParserMap[key]!!.add(value.asText())
                    }

                    keyCount++ // Increment count of keys encountered
                }

                // Check for feature toggle based on feature ID
                if (keyEnum == SegmentOperatorValueEnum.FEATURE_ID) {
                    val featureIdObject: JsonNode = dsl.get(key)
                    val featureIdKeys: Iterator<String> = featureIdObject.fieldNames()
                    if (featureIdKeys.hasNext()) {
                        val featureIdKey = featureIdKeys.next()
                        val featureIdValue: String = featureIdObject.get(featureIdKey).asText()

                        if (featureIdValue == "on" || featureIdValue == "off") {
                            val features: List<Feature?>? = settings?.features
                            val feature = features?.firstOrNull { it?.id == featureIdKey.toInt() }

                            if (feature != null) {
                                val featureKey = feature.key
                                val result = context?.let {
                                    if (featureKey != null) {
                                        checkInUserStorage(featureKey, it)
                                    } else false
                                }?:false
                                if (featureIdValue == "off") {
                                    return !result
                                }
                                return result
                            } else {
                                LoggerService.log(
                                    LogLevelEnum.DEBUG,
                                    "Feature not found with featureIdKey: $featureIdKey"
                                )
                                return false // Handle the case when feature is not found
                            }
                        }
                    }
                }
            }

            // Check if the count of keys encountered is equal to dslNodes.size()
            if (isUaParser && keyCount == dslNodes.size()) {
                try {
                    val uaParserResult = checkUserAgentParser(uaParserMap)
                    return uaParserResult
                } catch (err: Exception) {
                    LoggerService.log(
                        LogLevelEnum.ERROR,
                        "Failed to validate User Agent. Error: $err"
                    )
                }
            }

            // Recursively check each DSL node
            if (isSegmentationValid(dsl, customVariables)) {
                return true
            }
        }
        return false
    }

    /**
     * Evaluates all DSL nodes using the AND logic.
     * @param dslNodes Array of DSL nodes to evaluate.
     * @param customVariables Custom variables provided for evaluation.
     * @return A boolean indicating if all nodes are valid.
     */
    fun every(dslNodes: JsonNode, customVariables: Map<String, Any>): Boolean {
        val locationMap: MutableMap<String, Any> = HashMap()
        for (dsl in dslNodes) {
            val fieldNames: Iterator<String> = dsl.fieldNames()
            while (fieldNames.hasNext()) {
                val key = fieldNames.next()
                // Check if the DSL node contains location-related keys
                val keyEnum: SegmentOperatorValueEnum =
                    SegmentOperatorValueEnum.Companion.fromValue(key)
                if (keyEnum == SegmentOperatorValueEnum.COUNTRY || keyEnum == SegmentOperatorValueEnum.REGION || keyEnum == SegmentOperatorValueEnum.CITY) {
                    addLocationValuesToMap(dsl, locationMap)
                    // Check if the number of location keys matches the number of DSL nodes
                    if (locationMap.size == dslNodes.size()) {
                        return checkLocationPreSegmentation(locationMap)
                    }
                    continue
                }
                val res = isSegmentationValid(dsl, customVariables)
                if (!res) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Adds location values from a DSL node to a map.
     * @param dsl DSL node containing location data.
     * @param locationMap Map to store location data.
     */
    fun addLocationValuesToMap(dsl: JsonNode, locationMap: MutableMap<String, Any>) {
        // Add country, region, and city information to the location map if present
        val keyEnum: SegmentOperatorValueEnum =
            SegmentOperatorValueEnum.fromValue(dsl.fieldNames().next())
        if (keyEnum == SegmentOperatorValueEnum.COUNTRY) {
            locationMap[keyEnum.value] = dsl.get(keyEnum.value).asText()
        }
        if (keyEnum == SegmentOperatorValueEnum.REGION) {
            locationMap[keyEnum.value] = dsl.get(keyEnum.value).asText()
        }
        if (keyEnum == SegmentOperatorValueEnum.CITY) {
            locationMap[keyEnum.value] = dsl.get(keyEnum.value).asText()
        }
    }

    /**
     * Checks if the user's location matches the expected location criteria.
     * @param locationMap Map of expected location values.
     * @return A boolean indicating if the location matches.
     */
    fun checkLocationPreSegmentation(locationMap: Map<String, Any>): Boolean {
        // Ensure user's IP address is available
        val ipAddress = context?.ipAddress
        if (ipAddress.isNullOrEmpty()) {
            LoggerService.log(
                LogLevelEnum.INFO,
                "To evaluate location pre Segment, please pass ipAddress in context object"
            )
            return false
        }
        // Check if location data is available and matches the expected values
        val location = context?.vwo?.location
        if (location.isNullOrEmpty()) {
            return false
        }
        return SegmentUtil.valuesMatch(locationMap, location)
    }

    /**
     * Checks if the user's device information matches the expected criteria.
     * @param uaParserMap Map of expected user agent values.
     * @return A boolean indicating if the user agent matches.
     */
    fun checkUserAgentParser(uaParserMap: Map<String, MutableList<String>>): Boolean {
        // Ensure user's user agent is available
        val userAgent = context?.userAgent
        if (userAgent.isNullOrEmpty()) {
            LoggerService.log(LogLevelEnum.INFO,
                "To evaluate user agent related segments, please pass userAgent in context object"
            )
            return false
        }
        // Check if user agent data is available and matches the expected values
        val userAgentContext = context?.vwo?.userAgent
        if (userAgentContext.isNullOrEmpty()) {
            return false
        }

        return SegmentUtil.checkValuePresent(uaParserMap, userAgentContext)
    }

    /**
     * Checks if the feature is enabled for the user by querying the storage.
     * @param settings The settings model containing configuration.
     * @param featureKey The key of the feature to check.
     * @param context The context object to check against.
     * @return A boolean indicating if the feature is enabled for the user.
     */
    fun checkInUserStorage(
        featureKey: String,
        context: VWOContext
    ): Boolean {
        val storageService = StorageService()
        val storedDataMap: Map<String, Any>? = StorageDecorator().getFeatureFromStorage(featureKey, context, storageService)
        try {
            val storageMapAsString: String =
                VWOClient.objectMapper.writeValueAsString(storedDataMap)
            val storedData: Storage? =
                VWOClient.objectMapper.readValue(storageMapAsString, Storage::class.java)

            return storedData != null && (storedDataMap?.size ?: 0) > 1
        } catch (exception: Exception) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "Error in checking feature in user storage. Got error: $exception"
            )
            return false
        }
    }
}