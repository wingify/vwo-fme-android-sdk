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

class PanicManagerSettingsTest {

    private lateinit var service: PanicManager

    @Before
    fun setUp() {
        service = PanicManager()
    }

    private fun snapshot(
        lastUpdatedOn: Long,
        panic: Long? = null,
        refresh: Long? = null,
        version: Int? = null,
    ): Settings = Settings().apply {
        this.lastUpdatedOn = lastUpdatedOn
        this.version = version
        sdkMetaInfo = SdkMetaInfo(panic = panic, refresh = refresh)
    }

    @Test
    fun ignoresDuplicateLastUpdatedOn() {
        val payload = snapshot(1777026135, panic = 10)
        assertTrue(service.applySettingsControl(payload).isPanicActive)
        val second = service.applySettingsControl(payload)
        assertFalse(second.shouldTriggerForceRefresh)
        assertTrue(service.isPanicActive())
    }

    @Test
    fun defersPanicClearWhenPanicMissingWhileLatched() {
        service.applySettingsControl(snapshot(1, panic = 1776169618))
        val next = service.applySettingsControl(snapshot(2))
        assertTrue(next.needsCollectPingConfirmation)
        assertTrue(service.isPanicActive())
    }

    @Test
    fun extraFetchesOnInitWhenRefreshPresent() {
        val result = service.applySettingsControl(snapshot(1, refresh = 1776162641))
        assertTrue(result.shouldTriggerForceRefresh)
    }

    @Test
    fun doesNotExtraFetchOnInitWhenRefreshOmitted() {
        assertFalse(service.applySettingsControl(snapshot(1)).shouldTriggerForceRefresh)
    }

    @Test
    fun extraFetchesWhenRefreshTokenIsNewer() {
        service.applySettingsControl(snapshot(1, refresh = 1776162641))
        val next = service.applySettingsControl(snapshot(2, refresh = 1776169999))
        assertTrue(next.shouldTriggerForceRefresh)
    }

    @Test
    fun ignoresStalePanicOnApply() {
        service.applySettingsControl(snapshot(1, panic = 2000))
        service.applyResponseControl("{}")
        assertFalse(service.isPanicActive())
        val next = service.applySettingsControl(snapshot(2, panic = 1000))
        assertFalse(service.isPanicActive())
        assertFalse(next.needsCollectPingConfirmation)
        assertFalse(next.shouldTriggerForceRefresh)
        // last seen panic ts must stay 2000 so a mid-range value is also ignored
        service.applySettingsControl(snapshot(3, panic = 1500))
        assertFalse(service.isPanicActive())
    }

    @Test
    fun olderPanicTimestampWhileActiveChangesNothing() {
        service.applySettingsControl(snapshot(1, panic = 2000))
        val next = service.applySettingsControl(snapshot(2, panic = 1000))
        assertTrue(service.isPanicActive())
        assertFalse(next.shouldTriggerForceRefresh)
        assertFalse(next.needsCollectPingConfirmation)
        service.applySettingsControl(snapshot(3, panic = 1500))
        assertTrue(service.isPanicActive())
    }

    @Test
    fun ignoresStaleRefreshOnApply() {
        service.applySettingsControl(snapshot(1, refresh = 1776162641))
        val next = service.applySettingsControl(snapshot(2, refresh = 1776162641))
        assertFalse(next.shouldTriggerForceRefresh)
    }

    @Test
    fun pollingDoesNotLatchPanic() {
        service.applySettingsControl(snapshot(1))
        val polled = service.applySettingsControl(
            snapshot(2, panic = 1776169618),
            SettingsControlApplyMode.POLLING,
        )
        assertFalse(service.isPanicActive())
        assertTrue(polled.needsCollectPingConfirmation)
        assertFalse(polled.shouldTriggerForceRefresh)
    }

    @Test
    fun pollingMetaTriggersForceRefreshOnNewerRefresh() {
        service.applySettingsControl(snapshot(1, refresh = 1000))
        commitArmedRefresh()
        val polled = service.evaluateForceRefreshFromPollMeta(snapshot(2, refresh = 2000))
        assertTrue(polled.shouldTriggerForceRefresh)
        assertFalse(polled.needsCollectPingConfirmation)
    }

    @Test
    fun pollingMetaTriggersForceRefreshWhenSnapshotIdIsStale() {
        service.applySettingsControl(snapshot(5000, refresh = 1000))
        commitArmedRefresh()
        val stalePoll = snapshot(lastUpdatedOn = 1000, refresh = 2000)
        assertFalse(service.shouldApplySettingsSnapshot(stalePoll))
        assertTrue(service.evaluateForceRefreshFromPollMeta(stalePoll).shouldTriggerForceRefresh)
    }

    @Test
    fun pendingRefreshBlocksDuplicateTriggerUntilApplied() {
        assertTrue(service.applySettingsControl(snapshot(1, refresh = 1776162641)).shouldTriggerForceRefresh)
        assertFalse(service.applySettingsControl(snapshot(2, refresh = 1776162641)).shouldTriggerForceRefresh)
    }

    @Test
    fun abandonPendingForceRefreshAllowsRetry() {
        assertTrue(service.applySettingsControl(snapshot(1, refresh = 1776162641)).shouldTriggerForceRefresh)
        assertTrue(service.tryStartRefresh())
        service.abandonPendingForceRefresh()
        service.finishRefresh(shouldLog = false)
        assertTrue(service.evaluateForceRefreshFromPollMeta(snapshot(2, refresh = 1776162641)).shouldTriggerForceRefresh)
    }

    @Test
    fun recordForceRefreshAppliedCommitsTimestamp() {
        service.applySettingsControl(snapshot(1, refresh = 1776162641))
        commitArmedRefresh()
        assertFalse(service.hasPendingForceRefresh())
        assertFalse(service.evaluateForceRefreshFromPollMeta(snapshot(2, refresh = 1776162641)).shouldTriggerForceRefresh)
    }

