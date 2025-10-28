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
package com.vwo.packages.segmentation_evaluator.evaluators

import com.vwo.utils.JsonNode
import com.vwo.utils.*
import com.vwo.constants.Constants
import com.vwo.enums.UrlEnum
import com.vwo.models.Feature
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.segmentation_evaluator.enums.SegmentOperandRegexEnum
import com.vwo.packages.segmentation_evaluator.enums.SegmentOperandValueEnum
import com.vwo.packages.segmentation_evaluator.utils.SegmentUtil
import com.vwo.packages.storage.GatewayResponseStore
import com.vwo.providers.StorageProvider
import com.vwo.services.LoggerService
import com.vwo.utils.DataTypeUtil
import com.vwo.utils.GatewayServiceUtil
import java.net.URLDecoder
import java.text.DecimalFormat
import java.util.regex.Pattern

private const val OPERAND_VALUE = "operandValue"

private const val OPERAND_TYPE = "operandType"

private const val TAG_VALUE = "tagValue"

/**
 * Evaluates segment operands for user targeting.
 *
 * This class is responsible for processing and evaluating individual operands within segmentation
 * rules to determine if a user meets the specified criteria.
 */
class SegmentOperandEvaluator {

    /**
     * Evaluates a custom variable DSL operand against user properties.
     *
     * @param dslOperandValue The DSL operand value as a JSON node.
     * @param properties The user properties to evaluate against.
     * @return `true` if the operand matches the user properties, `false` otherwise.
     */
    fun evaluateCustomVariableDSL(
        dslOperandValue: JsonNode,
        properties: Map<String, Any>,
        accountId: Int?,
        feature: Feature?
    ): Boolean {
        val entry: Map.Entry<String, JsonNode> = SegmentUtil.getKeyValue(dslOperandValue)
        val operandKey = entry.key
        val operandValueNode: JsonNode = entry.value
        val operandValue: String = operandValueNode.asText()

        // Check if the property exists
        if (!properties.containsKey(operandKey)) {
            return false
        }

        // Handle 'inlist' operand
        if (operandValue.contains("inlist")) {
            val listIdPattern = Pattern.compile("inlist\\(([^)]+)\\)")
            val matcher = listIdPattern.matcher(operandValue)
            if (!matcher.find()) {
                LoggerService.log(LogLevelEnum.ERROR, "Invalid 'inList' operand format")
                return false
            }
            val listId = matcher.group(1)
            // Process the tag value and prepare query parameters
            val tagValue = properties[operandKey]
            val attributeValue = preProcessTagValue(tagValue.toString())
            val queryParamsObj: MutableMap<String, String> = HashMap()
            queryParamsObj["attribute"] = attributeValue
            if (listId != null) queryParamsObj["listId"] = listId
            accountId?.toString()?.let { queryParamsObj["accountId"] = it }

            return evaluateListAttribute(
                feature,
                listId,
                attributeValue,
                properties,
                queryParamsObj,
                true
            )
        } else {
            // Process other types of operands
            var tagValue:Any? = properties[operandKey]
            if (tagValue == null) {
                tagValue = ""
            }
            tagValue = preProcessTagValue(tagValue.toString())
            val preProcessOperandValue = preProcessOperandValue(operandValue)
            val processedValues = preProcessOperandValue[OPERAND_VALUE]?.let {
                processValues(it, tagValue as String)
            }

            // Convert numeric values to strings if processing wildcard pattern
            val operandType = preProcessOperandValue[OPERAND_TYPE] as SegmentOperandValueEnum?
            if (operandType == SegmentOperandValueEnum.STARTING_ENDING_STAR_VALUE ||
                operandType == SegmentOperandValueEnum.STARTING_STAR_VALUE ||
                operandType == SegmentOperandValueEnum.ENDING_STAR_VALUE ||
                operandType == SegmentOperandValueEnum.REGEX_VALUE) {
                processedValues?.set(TAG_VALUE, processedValues[TAG_VALUE].toString())
            }

            tagValue = processedValues?.get(TAG_VALUE)
            return extractResult(
                operandType,
                processedValues?.get(OPERAND_VALUE).toString()
                    .trim { it <= ' ' }
                    .replace("\"", ""),
                tagValue.toString())
        }
    }

