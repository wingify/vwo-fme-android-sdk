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
package com.vwo

import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.VWOInitOptions
import com.vwo.utils.DummySettingsReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VWOInitTest {

    @Test
    fun testInitWithNullSdkKey() {
        val latch = CountDownLatch(1)
        var errorMessage: String? = null

        val options = VWOInitOptions()
        options.accountId = 12345
        options.sdkKey = null
        options.isUsageStatsDisabled = true

        VWO.init(options, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                errorMessage = message
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)
        assertEquals(
            "SDK key is required to initialize VWO. Please provide the sdkKey in the options.",
            errorMessage
        )
    }

    @Test
    fun testInitWithEmptySdkKey() {
        val latch = CountDownLatch(1)
        var errorMessage: String? = null

        val options = VWOInitOptions()
        options.accountId = 12345
        options.sdkKey = ""
        options.isUsageStatsDisabled = true

        VWO.init(options, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                errorMessage = message
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)
        assertEquals(
            "SDK key is required to initialize VWO. Please provide the sdkKey in the options.",
            errorMessage
        )
    }

    @Test
    fun testInitWithNullAccountId() {
        val latch = CountDownLatch(1)
        var errorMessage: String? = null

        val options = VWOInitOptions()
        options.sdkKey = "valid-sdk-key"
        options.accountId = null
        options.isUsageStatsDisabled = true

        VWO.init(options, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                errorMessage = message
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)
        assertEquals(
            "Account ID is required to initialize VWO. Please provide the accountId in the options.",
            errorMessage
        )
    }

    @Test
    fun testInitWithValidOptions() {
        val latch = CountDownLatch(1)
        var successMessage: String? = null
        var vwoInstance: VWO? = null

        val options = VWOInitOptions()
        options.sdkKey = "valid-sdk-key"
        options.accountId = 12345
        options.isUsageStatsDisabled = true
        setupSpySettings(options)

        VWO.init(options, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                vwoInstance = vwo
                successMessage = message
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)

        if (vwoInstance != null) {
            assertEquals("VWO initialized successfully", successMessage)
            assertNotNull(vwoInstance)
        }
    }

    private fun setupSpySettings(vwoInitOptions : VWOInitOptions){
        val settingsReader = DummySettingsReader()
        val settingsMap = settingsReader.settingsMap
        val settings = settingsMap["BASIC_ROLLOUT_SETTINGS"]

        val vwoBuilder = VWOBuilder(vwoInitOptions)
        val vwoBuilderSpy: VWOBuilder = spy(vwoBuilder)
        settings?.let {
            whenever(vwoBuilderSpy.getSettings(false)).thenReturn(settings)
        }
        vwoInitOptions.vwoBuilder = vwoBuilderSpy
    }
}