    @Test
    fun newerPendingSurvivesInFlightSuccess() {
        assertTrue(service.applySettingsControl(snapshot(1, refresh = 1000)).shouldTriggerForceRefresh)
        assertTrue(service.tryStartRefresh())
        assertTrue(service.evaluateForceRefreshFromPollMeta(snapshot(2, refresh = 2000)).shouldTriggerForceRefresh)
        service.recordForceRefreshApplied()
        service.finishRefresh(shouldLog = false)
        assertTrue(service.hasPendingForceRefresh())
        assertFalse(service.evaluateForceRefreshFromPollMeta(snapshot(3, refresh = 2000)).shouldTriggerForceRefresh)
    }

    @Test
    fun newerPendingSurvivesInFlightFailure() {
        assertTrue(service.applySettingsControl(snapshot(1, refresh = 1000)).shouldTriggerForceRefresh)
        assertTrue(service.tryStartRefresh())
        assertTrue(service.evaluateForceRefreshFromPollMeta(snapshot(2, refresh = 2000)).shouldTriggerForceRefresh)
        service.abandonPendingForceRefresh()
        service.finishRefresh(shouldLog = false)
        assertTrue(service.hasPendingForceRefresh())
        assertFalse(service.evaluateForceRefreshFromPollMeta(snapshot(3, refresh = 1000)).shouldTriggerForceRefresh)
    }

    private fun commitArmedRefresh() {
        assertTrue(service.tryStartRefresh())
        service.recordForceRefreshApplied()
        service.finishRefresh(shouldLog = false)
    }

    @Test
    fun pollingPingsWhenPanicOffWhileLatched() {
        service.applySettingsControl(snapshot(1, panic = 1, refresh = 1))
        val polled = service.applySettingsControl(snapshot(2), SettingsControlApplyMode.POLLING)
        assertTrue(service.isPanicActive())
        assertTrue(polled.needsCollectPingConfirmation)
        assertFalse(polled.shouldTriggerForceRefresh)
    }

    @Test
    fun pollingRejectsStalePanicWithoutPing() {
        service.applySettingsControl(snapshot(1, panic = 2000, refresh = 2000))
        val polled = service.applySettingsControl(
            snapshot(2, panic = 1000, refresh = 2000),
            SettingsControlApplyMode.POLLING,
        )
        assertTrue(service.isPanicActive())
        assertFalse(polled.needsCollectPingConfirmation)
    }

    @Test
    fun pollingThenPingConfirmsOnLatchesPanic() {
        service.applySettingsControl(snapshot(1))
        val polled = service.applySettingsControl(
            snapshot(2, panic = 1776669404),
            SettingsControlApplyMode.POLLING,
        )
        assertTrue(polled.needsCollectPingConfirmation)
        assertFalse(service.isPanicActive())

        val ping = service.applyResponseControl("""{"panicMode":1776669404}""")
        assertTrue(service.isPanicActive())
        assertTrue(ping.isPanicActive)
    }

    @Test
    fun pollingThenPingConfirmsOffClearsPanic() {
        service.applySettingsControl(snapshot(1, panic = 1000))
        assertTrue(service.isPanicActive())
        val polled = service.applySettingsControl(snapshot(2), SettingsControlApplyMode.POLLING)
        assertTrue(polled.needsCollectPingConfirmation)
        assertTrue(service.isPanicActive())

        val ping = service.applyResponseControl("{}")
        assertFalse(service.isPanicActive())
        assertTrue(ping.isRefreshFromPanicClear)
    }

    @Test
    fun postPingFetchDoesNotRelatch() {
        service.applySettingsControl(snapshot(1, panic = 1705924217))
        service.applyResponseControl("{}")
        assertFalse(service.isPanicActive())
        val fetched = service.applySettingsControl(
            snapshot(2, panic = 1705929999),
            SettingsControlApplyMode.POST_PING_FETCH,
        )
        assertFalse(service.isPanicActive())
        assertFalse(fetched.shouldTriggerForceRefresh)
        assertFalse(fetched.needsCollectPingConfirmation)
    }

    @Test
    fun postPingFetchIgnoresStalePanic() {
        var activated = 0
        service.setOnPanicActivated { activated++ }
        service.applySettingsControl(snapshot(1, panic = 2000))
        assertEquals(1, activated)
        service.applyResponseControl("{}")
        service.applySettingsControl(
            snapshot(2, panic = 1500),
            SettingsControlApplyMode.POST_PING_FETCH,
        )
        assertFalse(service.isPanicActive())
        assertEquals(1, activated)
    }

    @Test
    fun shouldApplySettingsSnapshotRejectsStaleAndDuplicate() {
        assertTrue(service.shouldApplySettingsSnapshot(snapshot(1000)))
        service.applySettingsControl(snapshot(1000))
        assertTrue(service.shouldApplySettingsSnapshot(snapshot(2000)))
        service.applySettingsControl(snapshot(2000))
        assertFalse(service.shouldApplySettingsSnapshot(snapshot(1500)))
        assertFalse(service.shouldApplySettingsSnapshot(snapshot(2000)))
    }

    @Test
    fun fallsBackToVersionWhenLastUpdatedOnMissing() {
        val first = Settings().apply { version = 1705924217 }
        service.applySettingsControl(first)
        assertFalse(service.shouldApplySettingsSnapshot(Settings().apply { version = 1705924000 }))
        assertTrue(service.shouldApplySettingsSnapshot(Settings().apply { version = 1705925000 }))
        assertFalse(service.shouldApplySettingsSnapshot(Settings()))
    }
}
