/*
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
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

import com.vwo.SDKState
import com.vwo.VWO
import com.vwo.VWO.init
import com.vwo.VWOBuilder
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.storage.Connector
import com.vwo.testcases.StorageTest
import com.vwo.testcases.TestData
import com.vwo.testcases.TestDataReader
import com.vwo.utils.DummySettingsReader
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.spy
import org.mockito.Mockito.`when`
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GetFlagTests {
    private val sdkKey: String = "abcd"
    private val accountId: Int = 1234
    private lateinit var vwo: VWO
    private val testCases = TestDataReader().testCases

    private fun resetVWOState() {
        try {
            // Reset the state
            val stateField = VWO::class.java.getDeclaredField("state")
            stateField.isAccessible = true
            stateField.set(null, SDKState.NOT_INITIALIZED)

            // Reset the instance (CRUCIAL - this was missing)
            val instanceField = VWO::class.java.getDeclaredField("instance")
            instanceField.isAccessible = true
            instanceField.set(null, null)

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

    @Test
    fun testGetFlagWithoutStorage() {
        testCases?.getFlagWithoutStorage?.let { runTests(it) }
    }

    @Test
    fun testGetFlagWithSalt() {
        testCases?.GETFLAG_WITH_SALT?.let { runSaltTest(it) }
    }

    @Test
    fun testGetFlagWithMegRandom() {
        testCases?.getFlagMegRandom?.let { runTests(it) }
    }

    @Test
    fun testGetFlagWithMegAdvance() {
        testCases?.getFlagMegAdvance?.let { runTests(it) }
    }

    @Test
    fun testGetFlagWithStorage() {
        testCases?.getFlagWithStorage?.let { runTests(it, StorageTest()) }
    }

    private fun runTests(tests: List<TestData>, storage: Connector? = null) {

        var featureFlag: GetFlag? = null
        tests.forEach { testData ->

            resetVWOState()

            val vwoInitOptions = VWOInitOptions()
            vwoInitOptions.sdkKey = sdkKey
            vwoInitOptions.accountId = accountId
            vwoInitOptions.isUsageStatsDisabled = true
            if (storage != null) {
                vwoInitOptions.storage = storage
            }

            val settingsReader = DummySettingsReader()
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

            if (storage != null) {
                val storageData =
                    storage.get(testData.featureKey, testData.context?.id) as? Map<String, Any>

                assertNull(storageData)
            }

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
                getVariableAsInt(variable)
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
            settingsReader.assertMapsEqual(
                testData.expectation?.jsonVariable,
                featureFlag?.getVariable("json", HashMap<Any, Any>()) as? Map<String, Any>
            )

            if (storage != null) {
                val storageData =
                    storage.get(testData.featureKey, testData.context?.id) as? Map<String, Any>

                assertEquals(
                    storageData?.get("rolloutKey"),
                    testData.expectation!!.storageData?.rolloutKey
                )
                assertEquals(
                    storageData?.get("rolloutVariationId"),
                    testData.expectation!!.storageData?.rolloutVariationId
                )
                assertEquals(
                    storageData?.get("experimentKey"),
                    testData.expectation!!.storageData?.experimentKey
                )
                assertEquals(
                    storageData?.get("experimentVariationId"),
                    testData.expectation!!.storageData?.experimentVariationId
                )
            }
        }
    }

    private fun getVariableAsInt(variable: Any?): Int {

        return when (variable) {
            is Int -> variable
            is Long -> variable.toInt()
            is Double -> variable.toInt()
            else -> 1
        }
    }

    private fun runSaltTest(tests: List<TestData>) {
        for (testData in tests) {

            resetVWOState()

            val vwoInitOptions = VWOInitOptions()
            vwoInitOptions.sdkKey = sdkKey
            vwoInitOptions.accountId = accountId
            vwoInitOptions.isUsageStatsDisabled = true

            val vwoBuilder = VWOBuilder(vwoInitOptions)
            val vwoBuilderSpy = spy(vwoBuilder)

            val settingsReader = DummySettingsReader()
            val settingsMap = settingsReader.settingsMap
            `when`<String?>(vwoBuilderSpy.getSettings(false)).thenReturn(settingsMap[testData.settings])

            vwoInitOptions.vwoBuilder = vwoBuilderSpy
            val latch = CountDownLatch(1)
            init(vwoInitOptions, object : IVwoInitCallback {
                override fun vwoInitSuccess(vwo: VWO, message: String) {
                    this@GetFlagTests.vwo = vwo
                    latch.countDown()
                }

                override fun vwoInitFailed(message: String) {
                    println("VWO $message")
                    latch.countDown()
                }
            })
            latch.await(10, TimeUnit.SECONDS)
            if (!::vwo.isInitialized) return

            val userIds = testData.userIds

            for (userId in userIds!!) {
                val vwoContext = VWOUserContext()
                vwoContext.id = userId

                val featureFlag = getFlagCountDownLatchPair(testData.featureKey!!, vwoContext)
                val featureFlag2 = getFlagCountDownLatchPair(testData.featureKey2!!, vwoContext)
                if (featureFlag == null || featureFlag2 == null) return

                val featureFlagVariables = featureFlag.getVariables()
                val featureFlag2Variables = featureFlag2.getVariables()
                if (testData.expectation!!.shouldReturnSameVariation!!) {
                    assertEquals(
                        "The feature flag variables are not equal!",
                        featureFlagVariables,
                        featureFlag2Variables,
                    )
                } else {
                    val areEqual = featureFlagVariables == featureFlag2Variables
                    assertFalse("The feature flag variables are equal!", areEqual)
                }
            }
        }
    }

    private fun getFlagCountDownLatchPair(
        flagName: String,
        context: VWOUserContext
    ): GetFlag? {

        val latch = CountDownLatch(1)
        var featureFlag: GetFlag? = null
        vwo.getFlag(flagName, context, object : IVwoListener {
            override fun onSuccess(data: Any) {
                featureFlag = data as? GetFlag
                latch.countDown()
            }

            override fun onFailure(message: String) {
                println(message)
                latch.countDown()
            }
        })
        // Wait for onSuccess/onFailure to be called
        latch.await(5, TimeUnit.SECONDS)
        return featureFlag
    }
}