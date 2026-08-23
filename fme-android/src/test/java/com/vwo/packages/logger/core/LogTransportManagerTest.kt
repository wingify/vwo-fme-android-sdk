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
package com.vwo.packages.logger.core

import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.ServiceContainer
import com.vwo.models.user.VWOInitOptions
import com.wingify.models.user.WingifyInitOptions
import com.wingify.packages.logger.core.LogManager
import com.wingify.packages.logger.core.LogTransportManager
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@RunWith(MockitoJUnitRunner::class)
class LogTransportManagerTest {

    private lateinit var logTransportManager: LogTransportManager
    private val config = mapOf<String, Any>("level" to "INFO")

    @Mock
    private lateinit var mockTransport1: LogTransport

    @Mock
    private lateinit var mockTransport2: LogTransport

    private fun plainFormattedPattern(
        level: LogLevelEnum,
        message: String,
        prefix: String? = null
    ): String {
        return if (prefix.isNullOrBlank()) {
            """\[${level.name}\]: ${Regex.escape(message)}"""
        } else {
            """\[${level.name}\]: $prefix ${Regex.escape(message)}"""
        }
    }

    private fun verifyPlainFormattedLog(
        transport: LogTransport,
        level: LogLevelEnum,
        message: String,
        prefix: String? = null
    ) {
        verify(transport).log(
            eq(level),
            argThat { actual: String? ->
                actual?.matches(plainFormattedPattern(level, message, prefix).toRegex()) == true
            }
        )
    }

    @Before
    fun setup() {
        val loggerConfig = mutableMapOf<String, Any>().apply {
            put("level", "TRACE")
        }
        val mockServiceContainer = ServiceContainer(
            settingsManager = null,
            options = VWOInitOptions(),
            settings = null,
            loggerService = null
        )
        val logManager = LogManager(loggerConfig, mockServiceContainer)
        logTransportManager = LogTransportManager(config, logManager)
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
        verifyPlainFormattedLog(mockTransport1, LogLevelEnum.TRACE, message)
        verifyPlainFormattedLog(mockTransport2, LogLevelEnum.TRACE, message)
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
        verifyPlainFormattedLog(mockTransport1, LogLevelEnum.DEBUG, message)
        verifyPlainFormattedLog(mockTransport2, LogLevelEnum.DEBUG, message)
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
        verifyPlainFormattedLog(mockTransport1, LogLevelEnum.INFO, message)
        verifyPlainFormattedLog(mockTransport2, LogLevelEnum.INFO, message)
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
        verifyPlainFormattedLog(mockTransport1, LogLevelEnum.WARN, message)
        verifyPlainFormattedLog(mockTransport2, LogLevelEnum.WARN, message)
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
        verifyPlainFormattedLog(mockTransport1, LogLevelEnum.ERROR, message)
        verifyPlainFormattedLog(mockTransport2, LogLevelEnum.ERROR, message)
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
        val mockServiceContainer = ServiceContainer(
            settingsManager = null,
            options = VWOInitOptions(),
            settings = null,
            loggerService = null
        )
        val logManager = LogManager(debugConfig, mockServiceContainer)
        val debugManager = LogTransportManager(debugConfig, logManager)
        debugManager.addTransport(mockTransport1)
        debugManager.addTransport(mockTransport2)

        // Act
        debugManager.debug("Debug message")
        debugManager.info("Info message")
        debugManager.warn("Warn message")
        debugManager.error("Error message")

        // Assert
        verify(mockTransport1).log(eq(LogLevelEnum.DEBUG), argThat { actual -> actual?.contains("Debug message") == true })
        verify(mockTransport1).log(eq(LogLevelEnum.INFO), argThat { actual -> actual?.contains("Info message") == true })
        verify(mockTransport1).log(eq(LogLevelEnum.WARN), argThat { actual -> actual?.contains("Warn message") == true })
        verify(mockTransport1).log(eq(LogLevelEnum.ERROR), argThat { actual -> actual?.contains("Error message") == true })
        verify(mockTransport2).log(eq(LogLevelEnum.DEBUG), argThat { actual -> actual?.contains("Debug message") == true })
        verify(mockTransport2).log(eq(LogLevelEnum.INFO), argThat { actual -> actual?.contains("Info message") == true })
        verify(mockTransport2).log(eq(LogLevelEnum.WARN), argThat { actual -> actual?.contains("Warn message") == true })
        verify(mockTransport2).log(eq(LogLevelEnum.ERROR), argThat { actual -> actual?.contains("Error message") == true })
    }

    @Test
    fun `test log replaces VWO with Wingify when Wingify SDK is active`() {
        val wingifyOptions = WingifyInitOptions()
        val wingifyServiceContainer = ServiceContainer(
            settingsManager = null,
            options = wingifyOptions,
            settings = null,
            loggerService = null
        )
        val wingifyLogManager = LogManager(
            mutableMapOf<String, Any>("level" to "TRACE"),
            wingifyServiceContainer
        )
        val wingifyTransportManager = LogTransportManager(config, wingifyLogManager)
        wingifyTransportManager.addTransport(mockTransport1)

        wingifyTransportManager.error("[ERROR]: VWO-SDK Options should be of type object")

        verify(mockTransport1).log(
            eq(LogLevelEnum.ERROR),
            argThat { actual ->
                actual?.startsWith("[ERROR]:") == true &&
                    actual.contains("Wingify-SDK Options should be of type object")
            }
        )
    }

    @Test
    fun `test log does not replace vwo underscore identifiers when Wingify SDK is active`() {
        val wingifyOptions = WingifyInitOptions()
        val wingifyServiceContainer = ServiceContainer(
            settingsManager = null,
            options = wingifyOptions,
            settings = null,
            loggerService = null
        )
        val wingifyLogManager = LogManager(
            mutableMapOf<String, Any>("level" to "TRACE"),
            wingifyServiceContainer
        )
        val wingifyTransportManager = LogTransportManager(config, wingifyLogManager)
        wingifyTransportManager.addTransport(mockTransport1)

        wingifyTransportManager.error("VWO init failed for _vwo_meta key")

        verifyPlainFormattedLog(
            mockTransport1,
            LogLevelEnum.ERROR,
            "Wingify init failed for _vwo_meta key"
        )
    }

    @Test
    fun `test log applies custom prefix in plain formatted message`() {
        val loggerConfig = mutableMapOf<String, Any>(
            "level" to "TRACE",
            "prefix" to "MyCustomPrefix"
        )
        val mockServiceContainer = ServiceContainer(
            settingsManager = null,
            options = VWOInitOptions(),
            settings = null,
            loggerService = null
        )
        val logManager = LogManager(loggerConfig, mockServiceContainer)
        val transportManager = LogTransportManager(loggerConfig, logManager)
        transportManager.addTransport(mockTransport1)

        transportManager.info("Settings fetched")

        verifyPlainFormattedLog(
            mockTransport1,
            LogLevelEnum.INFO,
            "Settings fetched",
            prefix = "MyCustomPrefix"
        )
    }
} 