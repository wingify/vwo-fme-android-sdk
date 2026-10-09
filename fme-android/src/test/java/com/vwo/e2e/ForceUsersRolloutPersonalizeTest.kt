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

package com.vwo.e2e

import com.vwo.VWO
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.vwo.utils.DummySettingsReader
import com.wingify.WingifyBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Force users (`whitelistedSegments`) on Rollout / Personalize.
 *
 * Mirrors proxy QA matrix (Runs A–F):
 *  A — Rollout force On @ 0% traffic
 *  B — Rollout force + Personalize force (gate passes, then personalize)
 *  C — Personalize force alone under 0% Rollout (R5: gate fails)
 *  D — Multi-rollout priority: force only on 2nd rollout
 *  E — Force disabled (`isForcedVariationEnabled=false`) ignores list
 *  F — Control / non-forced user unchanged
 *  G — Force Off (`not`) excludes the user even at 100% traffic;
 *      mixed On + multiple top-level `not` siblings; Force On at 0%
 *  H — Off-only (`not` root): H1 inversion at 0%; H2 fail-gate at 100%
 *  I — `and` with `not` first then On `or`; Off fail-gate at 100%,
 *      Force On and inversion at 0%
 *  J — `not` nested inside `or`; Off fail-gate at 100%, Force On and inversion at 0%
 */
class ForceUsersRolloutPersonalizeTest {

    private val sdkKey = "abcd"
    private val accountId = 1234
    private lateinit var vwo: VWO
    private val settingsReader = DummySettingsReader()

    private val forcedUser = "qa_forced"
    private val forcedUser2 = "qa_forced_2"
    private val forcedOffUser = "qa_forced_off"
    private val forcedOffUser2 = "qa_forced_off_2"
    private val controlUser = "other_user"

    @Before
    fun setup() {
        resetVWOState()
    }

    @After
    fun teardown() {
        resetVWOState()
    }

    private fun resetVWOState() {
        try {
            VWO.clearAllInstances()
        } catch (e: Exception) {
            println("Failed to reset VWO state: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Run A — Rollout force On @ 0%
    // -------------------------------------------------------------------------

    @Test
    fun `A1 rollout forced user gets On at 0 percent traffic`() {
        logRun(
            "A1",
            "Rollout force On @ 0% traffic",
            "ROLLOUT_FORCE_USERS_SETTINGS",
            forcedUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_USERS_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser))
        logResult(flag)
        assertNotNull(flag)
        assertTrue("Forced user must be enabled at 0% traffic", flag!!.isEnabled())
        assertEquals(42L, asLong(flag.getVariable("int", 0)))
        assertEquals("forced_rollout", flag.getVariable("string", ""))
    }

    @Test
    fun `A2 rollout comma-string second user is also forced`() {
        logRun(
            "A2",
            "Comma-string multi-user (qa_forced_2)",
            "ROLLOUT_FORCE_USERS_SETTINGS",
            forcedUser2,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_USERS_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser2))
        logResult(flag)
        assertNotNull(flag)
        assertTrue(flag!!.isEnabled())
        assertEquals("forced_rollout", flag.getVariable("string", ""))
    }

    // -------------------------------------------------------------------------
    // Run B — Rollout + Personalize both forced
    // -------------------------------------------------------------------------

    @Test
    fun `B1 rollout and personalize both forced returns personalize variables`() {
        logRun(
            "B1",
            "Force Rollout (0%) + Personalize — gate passes then personalize force",
            "ROLLOUT_AND_PERSONALIZE_FORCE_SETTINGS",
            forcedUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_AND_PERSONALIZE_FORCE_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser))
        logResult(flag)
        assertNotNull(flag)
        assertTrue("Gate must pass via forced rollout", flag!!.isEnabled())
        assertEquals(
            "Personalize force should win variables after gate",
            "personalize_forced",
            flag.getVariable("string", "")
        )
    }

