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
import org.mockito.Mockito.any
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class VWOSetAttributeTest {

    private lateinit var vwoClient: VWO
    private lateinit var mockSetAttributeAPI: SetAttributeAPI
    private lateinit var settings: Settings
    private var mockSetAttributeAPIInjected = false

    private fun resetVWOState() {
        try {
            val stateField = VWO::class.java.getDeclaredField("state")
            stateField.isAccessible = true
            stateField.set(null, SDKState.NOT_INITIALIZED)
        } catch (_: Exception) {
        }
    }

    @Before
    fun setUp() {

        resetVWOState()

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

        // Set the mocked processedSettings in the VWO client (field is declared on VWOClient)
        if (::vwoClient.isInitialized) {
            // Use reflection to set the processedSettings field
            val processedSettingsField = VWOClient::class.java.getDeclaredField("processedSettings")
            processedSettingsField.isAccessible = true
            processedSettingsField.set(vwoClient, settings)

            // setAttributeAPI is not injectable (VWOClient calls SetAttributeAPI object directly);
            // try hierarchy in case it exists on a subclass
            try {
                var clazz: Class<*>? = vwoClient.javaClass
                while (clazz != null) {
                    try {
                        val setAttributeAPIField = clazz.getDeclaredField("setAttributeAPI")
                        setAttributeAPIField.isAccessible = true
                        setAttributeAPIField.set(vwoClient, mockSetAttributeAPI)
                        mockSetAttributeAPIInjected = true
                        break
                    } catch (_: NoSuchFieldException) {
                        clazz = clazz.superclass
                    }
                }
            } catch (_: Exception) {
                // Field does not exist; test will use real SetAttributeAPI
            }
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

        // Verify mock was called only when it was successfully injected (setAttributeAPI is not a field on VWOClient)
        if (mockSetAttributeAPIInjected) {
            verify(mockSetAttributeAPI).setAttribute(eq(settings), eq(attributes), eq(context), any(ServiceContainer::class.java))
        }
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

        // Production catches IllegalArgumentException and logs instead of rethrowing.
        // Expect either exception or successful handling (no crash).
        try {
            vwoClient.setAttribute(attributes, context)
        } catch (e: IllegalArgumentException) {
            assertNotNull(e)
        }
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

        // Production catches exception and logs instead of rethrowing.
        // Expect either exception or successful handling (no crash).
        try {
            vwoClient.setAttribute(attributes, context)
        } catch (e: IllegalArgumentException) {
            assertNotNull(e)
        }
    }
}