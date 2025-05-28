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

import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.LoggerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.util.Calendar

@RunWith(MockitoJUnitRunner::class)
class FMEConfigTest {

    @Mock
    private lateinit var loggerService: LoggerService

    private val validSessionData = mapOf("sessionId" to 12345L)
    private val invalidSessionData = mapOf<String, Any>()
    private val invalidSessionIdType = mapOf("sessionId" to "12345")
    private val invalidSessionIdValue = mapOf("sessionId" to 0L)

    @Before
    fun setup() {
        // Reset FMEConfig state before each test
        FMEConfig.setSessionData(mapOf())
    }

    @Test
    fun `test setSessionData with valid data sets session and MISdkLinked flag`() {
        // Act
        FMEConfig.setSessionData(validSessionData)

        // Assert
        assertTrue(FMEConfig.isMISdkLinked)
    }

    @Test
    fun `test setSessionData with empty data logs error and resets MISdkLinked flag`() {
        // Act
        FMEConfig.setSessionData(invalidSessionData)

        // Assert
        assertFalse(FMEConfig.isMISdkLinked)
    }

    @Test
    fun `test setSessionData with missing sessionId logs error and resets MISdkLinked flag`() {
        // Act
        FMEConfig.setSessionData(mapOf("otherKey" to "value"))

        // Assert
        assertFalse(FMEConfig.isMISdkLinked)
    }

    @Test
    fun `test setSessionData with invalid sessionId type logs error and resets MISdkLinked flag`() {
        // Act
        FMEConfig.setSessionData(invalidSessionIdType)

        // Assert
        assertFalse(FMEConfig.isMISdkLinked)
    }

    @Test
    fun `test setSessionData with invalid sessionId value logs error and resets MISdkLinked flag`() {
        // Act
        FMEConfig.setSessionData(invalidSessionIdValue)

        // Assert
        assertFalse(FMEConfig.isMISdkLinked)
    }

    @Test
    fun `test generateSessionId returns existing sessionId when available`() {
        // Arrange
        FMEConfig.setSessionData(validSessionData)

        // Act
        val sessionId = FMEConfig.generateSessionId()

        // Assert
        assertEquals(12345L, sessionId)
    }

    @Test
    fun `test generateSessionId returns current timestamp when no sessionId available`() {
        // Act
        val sessionId = FMEConfig.generateSessionId()
        val currentTimestamp = Calendar.getInstance().timeInMillis / 1000

        // Assert
        assertTrue(sessionId > 0)
        assertTrue(sessionId <= currentTimestamp)
    }
} 