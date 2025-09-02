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

import org.junit.Assert
import org.junit.ComparisonFailure

class DummySettingsReader {
    var settingsMap: MutableMap<String, String> = mutableMapOf()

    val fileNames = listOf(
        "BASIC_ROLLOUT_TESTING_RULE_SETTINGS",
        "MEG_CAMPAIGN_ADVANCE_ALGO_SETTINGS",
        "MEG_CAMPAIGN_RANDOM_ALGO_SETTINGS",
        "NO_ROLLOUT_ONLY_TESTING_RULE_SETTINGS",
        "ROLLOUT_TESTING_PRE_SEGMENT_RULE_SETTINGS",
        "TESTING_WHITELISTING_SEGMENT_RULE_SETTINGS",
        "BASIC_ROLLOUT_SETTINGS",
        "SETTINGS_WITH_DIFFERENT_SALT",
        "SETTINGS_WITH_SAME_SALT"
    )

    /**
     * Constructor for DummySettingsReader.
     * Initializes the settingsMap by reading JSON files from a specified directory.
     */
    init {
        for (file in fileNames) {
            val fileName = "$file.json"
            readFile(fileName)?.let { settingsMap.put(file, it) }
        }
    }

    /**
     * Reads the log files and returns the messages in a map.
     */
    private fun readFile(fileName: String): String? {
        try {
            val inputStream = this.javaClass.classLoader?.getResourceAsStream(fileName)
            val contents = inputStream?.bufferedReader().use { it?.readText() }
            return contents
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return null
    }

    fun assertMapsEqual(expected: Map<String, Any>?, actual: Map<String, Any>?) {
        when {
            expected == null && actual == null -> {
                // Both are null, so they are considered equal in this context.
                return
            }
            expected == null -> {
                // Only expected is null, so fail.
                throw ComparisonFailure("Expected map was null, but actual map was not.", "null", actual.toString())
            }
            actual == null -> {
                // Only actual is null, so fail.
                throw ComparisonFailure("Expected map was not null, but actual map was null.", expected.toString(), "null")
            }
        }
        Assert.assertEquals("Maps have different sizes", expected!!.size, actual!!.size)
        for ((key, expectedValue) in expected) {
            Assert.assertTrue("Map 'actual' is missing key '$key'", actual.containsKey(key))
            val actualValue = actual[key]

            when {
                expectedValue is Number && actualValue is Number -> {
                    // Compare their double values for numerical equality
                    // Add a delta for floating point comparisons if needed
                    Assert.assertEquals(
                        "Values for key '$key' are not numerically equal",
                        expectedValue.toDouble(),
                        actualValue.toDouble(),
                        0.00001 // Optional delta for floating point precision
                    )
                }
                else -> {
                    Assert.assertEquals("Values for key '$key' are not equal", expectedValue, actualValue)
                }
            }
        }
    }
}
