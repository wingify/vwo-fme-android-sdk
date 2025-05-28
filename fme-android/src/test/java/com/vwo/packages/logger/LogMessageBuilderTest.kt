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

package com.vwo.packages.logger

import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.enums.LogLevelEnum
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogMessageBuilderTest {

    @Mock
    private lateinit var mockTransport: LogTransport

    private lateinit var defaultConfig: Map<String, Any>
    private lateinit var customConfig: Map<String, Any>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        defaultConfig = emptyMap()
        customConfig = mapOf(
            "prefix" to "CUSTOM-PREFIX",
            "dateTimeFormat" to "yyyy-MM-dd HH:mm:ss"
        )
    }

    @Test
    fun `formatMessage with default config should return properly formatted message`() {
        // Arrange
        val builder = LogMessageBuilder(defaultConfig, mockTransport)
        val testMessage = "Test log message"
        val testLevel = LogLevelEnum.INFO

        // Act
        val result = builder.formatMessage(testLevel, testMessage)

        // Assert
        val expectedPrefix = "${AnsiColorEnum.BOLD}${AnsiColorEnum.GREEN}VWO-SDK${AnsiColorEnum.RESET}"
        val expectedLevel = "${AnsiColorEnum.BOLD}${AnsiColorEnum.CYAN}INFO${AnsiColorEnum.RESET}"
        val datePattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{4}"
        //val expectedRegex = "\\[$expectedLevel]: $expectedPrefix $datePattern $testMessage"
        val expectedRegex = "\\[${Regex.escape(expectedLevel)}]: ${Regex.escape(expectedPrefix)} $datePattern ${Regex.escape(testMessage)}"
        assert(result.matches(expectedRegex.toRegex()))
    }

    @Test
    fun `formatMessage with custom config should use custom prefix and date format`() {
        // Arrange
        val builder = LogMessageBuilder(customConfig, mockTransport)
        val testMessage = "Custom config test"
        val testLevel = LogLevelEnum.DEBUG

        // Act
        val result = builder.formatMessage(testLevel, testMessage)

        // Assert
        val expectedPrefix = "${AnsiColorEnum.BOLD}${AnsiColorEnum.GREEN}CUSTOM-PREFIX${AnsiColorEnum.RESET}"
        val expectedLevel = "${AnsiColorEnum.BOLD}${AnsiColorEnum.LIGHTBLUE}DEBUG${AnsiColorEnum.RESET}"
        val datePattern = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"
        //val expectedRegex = "\\[$expectedLevel]: $expectedPrefix $datePattern $testMessage"
        val expectedRegex = "\\[${Regex.escape(expectedLevel)}]: ${Regex.escape(expectedPrefix)} $datePattern ${Regex.escape(testMessage)}"
        assert(result.matches(expectedRegex.toRegex()))
    }

    @Test
    fun `formatMessage with null level should throw exception`() {
        // Arrange
        val builder = LogMessageBuilder(defaultConfig, mockTransport)

        // Act & Assert
        assertThrows(NullPointerException::class.java) {
            builder.formatMessage(null, "Should fail")
        }
    }

    @Test
    fun `formatMessage with null message should include null in output`() {
        // Arrange
        val builder = LogMessageBuilder(defaultConfig, mockTransport)
        val testLevel = LogLevelEnum.WARN

        // Act
        val result = builder.formatMessage(testLevel, null)

        // Assert
        assert(result.contains("null"))
    }

    @Test
    fun `getFormattedLevel should return correct ANSI colors for each level`() {
        // Arrange
        val builder = LogMessageBuilder(defaultConfig, mockTransport)

        // Test each log level
        val testCases = mapOf(
            LogLevelEnum.TRACE to AnsiColorEnum.WHITE,
            LogLevelEnum.DEBUG to AnsiColorEnum.LIGHTBLUE,
            LogLevelEnum.INFO to AnsiColorEnum.CYAN,
            LogLevelEnum.WARN to AnsiColorEnum.YELLOW,
            LogLevelEnum.ERROR to AnsiColorEnum.RED
        )

        testCases.forEach { (level, expectedColor) ->
            // Act
            val result = builder.formatMessage(level, "color test")

            // Assert
            val expectedColorCode = "${AnsiColorEnum.BOLD}$expectedColor${level.name}${AnsiColorEnum.RESET}"
            assert(result.contains(expectedColorCode))
        }
    }

    @Test
    fun `formattedDateTime should use configured date format`() {
        // Arrange
        val customFormat = "MM/dd/yyyy HH:mm"
        val builder = LogMessageBuilder(mapOf("dateTimeFormat" to customFormat), mockTransport)

        // Act
        val result = builder.formatMessage(LogLevelEnum.INFO, "date test")

        // Assert
        val datePattern = "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}"
        assert(result.matches(".*$datePattern.*".toRegex()))
    }

    @Test
    fun `getFormattedPrefix should use configured prefix`() {
        // Arrange
        val customPrefix = "TEST-PREFIX"
        val builder = LogMessageBuilder(mapOf("prefix" to customPrefix), mockTransport)

        // Act
        val result = builder.formatMessage(LogLevelEnum.INFO, "prefix test")

        // Assert
        val expectedPrefix = "${AnsiColorEnum.BOLD}${AnsiColorEnum.GREEN}$customPrefix${AnsiColorEnum.RESET}"
        assert(result.contains(expectedPrefix))
    }
}