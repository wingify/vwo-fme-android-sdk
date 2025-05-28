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

package com.vwo.packages.logger.transports

import com.vwo.packages.logger.enums.LogLevelEnum
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@RunWith(MockitoJUnitRunner::class)
class ConsoleTransportTest {

    private lateinit var consoleTransport: ConsoleTransport
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    @Before
    fun setup() {
        // Redirect System.out to capture console output
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @After
    fun tearDown() {
        // Restore System.out
        System.setOut(standardOut)
    }

    @Test
    fun `test trace logs when level is TRACE`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.TRACE)
        val message = "This is a trace message"
        consoleTransport.trace(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test trace logs when level is DEBUG`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.DEBUG)
        val message = "This is a trace message"
        consoleTransport.trace(message)
        assertEquals("", outputStreamCaptor.toString()) // Should not log trace if level is DEBUG
    }

    @Test
    fun `test debug logs when level is TRACE`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.TRACE)
        val message = "This is a debug message"
        consoleTransport.debug(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test debug logs when level is DEBUG`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.DEBUG)
        val message = "This is a debug message"
        consoleTransport.debug(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test debug logs when level is INFO`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.INFO)
        val message = "This is a debug message"
        consoleTransport.debug(message)
        assertEquals("", outputStreamCaptor.toString()) // Should not log debug if level is INFO
    }

    @Test
    fun `test info logs when level is TRACE`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.TRACE)
        val message = "This is an info message"
        consoleTransport.info(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test info logs when level is DEBUG`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.DEBUG)
        val message = "This is an info message"
        consoleTransport.info(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test info logs when level is INFO`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.INFO)
        val message = "This is an info message"
        consoleTransport.info(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test info logs when level is WARN`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.WARN)
        val message = "This is an info message"
        consoleTransport.info(message)
        assertEquals("", outputStreamCaptor.toString()) // Should not log info if level is WARN
    }

    @Test
    fun `test warn logs when level is TRACE`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.TRACE)
        val message = "This is a warning message"
        consoleTransport.warn(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test warn logs when level is DEBUG`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.DEBUG)
        val message = "This is a warning message"
        consoleTransport.warn(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test warn logs when level is INFO`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.INFO)
        val message = "This is a warning message"
        consoleTransport.warn(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test warn logs when level is WARN`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.WARN)
        val message = "This is a warning message"
        consoleTransport.warn(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test warn logs when level is ERROR`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.ERROR)
        val message = "This is a warning message"
        consoleTransport.warn(message)
        assertEquals("", outputStreamCaptor.toString()) // Should not log warn if level is ERROR
    }

    @Test
    fun `test error logs when level is TRACE`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.TRACE)
        val message = "This is an error message"
        consoleTransport.error(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test error logs when level is DEBUG`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.DEBUG)
        val message = "This is an error message"
        consoleTransport.error(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test error logs when level is INFO`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.INFO)
        val message = "This is an error message"
        consoleTransport.error(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test error logs when level is WARN`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.WARN)
        val message = "This is an error message"
        consoleTransport.error(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test error logs when level is ERROR`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.ERROR)
        val message = "This is an error message"
        consoleTransport.error(message)
        assertEquals("$message\n", outputStreamCaptor.toString())
    }

    @Test
    fun `test log method with different levels and messages`() {
        consoleTransport = ConsoleTransport(com.vwo.packages.logger.enums.LogLevelEnum.INFO)

        consoleTransport.log(LogLevelEnum.TRACE, "Trace message")
        assertEquals("", outputStreamCaptor.toString()) // TRACE < INFO

        outputStreamCaptor.reset() // Clear the buffer

        consoleTransport.log(LogLevelEnum.DEBUG, "Debug message")
        assertEquals("", outputStreamCaptor.toString()) // DEBUG < INFO

        outputStreamCaptor.reset()

        consoleTransport.log(LogLevelEnum.INFO, "Info message")
        assertEquals("Info message\n", outputStreamCaptor.toString()) // INFO <= INFO

        outputStreamCaptor.reset()

        consoleTransport.log(LogLevelEnum.WARN, "Warn message")
        assertEquals("Warn message\n", outputStreamCaptor.toString()) // WARN <= INFO (Incorrect based on ordinal logic, but testing the code as is)

        outputStreamCaptor.reset()

        consoleTransport.log(LogLevelEnum.ERROR, "Error message")
        assertEquals("Error message\n", outputStreamCaptor.toString()) // ERROR <= INFO (Incorrect based on ordinal logic, but testing the code as is)
    }

    @Test
    fun `test log method with null message`() {
        consoleTransport = ConsoleTransport(LogLevelEnum.TRACE)
        consoleTransport.log(LogLevelEnum.INFO, null)
        assertEquals("null\n", outputStreamCaptor.toString())
    }
}