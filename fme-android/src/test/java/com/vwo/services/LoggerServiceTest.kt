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
package com.vwo.services

import android.util.Log
import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.core.LogManager
import com.vwo.packages.logger.enums.LogLevelEnum
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.After
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class LoggerServiceTest {

    private val originalOut = System.out
    private lateinit var outputStream: ByteArrayOutputStream
    private val loggedMessages = mutableListOf<String>()

    @Before
    fun setUp() {
        outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        // Initialize LogManager with a custom transport that captures messages
        val transports: MutableList<Map<String, Any>> = mutableListOf()
        val transport: MutableMap<String, Any> = mutableMapOf()

        transport["defaultTransport"] = object : LogTransport {
            override fun log(level: LogLevelEnum, message: String?) {
                if (message == null) return
                loggedMessages.add("[${level.name}] $message")
                Log.d("FME", message)
            }
        }

        transports.add(transport)

        val loggerConfig = mutableMapOf<String, Any>().apply {
            put("level", "TRACE")
            put("transports", transports)
        }
        LoggerService(loggerConfig)
    }

    @After
    fun tearDown() {
        System.setOut(originalOut)
        loggedMessages.clear()
    }

    @Test
    fun testLogWithMessage() {
        LoggerService.log(LogLevelEnum.INFO, "Test message")

        assertTrue(loggedMessages.any { it.contains("Test message") })
    }

    @Test
    fun testLogWithEventAndMetadata() {
        val metadata = mapOf<String?, String?>(
            "userId" to "test-user",
            "campaignKey" to "test-campaign",
            "status" to "passed"
        )

        LoggerService.log(LogLevelEnum.INFO, "SEGMENTATION_STATUS", metadata)

        assertTrue(loggedMessages.any { it.contains("Segmentation") })
        assertTrue(loggedMessages.any { it.contains("test-user") })
        assertTrue(loggedMessages.any { it.contains("test-campaign") })
        assertTrue(loggedMessages.any { it.contains("passed") })
    }

    @Test
    fun testLogWithDifferentLevels() {
        LoggerService.log(LogLevelEnum.DEBUG, "Debug message")
        LoggerService.log(LogLevelEnum.INFO, "Info message")
        LoggerService.log(LogLevelEnum.WARN, "Warning message")
        LoggerService.log(LogLevelEnum.ERROR, "Error message")
        LoggerService.log(LogLevelEnum.TRACE, "Trace message")

        assertTrue(loggedMessages.any { it.contains("Debug message") })
        assertTrue(loggedMessages.any { it.contains("Info message") })
        assertTrue(loggedMessages.any { it.contains("Warning message") })
        assertTrue(loggedMessages.any { it.contains("Error message") })
        assertTrue(loggedMessages.any { it.contains("Trace message") })
    }

    @Test
    fun testLogWithTraceLevel() {
        LoggerService.log(LogLevelEnum.TRACE, "Trace message")
        assertTrue(loggedMessages.any { it.contains("[TRACE]") })
    }

    @Test
    fun testLogWithDebugLevel() {
        LoggerService.log(LogLevelEnum.DEBUG, "Debug message")
        assertTrue(loggedMessages.any { it.contains("[DEBUG]") })
    }

    @Test
    fun testLogWithInfoLevel() {
        LoggerService.log(LogLevelEnum.INFO, "Info message")
        assertTrue(loggedMessages.any { it.contains("[INFO]") })
    }

    @Test
    fun testLogWithWarnLevel() {
        LoggerService.log(LogLevelEnum.WARN, "Warning message")
        assertTrue(loggedMessages.any { it.contains("[WARN]") })
    }

    @Test
    fun testLogWithErrorLevel() {
        LoggerService.log(LogLevelEnum.ERROR, "Error message")
        assertTrue(loggedMessages.any { it.contains("[ERROR]") })
    }

    @Test
    fun testLogWithErrorMessage() {
        val errorMessage = "DATABASE_ERROR"
        val errorDetails = mapOf<String?, String?>("err" to "Connection failed")

        LoggerService.log(LogLevelEnum.ERROR, errorMessage, errorDetails)

        assertTrue(loggedMessages.any { it.contains("Error while performing database operation.") })
        assertTrue(loggedMessages.any { it.contains("Connection failed") })
    }

    @Test
    fun testLogWithImpactAnalysis() {
        val metadata = mapOf<String?, String?>(
            "userId" to "test-user",
            "featureKey" to "test-feature",
            "status" to "enabled"
        )

        LoggerService.log(LogLevelEnum.INFO, "IMPACT_ANALYSIS", metadata)

        assertTrue(loggedMessages.any { it.contains("Sending data for Impact Campaign") })
        assertTrue(loggedMessages.any { it.contains("test-user") })
        assertFalse(loggedMessages.any { it.contains("test-feature") })
        assertFalse(loggedMessages.any { it.contains("enabled") })
    }

    @Test
    fun testLogWithBatchProcessing() {
        val metadata = mapOf<String?, String?>(
            "status" to "true",
            "name" to "test-batch",
            "count" to "10"
        )

        LoggerService.log(LogLevelEnum.INFO, "BATCH_PROCESSING_FINISHED", metadata)

        assertTrue(loggedMessages.any { it.contains("Batch: Finished uploading") })
        assertTrue(loggedMessages.any { it.contains("true") })
        assertTrue(loggedMessages.any { it.contains("test-batch") })
        assertTrue(loggedMessages.any { it.contains("10") })
    }
}