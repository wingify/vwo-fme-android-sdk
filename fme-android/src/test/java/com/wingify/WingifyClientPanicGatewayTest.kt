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

import com.vwo.VWOClient
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.wingify.enums.EventEnum
import com.wingify.interfaces.networking.NetworkClientInterface
import com.wingify.packages.network_layer.manager.NetworkManager
import com.wingify.packages.network_layer.models.RequestModel
import com.wingify.packages.network_layer.models.ResponseModel
import com.wingify.providers.ServiceContainerProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(MockitoJUnitRunner::class)
class WingifyClientPanicGatewayTest {

    @Mock
    private lateinit var mockNetworkClient: NetworkClientInterface

    private lateinit var client: VWOClient
    private val postedRequests = CopyOnWriteArrayList<RequestModel>()

    @Before
    fun setUp() {
        ServiceContainerProvider.clearAllServiceContainers()
        postedRequests.clear()

        val options = VWOInitOptions().apply {
            accountId = 12345
            sdkKey = "panic-gateway-sdk"
            isAliasingEnabled = true
            gatewayService = mapOf(
                "url" to "https://gateway.example.com",
                "host" to "gateway.example.com",
            )
        }
        val builder = WingifyBuilder(options).setLogger().setSettingsManager()
        builder.getSettingsManager()?.apply {
            hostname = "gateway.example.com"
            isGatewayHostValid = true
        }

        client = VWOClient(null, options, builder)
        client.createServiceContainer().panicManager.applyResponseControl("""{"panicMode":9999}""")
        assertTrue(client.createServiceContainer().panicManager.isPanicActive())

        NetworkManager.attachClient(mockNetworkClient)
        val okResponse = ResponseModel().apply {
            statusCode = 200
            data = """{"panicMode":9999}"""
        }
        whenever(mockNetworkClient.POST(any(), any())).thenAnswer { invocation ->
            postedRequests.add(invocation.getArgument(0))
            okResponse
        }
    }

    @After
    fun tearDown() {
        ServiceContainerProvider.clearAllServiceContainers()
        NetworkManager.attachClient()
    }

    @Test
    fun getFlag_doesNotResolveAliasViaGatewayWhenPanicIsActive() {
        val context = VWOUserContext().apply { id = "alias-context-id" }

        val flag = client.getFlag("any-feature", context)

        assertFalse(flag.isEnabled())
        verify(mockNetworkClient, never()).GET(any(), any())
    }

    @Test
    fun trackEvent_doesNotResolveAliasViaGatewayWhenPanicIsActive() {
        val context = VWOUserContext().apply { id = "alias-context-id" }

        val result = client.trackEvent("purchase", context)

        assertFalse(result["purchase"] == true)
        verify(mockNetworkClient, never()).GET(any(), any())
    }

    @Test
    fun publicApis_doNotSendSdkDebugWhenPanicIsActive() {
        val context = VWOUserContext().apply { id = "panic-user" }

        client.getFlag("any-feature", context)
        client.trackEvent("purchase", context)
        client.setAttribute(mapOf("plan" to "gold"), context)
        client.setAlias(context, "alias-1")
        client.sendSdkInitEvent()
        client.sendUsageStats(1L, client.createServiceContainer())

        waitForHeartbeatPosts()
        assertTrue("panic heartbeat must still be sent: $postedRequests", heartbeatPosts().isNotEmpty())
        assertTrue(
            "vwo_sdkDebug must not be sent while SDK is disabled: ${sdkDebugPosts()}",
            sdkDebugPosts().isEmpty()
        )
        assertTrue(client.createServiceContainer().panicManager.isPanicActive())
    }

    private fun waitForHeartbeatPosts() {
        val deadline = System.currentTimeMillis() + 2000
        while (heartbeatPosts().isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        Thread.sleep(250)
    }

    private fun heartbeatPosts(): List<RequestModel> =
        postsNamed(EventEnum.VWO_COLLECT_CONTROL_PING.value)

    private fun sdkDebugPosts(): List<RequestModel> =
        postsNamed(EventEnum.VWO_DEBUGGER_EVENT.value)

    private fun postsNamed(eventName: String): List<RequestModel> {
        return postedRequests.filter { request ->
            request.eventName == eventName || request.query?.get("en") == eventName
        }
    }
}
