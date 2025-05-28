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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VariationTest {

    private lateinit var variation: Variation

    @Before
    fun setup() {
        variation = Variation()
    }

    @Test
    fun `test default values`() {
        assertNull(variation.id)
        assertNull(variation.key)
        assertNull(variation.name)
        assertNull(variation.ruleKey)
        assertNull(variation.type)
        assertEquals(0.0, variation.weight, 0.0) // Assert default double with delta
        assertEquals(0, variation.startRangeVariation)
        assertEquals(0, variation.endRangeVariation)
        assertNotNull(variation.variables) // variables is initialized to ArrayList()
        assertTrue(variation.variables.isEmpty()) // variables should be an empty list by default
        assertNotNull(variation.variations) // variations is initialized to ArrayList()
        assertTrue(variation.variations.isEmpty()) // variations should be an empty list by default
        assertNotNull(variation.segments) // segments is initialized to HashMap()
        assertTrue(variation.segments.isEmpty()) // segments should be an empty map by default
        assertNull(variation.salt)
        assertNull(variation.segments_events)
    }

    @Test
    fun `test setting and getting id`() {
        val testId = 123
        variation.id = testId
        assertEquals(testId, variation.id)
    }

    @Test
    fun `test setting and getting key`() {
        val testKey = "test_key"
        variation.key = testKey
        assertEquals(testKey, variation.key)
    }

    @Test
    fun `test setting and getting name`() {
        val testName = "Test Variation"
        variation.name = testName
        assertEquals(testName, variation.name)
    }

    @Test
    fun `test setting and getting ruleKey`() {
        val testRuleKey = "test_rule_key"
        variation.ruleKey = testRuleKey
        assertEquals(testRuleKey, variation.ruleKey)
    }

    @Test
    fun `test setting and getting type`() {
        val testType = "CUSTOM"
        variation.type = testType
        assertEquals(testType, variation.type)
    }

    @Test
    fun `test setting and getting weight`() {
        val testWeight = 0.5
        variation.weight = testWeight
        assertEquals(testWeight, variation.weight, 0.0) // Assert double with delta
    }

    @Test
    fun `test setting and getting startRangeVariation`() {
        val testStartRangeVariation = 10
        variation.startRangeVariation = testStartRangeVariation
        assertEquals(testStartRangeVariation, variation.startRangeVariation)
    }

    @Test
    fun `test setting and getting endRangeVariation`() {
        val testEndRangeVariation = 20
        variation.endRangeVariation = testEndRangeVariation
        assertEquals(testEndRangeVariation, variation.endRangeVariation)
    }

    @Test
    fun `test setting and getting variables`() {
        val testVariables = listOf(
            Variable().apply {
                // Assuming Variable has some properties to set
                // For example: name = "var1", type = "string", value = "test"
            }
        )
        variation.variables = testVariables
        assertEquals(testVariables, variation.variables)
    }

    @Test
    fun `test setting and getting variations`() {
        val testVariations = listOf(
            Variation().apply {
                id = 1
                name = "Nested Variation"
            }
        )
        variation.variations = testVariations
        assertEquals(testVariations, variation.variations)
    }

    @Test
    fun `test setting and getting segments`() {
        val testSegments = mapOf(
            "segment1" to mapOf("key" to "value"),
            "segment2" to listOf(1, 2, 3)
        )
        variation.segments = testSegments
        assertEquals(testSegments, variation.segments)
    }

    @Test
    fun `test setting and getting salt`() {
        val testSalt = "test_salt"
        variation.salt = testSalt
        assertEquals(testSalt, variation.salt)
    }

    @Test
    fun `test setting and getting segments_events`() {
        val testSegmentsEvents = listOf("event1", 123, true)
        variation.segments_events = testSegmentsEvents
        assertEquals(testSegmentsEvents, variation.segments_events)
    }


    @Test
    fun `test setting and getting all properties`() {
        val testVariables = listOf(
            Variable().apply {
                // Assuming Variable has some properties to set
            }
        )
        val testVariations = listOf(
            Variation().apply {
                id = 1
                name = "Nested Variation"
            }
        )
        val testSegments = mapOf(
            "segment1" to mapOf("key" to "value")
        )
        val testSegmentsEvents = listOf("event1")


        variation.apply {
            id = 123
            key = "test_key"
            name = "Test Variation"
            ruleKey = "test_rule_key"
            type = "CUSTOM"
            weight = 0.5
            startRangeVariation = 10
            endRangeVariation = 20
            variables = testVariables
            variations = testVariations
            segments = testSegments
            salt = "test_salt"
            segments_events = testSegmentsEvents
        }

        assertEquals(123, variation.id)
        assertEquals("test_key", variation.key)
        assertEquals("Test Variation", variation.name)
        assertEquals("test_rule_key", variation.ruleKey)
        assertEquals("CUSTOM", variation.type)
        assertEquals(0.5, variation.weight, 0.0)
        assertEquals(10, variation.startRangeVariation)
        assertEquals(20, variation.endRangeVariation)
        assertEquals(testVariables, variation.variables)
        assertEquals(testVariations, variation.variations)
        assertEquals(testSegments, variation.segments)
        assertEquals("test_salt", variation.salt)
        assertEquals(testSegmentsEvents, variation.segments_events)
    }
}