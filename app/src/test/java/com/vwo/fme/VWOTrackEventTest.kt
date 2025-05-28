package com.vwo.fme

import com.vwo.VWO
import com.vwo.VWO.init
import com.vwo.VWOBuilder
import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.VWOUserContext
import com.vwo.models.user.VWOInitOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import com.vwo.utils.DummySettingsReader
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VWOTrackEventTest {
    private lateinit var vwoClient: VWO
    private lateinit var context: VWOUserContext
    private lateinit var vwoInitOptions: VWOInitOptions

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Initialize VWO SDK
        vwoInitOptions = VWOInitOptions()
        vwoInitOptions.sdkKey = "sdk-key"
        vwoInitOptions.accountId = 123
        
        // Create VWOUserContext object
        context = VWOUserContext()
        context.id = "123"
        context.customVariables = mutableMapOf("key1" to 21, "key2" to 0)
    }

    @Test
    fun `should track an event successfully`() {

        // Initialize VWO SDK
        init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwoClient: VWO, message: String) {
                this@VWOTrackEventTest.vwoClient = vwoClient
            }

            override fun vwoInitFailed(message: String) {
                // Initialization failed
            }
        })

        if (!::vwoClient.isInitialized) return

        // Mock input data
        val eventName = "custom1"
        val eventProperties = mutableMapOf<String, Any>("key" to "value")

        // Call the trackEvent method
        val result = vwoClient.trackEvent(eventName, context, eventProperties)

        // Assert that the method returns the correct data
        assertNotNull(result)
        assertEquals(true, result?.get(eventName))
    }

    @Test
    fun `should not track an event which has no metric corresponding to eventName`() {
        // Initialize VWO SDK
        init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwoClient: VWO, message: String) {
                this@VWOTrackEventTest.vwoClient = vwoClient
            }

            override fun vwoInitFailed(message: String) {
                // Initialization failed
            }
        })

        // Mock input data
        val eventName = "testEvent"
        val eventProperties = mutableMapOf<String, Any>("key" to "value")

        // Call the trackEvent method
        val result = vwoClient.trackEvent(eventName, context, eventProperties)

        // Assert that the method returns the correct data
        assertNotNull(result)
        assertEquals(false, result?.get(eventName))
    }

    @Test
    fun `should handle error when eventName is not a string`() {
        // Initialize VWO SDK
        init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwoClient: VWO, message: String) {
                this@VWOTrackEventTest.vwoClient = vwoClient
            }

            override fun vwoInitFailed(message: String) {
                // Initialization failed
            }
        })

        // Mock input data with invalid eventName
        val eventName = 123 // Invalid eventName
        val eventProperties = mutableMapOf<String, Any>("key" to "value")

        // Call the trackEvent method
        val result = vwoClient.trackEvent(eventName.toString(), context, eventProperties)

        // Assert that the method returns the correct data
        assertNotNull(result)
        assertEquals(false, result?.get(eventName.toString()))
    }

    @Test
    fun `should handle error when eventProperties is not an object`() {
        // Initialize VWO SDK
        init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwoClient: VWO, message: String) {
                this@VWOTrackEventTest.vwoClient = vwoClient
            }

            override fun vwoInitFailed(message: String) {
                // Initialization failed
            }
        })

        // Mock input data with invalid eventProperties
        val eventName = "testEvent"
        val eventProperties = "invalid" // Invalid eventProperties

        // Call the trackEvent method
        val result = vwoClient.trackEvent(eventName, context, mutableMapOf())

        // Assert that the method returns the correct data
        assertNotNull(result)
        assertEquals(false, result?.get(eventName))
    }

    @Test
    fun `should handle error when context does not have a valid User ID`() {
        // Initialize VWO SDK
        init(vwoInitOptions, object : IVwoInitCallback {
            override fun vwoInitSuccess(vwoClient: VWO, message: String) {
                this@VWOTrackEventTest.vwoClient = vwoClient
            }

            override fun vwoInitFailed(message: String) {
                // Initialization failed
            }
        })

        // Mock input data with invalid context
        val eventName = "testEvent"
        val eventProperties = mutableMapOf<String, Any>("key" to "value")
        val invalidContext = VWOUserContext() // Invalid context without userId

        // Call the trackEvent method
        val result = vwoClient.trackEvent(eventName, invalidContext, eventProperties)

        // Assert that the method returns the correct data
        assertNotNull(result)
        assertEquals(false, result?.get(eventName))
    }
} 