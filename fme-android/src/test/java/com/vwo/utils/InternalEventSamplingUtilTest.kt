/**
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
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
package com.vwo.utils

import com.google.gson.Gson
import com.wingify.models.ErrorLogSamplingCategory
import com.wingify.models.ErrorLogSamplingKeys
import com.wingify.models.Settings
import com.wingify.utils.InternalEventSamplingUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InternalEventSamplingUtilTest {

    private val gson = Gson()

    private fun makeSettings(samplingJson: String = "", alwaysApplySamplingJson: String = ""): Settings {
        val json = """
        {
            "accountId": 1,
            "version": 1,
            "features": [],
            "campaigns": []
            ${if (samplingJson.isEmpty()) "" else ",$samplingJson"}
            ${if (alwaysApplySamplingJson.isEmpty()) "" else ",$alwaysApplySamplingJson"}
        }
        """.trimIndent()
        return gson.fromJson(json, Settings::class.java)
    }

    @Test
    fun usageStatsSamplingRateDefaultsTo1WhenMissing() {
        val settings = makeSettings()
        assertEquals(1.0, InternalEventSamplingUtil.usageStatsSamplingRate(settings), 0.0)
        assertEquals(1.0, InternalEventSamplingUtil.usageStatsSamplingRate(null), 0.0)
    }

    @Test
    fun usageStatsSamplingRateReadsClientUsageValue() {
        val settings = makeSettings(
            samplingJson = """
            "sampling": {
                "usage": { "server": 10, "client": 20, "serverless": 30 },
                "debug": { "server": 40, "client": 50, "serverless": 60 }
            }
            """.trimIndent()
        )

        assertEquals(20.0, InternalEventSamplingUtil.usageStatsSamplingRate(settings), 0.0)
    }

    @Test
    fun qualifiesForSamplingWhenRandomValueIsWithinRate() {
        assertTrue(InternalEventSamplingUtil.doesQualifyForSampling(20.0, 20))
        assertTrue(InternalEventSamplingUtil.doesQualifyForSampling(20.0, 0))
        assertFalse(InternalEventSamplingUtil.doesQualifyForSampling(20.0, 21))
    }

    @Test
    fun qualifiesForSamplingWhenRateIs100() {
        assertTrue(InternalEventSamplingUtil.doesQualifyForSampling(100.0, 100))
    }

    @Test
    fun qualifiesForSamplingWhenRateIsZero() {
        assertFalse(InternalEventSamplingUtil.doesQualifyForSampling(0.0, 0))
    }

    @Test
    fun shouldAlwaysApplySamplingDefaultsToFalseWhenMissing() {
        val settings = makeSettings()
        assertFalse(InternalEventSamplingUtil.shouldAlwaysApplySampling(settings))
        assertFalse(InternalEventSamplingUtil.shouldAlwaysApplySampling(null))
    }

    @Test
    fun shouldAlwaysApplySamplingReadsClientFlag() {
        val enabled = makeSettings(
            alwaysApplySamplingJson = """
            "alwaysApplySampling": { "server": false, "client": true, "serverless": false }
            """.trimIndent()
        )
        val disabled = makeSettings(
            alwaysApplySamplingJson = """
            "alwaysApplySampling": { "server": true, "client": false, "serverless": true }
            """.trimIndent()
        )

        assertTrue(InternalEventSamplingUtil.shouldAlwaysApplySampling(enabled))
        assertFalse(InternalEventSamplingUtil.shouldAlwaysApplySampling(disabled))
    }

    @Test
    fun shouldSendUsageStatsEventUsesClientUsageSamplingWhenAlwaysApplyIsTrue() {
        val settings = makeSettings(
            samplingJson = """
            "sampling": {
                "usage": { "server": 100, "client": 25, "serverless": 100 }
            }
            """.trimIndent(),
            alwaysApplySamplingJson = """
            "alwaysApplySampling": { "server": true, "client": true, "serverless": true }
            """.trimIndent()
        )

        assertTrue(InternalEventSamplingUtil.isUsageStatsEventQualified(settings, 25))
        assertFalse(InternalEventSamplingUtil.isUsageStatsEventQualified(settings, 26))
    }

    @Test
    fun shouldAlwaysSendUsageStatsEventWhenAlwaysApplyIsFalse() {
        val settings = makeSettings(
            samplingJson = """
            "sampling": {
                "usage": { "server": 100, "client": 1, "serverless": 100 }
            }
            """.trimIndent(),
            alwaysApplySamplingJson = """
            "alwaysApplySampling": { "server": true, "client": false, "serverless": true }
            """.trimIndent()
        )

        assertTrue(InternalEventSamplingUtil.isUsageStatsEventQualified(settings, 100))
    }

    @Test
    fun shouldAlwaysSendUsageStatsEventWhenAlwaysApplyIsMissing() {
        val settings = makeSettings(
            samplingJson = """
            "sampling": {
                "usage": { "server": 100, "client": 1, "serverless": 100 }
            }
            """.trimIndent()
        )

        assertTrue(InternalEventSamplingUtil.isUsageStatsEventQualified(settings, 100))
        assertTrue(InternalEventSamplingUtil.isUsageStatsEventQualified(null, 100))
    }

    @Test
    fun debugSamplingRateDefaultsTo1WhenMissing() {
        val settings = makeSettings()
        assertEquals(1.0, InternalEventSamplingUtil.debugSamplingRate(settings), 0.0)
        assertEquals(1.0, InternalEventSamplingUtil.debugSamplingRate(null), 0.0)
    }

    @Test
    fun debugSamplingRateReadsClientDebugValue() {
        val settings = makeSettings(
            samplingJson = """
            "sampling": {
                "debug": { "server": 10, "client": 20, "serverless": 30 }
            }
            """.trimIndent()
        )

        assertEquals(20.0, InternalEventSamplingUtil.debugSamplingRate(settings), 0.0)
    }

    @Test
    fun shouldSendSampledDebugEventUsesClientDebugSamplingWhenAlwaysApplyIsTrue() {
        val settings = makeSettings(
            samplingJson = """
            "sampling": {
                "debug": { "server": 100, "client": 25, "serverless": 100 }
            }
            """.trimIndent(),
            alwaysApplySamplingJson = """
            "alwaysApplySampling": { "server": true, "client": true, "serverless": true }
            """.trimIndent()
        )

        assertTrue(InternalEventSamplingUtil.isSampledDebugEventQualified(settings, 25))
        assertFalse(InternalEventSamplingUtil.isSampledDebugEventQualified(settings, 26))
    }

    @Test
    fun shouldAlwaysSendSampledDebugEventWhenAlwaysApplyIsFalse() {
        val settings = makeSettings(
            samplingJson = """
            "sampling": {
                "debug": { "server": 100, "client": 1, "serverless": 100 }
            }
            """.trimIndent(),
            alwaysApplySamplingJson = """
            "alwaysApplySampling": { "server": true, "client": false, "serverless": true }
            """.trimIndent()
        )

        assertTrue(InternalEventSamplingUtil.isSampledDebugEventQualified(settings, 100))
    }

    @Test
    fun shouldAlwaysSendSampledDebugEventWhenAlwaysApplyIsMissing() {
        val settings = makeSettings(
            samplingJson = """
            "sampling": {
                "debug": { "server": 100, "client": 1, "serverless": 100 }
            }
            """.trimIndent()
        )

        assertTrue(InternalEventSamplingUtil.isSampledDebugEventQualified(settings, 100))
        assertTrue(InternalEventSamplingUtil.isSampledDebugEventQualified(null, 100))
    }

    @Test
    fun errorLogSamplingKeysCategorizesKeys() {
        assertEquals(
            ErrorLogSamplingCategory.ALWAYS_SEND,
            ErrorLogSamplingKeys.category("INVALID_CREDENTIALS")
        )
        assertEquals(
            ErrorLogSamplingCategory.ALWAYS_SEND,
            ErrorLogSamplingKeys.category("NETWORK_CALL_FAILED")
        )
        assertEquals(
            ErrorLogSamplingCategory.SAMPLED,
            ErrorLogSamplingKeys.category("FEATURE_NOT_FOUND")
        )
        assertEquals(
            ErrorLogSamplingCategory.SAMPLED,
            ErrorLogSamplingKeys.category("EVENT_NOT_FOUND")
        )
        assertEquals(
            ErrorLogSamplingCategory.SAMPLED,
            ErrorLogSamplingKeys.category("FEATURE_NOT_FOUND_WITH_ID")
        )
        assertEquals(
            ErrorLogSamplingCategory.ALWAYS_SEND,
            ErrorLogSamplingKeys.category("FLAG_DECISION_GIVEN")
        )
        assertNull(ErrorLogSamplingKeys.category(null))

        assertFalse(ErrorLogSamplingKeys.isSampled("INVALID_CREDENTIALS"))
        assertFalse(ErrorLogSamplingKeys.isSampled("NETWORK_CALL_FAILED"))
        assertTrue(ErrorLogSamplingKeys.isSampled("FEATURE_NOT_FOUND"))
        assertTrue(ErrorLogSamplingKeys.isSampled("EVENT_NOT_FOUND"))
        assertTrue(ErrorLogSamplingKeys.isSampled("FEATURE_NOT_FOUND_WITH_ID"))
        assertFalse(ErrorLogSamplingKeys.isSampled("FLAG_DECISION_GIVEN"))
        assertFalse(ErrorLogSamplingKeys.isSampled(null))
    }

    @Test
    fun settingsDecodeSamplingConfiguration() {
        val json = """
        {
            "accountId": 1,
            "version": 1,
            "features": [],
            "campaigns": [],
            "sampling": {
                "usage": { "server": 90, "client": 1, "serverless": 1 },
                "debug": { "server": 50, "client": 1, "serverless": 1 }
            },
            "alwaysApplySampling": {
                "server": true,
                "client": false,
                "serverless": true
            }
        }
        """.trimIndent()

        val settings = gson.fromJson(json, Settings::class.java)

        assertEquals(1.0, settings.sampling?.usage?.client ?: -1.0, 0.0)
        assertEquals(1.0, settings.sampling?.debug?.client ?: -1.0, 0.0)
        assertEquals(90.0, settings.sampling?.usage?.server ?: -1.0, 0.0)
        assertEquals(50.0, settings.sampling?.debug?.server ?: -1.0, 0.0)
        assertEquals(false, settings.alwaysApplySampling?.client)
        assertEquals(true, settings.alwaysApplySampling?.server)
        assertEquals(1.0, InternalEventSamplingUtil.usageStatsSamplingRate(settings), 0.0)
        assertEquals(1.0, InternalEventSamplingUtil.debugSamplingRate(settings), 0.0)
        assertFalse(InternalEventSamplingUtil.shouldAlwaysApplySampling(settings))
        assertTrue(InternalEventSamplingUtil.isUsageStatsEventQualified(settings, 100))
        assertTrue(InternalEventSamplingUtil.isSampledDebugEventQualified(settings, 100))
    }
}
