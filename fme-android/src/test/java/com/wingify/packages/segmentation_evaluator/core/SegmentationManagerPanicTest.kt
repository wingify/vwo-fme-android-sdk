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
package com.wingify.packages.segmentation_evaluator.core

import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.wingify.ServiceContainer
import com.wingify.enums.UrlEnum
import com.wingify.interfaces.networking.HttpMethods
import com.wingify.interfaces.networking.NetworkClientInterface
import com.wingify.models.Feature
import com.wingify.models.Settings
import com.wingify.packages.network_layer.manager.NetworkManager
import com.wingify.packages.network_layer.models.RequestModel
import com.wingify.packages.network_layer.models.ResponseModel
import com.wingify.packages.storage.GatewayResponseStore
import com.wingify.providers.StorageProvider
import com.wingify.services.SettingsManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SegmentationManagerPanicTest {

    @Mock
    private lateinit var mockNetworkClient: NetworkClientInterface

    @Mock
    private lateinit var mockGatewayStore: GatewayResponseStore

    private lateinit var serviceContainer: ServiceContainer
    private lateinit var segmentationManager: SegmentationManager

    private val accountId = 100
    private val sdkKey = "test-sdk-key"

    @Before
    fun setUp() {
        StorageProvider.userAgent = "VWO-Test-Agent/1.0"
        StorageProvider.gatewayStore = mockGatewayStore

        val options = VWOInitOptions().apply {
            this.accountId = accountId
            this.sdkKey = sdkKey
            gatewayService = mapOf(
                "url" to "https://gateway.example.com",
                "host" to "gateway.example.com",
            )
        }
        val settingsManager = SettingsManager(options).apply {
            hostname = "gateway.example.com"
            isGatewayHostValid = true
        }
        serviceContainer = ServiceContainer(
            settingsManager = settingsManager,
            options = options,
            settings = Settings(),
            loggerService = null,
        )
        segmentationManager = SegmentationManager()

        NetworkManager.attachClient(mockNetworkClient)
        whenever(mockNetworkClient.GET(any(), any())).thenReturn(ResponseModel().apply {
            statusCode = 200
            data = """{"userAgent":{"browser":"test"}}"""
        })
    }

    @After
    fun tearDown() {
        StorageProvider.userAgent = ""
        StorageProvider.gatewayStore = null
        NetworkManager.attachClient()
    }

    @Test
    fun setContextualData_skipsGetUserDetailsNetworkCallWhenPanicIsActive() {
        whenever(mockGatewayStore.getGatewayResponseExpiry(any(), any())).thenReturn(-1L)
        serviceContainer.panicManager.applyResponseControl("""{"panicMode":1000}""")
        assertTrue(serviceContainer.panicManager.isPanicActive())

        val context = VWOUserContext().apply { id = "user-1" }
        val feature = Feature().apply { isGatewayServiceRequired = true }
        val settings = Settings().apply { this.accountId = accountId }

        segmentationManager.setContextualData(settings, feature, context, serviceContainer)

        assertNull(context.vwo)
        verify(mockNetworkClient, never()).GET(any(), any())
    }

    @Test
    fun setContextualData_usesCachedGatewayResponseDuringPanicWithoutNetworkCall() {
        val cachedJson = """{"userAgent":{"browser":"cached"}}"""
        whenever(mockGatewayStore.getGatewayResponseExpiry(any(), any()))
            .thenReturn(System.currentTimeMillis() + 60_000)
        whenever(mockGatewayStore.getGatewayResponse(any(), any())).thenReturn(cachedJson)

        serviceContainer.panicManager.applyResponseControl("""{"panicMode":1000}""")
        assertTrue(serviceContainer.panicManager.isPanicActive())

        val context = VWOUserContext().apply { id = "user-1" }
        val feature = Feature().apply { isGatewayServiceRequired = true }
        val settings = Settings().apply { this.accountId = accountId }

        segmentationManager.setContextualData(settings, feature, context, serviceContainer)

        assertNotNull(context.vwo)
        verify(mockNetworkClient, never()).GET(any(), any())
    }

    @Test
    fun setContextualData_fetchesGetUserDetailsWhenPanicIsOffAndCacheMiss() {
        whenever(mockGatewayStore.getGatewayResponseExpiry(any(), any())).thenReturn(-1L)
        assertFalse(serviceContainer.panicManager.isPanicActive())

        val context = VWOUserContext().apply { id = "user-1" }
        val feature = Feature().apply { isGatewayServiceRequired = true }
        val settings = Settings().apply { this.accountId = accountId }

        segmentationManager.setContextualData(settings, feature, context, serviceContainer)

        assertNotNull(context.vwo)
        val requestCaptor = argumentCaptor<RequestModel>()
        verify(mockNetworkClient).GET(requestCaptor.capture(), any())
        assertEquals(UrlEnum.GET_USER_DATA.url, requestCaptor.firstValue.path)
        assertEquals(HttpMethods.GET.value, requestCaptor.firstValue.method)
    }
}
