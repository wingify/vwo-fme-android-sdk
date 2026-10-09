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
package com.vwo.api

import android.os.Build
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.storage.Connector
import com.vwo.sdk.fme.BuildConfig
import com.vwo.testcases.StorageTest
import com.vwo.utils.DummySettingsReader
import com.vwo.utils.GetFlagsStressTestSettingsFactory
import com.wingify.ServiceContainer
import com.wingify.WingifyBuilder
import com.wingify.WingifyClient
import com.wingify.api.GetFlagAPI
import com.wingify.api.GetFlagsAPI
import com.wingify.constants.Constants.PLATFORM
import com.wingify.models.Settings
import com.wingify.models.user.WingifyInitOptions
import com.wingify.models.user.WingifyUserContext
import com.wingify.packages.network_layer.manager.BatchManager
import com.wingify.packages.storage.Storage
import com.wingify.providers.StorageProvider
import com.wingify.services.SettingsManager
import com.wingify.utils.GsonUtil
import com.wingify.utils.MegGroupPartitionUtil
import com.wingify.utils.SettingsUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GetFlagsAPITest {

    private lateinit var settings: Settings
    private lateinit var multiFeatureSettings: Settings
    private lateinit var megSettings: Settings
    private lateinit var stressSettings: Settings
    private lateinit var serviceContainer: ServiceContainer
    private lateinit var context: WingifyUserContext

    @Before
    fun setup() {
        StorageProvider.userAgent =
            "VWO FME $PLATFORM ${BuildConfig.SDK_VERSION} ($PLATFORM/${Build.VERSION.RELEASE})"

        val reader = DummySettingsReader()
        settings = GsonUtil.gson.fromJson(
            reader.settingsMap["BASIC_ROLLOUT_SETTINGS"],
            Settings::class.java,
        )
        SettingsUtil.processSettings(settings)
        multiFeatureSettings = GsonUtil.gson.fromJson(
            reader.settingsMap["SETTINGS_WITH_DIFFERENT_SALT"],
            Settings::class.java,
        )
        SettingsUtil.processSettings(multiFeatureSettings)
        megSettings = GsonUtil.gson.fromJson(
            reader.settingsMap["MEG_CAMPAIGN_RANDOM_ALGO_SETTINGS"],
            Settings::class.java,
        )
        SettingsUtil.processSettings(megSettings)
        stressSettings = GetFlagsStressTestSettingsFactory.build()

        val options = VWOInitOptions().apply {
            accountId = 123456
            sdkKey = "test-sdk-key"
            isUsageStatsDisabled = true
        }
        serviceContainer = createServiceContainer(settings, options)
        context = WingifyUserContext().apply { id = "user-123" }
    }

    private fun createServiceContainer(
        settings: Settings,
        options: WingifyInitOptions,
    ): ServiceContainer {
        options.storage = StorageTest()
        val container = ServiceContainer(
            settingsManager = SettingsManager(options),
            options = options,
            settings = settings,
            loggerService = null,
        )
        container.storage = Storage().apply {
            attachConnector(options.storage as Connector)
        }
        return container
    }

    @Test
    fun `null flag names evaluates all configured flags`() {
        val collection = GetFlagsAPI.getFlags(null, settings, context, serviceContainer)
        assertEquals(1, collection.size())
        assertTrue(collection.get("feature1").isEnabled())
    }

    @Test
    fun `unknown flag key returns disabled flag in collection`() {
        val collection = GetFlagsAPI.getFlags(
            arrayOf("unknown_flag"),
            settings,
            context,
            serviceContainer,
        )
        assertEquals(1, collection.size())
        assertFalse(collection.get("unknown_flag").isEnabled())
    }

    @Test
    fun `parallel getFlags matches sequential getFlag results`() {
        val keys = arrayOf("feature1", "feature2")
        val container = createServiceContainer(multiFeatureSettings, serviceContainer.getInitOptions())
        val batch = GetFlagsAPI.getFlags(keys, multiFeatureSettings, context, container)
        keys.forEach { key ->
            val single = GetFlagAPI.getFlag(
                key,
                multiFeatureSettings,
                context.clone(),
                container,
                container.getHooksManager(),
            )
            assertEquals(single.isEnabled(), batch.get(key).isEnabled())
        }
    }

    @Test
    fun `meg getFlags matches sequential getFlag for same user`() {
        val megContext = WingifyUserContext().apply { id = "meg-user-1" }
        val megContainer = createServiceContainer(megSettings, serviceContainer.getInitOptions())
        val batch = GetFlagsAPI.getFlags(
            arrayOf("feature1"),
            megSettings,
            megContext,
            megContainer,
        )
        val single = GetFlagAPI.getFlag(
            "feature1",
            megSettings,
            megContext.clone(),
            megContainer,
            megContainer.getHooksManager(),
        )
        assertEquals(single.isEnabled(), batch.get("feature1").isEnabled())
    }

    @Test
    fun `parallel stress test with 12 flags matches sequential getFlag baseline across users`() {
        /*
         * Stress fixture layout (see GetFlagsStressTestSettingsFactory):
         *  - 6 rollout-only flags with no MEG dependency (parallel-safe).
         *  - 2 A/B flags with different salts (parallel-safe).
         *  - 2 MEG random-algo flags in group 1 (sequential within batch).
         *  - 2 MEG advance-algo flags in group 2 (sequential within batch).
         *
         * getFlags() runs up to Constants.GET_FLAGS_PARALLELISM (5) batches concurrently.
         * Each batch is either a single independent flag or a MEG-linked key set.
         */
        val allKeys = GetFlagsStressTestSettingsFactory.ALL_FLAG_KEYS
        assertEquals(
            "Stress fixture must evaluate at least 10 flags",
            12,
            allKeys.size,
        )

        val batches = MegGroupPartitionUtil.getFeatureFlagBatches(allKeys, stressSettings)
        assertEquals(
            "Expected 10 evaluation batches: 8 independent/salt singles + 2 MEG pairs. " +
                "Actual batches=${batches.map { it.keys }}",
            10,
            batches.size,
        )
        assertEquals(
            "MEG random pair must share one sequential batch",
            GetFlagsStressTestSettingsFactory.MEG_RANDOM_FLAG_KEYS.sorted(),
            batches.first { batch ->
                batch.keys.contains(GetFlagsStressTestSettingsFactory.MEG_RANDOM_FLAG_KEYS[0])
            }.keys.sorted(),
        )
        assertEquals(
            "MEG advance pair must share one sequential batch",
            GetFlagsStressTestSettingsFactory.MEG_ADVANCE_FLAG_KEYS.sorted(),
            batches.first { batch ->
                batch.keys.contains(GetFlagsStressTestSettingsFactory.MEG_ADVANCE_FLAG_KEYS[0])
            }.keys.sorted(),
        )

        val options = serviceContainer.getInitOptions()
        val userIds = (1..10).map { index -> "stress-user-$index" }

        userIds.forEach { userId ->
            val sequentialResults = evaluateSequentially(
                flagKeys = allKeys,
                settings = stressSettings,
                userId = userId,
                options = options,
            )

            val parallelContainer = createServiceContainer(stressSettings, options)
            val parallelContext = WingifyUserContext().apply { id = userId }
            val parallelCollection = GetFlagsAPI.getFlags(
                allKeys.toTypedArray(),
                stressSettings,
                parallelContext,
                parallelContainer,
            )

            assertEquals(
                "Parallel getFlags must return all requested keys for userId=$userId",
                allKeys.size,
                parallelCollection.size(),
            )

            allKeys.forEach { key ->
                val sequentialEnabled = sequentialResults.getValue(key)
                val parallelEnabled = parallelCollection.get(key).isEnabled()
                assertEquals(
                    "Parallel getFlags must match sequential getFlag baseline " +
                        "for userId=$userId featureKey=$key",
                    sequentialEnabled,
                    parallelEnabled,
                )
            }
        }
    }

    /**
     * Baseline: invoke unmodified [GetFlagAPI.getFlag] once per key on a fresh container,
     * preserving call order. MEG storage written by earlier keys affects later keys,
     * mirroring real-world sequential getFlag() usage.
     */
    private fun evaluateSequentially(
        flagKeys: List<String>,
        settings: Settings,
        userId: String,
        options: WingifyInitOptions,
    ): Map<String, Boolean> {
        val container = createServiceContainer(settings, options)
        val baseContext = WingifyUserContext().apply { id = userId }
        return flagKeys.associateWith { key ->
            GetFlagAPI.getFlag(
                key,
                settings,
                baseContext.clone(),
                container,
                container.getHooksManager(),
            ).isEnabled()
        }
    }

    @Test
    fun `triggers single batch upload when batching disabled`() = runBlocking {
        mockkObject(BatchManager)
        coEvery { BatchManager.start(any(), any()) } returns true

        GetFlagsAPI.getFlags(
            arrayOf("feature1"),
            settings,
            context,
            serviceContainer,
        )

        delay(500)

        coVerify(exactly = 1) {
            BatchManager.start("getFlags batch", serviceContainer)
        }
    }

    @Test
    fun `nested defer keeps uploads deferred until outermost getFlags completes`() = runBlocking {
        mockkObject(BatchManager)
        coEvery { BatchManager.start(any(), any()) } returns true

        // Simulate an outer getFlags still in progress while an inner one finishes.
        serviceContainer.beginDeferImmediateBatchUpload()
        try {
            GetFlagsAPI.getFlags(
                arrayOf("feature1"),
                settings,
                context,
                serviceContainer,
            )
            delay(300)
            // Inner call ended but outer defer is still active — must not flush yet.
            coVerify(exactly = 0) {
                BatchManager.start("getFlags batch", serviceContainer)
            }
            assertTrue(serviceContainer.deferImmediateBatchUpload)
        } finally {
            val shouldFlush = serviceContainer.endDeferImmediateBatchUpload()
            assertTrue(shouldFlush)
            assertFalse(serviceContainer.deferImmediateBatchUpload)
            if (shouldFlush && serviceContainer.onlineBatchUploadManager.isBatchingDisabled()) {
                BatchManager.start("getFlags batch", serviceContainer)
            }
        }

        delay(300)
        coVerify(exactly = 1) {
            BatchManager.start("getFlags batch", serviceContainer)
        }
    }

    @Test
    fun `WingifyClient getFlags delegates to batch API`() {
        val settingsJson = DummySettingsReader().settingsMap["SETTINGS_WITH_DIFFERENT_SALT"]!!
        val options = WingifyInitOptions().apply {
            accountId = 12345
            sdkKey = "test-sdk-key"
            isUsageStatsDisabled = true
        }
        val builder = WingifyBuilder(options)
        builder.setLogger()
            .setContext()
            .setSharePreferences()
            .setSettingsManager()
            .setStorage()
            .setNetworkManager()
            .setSegmentation()
        val client = WingifyClient(settingsJson, options, builder).apply {
            // Mirrors Wingify.init, which sets this after settings validation.
            isSettingsValid = true
        }
        val userContext = WingifyUserContext().apply { id = "client-user" }
        val collection = client.getFlags(null, userContext)
        assertEquals(2, collection.size())
        assertTrue(collection.keys().contains("feature1"))
        assertTrue(collection.keys().contains("feature2"))
    }
}
