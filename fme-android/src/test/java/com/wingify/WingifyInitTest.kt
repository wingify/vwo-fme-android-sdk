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
package com.wingify

import com.vwo.utils.DummySettingsReader
import com.wingify.interfaces.IWingifyInitCallback
import com.wingify.models.user.WingifyInitOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WingifyInitTest {

    @Before
    fun setUp() {
        Wingify.clearAllInstances()
    }

    @Test
    fun testInitWithNullSdkKey() {
        val latch = CountDownLatch(1)
        var errorMessage: String? = null

        val options = WingifyInitOptions()
        options.accountId = 12345
        options.sdkKey = null
        options.isUsageStatsDisabled = true

        Wingify.init(options, object : IWingifyInitCallback {
            override fun wingifyInitSuccess(wingify: Wingify, message: String) {
                latch.countDown()
            }

            override fun wingifyInitFailed(message: String) {
                errorMessage = message
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)
        assertEquals(
            "SDK key is required to initialize Wingify. Please provide the sdkKey in the options.",
            errorMessage,
        )
    }

    @Test
    fun testInitWithNullAccountId() {
        val latch = CountDownLatch(1)
        var errorMessage: String? = null

        val options = WingifyInitOptions()
        options.sdkKey = "valid-sdk-key"
        options.accountId = null
        options.isUsageStatsDisabled = true

        Wingify.init(options, object : IWingifyInitCallback {
            override fun wingifyInitSuccess(wingify: Wingify, message: String) {
                latch.countDown()
            }

            override fun wingifyInitFailed(message: String) {
                errorMessage = message
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)
        assertEquals(
            "Account ID is required to initialize Wingify. Please provide the accountId in the options.",
            errorMessage,
        )
    }

    @Test
    fun testInitWithValidOptions() {
        val latch = CountDownLatch(1)
        var successMessage: String? = null
        var wingifyInstance: Wingify? = null

        val options = WingifyInitOptions()
        options.sdkKey = "valid-sdk-key"
        options.accountId = 12345
        options.isUsageStatsDisabled = true
        setupSpySettings(options)

        Wingify.init(options, object : IWingifyInitCallback {
            override fun wingifyInitSuccess(wingify: Wingify, message: String) {
                wingifyInstance = wingify
                successMessage = message
                latch.countDown()
            }

            override fun wingifyInitFailed(message: String) {
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)

        if (wingifyInstance != null) {
            assertEquals("Wingify initialized successfully", successMessage)
            assertNotNull(wingifyInstance)
        }
    }

    @Test
    fun testGetInstanceReturnsSameAccount() {
        val options = WingifyInitOptions()
        options.sdkKey = "valid-sdk-key"
        options.accountId = 12345
        options.isUsageStatsDisabled = true
        setupSpySettings(options)

        val latch = CountDownLatch(1)
        Wingify.init(options, object : IWingifyInitCallback {
            override fun wingifyInitSuccess(wingify: Wingify, message: String) {
                latch.countDown()
            }

            override fun wingifyInitFailed(message: String) {
                latch.countDown()
            }
        })
        latch.await(5, TimeUnit.SECONDS)

        val cached = Wingify.getInstance(12345, "valid-sdk-key")
        assertNotNull(cached)
    }

    private fun setupSpySettings(wingifyInitOptions: WingifyInitOptions) {
        val settingsReader = DummySettingsReader()
        val settingsMap = settingsReader.settingsMap
        val settings = settingsMap["BASIC_ROLLOUT_SETTINGS"]

        val wingifyBuilder = WingifyBuilder(wingifyInitOptions)
        val wingifyBuilderSpy: WingifyBuilder = spy(wingifyBuilder)
        settings?.let {
            whenever(wingifyBuilderSpy.getSettings(false)).thenReturn(settings)
        }
        wingifyInitOptions.wingifyBuilder = wingifyBuilderSpy
    }
}
