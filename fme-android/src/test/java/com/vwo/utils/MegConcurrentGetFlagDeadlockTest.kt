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
package com.vwo.utils

import android.os.Build
import com.vwo.sdk.fme.BuildConfig
import com.vwo.testcases.StorageTest
import com.wingify.api.GetFlagAPI
import com.wingify.ServiceContainer
import com.wingify.constants.Constants.PLATFORM
import com.wingify.enums.CampaignTypeEnum
import com.wingify.models.Campaign
import com.wingify.models.Feature
import com.wingify.models.HoldoutGroup
import com.wingify.models.Rule
import com.wingify.models.Settings
import com.wingify.models.Variation
import com.wingify.models.user.WingifyInitOptions
import com.wingify.packages.storage.Connector
import com.wingify.packages.storage.Storage
import com.wingify.providers.StorageProvider
import com.wingify.services.SettingsManager
import com.wingify.services.StorageService
import com.wingify.utils.GsonUtil
import com.wingify.utils.ImpressionUtil
import com.wingify.utils.SettingsUtil
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

/**
 * Documents and reproduces nested per-feature lock ordering from concurrent MEG sibling [GetFlagAPI.getFlag]
 * calls (same entry points as [com.wingify.WingifyClient.getFlag] / [com.wingify.WingifyClient.fetchFlag]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MegConcurrentGetFlagDeadlockTest {

    private companion object {
        const val FEATURE1_KEY = "feature1"
        const val FEATURE2_KEY = "feature2"
        const val USER_ID = "meg_nested_lock_user"
        const val HOLDOUT_ON_FEATURE1_ID = 801
        const val HOLDOUT_ON_FEATURE2_ID = 802
        const val SIBLING_ROLLOUT_CAMPAIGN_ID = 99
        const val MEG_SIBLING_CAMPAIGN_ID = 98
        const val ROLLOUT_VARIATION_ID = 1
        const val JOIN_TIMEOUT_MS = 8_000L
    }

    private lateinit var settings: Settings
    private lateinit var storage: StorageTest
    private lateinit var serviceContainer: ServiceContainer

    @Before
    fun setup() {
        StorageProvider.userAgent =
            "VWO FME $PLATFORM ${BuildConfig.SDK_VERSION} ($PLATFORM/${Build.VERSION.RELEASE})"
        settings = megSiblingSettingsWithSelectiveHoldouts()
        storage = StorageTest()
        serviceContainer = newServiceContainer(storage)
        mockkObject(ImpressionUtil)
        every {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(ImpressionUtil)
    }

    /**
     * End-to-end [GetFlagAPI.getFlag] on both MEG siblings with stored rollouts (skips the pre-rollout
     * holdout gate) and selective holdouts so MegUtil pass 1 can persist holdout on the sibling key.
     *
     * **Settings**
     * - [FEATURE1_KEY] and [FEATURE2_KEY] share MEG group 1 ([MegWhitelistDeferredImpressionTest] wiring).
     * - Holdout H802 applies only to feature id 2; H801 only to feature id 1.
     *
     * **Sticky rollout (both keys, same user)**
     * - Rollout fields in storage enable the flag and set `shouldCheckForExperimentsRules` without running
     *   `if (!getFlag.isEnabled())` holdout exclusion ([GetFlagAPI] stored-rollout branch).
     *
     * **Thread A — `getFlag(FEATURE1_KEY)`**
     * 1. `serviceContainer.withFlagLock(FEATURE1_KEY, user)` for the whole evaluation.
     * 2. Stored rollout → experiment rules → [com.wingify.utils.DecisionUtil] → [MegUtil.evaluateGroups].
     * 3. MegUtil pass 1: for [FEATURE2_KEY], holdout H802 qualifies →
     *    `StorageDecorator.setDataInStorage` → `StorageService.setDataInStorage` →
     *    `withFlagLock(FEATURE2_KEY, user)` while still holding the FEATURE1 lock.
     *
     * **Thread B — `getFlag(FEATURE2_KEY)`** (mirror)
     * 1. Holds `withFlagLock(FEATURE2_KEY, user)`.
     * 2–3. MegUtil pass 1 persists holdout for [FEATURE1_KEY] via H801 → nested `withFlagLock(FEATURE1_KEY)`.
     *
     * **Deadlock (A→B, B→A)**
     * - Thread A: holds FEATURE1, waits FEATURE2; Thread B: holds FEATURE2, waits FEATURE1.
     * - [com.wingify.api.GetFlagsAPI] batches MEG keys sequentially on one thread, so a single `getFlags`
     *   call does not interleave two outer locks; parallel `getFlag` / `fetchFlag` on siblings does.
     *
     * **Nested lock (MegUtil pass 1 holdout persist — same mutex order as getFlag)**
     * - Outer: `GetFlagAPI.getFlag` → `withFlagLock(callerFeatureKey, user)` for the whole evaluation.
     * - Inner: `StorageDecorator.setDataInStorage` → `StorageService.setDataInStorage` →
     *   `withFlagLock(siblingFeatureKey, user)` while the outer lock is still held.
     *
     * The test body uses the same [withFlagLocksForEvaluation] + nested [StorageService.setDataInStorage]
     * pattern as production getFlag (sorted MEG locks, reentrant inner lock).
     */
    @Test
    fun concurrentGetFlagOnMegSiblings_completesWithoutCrossFeatureLockDeadlock() {
        val storageService = StorageService(serviceContainer)
        val barrier = CyclicBarrier(2)
        val errors = CopyOnWriteArrayList<Throwable>()

        fun megPass1HoldoutPersistOnSibling(callerFeatureKey: String, siblingFeatureKey: String) {
            try {
                barrier.await(5, TimeUnit.SECONDS)
                serviceContainer.withFlagLocksForEvaluation(settings, callerFeatureKey, USER_ID) {
                    storageService.setDataInStorage(
                        mapOf(
                            "featureKey" to siblingFeatureKey,
                            "userId" to USER_ID,
                            "holdoutId" to listOf(
                                if (siblingFeatureKey == FEATURE2_KEY) {
                                    HOLDOUT_ON_FEATURE2_ID
                                } else {
                                    HOLDOUT_ON_FEATURE1_ID
                                },
                            ),
                            "holdout" to true,
                        ),
                    )
                }
            } catch (t: Throwable) {
                errors.add(t)
            }
        }

        val threadFeature1 = Thread(
            { megPass1HoldoutPersistOnSibling(FEATURE1_KEY, FEATURE2_KEY) },
            "getFlag-feature1",
        )
        val threadFeature2 = Thread(
            { megPass1HoldoutPersistOnSibling(FEATURE2_KEY, FEATURE1_KEY) },
            "getFlag-feature2",
        )
        threadFeature1.start()
        threadFeature2.start()
        threadFeature1.join(JOIN_TIMEOUT_MS)
        threadFeature2.join(JOIN_TIMEOUT_MS)

        assertTrue(
            "Uncaught errors: ${errors.joinToString { it.message ?: it.toString() }}",
            errors.isEmpty(),
        )
        assertTrue(
            "Thread '${threadFeature1.name}' did not finish within ${JOIN_TIMEOUT_MS}ms. " +
                "Deadlock: holds withFlagLock($FEATURE1_KEY) then waits withFlagLock($FEATURE2_KEY) " +
                "via StorageService.setDataInStorage (MegUtil MEG pass-1 sibling holdout persist under getFlag).",
            !threadFeature1.isAlive,
        )
        assertTrue(
            "Thread '${threadFeature2.name}' did not finish within ${JOIN_TIMEOUT_MS}ms " +
                "(mirror: holds $FEATURE2_KEY, waits $FEATURE1_KEY).",
            !threadFeature2.isAlive,
        )
    }

    private fun newServiceContainer(storageConnector: Connector): ServiceContainer {
        val options = WingifyInitOptions().apply {
            accountId = settings.accountId
            sdkKey = "test-sdk-key"
            isUsageStatsDisabled = true
        }
        options.storage = storageConnector
        return ServiceContainer(
            settingsManager = SettingsManager(options),
            options = options,
            settings = settings,
            loggerService = null,
        ).apply {
            this.storage = Storage().apply { attachConnector(storageConnector) }
        }
    }

    private fun megSiblingSettingsWithSelectiveHoldouts(): Settings {
        val reader = DummySettingsReader()
        val meg = GsonUtil.gson.fromJson(
            reader.settingsMap["MEG_CAMPAIGN_RANDOM_ALGO_SETTINGS"],
            Settings::class.java,
        )
        val forced = GsonUtil.gson.fromJson(
            reader.settingsMap["ROLLOUT_FORCE_ON_OFF_0_SETTINGS"],
            Settings::class.java,
        )

        meg.features.forEach { it.impactCampaign.campaignId = null }

        val siblingRollout = forced.campaigns!!.first().apply { id = SIBLING_ROLLOUT_CAMPAIGN_ID }
        val sibling = Feature().apply {
            key = FEATURE2_KEY
            name = "Feature2"
            id = 2
            status = "ON"
            type = "FEATURE_FLAG"
            impactCampaign.campaignId = null
            rules = listOf(
                Rule().apply {
                    ruleKey = "rolloutRule1"
                    type = CampaignTypeEnum.ROLLOUT.value
                    campaignId = SIBLING_ROLLOUT_CAMPAIGN_ID
                    variationId = ROLLOUT_VARIATION_ID
                },
                Rule().apply {
                    ruleKey = "testingRule1"
                    type = CampaignTypeEnum.AB.value
                    campaignId = MEG_SIBLING_CAMPAIGN_ID
                },
            )
        }
        val groupPlaceholder = Campaign().apply {
            id = MEG_SIBLING_CAMPAIGN_ID
            key = "feature2_testing"
            name = "feature2_testing"
            type = CampaignTypeEnum.AB.value
            status = "RUNNING"
            percentTraffic = 100
            isForcedVariationEnabled = false
            segments = emptyMap()
            variations = listOf(Variation().apply { id = 1 })
        }

        meg.accountId = forced.accountId
        meg.features = meg.features + sibling
        meg.campaigns = meg.campaigns.orEmpty() + siblingRollout + groupPlaceholder
        val group = meg.groups!!["1"]!!
        group.campaigns = group.campaigns.orEmpty() + MEG_SIBLING_CAMPAIGN_ID.toString()
        meg.campaignGroups = meg.campaignGroups.orEmpty() + (MEG_SIBLING_CAMPAIGN_ID.toString() to 1)
        meg.holdoutGroups = listOf(
            holdoutForFeatureIds(
                id = HOLDOUT_ON_FEATURE2_ID,
                name = "Holdout on feature2 only",
                featureIds = listOf(2),
            ),
            holdoutForFeatureIds(
                id = HOLDOUT_ON_FEATURE1_ID,
                name = "Holdout on feature1 only",
                featureIds = listOf(1),
            ),
        )
        SettingsUtil.processSettings(meg)
        return meg
    }

    private fun holdoutForFeatureIds(
        id: Int,
        name: String,
        featureIds: List<Int>,
    ): HoldoutGroup =
        HoldoutGroup().apply {
            this.id = id
            this.name = name
            isGlobal = false
            this.featureIds = featureIds
            trafficPercent = 100
            segments = mapOf(
                "or" to listOf(
                    mapOf("os" to "wildcard(*android*)"),
                ),
            )
            metrics = listOf(
                HoldoutGroup.Metrics().apply {
                    type = "CUSTOM_GOAL"
                    this.id = 1
                    identifier = "holdout_metric_$id"
                },
            )
        }
}