    @Test
    fun `B2 personalize-only feature forced without audience match`() {
        logRun(
            "B2",
            "Personalize-only feature (no rollout) force without audience",
            "PERSONALIZE_FORCE_USERS_SETTINGS",
            forcedUser,
            "feature1"
        )
        initVWOWithSettings("PERSONALIZE_FORCE_USERS_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser))
        logResult(flag)
        assertNotNull(flag)
        assertTrue(flag!!.isEnabled())
        assertEquals(77L, asLong(flag.getVariable("int", 0)))
        assertEquals("forced_personalize", flag.getVariable("string", ""))
    }

    // -------------------------------------------------------------------------
    // Run C — R5: Personalize force does not bypass failed rollout gate
    // -------------------------------------------------------------------------

    @Test
    fun `C1 personalize force alone under 0 percent rollout leaves flag disabled`() {
        logRun(
            "C1",
            "R5 — Personalize forced, Rollout 0% NOT forced → gate fails",
            "ROLLOUT_0_PERSONALIZE_FORCE_ONLY_SETTINGS",
            forcedUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_0_PERSONALIZE_FORCE_ONLY_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "Personalize force must NOT bypass failed Rollout gate",
            flag!!.isEnabled()
        )
    }

    // -------------------------------------------------------------------------
    // Run D — Multi-rollout priority
    // -------------------------------------------------------------------------

    @Test
    fun `D1 first rollout misses then second forced rollout wins`() {
        logRun(
            "D1",
            "Multi-rollout — rule1 audience miss, rule2 forced @0% → second wins",
            "MULTI_ROLLOUT_FORCE_SECOND_SETTINGS",
            forcedUser,
            "feature1"
        )
        initVWOWithSettings("MULTI_ROLLOUT_FORCE_SECOND_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser))
        logResult(flag)
        assertNotNull(flag)
        assertTrue(flag!!.isEnabled())
        assertEquals("second_rollout_forced", flag.getVariable("string", ""))
    }

    // -------------------------------------------------------------------------
    // Run E — Force master switch off
    // -------------------------------------------------------------------------

    @Test
    fun `E1 isForcedVariationEnabled false ignores whitelistedSegments`() {
        logRun(
            "E1",
            "Force disabled — list present but isForcedVariationEnabled=false",
            "ROLLOUT_FORCE_DISABLED_SETTINGS",
            forcedUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_DISABLED_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "User in whitelistedSegments must be ignored when force toggle is off",
            flag!!.isEnabled()
        )
    }

    // -------------------------------------------------------------------------
    // Run F — Control user
    // -------------------------------------------------------------------------

    @Test
    fun `F1 control user not forced on rollout at 0 percent`() {
        logRun(
            "F1",
            "Control user — not on force list @ 0% rollout",
            "ROLLOUT_FORCE_USERS_SETTINGS",
            controlUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_USERS_SETTINGS")
        val flag = getFlag("feature1", user(controlUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(flag!!.isEnabled())
    }

    @Test
    fun `F2 control user not forced on personalize without audience`() {
        logRun(
            "F2",
            "Control user — personalize audience miss, not forced",
            "PERSONALIZE_FORCE_USERS_SETTINGS",
            controlUser,
            "feature1"
        )
        initVWOWithSettings("PERSONALIZE_FORCE_USERS_SETTINGS")
        val flag = getFlag("feature1", user(controlUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(flag!!.isEnabled())
    }

    @Test
    fun `F3 control user not forced when rollout and personalize lists exist`() {
        logRun(
            "F3",
            "Control user — both rules forced in settings but user not listed",
            "ROLLOUT_AND_PERSONALIZE_FORCE_SETTINGS",
            controlUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_AND_PERSONALIZE_FORCE_SETTINGS")
        val flag = getFlag("feature1", user(controlUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(flag!!.isEnabled())
    }

    // -------------------------------------------------------------------------
    // Run G — Force Off (`not`) is a fail-gate, not fall-through
    // -------------------------------------------------------------------------

    @Test
    fun `G1 force off user is excluded at 100 percent traffic`() {
        logRun(
            "G1",
            "Force Off (`not`) at 100% empty audience must not fall through to traffic",
            "ROLLOUT_FORCE_ON_OFF_SETTINGS",
            forcedOffUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_SETTINGS")
        val flag = getFlag("feature1", user(forcedOffUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "Force Off user must be excluded from the rollout, not enrolled via traffic",
            flag!!.isEnabled()
        )
    }

    @Test
    fun `G2 force on user still gets On when off list is present`() {
        logRun(
            "G2",
            "Force On at 0% uses On list only; traffic cannot enroll this user",
            "ROLLOUT_FORCE_ON_OFF_0_SETTINGS",
            forcedUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_0_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser))
        logResult(flag)
        assertNotNull(flag)
        assertTrue(
            "On-list user must be Force On at 0% traffic; 100% would also enroll via traffic",
            flag!!.isEnabled()
        )
        assertEquals("forced_rollout", flag.getVariable("string", ""))
    }

    @Test
    fun `G3 neither on nor off user follows traffic`() {
        logRun(
            "G3",
            "User on neither list at 100% traffic still enrolls normally",
            "ROLLOUT_FORCE_ON_OFF_SETTINGS",
            controlUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_SETTINGS")
        val flag = getFlag("feature1", user(controlUser))
        logResult(flag)
        assertNotNull(flag)
        assertTrue(
            "Neither-list user must still follow empty audience + 100% traffic",
            flag!!.isEnabled()
        )
    }

    @Test
    fun `G4 second top-level not user is also excluded`() {
        logRun(
            "G4",
            "Second top-level `not` sibling must Force Off, not only the first `not`",
            "ROLLOUT_FORCE_ON_OFF_SETTINGS",
            forcedOffUser2,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_SETTINGS")
        val flag = getFlag("feature1", user(forcedOffUser2))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "Second Off list user must be excluded; unwrapping must collect every top-level not",
            flag!!.isEnabled()
        )
    }

    @Test
    fun `G5 second On user is still Force On beside two nots`() {
        logRun(
            "G5",
            "Second On user at 0% is Force On beside two top-level `not`s; traffic cannot enroll",
            "ROLLOUT_FORCE_ON_OFF_0_SETTINGS",
            forcedUser2,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_0_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser2))
        logResult(flag)
        assertNotNull(flag)
        assertTrue(
            "Second On-list user must be Force On at 0% traffic beside two nots",
            flag!!.isEnabled()
        )
        assertEquals("forced_rollout", flag.getVariable("string", ""))
    }

    // -------------------------------------------------------------------------
    // Run H — Off-only (`not` root): inversion at 0%, fail-gate at 100%
    // -------------------------------------------------------------------------

    @Test
    fun `H1 off-only user not on list is not force-on at 0 percent`() {
        logRun(
            "H1",
            "Off-only `not` at 0% unmatched audience must fall through, not Force On",
            "ROLLOUT_FORCE_OFF_ONLY_SETTINGS",
            controlUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_OFF_ONLY_SETTINGS")
        val flag = getFlag("feature1", user(controlUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "User not on Off-only list must not be Force On; 0% + unmatched audience stays off",
            flag!!.isEnabled()
        )
    }

    @Test
    fun `H2 off-only listed user is still excluded`() {
        logRun(
            "H2",
            "Off-only listed user is Force Off at 100% empty audience, not traffic fall-through",
            "ROLLOUT_FORCE_OFF_ONLY_100_SETTINGS",
            forcedOffUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_OFF_ONLY_100_SETTINGS")
        val flag = getFlag("feature1", user(forcedOffUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "User on Off-only list must stay excluded at 100% traffic",
            flag!!.isEnabled()
        )
    }

    // -------------------------------------------------------------------------
    // Run I — not-first `and` + comma Off/On lists; Off at 100%, inversion at 0%
    // -------------------------------------------------------------------------

    @Test
    fun `I1 first comma Off user is excluded when not is first in and`() {
        logRun(
            "I1",
            "not-first and; first UUID in comma Off list is Force Off at 100% traffic",
            "ROLLOUT_FORCE_ON_OFF_NOT_FIRST_SETTINGS",
            forcedOffUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_NOT_FIRST_SETTINGS")
        val flag = getFlag("feature1", user(forcedOffUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "First comma Off user must be excluded at 100% traffic; not-first and must still unwrap Off",
            flag!!.isEnabled()
        )
    }

    @Test
    fun `I2 second comma Off user is excluded when not is first in and`() {
        logRun(
            "I2",
            "not-first and; second UUID in comma Off list is Force Off at 100% traffic",
            "ROLLOUT_FORCE_ON_OFF_NOT_FIRST_SETTINGS",
            forcedOffUser2,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_NOT_FIRST_SETTINGS")
        val flag = getFlag("feature1", user(forcedOffUser2))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "Second comma Off user must be excluded at 100% traffic; evaluateUserDSL splits the Off string",
            flag!!.isEnabled()
        )
    }

    @Test
    fun `I3 On user is Force On when not is first in and`() {
        logRun(
            "I3",
            "not-first and; On comma list is Force On at 0% after dropping not",
            "ROLLOUT_FORCE_ON_OFF_NOT_FIRST_0_SETTINGS",
            forcedUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_NOT_FIRST_0_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser))
        logResult(flag)
        assertNotNull(flag)
        assertTrue(
            "On-list user must be Force On at 0% when not is first in and",
            flag!!.isEnabled()
        )
        assertEquals("forced_rollout", flag.getVariable("string", ""))
    }

    @Test
    fun `I4 neither-list user is not force-on when not is first in and`() {
        logRun(
            "I4",
            "not-first and; user on neither list at 0% must not get inverted not as Force On",
            "ROLLOUT_FORCE_ON_OFF_NOT_FIRST_0_SETTINGS",
            controlUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_NOT_FIRST_0_SETTINGS")
        val flag = getFlag("feature1", user(controlUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "Neither-list user must not be Force On; inverted not would enable them at 0% traffic",
            flag!!.isEnabled()
        )
    }

    // -------------------------------------------------------------------------
    // Run J — `not` nested inside `or`; Off at 100%, inversion at 0%
    // -------------------------------------------------------------------------

    @Test
    fun `J1 nested or not excludes Off user`() {
        logRun(
            "J1",
            "or[ not(Off), or(On) ]; Off user is Force Off at 100% traffic",
            "ROLLOUT_FORCE_ON_OFF_OR_NESTED_SETTINGS",
            forcedOffUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_OR_NESTED_SETTINGS")
        val flag = getFlag("feature1", user(forcedOffUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "Off user nested under or must be excluded at 100% traffic",
            flag!!.isEnabled()
        )
    }

    @Test
    fun `J2 nested or not still Force On for On user`() {
        logRun(
            "J2",
            "or[ not(Off), or(On) ]; On user is Force On at 0% traffic",
            "ROLLOUT_FORCE_ON_OFF_OR_NESTED_0_SETTINGS",
            forcedUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_OR_NESTED_0_SETTINGS")
        val flag = getFlag("feature1", user(forcedUser))
        logResult(flag)
        assertNotNull(flag)
        assertTrue(
            "On user nested under or must be Force On at 0% traffic",
            flag!!.isEnabled()
        )
        assertEquals("forced_rollout", flag.getVariable("string", ""))
    }

    @Test
    fun `J3 nested or not does not Force On neither-list user`() {
        logRun(
            "J3",
            "or[ not(Off), or(On) ]; inverted not must not Force On Charlie at 0%",
            "ROLLOUT_FORCE_ON_OFF_OR_NESTED_0_SETTINGS",
            controlUser,
            "feature1"
        )
        initVWOWithSettings("ROLLOUT_FORCE_ON_OFF_OR_NESTED_0_SETTINGS")
        val flag = getFlag("feature1", user(controlUser))
        logResult(flag)
        assertNotNull(flag)
        assertFalse(
            "Neither-list user must not be Force On from nested not inside or",
            flag!!.isEnabled()
        )
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun user(id: String) = VWOUserContext().apply { this.id = id }

    private fun asLong(value: Any): Long = (value as Number).toLong()

    private fun logRun(
        runId: String,
        description: String,
        settingsKey: String,
        userId: String,
        featureKey: String
    ) {
        println("")
        println("========== FORCE USERS TEST [$runId] ==========")
        println("Case     : $description")
        println("Settings : $settingsKey")
        println("Feature  : $featureKey")
        println("UserId   : $userId")
        println("==============================================")
    }

    private fun logResult(flag: GetFlag?) {
        if (flag == null) {
            println("Result   : flag=null")
            return
        }
        val enabled = flag.isEnabled()
        val stringVar = flag.getVariable("string", "<missing>")
        val intVar = flag.getVariable("int", "<missing>")
        println("Result   : isEnabled=$enabled, string=$stringVar, int=$intVar")
        println("==============================================")
        println("")
    }

    private fun initVWOWithSettings(settingsKey: String) {
        val vwoInitOptions = VWOInitOptions().apply {
            this.sdkKey = this@ForceUsersRolloutPersonalizeTest.sdkKey
            this.accountId = this@ForceUsersRolloutPersonalizeTest.accountId
            this.isUsageStatsDisabled = true
        }

        val wingifyBuilder = WingifyBuilder(vwoInitOptions)
        val wingifyBuilderSpy = spy(wingifyBuilder)
        val settings = settingsReader.settingsMap[settingsKey]
        requireNotNull(settings) { "Missing settings fixture: $settingsKey" }
        whenever(wingifyBuilderSpy.getSettings(false)).thenReturn(settings)
        vwoInitOptions.wingifyBuilder = wingifyBuilderSpy

        val latch = CountDownLatch(1)
        VWO.init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                this@ForceUsersRolloutPersonalizeTest.vwo = vwo
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                println("VWO init failed: $message")
                latch.countDown()
            }
        })

        assertTrue("VWO init timed out for $settingsKey", latch.await(10, TimeUnit.SECONDS))
        assertTrue("VWO not initialized for $settingsKey", ::vwo.isInitialized)
    }

    private fun getFlag(featureKey: String, context: VWOUserContext): GetFlag? {
        if (!::vwo.isInitialized) return null

        val latch = CountDownLatch(1)
        var featureFlag: GetFlag? = null

        vwo.getFlag(featureKey, context, object : IVwoListener {
            override fun onSuccess(data: Any) {
                featureFlag = data as? GetFlag
                latch.countDown()
            }

            override fun onFailure(message: String) {
                println("getFlag failed: $message")
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)
        return featureFlag
    }
}
