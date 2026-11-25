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
import com.vwo.sdk.fme.BuildConfig
import com.vwo.testcases.StorageTest
import com.wingify.ServiceContainer
import com.wingify.api.GetFlagAPI
import com.wingify.api.GetFlagsAPI
import com.wingify.constants.Constants
import com.wingify.constants.Constants.PLATFORM
import com.wingify.enums.CampaignTypeEnum
import com.wingify.models.Campaign
import com.wingify.models.Feature
import com.wingify.models.Rule
import com.wingify.models.Settings
import com.wingify.models.Variation
import com.wingify.models.impression.ImpressionPayload
import com.wingify.models.user.WingifyUserContext
import com.wingify.packages.storage.Storage
import com.wingify.providers.StorageProvider
import com.wingify.services.SettingsManager
import com.wingify.services.StorageService
import com.wingify.utils.GsonUtil
import com.wingify.utils.ImpressionUtil
import com.wingify.utils.SettingsUtil
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sibling rollout whitelist discovered while choosing a MEG winner.
 * fetchFlag holds it on the returned flag; getFlag still sends it immediately.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MegWhitelistDeferredImpressionTest {

    private val siblingCampaignId = 99
    private val siblingFeatureId = 2
    private val ownerRolloutCampaignId = 1
    private val forcedUser = "qa_forced"
    private val plainUser = "other_user"

    private lateinit var settings: Settings
    private lateinit var container: ServiceContainer

    @Before
    fun setup() {
        StorageProvider.userAgent =
            "VWO FME $PLATFORM ${BuildConfig.SDK_VERSION} ($PLATFORM/${Build.VERSION.RELEASE})"
        settings = siblingMegSettings()
        mockkObject(ImpressionUtil)
        every {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(ImpressionUtil)
    }

    @Test
    fun `deferred fetch holds sibling whitelist until exposeResult`() {
        val flag = evaluate(shouldSend = false, userId = forcedUser)
        verifyNoSends()
        assertTrue(flag.hasPendingImpressions())
        assertNull(siblingVariationShownSent())

        val exposed = captureSends { assertTrue(flag.exposeResult()) }
        assertTrue(campaignIds(exposed).contains(ownerRolloutCampaignId))
        assertTrue(campaignIds(exposed).contains(siblingCampaignId))
        assertEquals(siblingFeatureId, featureId(exposed, siblingCampaignId))

        clearMocks(ImpressionUtil, answers = false)
        assertFalse(flag.exposeResult())
        verifyNoSends()
    }

    @Test
    fun `getFlag sends sibling whitelist immediately`() {
        val flag = evaluate(shouldSend = true, userId = forcedUser)
        val sent = captureSends { }
        assertTrue(campaignIds(sent).contains(siblingCampaignId))
        assertEquals(siblingFeatureId, featureId(sent, siblingCampaignId))

        clearMocks(ImpressionUtil, answers = false)
        assertFalse(flag.exposeResult())
        verifyNoSends()
    }

    @Test
    fun `user outside the force list does not stash the sibling rollout`() {
        val flag = evaluate(shouldSend = false, userId = plainUser)
        val exposed = captureSends { assertTrue(flag.exposeResult()) }
        assertTrue(campaignIds(exposed).contains(ownerRolloutCampaignId))
        assertFalse(campaignIds(exposed).contains(siblingCampaignId))
    }

    @Test
    fun `later fetch on the same storage does not send the sibling again`() {
        val storage = StorageTest()
        val first = evaluate(shouldSend = false, userId = forcedUser, storage = storage)
        assertTrue(first.exposeResult())

        clearMocks(ImpressionUtil, answers = false)
        val second = evaluate(shouldSend = false, userId = forcedUser, storage = storage)
        assertFalse(second.hasPendingImpressions())
        assertFalse(second.exposeResult())
        verifyNoSends()
    }

    @Test
    fun `fetchFlags defers the sibling whitelist onto the fetched flag`() {
        val storage = StorageTest()
        container = newContainer(storage)
        val flag = GetFlagsAPI.getFlags(
            arrayOf("feature1"),
            settings,
            user(forcedUser),
            container,
            shouldSendImpressionToServer = false,
        ).get("feature1")

        verifyNoSends()
        val exposed = captureSends { assertTrue(flag.exposeResult()) }
        assertEquals(siblingFeatureId, featureId(exposed, siblingCampaignId))
    }

    @Test
    fun `fetchFlags of both MEG members sends sibling whitelist once`() {
        container = newContainer(StorageTest())
        val flags = GetFlagsAPI.getFlags(
            arrayOf("feature1", "feature2"),
            settings,
            user(forcedUser),
            container,
            shouldSendImpressionToServer = false,
        )
        verifyNoSends()

        val exposed = captureSends {
            flags.get("feature1").exposeResult()
            flags.get("feature2").exposeResult()
        }
        assertEquals(1, campaignCount(exposed, siblingCampaignId))
    }

    @Test
    fun `new SDK instance does not resend sibling whitelist after owner expose`() {
        val storage = StorageTest()
        val owner = evaluate(shouldSend = false, userId = forcedUser, storage = storage)
        captureSends { assertTrue(owner.exposeResult()) }

        clearMocks(ImpressionUtil, answers = false)
        container = newContainer(storage)
        GetFlagAPI.getFlag(
            "feature2",
            settings,
            user(forcedUser),
            container,
            container.getHooksManager(),
            false,
        ).exposeResult()
        verifyNoSends()
    }

    private fun evaluate(
        shouldSend: Boolean,
        userId: String,
        storage: StorageTest = StorageTest(),
    ): GetFlag {
        container = newContainer(storage)
        return GetFlagAPI.getFlag(
            "feature1",
            settings,
            user(userId),
            container,
            container.getHooksManager(),
            shouldSend,
        )
    }

    private fun newContainer(storage: StorageTest): ServiceContainer {
        val options = VWOInitOptions().apply {
            accountId = settings.accountId
            sdkKey = "test-sdk-key"
            isUsageStatsDisabled = true
        }
        options.storage = storage
        val created = ServiceContainer(
            settingsManager = SettingsManager(options),
            options = options,
            settings = settings,
            loggerService = null,
        )
        created.storage = Storage().apply { attachConnector(storage) }
        return created
    }

    private fun user(userId: String): WingifyUserContext {
        return WingifyUserContext().apply {
            id = userId
            customVariables["price"] = "100"
        }
    }

    private fun siblingVariationShownSent(): Boolean? {
        val stored = StorageService(container).getDataInStorage("feature2", user(forcedUser))
        return stored?.get(Constants.KEY_VARIATION_SHOWN_SENT) as? Boolean
    }

    private fun captureSends(block: () -> Unit): List<ImpressionPayload> {
        val payloads = mutableListOf<ImpressionPayload>()
        block()
        verify(atLeast = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(
                any(),
                capture(payloads),
                any(),
                any(),
            )
        }
        return payloads
    }

    private fun campaignIds(payloads: List<ImpressionPayload>): Set<Int> {
        return payloads.flatMap { payload -> payload.impressionList.map { it.campaignId } }.toSet()
    }

    private fun campaignCount(payloads: List<ImpressionPayload>, campaignId: Int): Int {
        return payloads.flatMap { it.impressionList }.count { it.campaignId == campaignId }
    }

    private fun featureId(payloads: List<ImpressionPayload>, campaignId: Int): Int? {
        return payloads.flatMap { it.impressionList }
            .firstOrNull { it.campaignId == campaignId }
            ?.featureId
    }

    private fun verifyNoSends() {
        verify(exactly = 0) {
            ImpressionUtil.createAndSendImpressionForVariationShown(any(), any(), any(), any())
        }
    }

    private fun siblingMegSettings(): Settings {
        val reader = DummySettingsReader()
        val meg = GsonUtil.gson.fromJson(
            reader.settingsMap["MEG_CAMPAIGN_RANDOM_ALGO_SETTINGS"],
            Settings::class.java,
        )
        val forced = GsonUtil.gson.fromJson(
            reader.settingsMap["ROLLOUT_FORCE_ON_OFF_0_SETTINGS"],
            Settings::class.java,
        )
        val siblingRollout = forced.campaigns!!.first().apply { id = siblingCampaignId }
        val groupCampaignId = 98
        val sibling = Feature().apply {
            key = "feature2"
            name = "Feature2"
            id = siblingFeatureId
            status = "ON"
            type = "FEATURE_FLAG"
            rules = listOf(
                Rule().apply {
                    ruleKey = "rolloutRule1"
                    type = CampaignTypeEnum.ROLLOUT.value
                    campaignId = siblingCampaignId
                    variationId = 1
                },
                Rule().apply {
                    ruleKey = "testingRule1"
                    type = CampaignTypeEnum.AB.value
                    campaignId = groupCampaignId
                },
            )
        }
        val groupPlaceholder = Campaign().apply {
            id = groupCampaignId
            key = "feature2_testing"
            name = "feature2_testing"
            type = CampaignTypeEnum.AB.value
            status = "RUNNING"
            percentTraffic = 0
            isForcedVariationEnabled = false
            segments = emptyMap()
            variations = listOf(Variation().apply { id = 1 })
        }
        meg.accountId = forced.accountId
        meg.features = meg.features + sibling
        meg.campaigns = meg.campaigns.orEmpty() + siblingRollout + groupPlaceholder
        val group = meg.groups!!["1"]!!
        group.campaigns = group.campaigns.orEmpty() + groupCampaignId.toString()
        meg.campaignGroups = meg.campaignGroups.orEmpty() + (groupCampaignId.toString() to 1)
        SettingsUtil.processSettings(meg)
        return meg
    }
}
