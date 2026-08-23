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

import com.wingify.utils.toMap
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner

import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class JSONObjectExtensionTest {

    @Test
    fun `toMap should convert a simple JSONObject to a MutableMap`() {
        val jsonObject = JSONObject().apply {
            put("key1", "value1")
            put("key2", 123)
            put("key3", true)
        }

        val expectedMap = mutableMapOf<String, Any>(
            "key1" to "value1",
            "key2" to 123,
            "key3" to true
        )

        val resultMap = jsonObject.toMap()

        assertEquals(expectedMap.size, resultMap.size)
        assertTrue(resultMap.entries.containsAll(expectedMap.entries))
        assertTrue(expectedMap.entries.containsAll(resultMap.entries))
    }

    @Test
    fun `toMap should handle an empty JSONObject`() {
        val jsonObject = JSONObject()
        val expectedMap = mutableMapOf<String, Any>()

        val resultMap = jsonObject.toMap()

        assertTrue(resultMap.isEmpty())
        assertEquals(expectedMap, resultMap)
    }

    @Test
    fun `toMap should handle JSONObject with null values`() {
        val jsonObject = JSONObject().apply {
            put("key1", JSONObject.NULL)
            put("key2", "value2")
        }

        val expectedMap = mutableMapOf<String, Any>(
            "key1" to JSONObject.NULL,
            "key2" to "value2"
        )

        val resultMap = jsonObject.toMap()

        assertEquals(expectedMap.size, resultMap.size)
        assertTrue(resultMap.entries.containsAll(expectedMap.entries))
        assertTrue(expectedMap.entries.containsAll(resultMap.entries))
    }

    @Test
    fun `toMap should unwrap nested JSONArray and JSONObject values`() {
        val jsonObject = JSONObject(
            """
            {
              "rolloutKey": "r1",
              "holdoutIds": [],
              "notInHoldoutIds": [100, 200],
              "nested": {"a": 1}
            }
            """.trimIndent()
        )

        val resultMap = jsonObject.toMap()

        assertEquals("r1", resultMap["rolloutKey"])
        assertEquals(emptyList<Any>(), resultMap["holdoutIds"])
        assertEquals(listOf(100, 200), resultMap["notInHoldoutIds"])
        assertEquals(mapOf("a" to 1), resultMap["nested"])
        assertTrue(resultMap["notInHoldoutIds"] is List<*>)
        assertTrue(resultMap["nested"] is Map<*, *>)
    }

    @Test
    fun `toMap should unwrap all three holdout cache formats for GetFlagAPI path`() {
        // Format 1: raw arrays
        val raw = JSONObject(
            """{"holdoutIds":[],"notInHoldoutIds":[100,200]}"""
        ).toMap()
        assertEquals(emptyList<Any>(), raw["holdoutIds"])
        assertEquals(listOf(100, 200), raw["notInHoldoutIds"])
        assertTrue(raw["notInHoldoutIds"] is List<*>)

        // Format 2: {"values":[...]}
        val valuesWrapped = JSONObject(
            """{"holdoutIds":{"values":[11]},"notInHoldoutIds":{"values":[22,33]}}"""
        ).toMap()
        assertEquals(mapOf("values" to listOf(11)), valuesWrapped["holdoutIds"])
        assertEquals(mapOf("values" to listOf(22, 33)), valuesWrapped["notInHoldoutIds"])
        assertTrue(valuesWrapped["holdoutIds"] is Map<*, *>)

        // Format 3: {"myArrayList":[...]}
        val myArrayListWrapped = JSONObject(
            """{"holdoutIds":{"myArrayList":[]},"notInHoldoutIds":{"myArrayList":[100,200]}}"""
        ).toMap()
        assertEquals(mapOf("myArrayList" to emptyList<Any>()), myArrayListWrapped["holdoutIds"])
        assertEquals(mapOf("myArrayList" to listOf(100, 200)), myArrayListWrapped["notInHoldoutIds"])
        assertTrue(myArrayListWrapped["notInHoldoutIds"] is Map<*, *>)
    }
}