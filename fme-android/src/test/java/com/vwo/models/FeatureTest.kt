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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class FeatureTest {

    private lateinit var feature: Feature

    @Before
    fun setup() {
        feature = Feature()
    }

    @Test
    fun `test default values`() {
        assertNull(feature.name)
        assertNull(feature.key)
        assertNull(feature.type)
        assertNull(feature.status)
        assertNull(feature.variables)
    }

    @Test
    fun `test setting and getting name`() {
        val testName = "Test Feature"
        feature.name = testName
        assertEquals(testName, feature.name)
    }

    @Test
    fun `test setting and getting key`() {
        val testKey = "test_feature_key"
        feature.key = testKey
        assertEquals(testKey, feature.key)
    }

    @Test
    fun `test setting and getting type`() {
        val testType = "FEATURE_ROLLOUT"
        feature.type = testType
        assertEquals(testType, feature.type)
    }

    @Test
    fun `test setting and getting status`() {
        val testStatus = "RUNNING"
        feature.status = testStatus
        assertEquals(testStatus, feature.status)
    }

    @Test
    fun `test setting and getting variables`() {
        val testVariables = listOf(
            Variable().apply {
                key = "var1"
                type = "string"
                value = "test"
            }
        )
        feature.variables = testVariables
        assertEquals(testVariables, feature.variables)
    }

    @Test
    fun `test setting and getting all properties`() {
        val testVariables = listOf(
            Variable().apply {
                key = "var1"
                type = "string"
                value = "test"
            }
        )

        feature.apply {
            name = "Test Feature"
            key = "test_feature_key"
            type = "FEATURE_ROLLOUT"
            status = "RUNNING"
            variables = testVariables
        }

        assertEquals("Test Feature", feature.name)
        assertEquals("test_feature_key", feature.key)
        assertEquals("FEATURE_ROLLOUT", feature.type)
        assertEquals("RUNNING", feature.status)
        assertEquals(testVariables, feature.variables)
    }
} 