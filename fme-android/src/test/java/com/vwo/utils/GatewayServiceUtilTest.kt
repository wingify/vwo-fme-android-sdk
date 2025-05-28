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
package com.vwo.utils

import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.packages.network_layer.models.ResponseModel
import com.vwo.services.SettingsManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import com.vwo.interfaces.networking.NetworkClientInterface
import org.junit.Assert.assertFalse

@RunWith(MockitoJUnitRunner::class)
class GatewayServiceUtilTest {

    @Mock
    private lateinit var mockSettingsManager: SettingsManager

    @Mock
    private lateinit var mockNetworkClient: NetworkClientInterface

    @Before
    fun setUp() {
        val sdkKey = "test-sdk-key"
        val accountId = 1234
        val vwoInitOptions = VWOInitOptions()
        vwoInitOptions.sdkKey = sdkKey
        vwoInitOptions.accountId = accountId
        SettingsManager(vwoInitOptions)

        SettingsManager.instance?.hostname = "test.vwo.com"
        SettingsManager.instance?.protocol = "https"
        SettingsManager.instance?.port = 443

        NetworkManager.attachClient(mockNetworkClient)
    }

    @Test
    fun `getFromGatewayService returns response data when network call is successful`() {
        val queryParams = mutableMapOf("key" to "value")
        val endpoint = "/test-endpoint"
        val expectedResponse = "test response"

        val responseModel = ResponseModel().apply {
            statusCode = 200
            data = expectedResponse
        }
        `when`(mockNetworkClient.GET(any<RequestModel>())).thenReturn(responseModel)

        val result = GatewayServiceUtil.getFromGatewayService(queryParams, endpoint)

        assertEquals(expectedResponse, result)
        verify(mockNetworkClient).GET(org.mockito.kotlin.argThat { request ->
            request.url == "test.vwo.com" &&
                    request.method == "GET" &&
                    request.path == endpoint &&
                    request.query == queryParams &&
                    request.scheme == "https" &&
                    request.port == 443
        })
    }

    @Test
    fun `getFromGatewayService returns null when network call fails`() {
        val queryParams = mutableMapOf("key" to "value")
        val endpoint = "/test-endpoint"

        `when`(mockNetworkClient.GET(any<RequestModel>())).thenReturn(null)

        val result = GatewayServiceUtil.getFromGatewayService(queryParams, endpoint)

        assertNull(result)
        verify(mockNetworkClient).GET(org.mockito.kotlin.argThat { request ->
            request.url == "test.vwo.com" &&
                    request.path == endpoint
        })
    }

    @Test
    fun `getFromGatewayService returns null when exception occurs`() {
        val queryParams = mutableMapOf("key" to "value")
        val endpoint = "/test-endpoint"

        `when`(mockNetworkClient.GET(any<RequestModel>())).thenThrow(RuntimeException("Network error"))

        val result = GatewayServiceUtil.getFromGatewayService(queryParams, endpoint)

        assertNull(result)
        verify(mockNetworkClient).GET(org.mockito.kotlin.argThat { request ->
            request.url == "test.vwo.com" &&
                    request.path == endpoint
        })
    }

    @Test
    fun `getFromGatewayService uses custom response type when provided`() {
        val queryParams = mutableMapOf("key" to "value")
        val endpoint = "/test-endpoint"
        val expectedResponseType = "application/javascript"

        val responseModel = ResponseModel().apply {
            statusCode = 200
            data = "test response"
        }
        `when`(mockNetworkClient.GET(any<RequestModel>())).thenReturn(responseModel)

        val response = GatewayServiceUtil.getFromGatewayService(queryParams, endpoint, expectedResponseType)

        assertEquals(responseModel.data, response)
    }

    @Test
    fun `getQueryParams encodes parameter values`() {
        val queryParams = mapOf(
            "key1" to "value with spaces",
            "key2" to "special&chars",
            "key3" to null
        )

        val result = GatewayServiceUtil.getQueryParams(queryParams)

        assertEquals("value+with+spaces", result["key1"])
        assertEquals("special%26chars", result["key2"])
        assertFalse(result.containsKey("key3"))
    }

    @Test
    fun `getQueryParams handles empty map`() {
        val queryParams = emptyMap<String, String?>()

        val result = GatewayServiceUtil.getQueryParams(queryParams)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getQueryParams handles map with null values`() {
        val queryParams = mapOf(
            "key1" to null,
            "key2" to null
        )

        val result = GatewayServiceUtil.getQueryParams(queryParams)

        assertTrue(result.isEmpty())
    }
}