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
package com.vwo.packages.logger.core

import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.enums.LogLevelEnum
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull

@RunWith(MockitoJUnitRunner::class)
class LogTransportManagerTest {

    private lateinit var logTransportManager: LogTransportManager
    private val config = mapOf<String, Any>("level" to "INFO")

    @Mock
    private lateinit var mockTransport1: LogTransport

    @Mock
    private lateinit var mockTransport2: LogTransport

    @Before
    fun setup() {
        logTransportManager = LogTransportManager(config)
        val loggerConfig = mutableMapOf<String, Any>().apply {
            put("level", "TRACE")
        }
        LogManager(loggerConfig)
    }

    @Test
    fun `test addTransport adds transport correctly`() {
        // Act
        logTransportManager.addTransport(mockTransport1)

        // Assert
        assertEquals(1, logTransportManager.transports.size)
        assertTrue(logTransportManager.transports.contains(mockTransport1))
    }

    @Test
    fun `test addTransport ignores null transport`() {
        // Act
        logTransportManager.addTransport(null)

        // Assert
        assertTrue(logTransportManager.transports.isEmpty())
    }

    @Test
    fun `test shouldLog returns true when transport level is higher than config level`() {
        // Act & Assert
        assertTrue(logTransportManager.shouldLog("ERROR", "INFO"))
        assertTrue(logTransportManager.shouldLog("WARN", "INFO"))
        assertTrue(logTransportManager.shouldLog("INFO", "INFO"))
    }

    @Test
    fun `test shouldLog returns false when transport level is lower than config level`() {
        // Act & Assert
        assertFalse(logTransportManager.shouldLog("DEBUG", "INFO"))
        assertFalse(logTransportManager.shouldLog("TRACE", "INFO"))
    }

    @Test
    fun `test trace logs message to all transports when level allows`() {
        // Arrange
        logTransportManager.addTransport(mockTransport1)
        logTransportManager.addTransport(mockTransport2)
        val message = "Test trace message"

        // Act
        logTransportManager.trace(message)

        // Assert
        verify(mockTransport1, atMostOnce()).log(LogLevelEnum.TRACE, message)
        verify(mockTransport2, atMostOnce()).log(LogLevelEnum.TRACE, message)
    }

    @Test
    fun `test debug logs message to all transports when level allows`() {
        // Arrange
        logTransportManager.addTransport(mockTransport1)
        logTransportManager.addTransport(mockTransport2)
        val message = "Test debug message"

        // Act
        logTransportManager.debug(message)

        // Assert
        verify(mockTransport1, atMostOnce()).log(LogLevelEnum.DEBUG, message)
        verify(mockTransport2, atMostOnce()).log(LogLevelEnum.DEBUG, message)
    }

    @Test
    fun `test info logs message to all transports when level allows`() {
        // Arrange
        logTransportManager.addTransport(mockTransport1)
        logTransportManager.addTransport(mockTransport2)
        val message = "Test info message"

        // Act
        logTransportManager.info(message)

        // Assert
        verify(mockTransport1).log(LogLevelEnum.INFO, message)
        verify(mockTransport2).log(LogLevelEnum.INFO, message)
    }

    @Test
    fun `test warn logs message to all transports when level allows`() {
        // Arrange
        logTransportManager.addTransport(mockTransport1)
        logTransportManager.addTransport(mockTransport2)
        val message = "Test warn message"

        // Act
        logTransportManager.warn(message)

        // Assert
        verify(mockTransport1).log(LogLevelEnum.WARN, message)
        verify(mockTransport2).log(LogLevelEnum.WARN, message)
    }

    @Test
    fun `test error logs message to all transports when level allows`() {
        // Arrange
        logTransportManager.addTransport(mockTransport1)
        logTransportManager.addTransport(mockTransport2)
        val message = "Test error message"

        // Act
        logTransportManager.error(message)

        // Assert
        verify(mockTransport1).log(LogLevelEnum.ERROR, message)
        verify(mockTransport2).log(LogLevelEnum.ERROR, message)
    }

    @Test
    fun `test log with null message to transports`() {
        // Arrange
        logTransportManager.addTransport(mockTransport1)
        logTransportManager.addTransport(mockTransport2)

        // Act
        logTransportManager.log(LogLevelEnum.INFO, null)

        // Assert
        verify(mockTransport1, atMostOnce()).log(org.mockito.kotlin.any(), anyOrNull())
        verify(mockTransport2, atMostOnce()).log(org.mockito.kotlin.any(), anyOrNull())
    }

    @Test
    fun `test log with multiple transports and different levels`() {
        // Arrange
        val debugConfig = mapOf<String, Any>("level" to "DEBUG")
        val debugManager = LogTransportManager(debugConfig)
        debugManager.addTransport(mockTransport1)
        debugManager.addTransport(mockTransport2)

        // Act
        debugManager.debug("Debug message")
        debugManager.info("Info message")
        debugManager.warn("Warn message")
        debugManager.error("Error message")

        // Assert
        verify(mockTransport1).log(LogLevelEnum.DEBUG, "Debug message")
        verify(mockTransport1).log(LogLevelEnum.INFO, "Info message")
        verify(mockTransport1).log(LogLevelEnum.WARN, "Warn message")
        verify(mockTransport1).log(LogLevelEnum.ERROR, "Error message")
        verify(mockTransport2).log(LogLevelEnum.DEBUG, "Debug message")
        verify(mockTransport2).log(LogLevelEnum.INFO, "Info message")
        verify(mockTransport2).log(LogLevelEnum.WARN, "Warn message")
        verify(mockTransport2).log(LogLevelEnum.ERROR, "Error message")
    }
} 