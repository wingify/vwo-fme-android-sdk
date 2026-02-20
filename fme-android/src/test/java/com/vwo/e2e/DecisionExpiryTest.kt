/*
 * Copyright (c) 2025-2026 Wingify Software Pvt. Ltd.
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

package com.vwo.e2e

import com.vwo.VWO
import com.vwo.VWOBuilder
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.storage.Connector
import com.vwo.providers.ServiceContainerProvider
import com.vwo.utils.DummySettingsReader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * E2E tests for the decision expiry feature (cachedDecisionExpiryTime).
 *
 * Validates that:
 * - When cachedDecisionExpiryTime = 0, stored decisions never expire.
 * - When cachedDecisionExpiryTime > 0, decisions include an expiry timestamp.
 * - Expired stored decisions are ignored and the SDK re-evaluates.
 * - Old payloads without decisionExpiryTime are treated as valid forever.
 */
class DecisionExpiryTest {

    private val sdkKey = "abcd"
    private val accountId = 1234
    private lateinit var vwo: VWO
    private val settingsReader = DummySettingsReader()

    private fun resetVWOState() {
        try {
            VWO.clearAllInstances()
            ServiceContainerProvider.clearAllServiceContainers()
        } catch (e: Exception) {
            println("Failed to reset VWO state: ${e.message}")
        }
    }

    @Before
    fun setup() {
        resetVWOState()
    }

    @After
    fun teardown() {
        resetVWOState()
    }

    // ── Storage connector that tracks decisionExpiryTime ─────────────────

    private class TestExpiryStorage : Connector() {
        val storage: MutableMap<String, MutableMap<String, Any?>> = HashMap()

        override fun set(data: Map<String, Any>) {
            val key = "${data["featureKey"]}_${data["userId"]}"
            val value: MutableMap<String, Any?> = HashMap()
            value["rolloutKey"] = data["rolloutKey"]
            value["rolloutVariationId"] = data["rolloutVariationId"]
            value["experimentKey"] = data["experimentKey"]
            value["experimentVariationId"] = data["experimentVariationId"]
            value["rolloutId"] = data["rolloutId"]
            value["experimentId"] = data["experimentId"]
            data["decisionExpiryTime"]?.let { value["decisionExpiryTime"] = it }
            storage[key] = value
        }

        override fun get(featureKey: String?, userId: String?): Any? {
            return storage["${featureKey}_${userId}"]
        }

        fun getStoredEntry(featureKey: String, userId: String): Map<String, Any?>? {
            return storage["${featureKey}_${userId}"]
        }

        fun prePopulate(featureKey: String, userId: String, data: Map<String, Any?>) {
            storage["${featureKey}_${userId}"] = data.toMutableMap()
        }
    }

    // ── Helper: init VWO ────────────────────────────────────────────────

