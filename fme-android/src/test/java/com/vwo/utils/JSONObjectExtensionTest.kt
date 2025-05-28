/*
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
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
}