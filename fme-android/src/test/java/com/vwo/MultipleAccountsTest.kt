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

import android.content.Context
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.utils.DummySettingsReader
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Test class to verify that multiple VWO accounts can be used simultaneously
 * without conflicts. This addresses the multiple accounts issue by ensuring
 * each account has its own isolated ServiceContainer.
 */
@RunWith(MockitoJUnitRunner::class)
class MultipleAccountsTest {

    @Mock
    private lateinit var mockContext: Context

    @Test
    fun testMultipleAccountsCanBeInitializedSimultaneously() {
        // Test data for two different accounts
        val account1Options = VWOInitOptions().apply {
            sdkKey = "test-sdk-key-1"
            accountId = 12345
            context = mockContext
            isUsageStatsDisabled = true // Disable usage stats for test
        }

        val account2Options = VWOInitOptions().apply {
            sdkKey = "test-sdk-key-2" 
            accountId = 67890
            context = mockContext
            isUsageStatsDisabled = true // Disable usage stats for test
        }
        
        // Setup spy settings for both accounts to mock network calls
        setupSpySettings(account1Options)
        setupSpySettings(account2Options)

        // Use CountDownLatch to wait for both async operations to complete
        val latch = CountDownLatch(2)
        var vwo1: VWO? = null
        var vwo2: VWO? = null
        var initSuccess1 = false
        var initSuccess2 = false
        var error1: String? = null
        var error2: String? = null

        // Initialize first account
        VWO.init(account1Options, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                vwo1 = vwo
                initSuccess1 = true
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                error1 = message
                initSuccess1 = false
                latch.countDown()
            }
        })

        // Initialize second account  
        VWO.init(account2Options, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                vwo2 = vwo
                initSuccess2 = true
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                error2 = message
                initSuccess2 = false
                latch.countDown()
            }
        })

        // Wait for both initializations to complete (with timeout)
        val completed = latch.await(10, TimeUnit.SECONDS)
        assertTrue("Both initializations should complete within timeout", completed)

        // Verify both accounts were initialized successfully
        if (!initSuccess1) {
            println("Account 1 initialization failed: $error1")
        }
        if (!initSuccess2) {
            println("Account 2 initialization failed: $error2")
        }
        
        assertTrue("Account 1 should initialize successfully", initSuccess1)
        assertTrue("Account 2 should initialize successfully", initSuccess2)
        assertNotNull("VWO instance 1 should not be null", vwo1)
        assertNotNull("VWO instance 2 should not be null", vwo2)
        
        // Verify that the instances are different (proper isolation)
        assertNotSame("VWO instances should be different", vwo1, vwo2)
    }

    @Test
    fun testServiceContainerIsolation() {
        val options1 = VWOInitOptions().apply {
            sdkKey = "test-key-1"
            accountId = 111
            context = mockContext
        }

        val options2 = VWOInitOptions().apply {
            sdkKey = "test-key-2"
            accountId = 222  
            context = mockContext
        }

        // Create VWOBuilder instances for both accounts
        val builder1 = VWOBuilder(options1)
        val builder2 = VWOBuilder(options2)

        // Set up services for both builders
        builder1.setLogger().setSettingsManager()
        builder2.setLogger().setSettingsManager()

        // Verify that each builder has its own services
        val settingsManager1 = builder1.getSettingsManager()
        val settingsManager2 = builder2.getSettingsManager()

        assertNotNull("Builder 1 should have its own SettingsManager", settingsManager1)
        assertNotNull("Builder 2 should have its own SettingsManager", settingsManager2)
        
        // Verify they are different instances (this should pass with our changes)
        assertNotSame("SettingsManagers should be different instances", settingsManager1, settingsManager2)
        
        // Verify they have different account configurations
        assertEquals("Builder 1 should have account 111", 111, settingsManager1?.accountId)
        assertEquals("Builder 2 should have account 222", 222, settingsManager2?.accountId)
        
        assertEquals("Builder 1 should have test-key-1", "test-key-1", settingsManager1?.sdkKey)
        assertEquals("Builder 2 should have test-key-2", "test-key-2", settingsManager2?.sdkKey)
    }

    @Test
    fun testVWOClientServiceContainerCreation() {
        val options = VWOInitOptions().apply {
            sdkKey = "test-key"
            accountId = 123
            context = mockContext
        }

        val builder = VWOBuilder(options)
        builder.setLogger().setSettingsManager()

        // Create VWOClient with builder
        val vwoClient = VWOClient(null, options, builder)

        // Test that ServiceContainer can be created (via reflection since it's private)
        val createServiceContainerMethod = VWOClient::class.java.getDeclaredMethod("createServiceContainer")
        createServiceContainerMethod.isAccessible = true

        try {
            val serviceContainer = createServiceContainerMethod.invoke(vwoClient)
            // If no exception is thrown, the method works correctly
            // ServiceContainer creation will return null if processedSettings is null, which is expected
            // This test mainly verifies the method exists and can be called
            assertTrue("ServiceContainer creation method should be callable", true)
        } catch (e: Exception) {
            fail("ServiceContainer creation should not throw exception: ${e.message}")
        }
    }
    
    private fun setupSpySettings(vwoInitOptions: VWOInitOptions) {
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
