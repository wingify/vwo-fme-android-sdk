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
package com.vwo.e2e

import com.vwo.VWO
import com.vwo.VWOBuilder
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.vwo.utils.DummySettingsReader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * E2E Tests for Custom Bucketing Seed feature.
 *
 * This test class covers the following scenarios:
 *
 * Standard bucketing behaviour (CUSTOM_BUCKETING_SEED_SETTINGS):
 * 1. Standard bucketing (no custom seed) - different users get different variations
 * 2. Same bucketing seed = same variation for different users
 * 3. No seed provided = fallback to userId (different users get different variations)
 * 4. Different seeds = different variations for same user
 * 5. Empty string seed = fallback to userId (different users get different variations)
 *
 * Aliasing + bucketing seed (CUSTOM_BUCKETING_SEED_SETTINGS):
 * 6. Two users that naturally hash to DIFFERENT variations get the SAME variation when the
 *    same bucketing seed is applied — the seed takes priority over the resolved userId.
 * 7. Without a bucketing seed, those same two users still get DIFFERENT variations.
 *
 * Custom salt + bucketing seed combinations (SETTINGS_WITH_SAME_SALT):
 * 8. No seed, same salt — each of 10 users gets the identical variation across both features.
 * 9. Same seed AND same salt — all 10 users converge on a single variation across both features.
 *
 * Forced variation (whitelisting) + bucketing seed (CUSTOM_BUCKETING_SEED_SETTINGS):
 * 10. Whitelisted user receives the forced variation when no bucketing seed is provided.
 * 11. Whitelisted user still receives the forced variation when a bucketing seed is present.
 */
class CustomBucketingSeedTest {

    private val sdkKey: String = "abcdef"
    private val accountId: Int = 123456
    private lateinit var vwo: VWO
    private val settingsReader = DummySettingsReader()
    private val featureKey = "featureOne"

