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
package com.vwo.models.user

import com.vwo.enums.VariableTypeEnum
import com.vwo.models.Variable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class GetFlagTest {

    private lateinit var getFlag: GetFlag
    private lateinit var userContext: VWOUserContext
    private lateinit var variables: List<Variable>

    @Before
    fun setup() {
        userContext = VWOUserContext().apply {
            id = "test_user"
        }
        getFlag = GetFlag(userContext)
        
        // Setup test variables
        variables = listOf(
            Variable().apply {
                id = 1
                key = "string_var"
                value = "test_value"
                type = "string"
            },
            Variable().apply {
                id = 2
                key = "int_var"
                value = 42
                type = "integer"
            },
            Variable().apply {
                id = 3
                key = "float_var"
                value = 3.14
                type = "double"
            },
            Variable().apply {
                id = 4
                key = "boolean_var"
                value = true
                type = "boolean"
            },
            Variable().apply {
                id = 5
                key = "json_var"
                value = mapOf("key" to "value")
                type = "json"
            },
            Variable().apply {
                id = 6
                key = "recommendation_var"
                value = 123
                type = VariableTypeEnum.RECOMMENDATION.value
                displayConfiguration = mapOf("config" to "value")
            }
        )
    }

    @Test
    fun `test isEnabled returns correct status`() {
        // Arrange
        getFlag.setIsEnabled(true)

        // Act & Assert
        assertTrue(getFlag.isEnabled())

        // Arrange
        getFlag.setIsEnabled(false)

        // Act & Assert
        assertFalse(getFlag.isEnabled())
    }

    @Test
    fun `test setVariables stores variables correctly`() {
        // Act
        getFlag.setVariables(variables)

        // Assert
        assertEquals(variables, getFlag.variablesValue)
    }

    @Test
    fun `test getVariable returns correct value for string variable`() {
        // Arrange
        getFlag.setVariables(variables)

        // Act
        val value = getFlag.getVariable("string_var", "default")

        // Assert
        assertEquals("test_value", value)
    }

    @Test
    fun `test getVariable returns correct value for integer variable`() {
        // Arrange
        getFlag.setVariables(variables)

        // Act
        val value = getFlag.getVariable("int_var", 0)

        // Assert
        assertEquals(42, value)
    }

    @Test
    fun `test getVariable returns correct value for float variable`() {
        // Arrange
        getFlag.setVariables(variables)

        // Act
        val value = getFlag.getVariable("float_var", 0.0)

        // Assert
        assertEquals(3.14, value)
    }

    @Test
    fun `test getVariable returns correct value for boolean variable`() {
        // Arrange
        getFlag.setVariables(variables)

        // Act
        val value = getFlag.getVariable("boolean_var", false)

        // Assert
        assertEquals(true, value)
    }

    @Test
    fun `test getVariable returns correct value for json variable`() {
        // Arrange
        getFlag.setVariables(variables)

        // Act
        val value = getFlag.getVariable("json_var", mapOf<String, Any>())

        // Assert
        assertEquals(mapOf("key" to "value"), value)
    }

    @Test
    fun `test getVariable returns default value when variable not found`() {
        // Arrange
        getFlag.setVariables(variables)

        // Act
        val value = getFlag.getVariable("non_existent", "default")

        // Assert
        assertEquals("default", value)
    }

    @Test
    fun `test getVariables returns correct map format`() {
        // Arrange
        getFlag.setVariables(variables)

        // Act
        val result = getFlag.getVariables()

        // Assert
        assertEquals(5, result.size) // Should exclude recommendation type
        assertTrue(result.any { it["key"] == "string_var" && it["value"] == "test_value" })
        assertTrue(result.any { it["key"] == "int_var" && it["value"] == 42 })
        assertTrue(result.any { it["key"] == "float_var" && it["value"] == 3.14 })
        assertTrue(result.any { it["key"] == "boolean_var" && it["value"] == true })
        assertTrue(result.any { it["key"] == "json_var" && it["value"] == mapOf("key" to "value") })
    }
} 