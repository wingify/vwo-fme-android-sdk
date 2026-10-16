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
package com.wingify.services

import com.wingify.models.SdkMetaInfo
import com.wingify.models.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PanicManagerCollectTest {

    private lateinit var service: PanicManager

    @Before
    fun setUp() {
        service = PanicManager()
    }

    @Test
    fun appliesDaCdnPanicAndForceRefresh() {
        val result = service.applyResponseControl(
            """{"timestamp":"1777026135","forceRefresh":1776668404,"panicMode":1776669404}"""
        )
        assertTrue(result.isPanicActive)
        assertTrue(result.shouldTriggerForceRefresh)
        assertTrue(service.isPanicActive())
    }

    @Test
    fun clearsPanicOnBatchEchoWithoutControlKeys() {
        service.applyResponseControl("""{"panicMode":1}""")
        assertTrue(service.isPanicActive())
        val result = service.applyResponseControl("""{"ev":[]}""")
        assertFalse(service.isPanicActive())
        assertTrue(result.isRefreshFromPanicClear)
        assertTrue(result.shouldTriggerForceRefresh)
    }

    @Test
    fun clearsPanicOnCollectEchoWithoutControlKeys() {
        service.applyResponseControl("""{"panicMode":111}""")
        val result = service.applyResponseControl("""{"d":{}}""")
        assertFalse(service.isPanicActive())
        assertTrue(result.isRefreshFromPanicClear)
    }

    @Test
    fun clearsPanicWhenPanicModePresentButInvalid() {
        service.applyResponseControl("""{"panicMode":1}""")
        val result = service.applyResponseControl("""{"panicMode":""}""")
        assertFalse(service.isPanicActive())
        assertTrue(result.isRefreshFromPanicClear)
    }

    @Test
    fun emptyBodyClearsPanic() {
        service.applyResponseControl("""{"panicMode":1}""")
        val result = service.applyResponseControl("")
        assertFalse(service.isPanicActive())
        assertTrue(result.isRefreshFromPanicClear)
        assertTrue(result.shouldTriggerForceRefresh)
    }

    @Test
    fun nonObjectJsonClearsPanic() {
        service.applyResponseControl("""{"panicMode":1}""")
        val result = service.applyResponseControl("1")
        assertFalse(service.isPanicActive())
        assertTrue(result.isRefreshFromPanicClear)
    }

    @Test
    fun ignoresStaleDaCdnPanic() {
        service.applyResponseControl("""{"panicMode":2000}""")
        service.applyResponseControl("{}")
        assertFalse(service.isPanicActive())
        service.applyResponseControl("""{"panicMode":1000}""")
        assertFalse(service.isPanicActive())
        service.applyResponseControl("""{"panicMode":1500}""")
        assertFalse(service.isPanicActive())
    }

    @Test
    fun olderDaCdnPanicWhileActiveChangesNothing() {
        service.applyResponseControl("""{"panicMode":2000}""")
        val next = service.applyResponseControl("""{"panicMode":1000}""")
        assertTrue(service.isPanicActive())
        assertFalse(next.shouldTriggerForceRefresh)
        service.applyResponseControl("""{"panicMode":1500}""")
        assertTrue(service.isPanicActive())
    }

    @Test
    fun ignoresStaleForceRefresh() {
        service.applyResponseControl("""{"forceRefresh":1776660000}""")
        assertFalse(
            service.applyResponseControl("""{"forceRefresh":1776660000}""").shouldTriggerForceRefresh
        )
        assertFalse(
            service.applyResponseControl("""{"forceRefresh":1776659999}""").shouldTriggerForceRefresh
        )
    }

    @Test
    fun onPanicActivatedFiresOnceUntilCleared() {
        var count = 0
        service.setOnPanicActivated { count++ }
        service.applyResponseControl("""{"panicMode":1000}""")
        service.applyResponseControl("""{"panicMode":2000}""")
        assertEquals(1, count)
        service.applyResponseControl("{}")
        service.applyResponseControl("""{"panicMode":3000}""")
        assertEquals(2, count)
    }

    @Test
    fun onPanicActivatedFiresFromSettingsApply() {
        var count = 0
        service.setOnPanicActivated { count++ }
        service.applySettingsControl(Settings().apply {
            lastUpdatedOn = 1
            sdkMetaInfo = SdkMetaInfo(panic = 1705924217)
        })
        assertEquals(1, count)
    }

    @Test
    fun staleDaCdnPanicWhileOffDoesNotFireHook() {
        var count = 0
        service.setOnPanicActivated { count++ }
        service.applyResponseControl("""{"panicMode":2000}""")
        service.applyResponseControl("{}")
        count = 0
        service.applyResponseControl("""{"panicMode":1000}""")
        assertFalse(service.isPanicActive())
        assertEquals(0, count)
    }

    @Test
    fun refreshMutexAllowsOnlyOneInFlight() {
        assertTrue(service.tryStartRefresh())
        assertFalse(service.tryStartRefresh())
        service.finishRefresh()
        assertTrue(service.tryStartRefresh())
    }

    @Test
    fun doesNotLogReenabledWhenPanicStillOn() {
        service.applyResponseControl("""{"panicMode":9}""")
        assertTrue(service.tryStartRefresh())
        service.finishRefresh(isRefreshFromPanicClear = true)
        assertTrue(service.isPanicActive())
    }

    @Test
    fun pingConfirmationMutexAllowsOnlyOneInFlight() {
        assertTrue(service.tryStartPanicClearConfirmation())
        assertFalse(service.tryStartPanicClearConfirmation())
        service.finishPanicClearConfirmation()
        assertTrue(service.tryStartPanicClearConfirmation())
    }
}
