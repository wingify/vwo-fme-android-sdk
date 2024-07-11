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
import com.vwo.packages.logger.enums.LogLevelEnum
import java.net.URLDecoder
import java.util.regex.Pattern

class SegmentOperandEvaluator {
    fun evaluateCustomVariableDSL(
        dslOperandValue: JsonNode?,
        properties: Map<String?, Any>
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
            val listIdPattern = Pattern.compile("inlist\\((\\w+:\\d+)\\)")
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
            queryParamsObj["listId"] = listId

            // Make a web service call to check the attribute against the list
            val gatewayServiceResponse: String = GatewayServiceUtil.getFromGatewayService(
                queryParamsObj,
                UrlEnum.ATTRIBUTE_CHECK.url
            )
                ?: return false
            return gatewayServiceResponse.toBoolean()
        } else {
            // Process other types of operands
            var tagValue = properties[operandKey]
            tagValue = preProcessTagValue(tagValue.toString())
            val preProcessOperandValue = preProcessOperandValue(operandValue)
            val processedValues = processValues(preProcessOperandValue["operandValue"], tagValue)
            tagValue = processedValues["tagValue"]
            val operandType: SegmentOperandValueEnum? =
                preProcessOperandValue["operandType"] as SegmentOperandValueEnum?
            return extractResult(
                operandType,
                processedValues["operandValue"].toString().trim { it <= ' ' }
                    .replace("\"", ""),
                tagValue.toString())
        }
    }

    fun preProcessOperandValue(operand: String?): Map<String, Any?> {
        val operandType: SegmentOperandValueEnum
        var operandValue: String? = null

        if (SegmentUtil.matchWithRegex(operand, SegmentOperandRegexEnum.LOWER_MATCH.getRegex())) {
            operandType = SegmentOperandValueEnum.LOWER_VALUE
            operandValue =
                extractOperandValue(operand, SegmentOperandRegexEnum.LOWER_MATCH.getRegex())
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.WILDCARD_MATCH.getRegex()
            )
        ) {
            operandValue =
                extractOperandValue(operand, SegmentOperandRegexEnum.WILDCARD_MATCH.getRegex())
            val startingStar: Boolean = SegmentUtil.matchWithRegex(
                operandValue,
                SegmentOperandRegexEnum.STARTING_STAR.getRegex()
            )
            val endingStar: Boolean = SegmentUtil.matchWithRegex(
                operandValue,
                SegmentOperandRegexEnum.ENDING_STAR.getRegex()
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
                operandValue.replace(SegmentOperandRegexEnum.STARTING_STAR.getRegex().toRegex(), "")
                    .replace(SegmentOperandRegexEnum.ENDING_STAR.getRegex().toRegex(), "")
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.REGEX_MATCH.getRegex()
            )
        ) {
            operandType = SegmentOperandValueEnum.REGEX_VALUE
            operandValue =
                extractOperandValue(operand, SegmentOperandRegexEnum.REGEX_MATCH.getRegex())
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.GREATER_THAN_MATCH.getRegex()
            )
        ) {
            operandType = SegmentOperandValueEnum.GREATER_THAN_VALUE
            operandValue =
                extractOperandValue(operand, SegmentOperandRegexEnum.GREATER_THAN_MATCH.getRegex())
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.GREATER_THAN_EQUAL_TO_MATCH.getRegex()
            )
        ) {
            operandType = SegmentOperandValueEnum.GREATER_THAN_EQUAL_TO_VALUE
            operandValue = extractOperandValue(
                operand,
                SegmentOperandRegexEnum.GREATER_THAN_EQUAL_TO_MATCH.getRegex()
            )
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.LESS_THAN_MATCH.getRegex()
            )
        ) {
            operandType = SegmentOperandValueEnum.LESS_THAN_VALUE
            operandValue =
                extractOperandValue(operand, SegmentOperandRegexEnum.LESS_THAN_MATCH.getRegex())
        } else if (SegmentUtil.matchWithRegex(
                operand,
                SegmentOperandRegexEnum.LESS_THAN_EQUAL_TO_MATCH.getRegex()
            )
        ) {
            operandType = SegmentOperandValueEnum.LESS_THAN_EQUAL_TO_VALUE
            operandValue = extractOperandValue(
                operand,
                SegmentOperandRegexEnum.LESS_THAN_EQUAL_TO_MATCH.getRegex()
            )
        } else {
            operandType = SegmentOperandValueEnum.EQUAL_VALUE
            operandValue = operand
        }

        val result: MutableMap<String, Any?> = HashMap()
        result["operandType"] = operandType
        result["operandValue"] = operandValue
        return result
    }

    fun evaluateUserDSL(dslOperandValue: String, properties: Map<String?, Any>): Boolean {
        val users =
            dslOperandValue.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (user in users) {
            if (user.trim { it <= ' ' }.replace("\"", "") == properties["_vwoUserId"]) {
                return true
            }
        }
        return false
    }

    fun evaluateUserAgentDSL(dslOperandValue: String?, context: VWOContext?): Boolean {
        if (context == null || context.userAgent == null) {
            //LogManager.getInstance().info("To Evaluate UserAgent segmentation, please provide userAgent in context");
            return false
        }
        var tagValue = URLDecoder.decode(context.userAgent)
        val preProcessOperandValue = preProcessOperandValue(dslOperandValue)
        val processedValues = processValues(preProcessOperandValue["operandValue"], tagValue)
        tagValue = processedValues["tagValue"] as String?
        val operandType: SegmentOperandValueEnum? =
            preProcessOperandValue["operandType"] as SegmentOperandValueEnum?
        return extractResult(
            operandType,
            processedValues["operandValue"].toString().trim { it <= ' ' }
                .replace("\"", ""),
            tagValue)
    }

    fun preProcessTagValue(tagValue: String?): String {
        if (tagValue == null) {
            return ""
        }
        if (DataTypeUtil.isBoolean(tagValue)) {
            return tagValue.toBoolean().toString()
        }
        return tagValue.trim { it <= ' ' }
    }

    private fun processValues(operandValue: Any?, tagValue: Any?): Map<String, Any?> {
        // Convert operand and tag values to floats
        val processedOperandValue: Double
        val processedTagValue: Double
        val result: MutableMap<String, Any?> = HashMap()
        try {
            processedOperandValue = operandValue.toString().toDouble()
            processedTagValue = tagValue.toString().toDouble()
        } catch (e: NumberFormatException) {
            // Return original values if conversion fails
            result["operandValue"] = operandValue
            result["tagValue"] = tagValue
            return result
        }
        // Convert numeric values back to strings
        result["operandValue"] = processedOperandValue.toString()
        result["tagValue"] = processedTagValue.toString()
        return result
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
        tagValue: String?
    ): Boolean {
        var result = false

        when (operandType) {
            SegmentOperandValueEnum.LOWER_VALUE -> result =
                operandValue.toString().equals(tagValue, ignoreCase = true)

            SegmentOperandValueEnum.STARTING_ENDING_STAR_VALUE -> result =
                tagValue!!.contains(operandValue.toString())

            SegmentOperandValueEnum.STARTING_STAR_VALUE -> result =
                tagValue!!.endsWith(operandValue.toString())

            SegmentOperandValueEnum.ENDING_STAR_VALUE -> result =
                tagValue!!.startsWith(operandValue.toString())

            SegmentOperandValueEnum.REGEX_VALUE -> try {
                val pattern = Pattern.compile(operandValue.toString())
                val matcher = pattern.matcher(tagValue)
                result = matcher.matches()
            } catch (e: Exception) {
                result = false
            }

            SegmentOperandValueEnum.GREATER_THAN_VALUE -> result =
                tagValue!!.toFloat() > operandValue.toString().toFloat()

            SegmentOperandValueEnum.GREATER_THAN_EQUAL_TO_VALUE -> result =
                tagValue!!.toFloat() >= operandValue.toString().toFloat()

            SegmentOperandValueEnum.LESS_THAN_VALUE -> result =
                tagValue!!.toFloat() < operandValue.toString().toFloat()

            SegmentOperandValueEnum.LESS_THAN_EQUAL_TO_VALUE -> result =
                tagValue!!.toFloat() <= operandValue.toString().toFloat()

            else -> result = tagValue == operandValue.toString()
        }
        return result
    }

    /**
     * Extracts the operand value based on the provided regex pattern.
     *
     * @param operand The operand to be matched.
     * @param regex The regex pattern to match the operand against.
     * @return The extracted operand value or the original operand if no match is found.
     */
    fun extractOperandValue(operand: String?, regex: String?): String? {
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(operand)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return operand
    }
}