    private fun resetVWOState() {
        try {
            VWO.clearAllInstances()
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

    /**
     * Case 1: Standard bucketing (no custom seed).
     * Two different users ('KaustubhVWO', 'RandomUserVWO') with NO bucketing seed.
     * They should be bucketed into different variations based on their User IDs.
     */
    @Test
    fun testDifferentUsersGetDifferentVariationsWithoutSeed() {
        initVWO()

        val context1 = VWOUserContext().apply { id = "KaustubhVWO" }
        val context2 = VWOUserContext().apply { id = "RandomUserVWO" }

        val flag1 = getFlag(featureKey, context1)
        val flag2 = getFlag(featureKey, context2)

        assertNotNull("Flag 1 should not be null", flag1)
        assertNotNull("Flag 2 should not be null", flag2)

        assertNotEquals(
            "Users with different IDs should get different variations",
            flag1?.getVariables(),
            flag2?.getVariables()
        )
    }

    /**
     * Case 2: Bucketing Seed Provided.
     * Two different users ('KaustubhVWO', 'RandomUserVWO') are provided with the SAME bucketingSeed.
     * Since the seed is identical, they MUST get the same variation.
     */
    @Test
    fun testSameSeedProducesSameVariation() {
        initVWO()

        val sameBucketingSeed = "common-seed-123"

        val context1 = VWOUserContext().apply {
            id = "KaustubhVWO"
            bucketingSeed = sameBucketingSeed
        }
        val context2 = VWOUserContext().apply {
            id = "RandomUserVWO"
            bucketingSeed = sameBucketingSeed
        }

        val flag1 = getFlag(featureKey, context1)
        val flag2 = getFlag(featureKey, context2)

        assertNotNull("Flag 1 should not be null", flag1)
        assertNotNull("Flag 2 should not be null", flag2)

        assertEquals(
            "Users with same seed should get same variables",
            flag1?.getVariables(),
            flag2?.getVariables()
        )
    }

    /**
     * Case 3: No Seed provided -> Fallback to UserID.
     * The getFlag call does NOT provide a seed.
     * The SDK should fallback to using the User ID for bucketing.
     * Different users should get different variations.
     */
    @Test
    fun testFallbackToUserIdWhenSeedNotProvided() {
        initVWO()

        val context1 = VWOUserContext().apply { id = "KaustubhVWO" }
        val context2 = VWOUserContext().apply { id = "RandomUserVWO" }

        val flag1 = getFlag(featureKey, context1)
        val flag2 = getFlag(featureKey, context2)

        assertNotNull("Flag 1 should not be null", flag1)
        assertNotNull("Flag 2 should not be null", flag2)

        assertNotEquals(
            "Different users without seed should get different variations",
            flag1?.getVariables(),
            flag2?.getVariables()
        )
    }

    /**
     * Case 4: Different Seeds.
     * The SAME User ID is used, but with DIFFERENT bucketing seeds.
     * Since we use seeds known to produce different results ('KaustubhVWO' vs 'RandomUserVWO'),
     * the outcomes should differ.
     */
    @Test
    fun testDifferentSeedsProduceDifferentVariations() {
        initVWO()

        val context1 = VWOUserContext().apply {
            id = "sameId"
            bucketingSeed = "KaustubhVWO"
        }
        val context2 = VWOUserContext().apply {
            id = "sameId"
            bucketingSeed = "RandomUserVWO"
        }

        val flag1 = getFlag(featureKey, context1)
        val flag2 = getFlag(featureKey, context2)

        assertNotNull("Flag 1 should not be null", flag1)
        assertNotNull("Flag 2 should not be null", flag2)

        assertNotEquals(
            "Same user with different seeds should get different variations",
            flag1?.getVariables(),
            flag2?.getVariables()
        )
    }

    /**
     * Case 5: Empty String Seed.
     * bucketingSeed is provided but it's an empty string.
     * Empty string should fall back to userId. Different users should get different variations.
     */
    @Test
    fun testEmptyStringSeedFallsBackToUserId() {
        initVWO()

        val context1 = VWOUserContext().apply {
            id = "KaustubhVWO"
            bucketingSeed = ""
        }
        val context2 = VWOUserContext().apply {
            id = "RandomUserVWO"
            bucketingSeed = ""
        }

        val flag1 = getFlag(featureKey, context1)
        val flag2 = getFlag(featureKey, context2)

        assertNotNull("Flag 1 should not be null", flag1)
        assertNotNull("Flag 2 should not be null", flag2)

        assertNotEquals(
            "Users with empty seed should fall back to userId and get different variations",
            flag1?.getVariables(),
            flag2?.getVariables()
        )
    }

    // Helper methods

    /**
     * Initialises the VWO SDK using [CUSTOM_BUCKETING_SEED_SETTINGS] — the default settings file
     * used by the majority of tests in this class.
     */
    private fun initVWO() {
        initVWOWithSettings("CUSTOM_BUCKETING_SEED_SETTINGS")
    }

    /**
     * Initialises the VWO SDK using the settings JSON identified by [settingsKey].
     *
     * Shared by tests that need a different settings file (e.g. [SETTINGS_WITH_SAME_SALT]).
     *
     * @param settingsKey Key into [DummySettingsReader.settingsMap] for the desired settings JSON.
     * @param aliasingEnabled When `true`, sets [VWOInitOptions.isAliasingEnabled] on the options
     *   so that the aliasing code path is exercised.  Because no gateway URL is configured in
     *   unit-test scope, alias resolution will fall back to [VWOUserContext.id], which is exactly
     *   what we need to validate the bucketing-seed override behaviour without a real gateway.
     */
    private fun initVWOWithSettings(settingsKey: String, aliasingEnabled: Boolean = false) {
        val vwoInitOptions = VWOInitOptions().apply {
            this.sdkKey = this@CustomBucketingSeedTest.sdkKey
            this.accountId = this@CustomBucketingSeedTest.accountId
            this.isUsageStatsDisabled = true
            this.isAliasingEnabled = aliasingEnabled
        }

        val vwoBuilder = VWOBuilder(vwoInitOptions)
        val vwoBuilderSpy = spy(vwoBuilder)
        val settings = settingsReader.settingsMap[settingsKey]
        settings?.let {
            whenever(vwoBuilderSpy.getSettings(false)).thenReturn(settings)
        }
        vwoInitOptions.vwoBuilder = vwoBuilderSpy

        val latch = CountDownLatch(1)
        VWO.init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                this@CustomBucketingSeedTest.vwo = vwo
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                println("VWO init failed: $message")
                latch.countDown()
            }
        })