    /**
     * Pre-processes the operand value to determine the operand type and extract the value.
     *
     * @param operand The operand value to pre-process.
     * @return A map containing the operand type and the extracted operand value.
     */
    fun preProcessOperandValue(operand: String): Map<String, Any?> {
        val operandType: SegmentOperandValueEnum
        var operandValue: String?

        if (SegmentUtil.matchWithRegex(operand, SegmentOperandRegexEnum.LOWER_MATCH.regex)) {
            operandType = SegmentOperandValueEnum.LOWER_VALUE
            operandValue = extractOperandValue(operand, SegmentOperandRegexEnum.LOWER_MATCH.regex)
        } else if (SegmentUtil.matchWithRegex(operand, SegmentOperandRegexEnum.WILDCARD_MATCH.regex)) {
            operandValue = extractOperandValue(operand, SegmentOperandRegexEnum.WILDCARD_MATCH.regex)
            val startingStar: Boolean = SegmentUtil.matchWithRegex(
                operandValue,
                SegmentOperandRegexEnum.STARTING_STAR.regex
            )
            val endingStar: Boolean = SegmentUtil.matchWithRegex(
                operandValue,
                SegmentOperandRegexEnum.ENDING_STAR.regex
            )
            operandType = if (startingStar && endingStar) {
                SegmentOperandValueEnum.STARTING_ENDING_STAR_VALUE
            } else if (startingStar) {
                SegmentOperandValueEnum.STARTING_STAR_VALUE
            } else if (endingStar) {
                SegmentOperandValueEnum.ENDING_STAR_VALUE
            } else {
                SegmentOperandValueEnum.REGEX_VALUE
            }
            operandValue =
                operandValue?.replace(SegmentOperandRegexEnum.STARTING_STAR.regex.toRegex(), "")
                    ?.replace(SegmentOperandRegexEnum.ENDING_STAR.regex.toRegex(), "")
        } else if (SegmentUtil.matchWithRegex(operand, SegmentOperandRegexEnum.REGEX_MATCH.regex)) {
            operandType = SegmentOperandValueEnum.REGEX_VALUE
            operandValue = extractOperandValue(operand, SegmentOperandRegexEnum.REGEX_MATCH.regex)
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.GREATER_THAN_MATCH.regex
            )
        ) {
            operandType = SegmentOperandValueEnum.GREATER_THAN_VALUE
            operandValue =
                extractOperandValue(operand, SegmentOperandRegexEnum.GREATER_THAN_MATCH.regex)
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.GREATER_THAN_EQUAL_TO_MATCH.regex
            )
        ) {
            operandType = SegmentOperandValueEnum.GREATER_THAN_EQUAL_TO_VALUE
            operandValue = extractOperandValue(
                operand,
                SegmentOperandRegexEnum.GREATER_THAN_EQUAL_TO_MATCH.regex
            )
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.LESS_THAN_MATCH.regex
            )
        ) {
            operandType = SegmentOperandValueEnum.LESS_THAN_VALUE
            operandValue =
                extractOperandValue(operand, SegmentOperandRegexEnum.LESS_THAN_MATCH.regex)
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.LESS_THAN_EQUAL_TO_MATCH.regex
            )
        ) {
            operandType = SegmentOperandValueEnum.LESS_THAN_EQUAL_TO_VALUE
            operandValue = extractOperandValue(
                operand,
                SegmentOperandRegexEnum.LESS_THAN_EQUAL_TO_MATCH.regex
            )
        } else {
            operandType = SegmentOperandValueEnum.EQUAL_VALUE
            operandValue = operand
        }

        val result: MutableMap<String, Any?> = HashMap()
        result[OPERAND_TYPE] = operandType
        result[OPERAND_VALUE] = operandValue
        return result
    }

    /**
     * Evaluates a user DSL operand against user properties.
     *
     * @param dslOperandValue The DSL operand value as a string.
     * @param properties The user properties to evaluate against.
     * @return `true` if the operand matches the user properties, `false` otherwise.
     */
    fun evaluateUserDSL(
        dslOperandValue: String,
        properties: Map<String, Any>,
        accountId: Int?,
        feature: Feature?
    ): Boolean {
        // Handle 'inlist' operand
        if (dslOperandValue.contains("inlist")) {
            val listIdPattern = Pattern.compile("inlist\\(([^)]+)\\)")
            val matcher = listIdPattern.matcher(dslOperandValue)
            if (!matcher.find()) {
                LoggerService.log(LogLevelEnum.ERROR, "Invalid 'inList' operand format")
                return false
            }
            val listId = matcher.group(1)
            // Process the tag value and prepare query parameters
            val tagValue = properties["_vwoUserId"]
            val attributeValue = preProcessTagValue(tagValue.toString())
            val queryParamsObj: MutableMap<String, String> = HashMap()
            queryParamsObj["attribute"] = attributeValue
            if (listId != null) queryParamsObj["listId"] = listId
            accountId?.toString()?.let { queryParamsObj["accountId"] = it }

            return evaluateListAttribute(feature, listId, attributeValue, properties, queryParamsObj, false)
        } else {
            val users =
                dslOperandValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (user in users) {
                if (user.trim { it <= ' ' }.replace("\"", "") == properties["_vwoUserId"]) {
                    return true
                }
            }
            return false
        }
    }

    private fun evaluateListAttribute(
        feature: Feature?,
        listId: String?,
        attributeValue: String,
        properties: Map<String, Any>,
        queryParamsObj: MutableMap<String, String>,
        isCustomVariable: Boolean
    ): Boolean {

        val gatewayStore = StorageProvider.gatewayStore
        if (feature?.key != null && listId != null && gatewayStore != null) {
            val key = gatewayStore.getStorageKeyForAttributeCheck(
                feature.key!!,
                listId,
                attributeValue,
                properties["_vwoUserId"].toString(),
                isCustomVariable
            )
            val isValid = isCachedEvaluationValid(key, gatewayStore)
            val value = gatewayStore.getBoolean(key)

            if (value != null && isValid) {
                LoggerService.log(
                    LogLevelEnum.INFO,
                    "CACHED_EVALUATION_RESPONSE",
                    mapOf("value" to value.toString())
                )
                return value
            } else {
                // Make a web service call to check the attribute against the list
                val gatewayServiceResponse: String = GatewayServiceUtil.getFromGatewayService(
                    queryParamsObj, UrlEnum.ATTRIBUTE_CHECK.url, "application/javascript"
                ) ?: return false
                val result = gatewayServiceResponse.toBoolean()
                gatewayStore.saveBoolean(key, result)
                val expiryTime =
                    System.currentTimeMillis() + Constants.GATEWAY_LIST_EVALUATION_CACHE_DURATION
                gatewayStore.saveLong("${key}_expiry", expiryTime)
                return result
            }
        } else {
            // Make a web service call to check the attribute against the list
            val gatewayServiceResponse: String = GatewayServiceUtil.getFromGatewayService(
                queryParamsObj, UrlEnum.ATTRIBUTE_CHECK.url, "application/javascript"
            ) ?: return false
            return gatewayServiceResponse.toBoolean()
        }
    }

    private fun isCachedEvaluationValid(key: String, gatewayStore: GatewayResponseStore): Boolean {
        val expiryTime = gatewayStore.getLong("${key}_expiry")
        val isValid = expiryTime > 0 && expiryTime >= System.currentTimeMillis()
        return isValid
    }

    /**
     * Evaluates a user agent DSL operand against the user's context.
     *
     * @param dslOperandValue The DSL operand value as a string.
     * @param context The user's context containing the user agent information.
     * @return `true` if the operand matches the user agent, `false` otherwise.
     */
    fun evaluateUserAgentDSL(dslOperandValue: String, context: VWOUserContext?): Boolean {
        if (StorageProvider.userAgent == null) {
            //LogManager.getInstance().info("To Evaluate UserAgent segmentation, please provide userAgent in context");
            return false
        }
        var tagValue = URLDecoder.decode(StorageProvider.userAgent)
        val preProcessOperandValue = preProcessOperandValue(dslOperandValue)
        val processedValues = preProcessOperandValue[OPERAND_VALUE]?.let {
            processValues(it, tagValue)
        }?:return false

        tagValue = processedValues[TAG_VALUE] as String?
        val operandType = preProcessOperandValue[OPERAND_TYPE] as SegmentOperandValueEnum?
        return extractResult(
            operandType,
            processedValues[OPERAND_VALUE].toString().trim { it <= ' ' }
                .replace("\"", ""),
            tagValue)
    }

    /**
     * Evaluates a given string tag value against a DSL operand value.
     *
     * This function parses a DSL operand string to extract an operand type and its associated value.
     * It then compares this extracted value with the provided tag value to determine a boolean result.
     *
     * @param dslOperandValue The DSL operand string containing the operand type and value (e.g., "contains(\"value\")").
     * @param value The string tag value to be evaluated against the DSL operand.
     * @return `true` if the tag value matches the criteria defined by the DSL operand, `false` otherwise.
     */
    fun evaluateStringOperandDSL(dslOperandValue: String, value: String): Boolean {
        val tagValue: String = value

        // Pre-process the DSL operand string to extract its type and value.
        // This function is assumed to return a Map where OPERAND_TYPE and OPERAND_VALUE are keys.
        val preProcessOperandValue = preProcessOperandValue(dslOperandValue)

        // It attempts to convert the operand value and store it in a map.
        // If OPERAND_VALUE is not found, the function returns false immediately.
        val processedValues = preProcessOperandValue[OPERAND_VALUE]?.let {
            val result: MutableMap<String, Any> = HashMap()
            result[OPERAND_VALUE] = convertValue(it)
            result
        } ?: return false

        // Extract the operand type from the pre-processed map.
        val operandType = preProcessOperandValue[OPERAND_TYPE] as SegmentOperandValueEnum?

        // Perform the actual comparison based on the extracted operand type, operand and the tag value
        return extractResult(
            operandType,
            preProcessOperandValue[OPERAND_VALUE].toString().trim { it <= ' ' }
                .replace("\"", ""),
            tagValue)
    }

    /**
     * Pre-processes the tag value by trimming whitespace and converting booleans to strings.
     *
     * @param tagValue The tag value to pre-process.
     * @return The pre-processed tag value.
     */
    fun preProcessTagValue(tagValue: String): String {
        if (tagValue == null) {
            return ""
        }
        if (DataTypeUtil.isBoolean(tagValue)) {
            return tagValue.toBoolean().toString()
        }
        return tagValue.trim { it <= ' ' }
    }

    /**
     * Processes the operand and tag values by converting them to appropriate data types.
     *
     * @param operandValue The operand value to process.
     * @param tagValue The tag value to process.
     * @return A map containing the processed operand and tag values.
     */
    private fun processValues(operandValue: Any, tagValue: Any): MutableMap<String, Any> {
        val result: MutableMap<String, Any> = HashMap()
        // Process operandValue
        result[OPERAND_VALUE] = convertValue(operandValue)

        // Process tagValue
        result[TAG_VALUE] = convertValue(tagValue)

        return result
    }

    /**
     * Converts a value to a string representation, handling booleans and numbers appropriately.
     *
     * @param value The value to convert.
     * @return The string representation of the value.
     */
    private fun convertValue(value: Any): String {
        if (value is Boolean) {
            return value.toString() // Convert boolean to "true" or "false"
        }

        try {
            // Attempt to convert to a numeric value
            val numericValue = value.toString().toDouble()
            // Check if the numeric value is actually an integer
            if (numericValue == numericValue.toInt().toDouble()) {
                return numericValue.toInt().toString() // Remove '.0' by converting to int
            } else {
                // Format float to avoid scientific notation for large numbers
                val df = DecimalFormat("#.##############") // Adjust the pattern as needed
                return df.format(numericValue)
            }
        } catch (e: NumberFormatException) {
            // Return the value as-is if it's not a number
            return value.toString()
        }
    }

    /**
     * Extracts the result of the evaluation based on the operand type and values.
     * @param operandType The type of the operand.
     * @param operandValue The value of the operand.
     * @param tagValue The value of the tag to compare against.
     * @return A boolean indicating the result of the evaluation.
     */
    fun extractResult(
        operandType: SegmentOperandValueEnum?,
        operandValue: Any,
        tagValue: String
    ): Boolean {
        var result: Boolean

        when (operandType) {
            SegmentOperandValueEnum.LOWER_VALUE -> result =
                operandValue.toString().equals(tagValue, ignoreCase = true)

            SegmentOperandValueEnum.STARTING_ENDING_STAR_VALUE -> result =
                tagValue.contains(operandValue.toString(), ignoreCase = true)

            SegmentOperandValueEnum.STARTING_STAR_VALUE -> result =
                tagValue.endsWith(operandValue.toString(), ignoreCase = true)

            SegmentOperandValueEnum.ENDING_STAR_VALUE -> result =
                tagValue.startsWith(operandValue.toString(), ignoreCase = true)

            SegmentOperandValueEnum.REGEX_VALUE -> try {
                val pattern = Pattern.compile(operandValue.toString())
                val matcher = pattern.matcher(tagValue)
                result = matcher.matches()
            } catch (e: Exception) {
                result = false
            }

            SegmentOperandValueEnum.GREATER_THAN_VALUE -> result =
                compareVersions(tagValue, operandValue.toString()) > 0

            SegmentOperandValueEnum.GREATER_THAN_EQUAL_TO_VALUE -> result =
                compareVersions(tagValue, operandValue.toString()) >= 0

            SegmentOperandValueEnum.LESS_THAN_VALUE -> result =
                compareVersions(tagValue, operandValue.toString()) < 0

            SegmentOperandValueEnum.LESS_THAN_EQUAL_TO_VALUE -> result =
                compareVersions(tagValue, operandValue.toString()) <= 0

            else -> result = tagValue.equals(operandValue.toString(), ignoreCase = true)
        }
        return result
    }

    /**
     * Compares two version strings using semantic versioning rules.
     * Supports formats like "1.2.3", "1.0", "2.1.4.5", etc.
     *
     * @param version1 First version string
     * @param version2 Second version string
     * @return -1 if version1 < version2, 0 if equal, 1 if version1 > version2
     */
    private fun compareVersions(version1: String, version2: String): Int {
        try {
            // Split versions by dots and convert to integers
            val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }

            // Find the maximum length to handle different version formats
            val maxLength = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLength) {
                val part1 = if (i < parts1.size) parts1[i] else 0
                val part2 = if (i < parts2.size) parts2[i] else 0

                when {
                    part1 < part2 -> return -1
                    part1 > part2 -> return 1
                }
            }
            return 0 // Versions are equal
        } catch (e: Exception) {
            // If version parsing fails, fall back to string comparison
            return version1.compareTo(version2)
        }
    }

    /**
     * Extracts the operand value based on the provided regex pattern.
     *
     * @param operand The operand to be matched.
     * @param regex The regex pattern to match the operand against.
     * @return The extracted operand value or the original operand if no match is found.
     */
    fun extractOperandValue(operand: String, regex: String): String? {
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(operand)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return operand
    }
}