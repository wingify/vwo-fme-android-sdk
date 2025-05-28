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
package com.vwo.models

import com.vwo.VWOBuilder
import com.vwo.constants.Constants
import com.vwo.interfaces.integration.IntegrationCallback
import com.vwo.interfaces.networking.NetworkClientInterface
import com.vwo.packages.segmentation_evaluator.evaluators.SegmentEvaluator
import com.vwo.packages.storage.Connector
import com.vwo.sdk.fme.BuildConfig


import android.content.Context
import com.vwo.models.user.VWOInitOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VWOInitOptionsTest {

    private lateinit var initOptions: VWOInitOptions

    @Mock // Mock the Context
    private lateinit var mockContext: Context

    @Mock // Mock the IntegrationCallback
    private lateinit var mockIntegrationCallback: IntegrationCallback

    @Mock // Mock the NetworkClientInterface
    private lateinit var mockNetworkClientInterface: NetworkClientInterface

    @Mock // Mock the SegmentEvaluator
    private lateinit var mockSegmentEvaluator: SegmentEvaluator

    @Mock // Mock the Connector
    private lateinit var mockConnector: Connector

    @Mock // Mock the VWOBuilder
    private lateinit var mockVWOBuilder: VWOBuilder


    @Before
    fun setup() {
        initOptions = VWOInitOptions()
    }

    @Test
    fun `test default values`() {
        assertNull(initOptions.sdkKey)
        assertNull(initOptions.accountId)
        assertNull(initOptions.integrations)
        assertNotNull(initOptions.logger) // logger is initialized to HashMap()
        assertTrue(initOptions.logger.isEmpty()) // logger should be an empty map by default
        assertNull(initOptions.networkClientInterface)
        assertNull(initOptions.segmentEvaluator)
        assertNotNull(initOptions.storage) // storage is initialized to MobileDefaultStorage()
        // You might want to assert the type of storage if needed:
        // assertTrue(initOptions.storage is MobileDefaultStorage)
        assertNull(initOptions.pollInterval)
        assertNull(initOptions.vwoBuilder)
        assertNotNull(initOptions.gatewayService) // gatewayService is initialized to HashMap()
        assertTrue(initOptions.gatewayService.isEmpty()) // gatewayService should be an empty map by default
        assertNull(initOptions.context)
        assertEquals(0, initOptions.cachedSettingsExpiryTime)
        assertEquals(Constants.SDK_NAME, initOptions.sdkName) // Assert default sdkName
        assertEquals(BuildConfig.SDK_VERSION, initOptions.sdkVersion) // Assert default sdkVersion
        assertEquals(-1, initOptions.batchMinSize) // Assert default batchMinSize
        assertEquals(
            -1L,
            initOptions.batchUploadTimeInterval
        ) // Assert default batchUploadTimeInterval
        assertFalse(initOptions.isUsageStatsDisabled) // Assert default isUsageStatsDisabled
        assertNotNull(initOptions._vwo_meta) // _vwo_meta is initialized to emptyMap()
        assertTrue(initOptions._vwo_meta.isEmpty()) // _vwo_meta should be an empty map by default
    }

    @Test
    fun `test setting and getting sdkKey`() {
        val testSdkKey = "test_sdk_key"
        initOptions.sdkKey = testSdkKey
        assertEquals(testSdkKey, initOptions.sdkKey)
    }

    @Test
    fun `test setting and getting accountId`() {
        val testAccountId = 123
        initOptions.accountId = testAccountId
        assertEquals(testAccountId, initOptions.accountId)
    }

    @Test
    fun `test setting and getting integrations`() {
        initOptions.integrations = mockIntegrationCallback
        assertEquals(mockIntegrationCallback, initOptions.integrations)
    }

    @Test
    fun `test setting and getting logger`() {
        val testLogger = mapOf("level" to "DEBUG", "output" to "console")
        initOptions.logger = testLogger
        assertEquals(testLogger, initOptions.logger)
    }

    @Test
    fun `test setting and getting networkClientInterface`() {
        initOptions.networkClientInterface = mockNetworkClientInterface
        assertEquals(mockNetworkClientInterface, initOptions.networkClientInterface)
    }

    @Test
    fun `test setting and getting segmentEvaluator`() {
        initOptions.segmentEvaluator = mockSegmentEvaluator
        assertEquals(mockSegmentEvaluator, initOptions.segmentEvaluator)
    }

    @Test
    fun `test setting and getting storage`() {
        initOptions.storage = mockConnector
        assertEquals(mockConnector, initOptions.storage)
    }

    @Test
    fun `test setting and getting pollInterval`() {
        val testPollInterval = 300
        initOptions.pollInterval = testPollInterval
        assertEquals(testPollInterval, initOptions.pollInterval)
    }

    @Test
    fun `test setting and getting vwoBuilder`() {
        initOptions.vwoBuilder = mockVWOBuilder
        assertEquals(mockVWOBuilder, initOptions.vwoBuilder)
    }

    @Test
    fun `test setting and getting gatewayService`() {
        val testGatewayService = mapOf("url" to "http://test.com")
        initOptions.gatewayService = testGatewayService
        assertEquals(testGatewayService, initOptions.gatewayService)
    }

    @Test
    fun `test setting and getting context`() {
        initOptions.context = mockContext
        assertEquals(mockContext, initOptions.context)
    }

    @Test
    fun `test setting and getting cachedSettingsExpiryTime`() {
        val testExpiryTime = 3600
        initOptions.cachedSettingsExpiryTime = testExpiryTime
        assertEquals(testExpiryTime, initOptions.cachedSettingsExpiryTime)
    }

    @Test
    fun `test setting and getting sdkName`() {
        val testSdkName = "react-native"
        initOptions.sdkName = testSdkName
        assertEquals(testSdkName, initOptions.sdkName)
    }

    @Test
    fun `test setting and getting sdkVersion`() {
        val testSdkVersion = "2.0.0"
        initOptions.sdkVersion = testSdkVersion
        assertEquals(testSdkVersion, initOptions.sdkVersion)
    }

    @Test
    fun `test setting and getting batchMinSize`() {
        val testBatchMinSize = 50
        initOptions.batchMinSize = testBatchMinSize
        assertEquals(testBatchMinSize, initOptions.batchMinSize)
    }

    @Test
    fun `test setting and getting batchUploadTimeInterval`() {
        val testBatchUploadTimeInterval = 120000L // 2 minutes
        initOptions.batchUploadTimeInterval = testBatchUploadTimeInterval
        assertEquals(testBatchUploadTimeInterval, initOptions.batchUploadTimeInterval)
    }

    @Test
    fun `test setting and getting isUsageStatsDisabled`() {
        val testIsUsageStatsDisabled = true
        initOptions.isUsageStatsDisabled = testIsUsageStatsDisabled
        assertEquals(testIsUsageStatsDisabled, initOptions.isUsageStatsDisabled)
    }

    @Test
    fun `test setting and getting _vwo_meta`() {
        val testVwoMeta = mapOf("key" to "value")
        initOptions._vwo_meta = testVwoMeta
        assertEquals(testVwoMeta, initOptions._vwo_meta)
    }

    @Test
    fun `test setting and getting all properties`() {
        val testLogger = mapOf("level" to "INFO")
        val testGatewayService = mapOf("url" to "http://another.com")
        val testVwoMeta = mapOf("meta_key" to "meta_value")

        initOptions.apply {
            sdkKey = "test_sdk_key_all"
            accountId = 456
            integrations = mockIntegrationCallback
            logger = testLogger
            networkClientInterface = mockNetworkClientInterface
            segmentEvaluator = mockSegmentEvaluator
            storage = mockConnector
            pollInterval = 600
            vwoBuilder = mockVWOBuilder
            gatewayService = testGatewayService
            context = mockContext
            cachedSettingsExpiryTime = 7200
            sdkName = "android-native"
            sdkVersion = "3.0.0"
            batchMinSize = 100
            batchUploadTimeInterval = 300000L // 5 minutes
            isUsageStatsDisabled = true
            _vwo_meta = testVwoMeta
        }

        assertEquals("test_sdk_key_all", initOptions.sdkKey)
        assertEquals(456, initOptions.accountId)
        assertEquals(mockIntegrationCallback, initOptions.integrations)
        assertEquals(testLogger, initOptions.logger)
        assertEquals(mockNetworkClientInterface, initOptions.networkClientInterface)
        assertEquals(mockSegmentEvaluator, initOptions.segmentEvaluator)
        assertEquals(mockConnector, initOptions.storage)
        assertEquals(600, initOptions.pollInterval)
        assertEquals(mockVWOBuilder, initOptions.vwoBuilder)
        assertEquals(testGatewayService, initOptions.gatewayService)
        assertEquals(mockContext, initOptions.context)
        assertEquals(7200, initOptions.cachedSettingsExpiryTime)
        assertEquals("android-native", initOptions.sdkName)
        assertEquals("3.0.0", initOptions.sdkVersion)
        assertEquals(100, initOptions.batchMinSize)
        assertEquals(300000L, initOptions.batchUploadTimeInterval)
        assertEquals(true, initOptions.isUsageStatsDisabled)
        assertEquals(testVwoMeta, initOptions._vwo_meta)
    }
}