        latch.await(10, TimeUnit.SECONDS)
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

    // -------------------------------------------------------------------------
    // Group: getFlag with aliasing enabled and bucketing seed
    // -------------------------------------------------------------------------
    //
    // 'RandomUserVWO' and 'WingifyVWO' are known to hash into DIFFERENT variations
    // when no bucketing seed is used — they serve as the baseline for these tests.
    //
    // Note: Full gateway-based alias resolution is not available in JVM unit tests.
    // When isAliasingEnabled = true but no gatewayService URL is configured, the SDK
    // logs a warning and falls back to VWOUserContext.id.  We therefore use the
    // resolved user IDs directly in the context to validate the same behavioural
    // contract: the bucketing seed takes precedence over the (resolved) user ID.
    // -------------------------------------------------------------------------

    /**
     * Case 6: Aliasing + same bucketing seed.
     *
     * 'RandomUserVWO' and 'WingifyVWO' would normally bucket into DIFFERENT variations.
     * When both calls share the SAME bucketingSeed the seed overrides the user ID for
     * bucketing, so both must resolve to the SAME variation.
     */
    @Test
    fun testAliasingEnabled_SameSeedOverridesUserIdAndProducesSameVariation() {
        initVWOWithSettings("CUSTOM_BUCKETING_SEED_SETTINGS", aliasingEnabled = true)

        val sameBucketingSeed = "shared-seed-abc"

        val context1 = VWOUserContext().apply {
            id = "RandomUserVWO"
            bucketingSeed = sameBucketingSeed
        }
        val context2 = VWOUserContext().apply {
            id = "WingifyVWO"
            bucketingSeed = sameBucketingSeed
        }

        val flag1 = getFlag(featureKey, context1)
        val flag2 = getFlag(featureKey, context2)

        assertNotNull("Flag 1 should not be null", flag1)
        assertNotNull("Flag 2 should not be null", flag2)

        assertEquals(
            "With the same bucketing seed, users that differ in userId must get the same variation",
            flag1?.getVariables(),
            flag2?.getVariables()
        )
    }

    /**
     * Case 7: Aliasing enabled, no bucketing seed.
     *
     * Without a seed, bucketing falls back to the resolved user ID.
     * 'RandomUserVWO' and 'WingifyVWO' hash into different buckets, so they
     * must receive DIFFERENT variations.
     */
    @Test
    fun testAliasingEnabled_WithoutSeedDifferentResolvedUserIdsGetDifferentVariations() {
        initVWOWithSettings("CUSTOM_BUCKETING_SEED_SETTINGS", aliasingEnabled = true)

        val context1 = VWOUserContext().apply { id = "RandomUserVWO" }
        val context2 = VWOUserContext().apply { id = "WingifyVWO" }

        val flag1 = getFlag(featureKey, context1)
        val flag2 = getFlag(featureKey, context2)

        assertNotNull("Flag 1 should not be null", flag1)
        assertNotNull("Flag 2 should not be null", flag2)

        assertNotEquals(
            "Without a bucketing seed, RandomUserVWO and WingifyVWO must get different variations",
            flag1?.getVariables(),
            flag2?.getVariables()
        )
    }

    // -------------------------------------------------------------------------
    // Group: getFlag with custom salt and bucketing seed combinations
    // -------------------------------------------------------------------------

