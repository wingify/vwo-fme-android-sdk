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
package com.vwo.packages.segmentation_evaluator.utils

import com.vwo.utils.JsonNode
import com.vwo.utils.*
import java.util.Locale
import java.util.regex.Pattern

/**
 * Utility class for segment operations.
 */
object SegmentUtil {
    /**
     * Checks if the actual values match the expected values specified in the map.
     * @param expectedMap A map of expected values for different keys.
     * @param actualMap A map of actual values to compare against.
     * @return A boolean indicating if all actual values match the expected values.
     */
    fun checkValuePresent(
        expectedMap: Map<String, List<String>>,
        actualMap: Map<String, String>
    ): Boolean {
        for (key in actualMap.keys) {
            if (expectedMap.containsKey(key)) {
                var expectedValues = expectedMap[key]!!
                // convert expectedValues to lowercase
                expectedValues = expectedValues.map { it.lowercase() }
                val actualValue = actualMap[key]

                // Handle wildcard patterns for all keys
                for (`val` in expectedValues) {
                    if (`val`.startsWith("wildcard(") && `val`.endsWith(")")) {
                        val wildcardPattern = `val`.substring(9, `val`.length - 1) // Extract pattern from wildcard string
                        val regex = Pattern.compile(
                            wildcardPattern.replace("*", ".*"),
                            Pattern.CASE_INSENSITIVE
                        ) // Convert wildcard pattern to regex
                        val matcher = regex.matcher(actualValue)
                        if (matcher.matches()) {
                            return true // Match found, return true
                        }
                    }
                }

                // Direct value check for all keys
                if (expectedValues.contains(actualValue!!.trim { it <= ' ' }
                        .lowercase(Locale.getDefault()))) {
                    return true // Direct value match found, return true
                }
            }
        }
        return false // No matches found
    }

    /**
     * Compares expected location values with user's location to determine a match.
     * @param expectedLocationMap A map of expected location values.
     * @param userLocation The user's actual location.
     * @return A boolean indicating if the user's location matches the expected values.
     */
    fun valuesMatch(
        expectedLocationMap: Map<String, Any>,
        userLocation: Map<String, String>
    ): Boolean {
        for ((key, value) in expectedLocationMap) {
            if (userLocation.containsKey(key)) {
                val normalizedValue1 = normalizeValue(value)
                val normalizedValue2 = normalizeValue(userLocation[key])
                if (normalizedValue1 != normalizedValue2) {
                    return false
                }
            } else {
                return false
            }
        }
        return true // If all values match, return true
    }

    /**
     * Normalizes a value to a consistent format for comparison.
     * @param value The value to normalize.
     * @return The normalized value.
     */
    fun normalizeValue(value: Any?): String? {
        if (value == null) {
            return null
        }
        // Remove quotes and trim whitespace
        return value.toString().replace("^\"|\"$".toRegex(), "").trim { it <= ' ' }
    }

    /**
     * Helper method to extract the first key-value pair from a map.
     */
    fun getKeyValue(node: JsonNode): Map.Entry<String, JsonNode> {
        val fields: Iterator<Map.Entry<String, JsonNode>> = node.fields()
        return fields.next()
    }

    /**
     * Matches a string against a regular expression and returns the match result.
     * @param string - The string to match against the regex.
     * @param regex - The regex pattern as a string.
     * @return The results of the regex match, or null if an error occurs.
     */
    fun matchWithRegex(string: String?, regex: String?): Boolean {
        try {
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(string)
            return matcher.find()
        } catch (e: Exception) {
            // Return null if an error occurs during regex matching
            return false
        }
    }
}
