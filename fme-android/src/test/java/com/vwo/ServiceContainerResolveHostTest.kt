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
package com.vwo

import com.wingify.ServiceContainer
import com.wingify.constants.Constants
import com.wingify.interfaces.networking.HttpMethods
import com.wingify.models.Settings
import com.vwo.models.user.VWOInitOptions
import com.wingify.models.user.WingifyInitOptions
import com.wingify.services.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceContainerResolveHostTest {

    @Test
    fun `resolveHost uses edge for GET when new endpoints enabled`() {
        val container = createContainer(newEndpoints = true)

        assertEquals(Constants.EDGE_HOST_NAME, container.resolveHost(HttpMethods.GET))
    }

    @Test
    fun `resolveHost uses collect for POST when new endpoints enabled`() {
        val container = createContainer(newEndpoints = true)

        assertEquals(Constants.COLLECT_HOST_NAME, container.resolveHost(HttpMethods.POST))
    }

    @Test
    fun `resolveHost appends collectionPrefix for POST when new endpoints enabled`() {
        val settings = Settings().apply { collectionPrefix = "as01" }
        val container = createContainer(newEndpoints = true, settings = settings)

        assertEquals(
            "${Constants.COLLECT_HOST_NAME}/as01",
            container.resolveHost(HttpMethods.POST),
        )
    }

    @Test
    fun `resolveHost does not append collectionPrefix for GET when new endpoints enabled`() {
        val settings = Settings().apply { collectionPrefix = "as01" }
        val container = createContainer(newEndpoints = true, settings = settings)

        assertEquals(Constants.EDGE_HOST_NAME, container.resolveHost(HttpMethods.GET))
    }

    @Test
    fun `resolveHost uses legacy hostname when new endpoints disabled`() {
        val container = createContainer(newEndpoints = false)

        assertEquals(Constants.HOST_NAME, container.resolveHost(HttpMethods.GET))
        assertEquals(Constants.HOST_NAME, container.resolveHost(HttpMethods.POST))
    }

    @Test
    fun `resolveHost uses valid gateway host for GET and POST`() {
        val options = VWOInitOptions().apply {
            gatewayService = mapOf(
                "url" to "gateway.example.com",
                "protocol" to "http",
            )
        }
        val settingsManager = SettingsManager(options)
        val container = ServiceContainer(settingsManager, options, Settings(), null)

        assertEquals("gateway.example.com", container.resolveHost(HttpMethods.GET))
        assertEquals("gateway.example.com", container.resolveHost(HttpMethods.POST))
    }

    @Test
    fun `resolveHost falls back to edge when gateway is invalid and new endpoints enabled`() {
        val options = WingifyInitOptions().apply {
            gatewayService = mapOf(
                "url" to "gateway.example.com",
                "port" to "not-a-port",
            )
        }
        val settingsManager = SettingsManager(options)
        val container = ServiceContainer(settingsManager, options, Settings(), null)

        assertEquals(false, settingsManager.isGatewayHostValid)
        assertEquals(Constants.EDGE_HOST_NAME, container.resolveHost(HttpMethods.GET))
    }

    @Test
    fun `resolveHost falls back to legacy host when gateway is invalid and new endpoints disabled`() {
        val options = VWOInitOptions().apply {
            gatewayService = mapOf(
                "url" to "gateway.example.com",
                "port" to "not-a-port",
            )
        }
        val settingsManager = SettingsManager(options)
        val container = ServiceContainer(settingsManager, options, Settings(), null)

        assertEquals(false, settingsManager.isGatewayHostValid)
        assertEquals(Constants.HOST_NAME, container.resolveHost(HttpMethods.GET))
    }

    @Test
    fun `resolveScheme returns gateway protocol when gateway is valid`() {
        val options = VWOInitOptions().apply {
            gatewayService = mapOf(
                "url" to "gateway.example.com",
                "protocol" to "http",
            )
        }
        val settingsManager = SettingsManager(options)
        val container = ServiceContainer(settingsManager, options, Settings(), null)

        assertEquals("http", container.resolveScheme())
    }

    @Test
    fun `resolveScheme returns https when gateway is invalid`() {
        val options = VWOInitOptions().apply {
            gatewayService = mapOf(
                "url" to "gateway.example.com",
                "port" to "not-a-port",
            )
        }
        val settingsManager = SettingsManager(options)
        val container = ServiceContainer(settingsManager, options, Settings(), null)

        assertEquals(false, settingsManager.isGatewayHostValid)
        assertEquals(Constants.HTTPS_PROTOCOL, container.resolveScheme())
    }

    @Test
    fun `resolveScheme returns https when no gateway is configured`() {
        val container = createContainer(newEndpoints = true)

        assertEquals(Constants.HTTPS_PROTOCOL, container.resolveScheme())
    }

    @Test
    fun `getBaseUrl matches resolveHost for POST`() {
        val container = createContainer(newEndpoints = true)

        assertEquals(container.resolveHost(HttpMethods.POST), container.getBaseUrl())
    }

    @Test
    fun `resolveBatchUploadHost uses collect when Wingify SDK active and skips gateway`() {
        val options = WingifyInitOptions().apply {
            gatewayService = mapOf(
                "url" to "10.0.2.2",
                "protocol" to "http",
                "port" to "8000",
            )
        }
        val settingsManager = SettingsManager(options)
        val container = ServiceContainer(settingsManager, options, Settings(), null)

        assertEquals(Constants.COLLECT_HOST_NAME, container.resolveBatchUploadHost())
        assertEquals("10.0.2.2", container.resolveHost(HttpMethods.POST))
    }

    @Test
    fun `resolveBatchUploadHost uses dev host when legacy VWO SDK active and skips gateway`() {
        val options = VWOInitOptions().apply {
            gatewayService = mapOf(
                "url" to "10.0.2.2",
                "protocol" to "http",
                "port" to "8000",
            )
        }
        val settingsManager = SettingsManager(options)
        val container = ServiceContainer(settingsManager, options, Settings(), null)

        assertEquals(Constants.HOST_NAME, container.resolveBatchUploadHost())
        assertEquals("10.0.2.2", container.resolveHost(HttpMethods.POST))
    }

    @Test
    fun `resolveBatchUploadHost appends collectionPrefix for Wingify SDK`() {
        val settings = Settings().apply { collectionPrefix = "as01" }
        val options = WingifyInitOptions()
        val container = ServiceContainer(SettingsManager(options), options, settings, null)

        assertEquals(
            "${Constants.COLLECT_HOST_NAME}/as01",
            container.resolveBatchUploadHost(),
        )
    }

    @Test
    fun `resolveBatchUploadScheme always returns https and ignores gateway`() {
        val options = VWOInitOptions().apply {
            gatewayService = mapOf(
                "url" to "gateway.example.com",
                "protocol" to "http",
            )
        }
        val container = ServiceContainer(SettingsManager(options), options, Settings(), null)

        assertEquals("http", container.resolveScheme())
        assertEquals(Constants.HTTPS_PROTOCOL, container.resolveBatchUploadScheme())
    }

    private fun createContainer(
        newEndpoints: Boolean,
        settings: Settings = Settings(),
    ): ServiceContainer {
        val options: WingifyInitOptions = if (newEndpoints) {
            WingifyInitOptions()
        } else {
            VWOInitOptions()
        }.apply {
            sdkKey = "test-sdk-key"
            accountId = 123
        }
        val settingsManager = SettingsManager(options)
        return ServiceContainer(settingsManager, options, settings, null)
    }
}
