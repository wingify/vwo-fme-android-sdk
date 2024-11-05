/*
 * Copyright (c) 2024 Wingify Software Pvt. Ltd.
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
import com.vwo.VWO.init
import com.vwo.VWOBuilder
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOInitOptions
import com.vwo.testcases.TestData
import com.vwo.testcases.TestDataReader
import com.vwo.utils.DummySettingsReader
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GetFlagTests {
    private val SDK_KEY: String = "abcd"
    private val ACCOUNT_ID: Int = 1234
    private lateinit var settingsReader: DummySettingsReader
    private lateinit var vwo: VWO
    private var vwoInitOptions = VWOInitOptions()
    private val testCases = TestDataReader().testCases

    @Before
    fun setup() {
    }

    @Test
    fun testGetFlagWithoutStorage() {
        testCases?.getFlagWithoutStorage?.let { runTests(it) }
    }

    @Test
    fun testGetFlagWithMegRandom() {
        testCases?.getFlagMegRandom?.let { runTests(it) }
    }

    @Test
    fun testGetFlagWithMegAdvance() {
        testCases?.getFlagMegAdvance?.let { runTests(it) }
    }

    /*fun testGetFlagWithStorage() {
        testCases?.getFlagWithStorage?.let { runTests(it) }
    }*/

    fun runTests(tests: List<TestData>) {

        var featureFlag: GetFlag? = null
        tests.forEach { testData ->
            val vwoInitOptions = VWOInitOptions()
            vwoInitOptions.sdkKey = SDK_KEY
            vwoInitOptions.accountId = ACCOUNT_ID

            settingsReader = DummySettingsReader()
            val vwoBuilder = VWOBuilder(vwoInitOptions)
            val vwoBuilderSpy: VWOBuilder = spy(vwoBuilder)
            val settingsMap = settingsReader.settingsMap
            val settings = settingsMap[testData.settings]
            settings?.let {
                whenever(vwoBuilderSpy.getSettings(false)).thenReturn(settings)
            }
            /*val result = vwoBuilderSpy.getSettings(false)
            println("VWO - Mocked settings matching: ${result == settings}")*/
            vwoInitOptions.vwoBuilder = vwoBuilderSpy

            var latch = CountDownLatch(1)
            init(vwoInitOptions, object : IVwoInitCallback {
                override fun vwoInitSuccess(vwo: VWO, message: String) {
                    this@GetFlagTests.vwo = vwo
                    latch.countDown()
                }

                override fun vwoInitFailed(message: String) {
                    println("VWO $message")
                }
            })

            latch.await(10, TimeUnit.SECONDS)
            if (!::vwo.isInitialized) return
            latch = CountDownLatch(1)
            vwo.getFlag(testData.featureKey!!, testData.context!!, object : IVwoListener {
                override fun onSuccess(data: Any) {
                    featureFlag = data as? GetFlag
                    latch.countDown()
                }

                override fun onFailure(message: String) {
                    println(message)
                }
            })

            // Wait for onSuccess to be called
            latch.await(5, TimeUnit.SECONDS)
            val isFeatureFlagEnabled = featureFlag?.isEnabled()
            assertEquals(testData.expectation?.isEnabled, isFeatureFlagEnabled)
            val variable = featureFlag?.getVariable("int", 1)
            assertEquals(
                testData.expectation?.intVariable,
                (variable as? Int) ?: (variable as? Double)?.toInt() ?: 1
            )
            assertEquals(
                testData.expectation?.stringVariable,
                featureFlag?.getVariable("string", "VWO")
            )
            assertEquals(
                testData.expectation?.floatVariable,
                featureFlag?.getVariable("float", 1.1)
            )
            assertEquals(
                testData.expectation?.booleanVariable,
                featureFlag?.getVariable("boolean", false)
            )
            assertEquals(
                testData.expectation?.jsonVariable,
                featureFlag?.getVariable("json", HashMap<Any, Any>())
            )
        }
    }
}