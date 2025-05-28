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

import com.vwo.api.SetAttributeAPI
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.Settings
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VWOSetAttributeTest {

    private lateinit var vwoClient: VWO
    private lateinit var mockSetAttributeAPI: SetAttributeAPI
    private lateinit var settings: Settings

    @Before
    fun setUp() {
        // Create a mock SetAttributeAPI
        mockSetAttributeAPI = mock(SetAttributeAPI::class.java)

        // Create a mock Settings object
        settings = mock(Settings::class.java)

        // Initialize VWO client
        val latch = CountDownLatch(1)

        val vwoInitOptions = VWOInitOptions()
        vwoInitOptions.sdkKey = "test-sdk-key"
        vwoInitOptions.accountId = 12345
        vwoInitOptions.isUsageStatsDisabled = true

        VWO.init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwo: VWO, message: String) {
                vwoClient = vwo
                latch.countDown()
            }

            override fun vwoInitFailed(message: String) {
                latch.countDown()
            }
        })

        latch.await(5, TimeUnit.SECONDS)

        // Set the mocked processedSettings in the VWO client
        if (::vwoClient.isInitialized) {
            // Use reflection to set the processedSettings field
            val processedSettingsField = VWO::class.java.getDeclaredField("processedSettings")
            processedSettingsField.isAccessible = true
            processedSettingsField.set(vwoClient, settings)

            // Use reflection to set the mockSetAttributeAPI
            val setAttributeAPIField = VWO::class.java.getDeclaredField("setAttributeAPI")
            setAttributeAPIField.isAccessible = true
            setAttributeAPIField.set(vwoClient, mockSetAttributeAPI)
        }
    }

    @Test
    fun testSetAttributeCallsSetAttributeAPI() {
        // Skip if VWO client initialization failed
        if (!::vwoClient.isInitialized) return

        // Create test data
        val context = VWOUserContext()
        context.id = "test-user-id"

        val attributes = mutableMapOf<String, Any>()
        attributes["age"] = 25
        attributes["name"] = "Test User"
        attributes["isActive"] = true

        // Call the setAttribute method
        vwoClient.setAttribute(attributes, context)

        // Verify that SetAttributeAPI.setAttribute was called with the correct parameters
        verify(mockSetAttributeAPI).setAttribute(eq(settings), eq(attributes), eq(context))
    }

    @Test
    fun testSetAttributeWithInvalidAttributeType() {
        // Skip if VWO client initialization failed
        if (!::vwoClient.isInitialized) return

        // Create test data with invalid attribute type (complex object)
        val context = VWOUserContext()
        context.id = "test-user-id"

        val attributes = mutableMapOf<String, Any>()
        attributes["invalidObject"] = object {} // Complex object that's not String, Number, or Boolean

        // This should throw an IllegalArgumentException
        try {
            vwoClient.setAttribute(attributes, context)
        } catch (e: IllegalArgumentException) {
            // Expected exception
            assertNotNull(e)
            return
        }

        // If we get here, the test failed
        assert(false) { "Expected IllegalArgumentException was not thrown" }
    }

    @Test
    fun testSetAttributeWithNullUserId() {
        // Skip if VWO client initialization failed
        if (!::vwoClient.isInitialized) return

        // Create test data with null user ID
        val context = VWOUserContext()
        // context.id is null

        val attributes = mutableMapOf<String, Any>()
        attributes["age"] = 25

        // This should throw an IllegalArgumentException
        try {
            vwoClient.setAttribute(attributes, context)
        } catch (e: IllegalArgumentException) {
            // Expected exception
            assertNotNull(e)
            return
        }

        // If we get here, the test failed
        assert(false) { "Expected IllegalArgumentException was not thrown" }
    }
}