    /**
     * Case 8: No bucketing seed, same salt across both features.
     *
     * For each of 10 users, 'feature1' and 'feature2' share the same campaign salt,
     * so bucketing for both features uses the same hash.  Each user may land on a
     * different variation (random distribution), but for any single user both
     * features must always return the SAME variation.
     */
    @Test
    fun testNoSeedSameSalt_EachUserGetsSameVariationAcrossBothFeatures() {
        initVWOWithSettings("SETTINGS_WITH_SAME_SALT")

        for (i in 1..10) {
            val userId = "user$i"
            val context1 = VWOUserContext().apply { id = userId }
            val context2 = VWOUserContext().apply { id = userId }

            val flag1 = getFlag("feature1", context1)
            val flag2 = getFlag("feature2", context2)

            assertNotNull("Flag 1 for $userId should not be null", flag1)
            assertNotNull("Flag 2 for $userId should not be null", flag2)

            assertEquals(
                "$userId: both features with the same salt must yield the same variation",
                flag1?.getVariables(),
                flag2?.getVariables()
            )
        }
    }

    /**
     * Case 9: Same bucketing seed AND same salt across both features.
     *
     * All 10 users share an identical bucketingSeed.  Because the seed fully
     * determines the bucket value, every user must land on the SAME variation —
     * and that variation must also be identical across 'feature1' and 'feature2'
     * (which share the same salt).
     */
    @Test
    fun testSeedAndSameSalt_AllUsersGetSameVariationAcrossFeaturesAndAmongEachOther() {
        initVWOWithSettings("SETTINGS_WITH_SAME_SALT")

        val commonBucketingSeed = "common_seed_456"
        val variationsAssigned = mutableSetOf<String>()

        for (i in 1..10) {
            val userId = "user$i"
            val context1 = VWOUserContext().apply {
                id = userId
                bucketingSeed = commonBucketingSeed
            }
            val context2 = VWOUserContext().apply {
                id = userId
                bucketingSeed = commonBucketingSeed
            }

            val flag1 = getFlag("feature1", context1)
            val flag2 = getFlag("feature2", context2)

            assertNotNull("Flag 1 for $userId should not be null", flag1)
            assertNotNull("Flag 2 for $userId should not be null", flag2)

            assertEquals(
                "$userId: both features with the same seed and salt must yield the same variation",
                flag1?.getVariables(),
                flag2?.getVariables()
            )

            flag1?.getVariables()?.let { variationsAssigned.add(it.toString()) }
        }

        assertEquals(
            "With an identical bucketing seed, all 10 users must land on the same variation",
            1,
            variationsAssigned.size
        )
    }

    // -------------------------------------------------------------------------
    // Group: getFlag with forced variation (whitelisting) and bucketing seed
    // -------------------------------------------------------------------------
    //
    // In CUSTOM_BUCKETING_SEED_SETTINGS, 'forcedWingify' is whitelisted to
    // Variation-2 (variable value: 'var2').
    // -------------------------------------------------------------------------

    /**
     * Case 10: Forced variation without bucketing seed.
     *
     * A whitelisted user must always receive the forced variation regardless of
     * normal bucketing — even when no bucketingSeed is supplied.
     */
    @Test
    fun testForcedVariationWithoutBucketingSeed() {
        initVWO()

        val context = VWOUserContext().apply { id = "forcedWingify" }
        val flag = getFlag(featureKey, context)

        assertNotNull("Flag for forcedWingify should not be null", flag)
        assertTrue(
            "forcedWingify must receive Variation-2 (value: var2) even without a bucketing seed",
            flag?.getVariables()?.any { it["value"] == "var2" } == true
        )
    }

    /**
     * Case 11: Forced variation with bucketing seed present.
     *
     * A bucketing seed must NOT override whitelisting — the forced variation
     * takes precedence and the user must still receive Variation-2 (value: 'var2').
     */
    @Test
    fun testForcedVariationWithBucketingSeedStillReturnsWhitelistedVariation() {
        initVWO()

        val context = VWOUserContext().apply {
            id = "forcedWingify"
            bucketingSeed = "some-seed-xyz"
        }
        val flag = getFlag(featureKey, context)

        assertNotNull("Flag for forcedWingify should not be null", flag)
        assertTrue(
            "forcedWingify must receive Variation-2 (value: var2) even when a bucketing seed is present",
            flag?.getVariables()?.any { it["value"] == "var2" } == true
        )
    }
}
