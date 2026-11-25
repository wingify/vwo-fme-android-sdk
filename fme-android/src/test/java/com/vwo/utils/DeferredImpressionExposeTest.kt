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
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.storage.Connector
import com.vwo.sdk.fme.BuildConfig
import com.vwo.testcases.StorageTest
import com.vwo.utils.DummySettingsReader
import com.wingify.ServiceContainer
import com.wingify.WingifyBuilder
import com.wingify.WingifyClient
import com.wingify.api.GetFlagAPI
import com.wingify.api.GetFlagsAPI
import com.wingify.constants.Constants
import com.wingify.constants.Constants.PLATFORM
import com.wingify.models.HoldoutGroup
import com.wingify.models.Settings
import com.wingify.packages.decision_maker.DecisionMaker
import com.wingify.models.impression.ImpressionPayload
import com.wingify.models.user.WingifyInitOptions
import com.wingify.models.user.WingifyUserContext
import com.wingify.packages.storage.Storage
import com.wingify.providers.StorageProvider
import com.wingify.services.SettingsManager
import com.wingify.services.StorageService
import com.wingify.utils.GsonUtil
import com.wingify.utils.ImpressionExposureHelper
import com.wingify.utils.ImpressionUtil
import com.wingify.utils.KeyedLockUtil
import com.wingify.utils.SettingsUtil
import com.wingify.utils.UserTrackingUsageUtil
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DeferredImpressionExposeTest {

    private lateinit var settings: Settings
    private lateinit var serviceContainer: ServiceContainer
    private lateinit var context: WingifyUserContext
    private lateinit var storage: StorageTest

    @Before
    fun setup() {
        StorageProvider.userAgent =
            "VWO FME $PLATFORM ${BuildConfig.SDK_VERSION} ($PLATFORM/${Build.VERSION.RELEASE})"

        val reader = DummySettingsReader()
        settings = GsonUtil.gson.fromJson(
            reader.settingsMap["BASIC_ROLLOUT_SETTINGS"],
            Settings::class.java,
        )
        // Ensure rebuild-with-impact coverage.
        settings.features.first().impactCampaign.campaignId = 9001
        SettingsUtil.processSettings(settings)

        val options = VWOInitOptions().apply {
            accountId = 123456
            sdkKey = "test-sdk-key"
            isUsageStatsDisabled = true
        }
        storage = StorageTest()
        serviceContainer = createServiceContainer(settings, options, storage)
        context = WingifyUserContext().apply { id = "defer-user-1" }

        mockkObject(ImpressionUtil)
        every {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(ImpressionUtil)
    }

    private fun createServiceContainer(
        settings: Settings,
        options: WingifyInitOptions,
        connector: Connector,
    ): ServiceContainer {
        options.storage = connector
        val container = ServiceContainer(
            settingsManager = SettingsManager(options),
            options = options,
            settings = settings,
            loggerService = null,
        )
        container.storage = Storage().apply {
            attachConnector(connector)
        }
        return container
    }

    private fun evaluate(shouldSend: Boolean): GetFlag {
        return GetFlagAPI.getFlag(
            "feature1",
            settings,
            context.clone(),
            serviceContainer,
            serviceContainer.getHooksManager(),
            shouldSend,
        )
    }

    private fun storedVariationShownSent(): Boolean? {
        val map = StorageService(serviceContainer).getDataInStorage("feature1", context)
        return map?.get(Constants.KEY_VARIATION_SHOWN_SENT) as? Boolean
    }

    /** Seeds a pre-feature sticky decision: no `variationShownSent` key at all. */
    private fun seedLegacySticky() {
        storage.set(
            mapOf(
                "featureKey" to "feature1",
                "userId" to context.id!!,
                "rolloutId" to 1,
                "rolloutKey" to "feature1_rolloutRule1",
                "rolloutVariationId" to 1,
            )
        )
    }

    /** Seeds a deferred rollout stickiness for [userId], as a `fetchFlag` would have written it. */
    private fun seedSticky(userId: String) {
        storage.set(
            mapOf(
                "featureKey" to "feature1",
                "userId" to userId,
                "rolloutId" to 1,
                "rolloutKey" to "feature1_rolloutRule1",
                "rolloutVariationId" to 1,
                Constants.KEY_VARIATION_SHOWN_SENT to false,
            )
        )
    }

    private val mauCalls = mutableListOf<String>()

    private fun withUsageTracking(block: () -> Unit) {
        settings.isUsageTracked = true
        mauCalls.clear()
        mockkObject(UserTrackingUsageUtil)
        every {
            UserTrackingUsageUtil.sendTrackingUsage(any(), any(), any(), matchNullable { true }, any())
        } answers { mauCalls.add("mau") }
        try {
            block()
        } finally {
            unmockkObject(UserTrackingUsageUtil)
        }
    }

    private fun verifyUsageNotSent() {
        verify(exactly = 0) {
            UserTrackingUsageUtil.sendTrackingUsage(any(), any(), any(), matchNullable { true }, any())
        }
    }

    @Test
    fun `default getFlag enqueues variationShown and marks sent`() {
        val flag = evaluate(shouldSend = true)
        assertTrue(flag.isEnabled())
        verify(atLeast = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
        assertEquals(true, storedVariationShownSent())
    }

    @Test
    fun `fetch stashes the impression but still tracks usage`() {
        withUsageTracking {
            val deferred = evaluate(shouldSend = false)
            assertTrue(deferred.hasPendingImpressions())
            // A stashed impression never reached the server, so it cannot cover MAU billing.
            assertEquals(1, mauCalls.size)

            // exposeResult sends the impression but must not add a second MAU event.
            assertTrue(deferred.exposeResult())
            assertEquals(1, mauCalls.size)
        }
    }

    @Test
    fun `getFlag that dispatched an impression does not track usage`() {
        withUsageTracking {
            val flag = evaluate(shouldSend = true)
            assertFalse(flag.hasPendingImpressions())
            assertEquals(0, mauCalls.size)
        }
    }

    @Test
    fun `follow-up getFlag on a deferred decision tracks usage each evaluation`() {
        withUsageTracking {
            val deferred = evaluate(shouldSend = false)
            assertEquals(1, mauCalls.size)

            evaluate(shouldSend = true)
            // Campaign-level send-once blocks the repeat impression, so this call also
            // dispatched nothing and is billed through MAU. Server dedupes per user.
            assertEquals(2, mauCalls.size)

            assertTrue(deferred.exposeResult())
            assertEquals(2, mauCalls.size)
        }
    }

    @Test
    fun `sent variationShown skips usage event`() {
        withUsageTracking {
            evaluate(shouldSend = true)
            verify(atLeast = 1) {
                ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
            }
            verifyUsageNotSent()
        }
    }

    @Test
    fun `evaluation with nothing to show sends usage event`() {
        withUsageTracking {
            GetFlagAPI.getFlag(
                "missing-feature",
                settings,
                context.clone(),
                serviceContainer,
                serviceContainer.getHooksManager(),
                false,
            )
            verify(exactly = 1) {
                UserTrackingUsageUtil.sendTrackingUsage(any(), any(), any(), matchNullable { true }, any())
            }
        }
    }

    @Test
    fun `fetch defer then expose sends once and second expose is no-op`() {
        val deferred = evaluate(shouldSend = false)
        assertTrue(deferred.isEnabled())
        verify(exactly = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
        assertEquals(false, storedVariationShownSent())
        assertTrue(deferred.hasPendingImpressions())

        assertTrue(deferred.exposeResult())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
        assertEquals(true, storedVariationShownSent())

        assertFalse(deferred.exposeResult())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
    }

    @Test
    fun `fetch after expose keeps variationShownSent true`() {
        val deferred = evaluate(shouldSend = false)
        assertEquals(false, storedVariationShownSent())
        assertTrue(deferred.exposeResult())
        assertEquals(true, storedVariationShownSent())

        evaluate(shouldSend = false)
        assertEquals(true, storedVariationShownSent())
    }

    @Test
    fun `fetch after sent holdout keeps variationShownSent true`() {
        val holdoutId = 501
        settings.holdoutGroups = listOf(
            HoldoutGroup().apply {
                id = holdoutId
                name = "holdout"
                trafficPercent = 10
                isGlobal = false
                featureIds = listOfNotNull(settings.features.first().id)
            }
        )
        val decisionMaker = DecisionMaker()
        context.id = (1..500).asSequence().map { "holdout-sent-$it" }.first { userId ->
            val bucket = decisionMaker.getBucketValueForUser(
                "${settings.accountId}_${holdoutId}_$userId"
            )
            bucket != 0 && bucket <= 10
        }

        val sent = evaluate(shouldSend = true)
        assertFalse(sent.isEnabled())
        assertEquals(true, storedVariationShownSent())

        val fetched = evaluate(shouldSend = false)
        assertFalse(fetched.isEnabled())
        assertEquals(true, storedVariationShownSent())
    }

    @Test
    fun `fetch stashes a new holdout row on a sent sticky and expose sends it`() {
        assertTrue(evaluate(shouldSend = true).isEnabled())
        assertEquals(true, storedVariationShownSent())
        clearMocks(ImpressionUtil, answers = false)

        settings.holdoutGroups = listOf(
            HoldoutGroup().apply {
                id = 777
                name = "new-holdout"
                trafficPercent = 100
                isGlobal = true
                featureIds = listOfNotNull(settings.features.first().id)
            }
        )
        SettingsUtil.processSettings(settings)

        // Fetch stashes the new not-in-holdout row while the sticky stays sent.
        val fetched = evaluate(shouldSend = false)
        assertTrue(fetched.hasPendingImpressions())
        assertEquals(true, storedVariationShownSent())

        // The undetached stash must still reach the server, exactly once.
        val sent = slot<ImpressionPayload>()
        assertTrue(fetched.exposeResult())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(
                any(), capture(sent), any(), any()
            )
        }
        assertTrue(
            "sent rows: ${sent.captured.impressionList.map { it.campaignId }}",
            sent.captured.impressionList.any { it.campaignId == 777 },
        )
        assertEquals(true, storedVariationShownSent())
        assertFalse(fetched.exposeResult())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
    }

    @Test
    fun `getFlags defer then collection expose sends once and second expose is no-op`() {
        val flag = GetFlagsAPI.getFlags(
            arrayOf("feature1"),
            settings,
            context.clone(),
            serviceContainer,
            shouldSendImpressionToServer = false,
        ).get("feature1")
        assertTrue(flag.isEnabled())
        verify(exactly = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
        assertEquals(false, storedVariationShownSent())

        assertTrue(flag.exposeResult())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
        assertEquals(true, storedVariationShownSent())

        assertFalse(flag.exposeResult())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
    }

    @Test
    fun `deferred holdout stays unsent across a later getFlag`() {
        val holdoutId = 501
        settings.holdoutGroups = listOf(
            HoldoutGroup().apply {
                id = holdoutId
                name = "holdout"
                trafficPercent = 10
                isGlobal = false
                featureIds = listOfNotNull(settings.features.first().id)
            }
        )
        val decisionMaker = DecisionMaker()
        context.id = (1..500).asSequence().map { "holdout-user-$it" }.first { userId ->
            val bucket = decisionMaker.getBucketValueForUser(
                "${settings.accountId}_${holdoutId}_$userId"
            )
            bucket != 0 && bucket <= 10
        }

        val deferred = evaluate(shouldSend = false)
        assertFalse(deferred.isEnabled())
        assertEquals(false, storedVariationShownSent())
        verify(exactly = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }

        val again = evaluate(shouldSend = true)
        verify(exactly = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
        assertEquals(false, storedVariationShownSent())

        val sent = slot<ImpressionPayload>()
        assertTrue(again.exposeResult())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), capture(sent), any(), any())
        }
        val ids = sent.captured.impressionList.map { it.campaignId }.toSet()
        assertTrue("holdout must be sent, got $ids", ids.contains(holdoutId))
        assertFalse("holdout-only must not send impact, got $ids", ids.contains(9001))
        assertEquals(true, storedVariationShownSent())
    }

    @Test
    fun `legacy holdout later getFlag still sends newly added holdout`() {
        // Pre-fetchFlag storage has no variationShownSent key. Later getFlag must still
        // auto-send newly added holdouts (old happy path). Only explicit false is deferred.
        val storedHoldoutId = 501
        val newHoldoutId = 502
        val featureId = settings.features.first().id
        settings.features.first().impactCampaign.campaignId = null
        settings.holdoutGroups = listOf(
            HoldoutGroup().apply {
                id = storedHoldoutId
                name = "legacy-holdout"
                trafficPercent = 100
                isGlobal = false
                featureIds = listOfNotNull(featureId)
            },
            HoldoutGroup().apply {
                id = newHoldoutId
                name = "new-holdout"
                trafficPercent = 100
                isGlobal = false
                featureIds = listOfNotNull(featureId)
            },
        )
        storage.set(
            mapOf(
                "featureKey" to "feature1",
                "userId" to context.id!!,
                "holdoutIds" to listOf(storedHoldoutId),
            )
        )
        assertEquals(null, storedVariationShownSent())

        val sent = slot<ImpressionPayload>()
        val flag = evaluate(shouldSend = true)
        assertFalse(flag.isEnabled())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), capture(sent), any(), any())
        }
        val ids = sent.captured.impressionList.map { it.campaignId }.toSet()
        assertTrue("new holdout must auto-send, got $ids", ids.contains(newHoldoutId))
        assertFalse("sticky holdout must not resend, got $ids", ids.contains(storedHoldoutId))
    }

    @Test
    fun `cached awaiting expose does not auto-send on default getFlag`() {
        evaluate(shouldSend = false)
        assertEquals(false, storedVariationShownSent())

        val cached = evaluate(shouldSend = true)
        assertTrue(cached.isEnabled())
        verify(exactly = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
    }

    @Test
    fun `expose on later getFlag sends stored rollout and impact`() {
        val flag1 = evaluate(shouldSend = false)
        val rolloutId = StorageService(serviceContainer)
            .getDataInStorage("feature1", context)
            ?.get("rolloutId")
        assertTrue(flag1.hasPendingImpressions())
        assertEquals(false, storedVariationShownSent())

        val flag2 = evaluate(shouldSend = true)
        verify(exactly = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }

        val sent = slot<ImpressionPayload>()
        assertTrue(flag2.exposeResult())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), capture(sent), any(), any())
        }
        val rows = sent.captured.impressionList.joinToString { "campaign=${it.campaignId}" }
        val ids = sent.captured.impressionList.map { it.campaignId }.toSet()
        assertTrue("sent rows: $rows", ids.contains(rolloutId) && ids.contains(9001))
        assertEquals(true, storedVariationShownSent())
    }

    @Test
    fun `deferred record keeps a legacy sticky without a sent key unset`() {
        seedLegacySticky()
        val flag = GetFlag(context)
        ImpressionExposureHelper.record(
            settings = settings,
            impressionPayload = ImpressionPayload().apply { add(7, 2) },
            context = context,
            serviceContainer = serviceContainer,
            shouldSend = false,
            getFlag = flag,
            featureKey = "feature1",
            feature = settings.features.first(),
        )

        assertTrue(flag.hasPendingImpressions())
        // Legacy absent key stays absent; it must not be downgraded to an explicit false.
        assertEquals(null, storedVariationShownSent())
    }

    @Test
    fun `fetch keeps a legacy sticky without a sent key marked as sent`() {
        // Pre-feature storage: decision present, variationShownSent absent => already sent.
        seedLegacySticky()
        assertEquals(null, storedVariationShownSent())

        assertEquals(true, evaluate(shouldSend = false).isEnabled())
        // A fetch must not rewrite the legacy sticky to false.
        assertEquals(true, storedVariationShownSent())
    }

    @Test
    fun `legacy absent variationShownSent is treated as already sent on expose`() {
        seedLegacySticky()

        val flag = GetFlag(context).apply {
            setIsEnabled(true)
            attachExposeDeps(
                ImpressionExposureHelper.ExposeDeps(
                    settings = settings,
                    featureKey = "feature1",
                    context = context,
                    serviceContainer = serviceContainer,
                    isEnabled = true,
                )
            )
        }

        // Absent key + decision ⇒ already sent; expose must not enqueue.
        assertFalse(flag.exposeResult())
        verify(exactly = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
    }

    @Test
    fun `rebuild after stash miss includes impact`() {
        evaluate(shouldSend = false)
        val flag = evaluate(shouldSend = false)
        // Clear in-memory stash to force rebuild path.
        flag.consumePendingImpressions()
        assertFalse(flag.hasPendingImpressions())

        assertTrue(flag.exposeResult())
        verify(atLeast = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(
                any(),
                match { payload: ImpressionPayload ->
                    payload.impressionList.any { it.campaignId == 9001 }
                },
                any(),
                any(),
            )
        }
    }

    @Test
    fun `mid-eval stash merge keeps multiple impressions for expose`() {
        val flag = GetFlag(context)
        val first = ImpressionPayload().apply { add(1, 1) }
        val second = ImpressionPayload().apply { add(2, 2) }
        ImpressionExposureHelper.record(
            settings = settings,
            impressionPayload = first,
            context = context,
            serviceContainer = serviceContainer,
            shouldSend = false,
            getFlag = flag,
            featureKey = "feature1",
            feature = settings.features.first(),
        )
        ImpressionExposureHelper.record(
            settings = settings,
            impressionPayload = second,
            context = context,
            serviceContainer = serviceContainer,
            shouldSend = false,
            getFlag = flag,
            featureKey = "feature1",
            feature = settings.features.first(),
        )
        assertTrue(flag.hasPendingImpressions())
        assertTrue(flag.exposeResult())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(
                any(),
                match { payload: ImpressionPayload -> payload.size() >= 2 },
                any(),
                any(),
            )
        }
    }

    /** Rollout traffic comes from the first variation's weight, not `percentTraffic`. */
    private fun excludeUserFromRollout() {
        settings.features.first().rulesLinkedCampaign.first().variations!!.first().weight = 0.0
    }

    @Test
    fun `disabled fetch persists a record so the decision survives process death`() {
        excludeUserFromRollout()

        val flag = evaluate(shouldSend = false)

        assertFalse(flag.isEnabled())
        assertNotNull(
            "disabled flag must still write a sticky decision",
            StorageService(serviceContainer).getDataInStorage("feature1", context),
        )
        assertEquals(false, storedVariationShownSent())
    }

    @Test
    fun `expose by key rebuilds the disabled impact impression from storage`() {
        excludeUserFromRollout()

        // First session: fetch (defers), then drop the flag like a process death would.
        evaluate(shouldSend = false)

        // Second session: only the feature key + context are available.
        assertTrue(
            ImpressionExposureHelper.exposeByKey("feature1", context, serviceContainer),
        )
        verify(atLeast = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(
                any(),
                match { payload: ImpressionPayload ->
                    // Disabled → impact variation 1.
                    payload.impressionList.any { it.campaignId == 9001 && it.variationId == 1 }
                },
                any(),
                any(),
            )
        }
        assertEquals(true, storedVariationShownSent())
    }

    @Test
    fun `disabled default getFlag persists sent intent`() {
        excludeUserFromRollout()

        val flag = evaluate(shouldSend = true)

        assertFalse(flag.isEnabled())
        // Pins variationShownSentToStore() on the disabled write path: a raw
        // effectiveShouldSend() would also be true here, so this guards the sticky contract.
        assertEquals(true, storedVariationShownSent())
    }

    @Test
    fun `expose by key resolves a device id user`() {
        val app = RuntimeEnvironment.getApplication()
        android.provider.Settings.Secure.putString(
            app.contentResolver, android.provider.Settings.Secure.ANDROID_ID, "device-user-1",
        )
        StorageProvider.contextRef = WeakReference(app)
        serviceContainer.getInitOptions().context = app
        seedSticky(userId = "device-user-1")

        // The next process only has the caller's context: no explicit id.
        val sessionContext = WingifyUserContext().apply { shouldUseDeviceIdAsUserId = true }

        assertTrue(
            ImpressionExposureHelper.exposeByKey("feature1", sessionContext, serviceContainer),
        )
        assertNull("caller context must not be mutated", sessionContext.id)
        assertEquals(
            true,
            StorageService(serviceContainer)
                .getDataInStorage("feature1", sessionContext.apply { id = "device-user-1" })
                ?.get(Constants.KEY_VARIATION_SHOWN_SENT),
        )
    }

    @Test
    fun `enabled fetch still persists a record with deferred intent`() {
        val flag = evaluate(shouldSend = false)

        assertTrue(flag.isEnabled())
        assertNotNull(StorageService(serviceContainer).getDataInStorage("feature1", context))
        assertEquals(false, storedVariationShownSent())
    }

    @Test
    fun `client exposeResult rebuilds a disabled decision from a previous session`() {
        excludeUserFromRollout()
        evaluate(shouldSend = false)

        // A fresh client over the same storage stands in for the next process. Unique
        // account/sdk-key so the shared ServiceContainerProvider cache does not leak another
        // test's container into this one.
        val options = VWOInitOptions().apply {
            accountId = 987654
            sdkKey = "deferred-expose-key"
            isUsageStatsDisabled = true
        }
        options.storage = storage
        val builder = WingifyBuilder(options)
            .setLogger()
            .setSettingsManager()
            .setStorage()
            .setNetworkManager()
            .setSegmentation()
        val client = WingifyClient(
            GsonUtil.gson.toJson(settings),
            options,
            builder,
        )

        assertTrue(client.exposeResult("feature1", context))
        verify(atLeast = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(
                any(),
                match { payload: ImpressionPayload ->
                    payload.impressionList.any { it.campaignId == 9001 && it.variationId == 1 }
                },
                any(),
                any(),
            )
        }
    }

    @Test
    fun `disabled holdout-only sticky rebuild omits impact`() {
        val holdoutId = 501
        storage.set(
            mapOf(
                "featureKey" to "feature1",
                "userId" to context.id!!,
                Constants.Holdouts.KEY_STORAGE_HOLDOUT_IDS to listOf(holdoutId),
                Constants.KEY_VARIATION_SHOWN_SENT to false,
            )
        )

        assertTrue(ImpressionExposureHelper.exposeByKey("feature1", context, serviceContainer))
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(
                any(),
                match { payload: ImpressionPayload ->
                    val ids = payload.impressionList.map { it.campaignId }
                    ids.contains(holdoutId) && !ids.contains(9001)
                },
                any(),
                any(),
            )
        }
    }

    @Test
    fun `expose by key is a no-op without a stored decision`() {
        assertFalse(ImpressionExposureHelper.exposeByKey("feature1", context, serviceContainer))
        verify(exactly = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
    }

    @Test
    fun `Storage isVariationShownAlreadySent legacy absent with decision is true`() {
        val stored = com.wingify.models.Storage().apply {
            experimentId = 10
            experimentVariationId = 1
            variationShownSent = null
        }
        assertTrue(stored.isVariationShownAlreadySent())
        stored.variationShownSent = false
        assertFalse(stored.isVariationShownAlreadySent())
        stored.variationShownSent = true
        assertTrue(stored.isVariationShownAlreadySent())
    }

    @Test
    fun `flagLockKey differs across accounts and sdk keys`() {
        val a = KeyedLockUtil.flagLockKey(1, "sdk-a", "feature1", "user-1")
        val b = KeyedLockUtil.flagLockKey(2, "sdk-a", "feature1", "user-1")
        val c = KeyedLockUtil.flagLockKey(1, "sdk-b", "feature1", "user-1")
        assertEquals("1|sdk-a|feature1|user-1", a)
        assertTrue(a != b)
        assertTrue(a != c)
    }

    @Test
    fun `concurrent same-key getFlag enqueues variationShown once`() {
        // Avoid impact re-send on the sticky second pass so the assertion is about race prevention.
        settings.features.first().impactCampaign.campaignId = null

        val barrier = java.util.concurrent.CyclicBarrier(2)
        val errors = java.util.concurrent.CopyOnWriteArrayList<Throwable>()
        val run = Runnable {
            try {
                barrier.await()
                GetFlagAPI.getFlag(
                    "feature1",
                    settings,
                    context.clone(),
                    serviceContainer,
                    serviceContainer.getHooksManager(),
                    true,
                )
            } catch (t: Throwable) {
                errors.add(t)
            }
        }
        val t1 = Thread(run, "getFlag-a")
        val t2 = Thread(run, "getFlag-b")
        t1.start()
        t2.start()
        t1.join()
        t2.join()

        assertTrue(errors.isEmpty())
        verify(exactly = 1) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
        assertEquals(true, storedVariationShownSent())
    }
}
