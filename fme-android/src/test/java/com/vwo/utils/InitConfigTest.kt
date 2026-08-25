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
package com.vwo.utils

import com.wingify.ServiceContainer
import com.wingify.enums.EventEnum
import com.wingify.models.Settings
import com.wingify.models.user.WingifyInitOptions
import com.wingify.interfaces.integration.IntegrationCallback
import com.wingify.interfaces.networking.NetworkClientInterface
import com.wingify.packages.segmentation_evaluator.evaluators.SegmentEvaluator
import com.wingify.packages.storage.Connector
import com.wingify.services.SettingsManager
import com.wingify.utils.InitConfig
import com.wingify.utils.NetworkUtil
import com.wingify.utils.SDKMetaUtil
import com.wingify.utils.UsageStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock

class InitConfigTest {

    @Test
    fun `from uses Node property names and omits Android-only aliases`() {
        val options = WingifyInitOptions().apply {
            accountId = 1204581
            sdkKey = "test-sdk-key"
            pollInterval = 60_000
            logger = mapOf("level" to "DEBUG")
            _vwo_meta = mapOf("ea" to 1)
            gatewayService = mapOf("url" to "https://gw.example.com")
            isAliasingEnabled = true
            isUsageStatsDisabled = true
            shouldTriggerIntegrationCallbackAlways = true
            cachedSettingsExpiryTime = 5_000
            segmentEvaluator = mock<SegmentEvaluator>()
            networkClientInterface = mock<NetworkClientInterface>()
            integrations = mock<IntegrationCallback>()
            storage = object : Connector() {
                override fun set(data: Map<String, Any>) {}
                override fun get(featureKey: String?, userId: String?): Any? = null
            }
        }

        val initConfig = InitConfig.from(options)

        assertEquals(1204581, initConfig["accountId"])
        assertEquals("test-sdk-key", initConfig["sdkKey"])
        assertEquals(60_000, initConfig["pollInterval"])
        assertEquals(SDKMetaUtil.sdkName, initConfig["sdkName"])
        assertEquals(SDKMetaUtil.sdkVersion, initConfig["sdkVersion"])
        assertEquals(true, initConfig["isUsageStatsDisabled"])
        assertEquals(true, initConfig["isAliasingEnabled"])
        assertEquals(true, initConfig["shouldTriggerIntegrationCallbackAlways"])
        assertEquals(5_000, initConfig["cachedSettingsExpiryTime"])
        assertEquals(mapOf("url" to "https://gw.example.com"), initConfig["gatewayService"])
        assertEquals(mapOf("ea" to 1), initConfig["_vwo_meta"])
        assertEquals(mapOf("level" to "DEBUG"), initConfig["logger"])
        assertEquals(true, initConfig["segmentation"])
        assertEquals(true, initConfig["network"])
        assertEquals(true, initConfig["integrations"])
        assertEquals(true, initConfig["storage"])

        assertFalse(initConfig.containsKey("retryConfig"))
        assertFalse(initConfig.containsKey("vwoMeta"))
        assertFalse(initConfig.containsKey("storageConnector"))
        assertFalse(initConfig.containsKey("logTransport"))
        assertFalse(initConfig.containsKey("networkClientInterface"))
        assertFalse(initConfig.containsKey("segmentEvaluator"))
        assertFalse(initConfig.containsKey("isGatewayServiceConfigured"))
    }

    @Test
    fun `from omits default storage and empty optional maps`() {
        val options = WingifyInitOptions().apply {
            accountId = 1
            sdkKey = "k"
        }

        val initConfig = InitConfig.from(options)

        assertFalse(initConfig.containsKey("storage"))
        assertFalse(initConfig.containsKey("logger"))
        assertFalse(initConfig.containsKey("gatewayService"))
        assertFalse(initConfig.containsKey("_vwo_meta"))
        assertFalse(initConfig.containsKey("cachedSettingsExpiryTime"))
        assertFalse(initConfig.containsKey("batchMinSize"))
        assertFalse(initConfig.containsKey("retryConfig"))
    }

    @Test
    fun `sdk init payload includes only isSDKInitialized in event data`() {
        val options = WingifyInitOptions().apply {
            accountId = 99
            sdkKey = "payload-key"
            logger = mapOf("level" to "INFO")
        }
        val container = ServiceContainer(
            SettingsManager(options),
            options,
            Settings(),
            null,
        )

        val payload = NetworkUtil.getSDKInitEventPayload(
            EventEnum.VWO_INIT_CALLED.value,
            container,
        )

        val data = payload.eventData()
        assertEquals(true, data["isSDKInitialized"])
        assertFalse(data.containsKey("settingsFetchTime"))
        assertFalse(data.containsKey("sdkInitTime"))
        assertFalse(data.containsKey("initConfig"))
    }

    @Test
    fun `usage stats payload nests initConfig under event data beside vwoMeta`() {
        val options = WingifyInitOptions().apply {
            accountId = 99
            sdkKey = "payload-key"
            logger = mapOf("level" to "INFO")
        }
        val container = ServiceContainer(
            SettingsManager(options),
            options,
            Settings(),
            null,
        )
        container.usageStats = UsageStats()

        val payload = NetworkUtil.getSDKUsageStatsEventPayload(
            EventEnum.VWO_USAGE_STATS,
            1,
            container,
            settingsFetchTime = 10L,
            sdkInitTime = 20L,
        )

        @Suppress("UNCHECKED_CAST")
        val data = payload.eventProps()["data"] as Map<String, Any>
        assertEquals(10L, (data["settingsFetchTime"] as Number).toLong())
        assertEquals(20L, (data["sdkInitTime"] as Number).toLong())
        @Suppress("UNCHECKED_CAST")
        val initConfig = data["initConfig"] as Map<String, Any>
        assertEquals(99, (initConfig["accountId"] as Number).toInt())
        assertEquals("payload-key", initConfig["sdkKey"])
        assertEquals(mapOf("level" to "INFO"), initConfig["logger"])
        assertFalse(initConfig.containsKey("retryConfig"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.eventProps(): Map<String, Any> {
        val d = this["d"] as Map<String, Any>
        val event = d["event"] as Map<String, Any>
        return event["props"] as Map<String, Any>
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any>.eventData(): Map<String, Any> {
        val d = this["d"] as Map<String, Any>
        val event = d["event"] as Map<String, Any>
        val props = event["props"] as Map<String, Any>
        return props["data"] as Map<String, Any>
    }
}
