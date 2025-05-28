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
class VariableTest {

    private lateinit var variable: Variable

    @Before
    fun setup() {
        variable = Variable()
    }

    @Test
    fun `test default values`() {
        assertNull(variable.key)
        assertNull(variable.type)
        assertNull(variable.value)
    }

    @Test
    fun `test setting and getting name`() {
        val testName = "Test Variable"
        variable.key = testName
        assertEquals(testName, variable.key)
    }

    @Test
    fun `test setting and getting type`() {
        val testType = "string"
        variable.type = testType
        assertEquals(testType, variable.type)
    }

    @Test
    fun `test setting and getting value`() {
        val testValue = "test_value"
        variable.value = testValue
        assertEquals(testValue, variable.value)
    }

    @Test
    fun `test setting and getting all properties`() {
        variable.apply {
            key = "Test Variable"
            type = "string"
            value = "test_value"
        }

        assertEquals("Test Variable", variable.key)
        assertEquals("string", variable.type)
        assertEquals("test_value", variable.value)
    }
} 