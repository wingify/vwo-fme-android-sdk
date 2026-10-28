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
package com.vwo.packages.segmentor

import android.content.res.Configuration
import com.vwo.models.user.VWOInitOptions
import com.wingify.ServiceContainer
import com.wingify.models.user.GatewayService
import com.wingify.models.user.WingifyUserContext
import com.wingify.packages.segmentation_evaluator.core.SegmentationManager
import com.wingify.packages.segmentation_evaluator.evaluators.SegmentEvaluator
import com.wingify.providers.StorageProvider
import com.wingify.utils.DeviceInfo
import com.wingify.utils.deviceTypeLookupError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DeviceTypeOperandTests {

    private val segmentationManager = SegmentationManager()
    private val context by lazy { RuntimeEnvironment.getApplication() }
    private lateinit var evaluator: SegmentEvaluator

    @Before
    fun setup() {
        context.resources.configuration.smallestScreenWidthDp = 320
        context.resources.configuration.uiMode = Configuration.UI_MODE_TYPE_NORMAL
        StorageProvider.contextRef = WeakReference(context)
        evaluator = SegmentEvaluator(
            ServiceContainer(
                settingsManager = null,
                options = VWOInitOptions(),
                settings = null,
                loggerService = null
            )
        )
        segmentationManager.attachEvaluator(evaluator)
    }

    @Test
    fun deviceTypeIsMobileOnPhone() {
        assertNull(deviceTypeLookupError(context))
        assertEquals("mobile", DeviceInfo().getDeviceType(context))
    }

    @Test
    fun deviceTypeIsTabletWhenSw600() {
        context.resources.configuration.smallestScreenWidthDp = 600
        assertEquals("tablet", DeviceInfo().getDeviceType(context))
    }

    @Test
    fun deviceTypeIsTvWhenTelevisionUiMode() {
        context.resources.configuration.uiMode = Configuration.UI_MODE_TYPE_TELEVISION
        assertEquals("tv", DeviceInfo().getDeviceType(context))
    }

    @Test
    fun mobileWildcardMatchesLocalDeviceType() {
        assertTrue(
            segmentationManager.validateSegmentation(
                "{ \"or\": [ { \"device_type\": \"wildcard(*mobile*)\" } ] }",
                emptyMap()
            )
        )
    }

    @Test
    fun desktopWildcardDoesNotMatchPhone() {
        assertFalse(
            segmentationManager.validateSegmentation(
                "{ \"or\": [ { \"device_type\": \"wildcard(*desktop*)\" } ] }",
                emptyMap()
            )
        )
    }

    @Test
    fun mixedOrUsesUaParserWhenLocalDeviceTypeMisses() {
        stubGatewayUserAgent(mapOf("browser_string" to "Chrome"))
        assertTrue(
            segmentationManager.validateSegmentation(
                "{ \"or\": [ { \"device_type\": \"wildcard(*desktop*)\" }, { \"browser_string\": \"wildcard(*Chrome*)\" } ] }",
                emptyMap()
            )
        )
    }

    @Test
    fun mixedOrUsesUaParserWhenLocalOsMisses() {
        stubGatewayUserAgent(mapOf("browser_string" to "Chrome"))
        assertTrue(
            segmentationManager.validateSegmentation(
                "{ \"or\": [ { \"os\": \"wildcard(*iOS*)\" }, { \"browser_string\": \"wildcard(*Chrome*)\" } ] }",
                emptyMap()
            )
        )
    }

    @Test
    fun mixedOrIsFalseWhenLocalAndUaParserBothMiss() {
        stubGatewayUserAgent(mapOf("browser_string" to "Safari"))
        assertFalse(
            segmentationManager.validateSegmentation(
                "{ \"or\": [ { \"os\": \"wildcard(*iOS*)\" }, { \"browser_string\": \"wildcard(*Chrome*)\" } ] }",
                emptyMap()
            )
        )
    }

    @Test
    fun evaluateUaParserMapIsFalseWhenEmpty() {
        assertFalse(evaluator.evaluateUaParserMap(emptyMap()))
    }

    @Test
    fun evaluateUaParserMapIsTrueWhenBrowserStringMatches() {
        stubGatewayUserAgent(mapOf("browser_string" to "Chrome"))
        assertTrue(
            evaluator.evaluateUaParserMap(
                mapOf("browser_string" to mutableListOf("wildcard(*Chrome*)"))
            )
        )
    }

    @Test
    fun evaluateUaParserMapIsFalseWhenBrowserStringMisses() {
        stubGatewayUserAgent(mapOf("browser_string" to "Safari"))
        assertFalse(
            evaluator.evaluateUaParserMap(
                mapOf("browser_string" to mutableListOf("wildcard(*Chrome*)"))
            )
        )
    }

    @Test
    fun evaluateUaParserMapIsTrueWhenDeviceMatches() {
        stubGatewayUserAgent(mapOf("device" to "iPhone"))
        assertTrue(
            evaluator.evaluateUaParserMap(
                mapOf("device" to mutableListOf("wildcard(*iPhone*)"))
            )
        )
    }

    @Test
    fun evaluateUaParserMapIsFalseWhenParserThrows() {
        stubGatewayUserAgent(mapOf("browser_string" to "Chrome"))
        assertFalse(
            evaluator.evaluateUaParserMap(
                mapOf("browser_string" to mutableListOf("wildcard(*[*)"))
            )
        )
    }

    private fun stubGatewayUserAgent(parsed: Map<String, String>) {
        StorageProvider.userAgent = "test-agent"
        evaluator.context = WingifyUserContext().apply {
            vwo = GatewayService().apply { userAgent = parsed }
        }
    }
}
