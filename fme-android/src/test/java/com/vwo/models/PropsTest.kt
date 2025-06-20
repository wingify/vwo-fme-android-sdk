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
package com.vwo.models

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vwo.models.request.Props
import com.vwo.models.request.PropsSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PropsTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Props::class.java, PropsSerializer())
        .create()

    @Test
    fun testPropsSerialization() {
        val props = Props().apply {
            setSdkName("android")
            setSdkVersion("1.0.0")
            setEnvKey("test_env_key")
            variation = "control"
            id = 123
            setFirst(1)
            setIsMII(true)
            setCustomEvent(false)
            setProduct("sdk")
            setData(mapOf("custom_data_key" to "custom_data_value"))
            setVwoMeta(mapOf("stats_key" to 456))
            setAdditionalProperties(mapOf("extra_key" to "extra_value"))
        }

        val json = gson.toJson(props)

        assertNotNull(json)
        assertTrue(json.contains("\"vwo_sdkName\":\"android\""))
        assertTrue(json.contains("\"vwo_sdkVersion\":\"1.0.0\""))
        assertTrue(json.contains("\"vwo_envKey\":\"test_env_key\""))
        assertTrue(json.contains("\"variation\":\"control\""))
        assertTrue(json.contains("\"id\":123"))
        assertTrue(json.contains("\"isFirst\":1"))
        assertTrue(json.contains("\"isMII\":true"))
        assertTrue(json.contains("\"isCustomEvent\":false"))
        assertTrue(json.contains("\"product\":\"sdk\""))
        assertTrue(json.contains("\"data\":{\"custom_data_key\":\"custom_data_value\"}"))
        assertTrue(json.contains("\"vwoMeta\":{\"stats_key\":456}"))
        assertTrue(json.contains("\"extra_key\":\"extra_value\"")) // Test additional property
        assertFalse(json.contains("additionalProperties")) // Test @JsonIgnore
    }

    @Test
    fun testPropsDeserialization() {
        val json = """
            {
              "vwo_sdkName": "android",
              "vwo_sdkVersion": "1.0.0",
              "vwo_envKey": "test_env_key",
              "variation": "control",
              "id": 123,
              "isFirst": 1,
              "isMII": true,
              "isCustomEvent": false,
              "product": "sdk",
              "data": {
                "custom_data_key": "custom_data_value"
              },
              "vwoMeta": {
                "stats_key": 456
              }
            }
        """.trimIndent()

        val props: Props = gson.fromJson(json, Props::class.java)

        assertNotNull(props)
        // Note: We can't directly access private properties like vwo_sdkName
        // So we'll test the values that are set via setters or are public
        assertEquals("control", props.variation)
        assertEquals(123, props.id)
        // We can't directly test isFirst, isMII, isCustomEvent, product, data, vwoMeta
        // as they are private and don't have getters.
        // If you need to test these, you would need to add public getters in your Props class.

        // Test additional property
        val additionalProperties = props.getAdditionalProperties()
        assertNotNull(additionalProperties)
    }

    @Test
    fun testPropsWithNullValuesSerialization() {
        val props = Props().apply {
            // Leave some properties as null
            setSdkName(null)
            setSdkVersion("1.0.0")
            // variation is null by default
            id = null
            setFirst(null)
            setIsMII(false) // Set to false
            setCustomEvent(null)
            setProduct(null)
            setData(null)
            setVwoMeta(emptyMap()) // Set to empty map
            setAdditionalProperties(emptyMap()) // Set to empty map
        }

        val json = gson.toJson(props)

        assertNotNull(json)
        assertFalse(json.contains("vwo_sdkName")) // Should be excluded because it's null
        assertTrue(json.contains("\"vwo_sdkVersion\":\"1.0.0\""))
        assertFalse(json.contains("variation")) // Should be excluded because it's null
        assertFalse(json.contains("id")) // Should be excluded because it's null
        assertFalse(json.contains("isFirst")) // Should be excluded because it's null
        assertTrue(json.contains("\"isMII\":false")) // Should be included even if false
        assertFalse(json.contains("isCustomEvent")) // Should be excluded because it's null
        assertFalse(json.contains("product")) // Should be excluded because it's null
        assertFalse(json.contains("data")) // Should be excluded because it's null
        assertTrue(json.contains("\"vwoMeta\":{}")) // Should be included even if empty
        assertFalse(json.contains("extra_key")) // Should be excluded because additionalProperties is empty
    }

    @Test
    fun testPropsDeserializationWithMissingFields() {
        val json = """
            {
              "vwo_sdkName": "android",
              "variation": "control"
            }
        """.trimIndent()

        val props: Props = gson.fromJson(json, Props::class.java)

        assertNotNull(props)
        // Note: We can't directly access private properties like vwo_sdkName
        // So we'll test the values that are set via setters or are public
        assertEquals("control", props.variation)
        assertNull(props.id) // Should be null if missing in JSON
        // We can't directly test other private properties
    }
}