    private fun initVWO(
        settingsName: String,
        storage: Connector? = null,
        cachedDecisionExpiryTime: Int = 0
    ): Boolean {
        val options = VWOInitOptions().apply {
            this.sdkKey = this@DecisionExpiryTest.sdkKey
            this.accountId = this@DecisionExpiryTest.accountId
            this.isUsageStatsDisabled = true
            this.cachedDecisionExpiryTime = cachedDecisionExpiryTime
            if (storage != null) this.storage = storage
        }

        val vwoBuilder = VWOBuilder(options)
        val vwoBuilderSpy = spy(vwoBuilder)
        val settings = settingsReader.settingsMap[settingsName] ?: return false
        whenever(vwoBuilderSpy.getSettings(false)).thenReturn(settings)
        options.vwoBuilder = vwoBuilderSpy

        var initSuccess = false
        val latch = CountDownLatch(1)
        VWO.init(options, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                this@DecisionExpiryTest.vwo = vwo
                initSuccess = true
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                println("VWO init failed: $message")
                latch.countDown()
            }
        })
        latch.await(10, TimeUnit.SECONDS)
        return initSuccess
    }

    // ── Helper: call getFlag ────────────────────────────────────────────

    private fun callGetFlag(featureKey: String, userId: String): GetFlag? {
        val context = VWOUserContext().apply { id = userId }
        return callGetFlag(featureKey, context)
    }

    private fun callGetFlag(featureKey: String, context: VWOUserContext): GetFlag? {
        val latch = CountDownLatch(1)
        var result: GetFlag? = null
        vwo.getFlag(featureKey, context, object : IVwoListener {
            override fun onSuccess(data: Any) {
                result = data as? GetFlag
                latch.countDown()
            }

            override fun onFailure(message: String) {
                println("getFlag failed: $message")
                latch.countDown()
            }
        })
        latch.await(5, TimeUnit.SECONDS)
        return result
    }

    // ── Tests ───────────────────────────────────────────────────────────

    @Test
    fun `decision stored without expiry when cachedDecisionExpiryTime is zero`() {
        val storage = TestExpiryStorage()
        val featureKey = "feature1"
        val userId = "user_id"

        if (!initVWO("BASIC_ROLLOUT_SETTINGS", storage, cachedDecisionExpiryTime = 0)) return

        val flag = callGetFlag(featureKey, userId)
        assertNotNull("Flag should not be null", flag)
        assertTrue(flag!!.isEnabled())

        val stored = storage.getStoredEntry(featureKey, userId)
        assertNotNull(stored)
        assertNull(
            "decisionExpiryTime should not be set when cachedDecisionExpiryTime is 0",
            stored!!["decisionExpiryTime"]
        )
    }

    @Test
    fun `decision stored with expiry when cachedDecisionExpiryTime is positive`() {
        val storage = TestExpiryStorage()
        val featureKey = "feature1"
        val userId = "user_id"
        val expiryMs = 60_000

        val beforeCall = System.currentTimeMillis()
        if (!initVWO("BASIC_ROLLOUT_SETTINGS", storage, cachedDecisionExpiryTime = expiryMs)) return

        val flag = callGetFlag(featureKey, userId)
        assertNotNull("Flag should not be null", flag)
        assertTrue(flag!!.isEnabled())
        val afterCall = System.currentTimeMillis()

        val stored = storage.getStoredEntry(featureKey, userId)
        assertNotNull(stored)

        val storedExpiry = stored!!["decisionExpiryTime"] as? Long
        assertNotNull("decisionExpiryTime should be set when cachedDecisionExpiryTime > 0", storedExpiry)
        assertTrue(
            "Stored expiry should be roughly currentTime + expiryMs",
            storedExpiry!! in (beforeCall + expiryMs)..(afterCall + expiryMs)
        )
    }

    @Test
    fun `expired stored decision is ignored and SDK re-evaluates`() {
        val storage = TestExpiryStorage()
        val featureKey = "feature1"
        val userId = "user_id"

        storage.prePopulate(featureKey, userId, mapOf(
            "rolloutKey" to "feature1_rolloutRule1",
            "rolloutVariationId" to 1,
            "rolloutId" to 1,
            "decisionExpiryTime" to (System.currentTimeMillis() - 10_000L)
        ))

        if (!initVWO("BASIC_ROLLOUT_SETTINGS", storage, cachedDecisionExpiryTime = 60_000)) return

        val flag = callGetFlag(featureKey, userId)
        assertNotNull(flag)
        assertTrue(
            "Flag should be enabled after re-evaluation even though stored decision expired",
            flag!!.isEnabled()
        )

        val stored = storage.getStoredEntry(featureKey, userId)
        assertNotNull(stored)
        val newExpiry = stored!!["decisionExpiryTime"] as? Long
        assertNotNull("A new decisionExpiryTime should be written after re-evaluation", newExpiry)
        assertTrue(
            "New expiry should be in the future",
            newExpiry!! > System.currentTimeMillis()
        )
    }

    @Test
    fun `non-expired stored decision is used directly`() {
        val storage = TestExpiryStorage()
        val featureKey = "feature1"
        val userId = "user_id"

        val futureExpiry = System.currentTimeMillis() + 300_000L
        storage.prePopulate(featureKey, userId, mapOf(
            "experimentKey" to "feature1_testingRule1",
            "experimentVariationId" to 2,
            "experimentId" to 100,
            "decisionExpiryTime" to futureExpiry
        ))

        if (!initVWO("BASIC_ROLLOUT_TESTING_RULE_SETTINGS", storage, cachedDecisionExpiryTime = 60_000)) return

        val flag = callGetFlag(featureKey, userId)
        assertNotNull(flag)
        assertTrue("Flag should be enabled from the stored (non-expired) decision", flag!!.isEnabled())

        val stored = storage.getStoredEntry(featureKey, userId)
        val currentExpiry = stored?.get("decisionExpiryTime") as? Long
        assertEquals(
            "Expiry should remain unchanged because the stored decision was used directly",
            futureExpiry,
            currentExpiry
        )
    }

    @Test
    fun `old payload without decisionExpiryTime is treated as valid forever`() {
        val storage = TestExpiryStorage()
        val featureKey = "feature1"
        val userId = "user_id"

        storage.prePopulate(featureKey, userId, mapOf(
            "experimentKey" to "feature1_testingRule1",
            "experimentVariationId" to 2,
            "experimentId" to 100
        ))

        if (!initVWO("BASIC_ROLLOUT_TESTING_RULE_SETTINGS", storage, cachedDecisionExpiryTime = 60_000)) return

        val flag = callGetFlag(featureKey, userId)
        assertNotNull(flag)
        assertTrue(
            "Flag should be enabled from old payload without decisionExpiryTime (infinite validity)",
            flag!!.isEnabled()
        )

        val stored = storage.getStoredEntry(featureKey, userId)
        assertNull(
            "Old entry should still lack decisionExpiryTime (wasn't re-evaluated)",
            stored?.get("decisionExpiryTime")
        )
    }

    @Test
    fun `expired experiment decision triggers re-evaluation with new expiry`() {
        val storage = TestExpiryStorage()
        val featureKey = "feature1"
        val userId = "user_id"

        storage.prePopulate(featureKey, userId, mapOf(
            "experimentKey" to "feature1_testingRule1",
            "experimentVariationId" to 2,
            "experimentId" to 100,
            "decisionExpiryTime" to (System.currentTimeMillis() - 5_000L)
        ))

        val expiryMs = 120_000
        if (!initVWO("BASIC_ROLLOUT_TESTING_RULE_SETTINGS", storage, cachedDecisionExpiryTime = expiryMs)) return

        val beforeCall = System.currentTimeMillis()
        val flag = callGetFlag(featureKey, userId)
        val afterCall = System.currentTimeMillis()

        assertNotNull(flag)
        assertTrue("Flag should be enabled after re-evaluation", flag!!.isEnabled())

        val stored = storage.getStoredEntry(featureKey, userId)
        assertNotNull(stored)
        val newExpiry = stored!!["decisionExpiryTime"] as? Long
        assertNotNull("New expiry should be written", newExpiry)
        assertTrue(
            "New expiry should be in the future relative to now + expiryMs",
            newExpiry!! in (beforeCall + expiryMs)..(afterCall + expiryMs)
        )
    }

    @Test
    fun `decision expiry with rollout-only settings stores correct expiry`() {
        val storage = TestExpiryStorage()
        val featureKey = "feature1"
        val userId = "user_id"
        val expiryMs = 30_000

        if (!initVWO("BASIC_ROLLOUT_SETTINGS", storage, cachedDecisionExpiryTime = expiryMs)) return

        val beforeCall = System.currentTimeMillis()
        val flag = callGetFlag(featureKey, userId)
        val afterCall = System.currentTimeMillis()

        assertNotNull("Flag should not be null", flag)
        assertTrue(flag!!.isEnabled())

        val stored = storage.getStoredEntry(featureKey, userId)
        assertNotNull(stored)

        val storedExpiry = stored!!["decisionExpiryTime"] as? Long
        assertNotNull(storedExpiry)
        assertTrue(
            "Rollout-only decision should also have correct decisionExpiryTime",
            storedExpiry!! in (beforeCall + expiryMs)..(afterCall + expiryMs)
        )
        assertNotNull("rolloutKey should be present", stored["rolloutKey"])
    }

    @Test
    fun `decision expiry with rollout and testing rule stores correct expiry`() {
        val storage = TestExpiryStorage()
        val featureKey = "feature1"
        val userId = "user_id"
        val expiryMs = 45_000

        if (!initVWO("BASIC_ROLLOUT_TESTING_RULE_SETTINGS", storage, cachedDecisionExpiryTime = expiryMs)) return

        val context = VWOUserContext().apply {
            id = userId
            customVariables = mutableMapOf("price" to 200 as Any)
        }

        val beforeCall = System.currentTimeMillis()
        val flag = callGetFlag(featureKey, context)
        val afterCall = System.currentTimeMillis()

        assertNotNull("Flag should not be null", flag)
        assertTrue(flag!!.isEnabled())

        val stored = storage.getStoredEntry(featureKey, userId)
        assertNotNull(stored)

        val storedExpiry = stored!!["decisionExpiryTime"] as? Long
        assertNotNull(storedExpiry)
        assertTrue(
            "Decision with rollout+testing rule should also have correct decisionExpiryTime",
            storedExpiry!! in (beforeCall + expiryMs)..(afterCall + expiryMs)
        )
    }
}
