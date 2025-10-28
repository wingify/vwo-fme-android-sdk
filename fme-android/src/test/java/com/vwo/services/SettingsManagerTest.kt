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
package com.vwo.services

import com.vwo.VWOClient
import com.vwo.interfaces.logger.LogTransport
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.interfaces.networking.NetworkClientInterface
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.packages.network_layer.models.ResponseModel
import com.vwo.constants.Constants
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsManagerTest {

    private val loggedLevels = mutableListOf<LogLevelEnum>()
    private val loggedMessages = mutableListOf<String>()

    @Before
    fun setUp() {
        // Attach a mock client that returns a controlled settings response
        val mockClient = object : NetworkClientInterface {
            override fun GET(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data = "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{},\"extraKey\":\"testValue\"}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    statusCode = 200
                    data = "{}"
                }
            }
        }
        NetworkManager.attachClient(mockClient)

        // Configure logger to capture messages
        val transports: MutableList<Map<String, Any>> = mutableListOf()
        val transport: MutableMap<String, Any> = mutableMapOf()

        transport["defaultTransport"] = object : LogTransport {
            override fun log(level: LogLevelEnum, message: String?) {
                if (message == null) return
                loggedLevels.add(level)
                loggedMessages.add(message)
            }
        }

        transports.add(transport)
        val loggerConfig = mutableMapOf<String, Any>().apply {
            put("level", "TRACE")
            put("transports", transports)
        }
        LoggerService(loggerConfig)
    }

    @After
    fun tearDown() {
        loggedLevels.clear()
        loggedMessages.clear()
    }

    @Test
    fun `getSettings handles empty campaigns with no error logs`() {
        // Override client for this test to focus on empty campaigns behavior
        val mockClient = object : NetworkClientInterface {
            override fun GET(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data = "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{}}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    statusCode = 200
                    data = "{}"
                }
            }
        }
        NetworkManager.attachClient(mockClient)

        val options = VWOInitOptions().apply {
            sdkKey = "dummy-sdk-key"
            accountId = 123456
            cachedSettingsExpiryTime = 0
        }

        val settingsManager = SettingsManager(options)

        val settingsJson = settingsManager.getSettings(false)
        assertNotNull("Settings should not be null", settingsJson)

        val node = VWOClient.objectMapper.readTree(settingsJson!!)

        // features and campaigns should be arrays (transformed from empty objects)
        assertTrue(node.asJsonObject.has("features"))
        assertTrue(node.asJsonObject.get("features").isJsonArray)
        assertEquals(0, node.asJsonObject.get("features").asJsonArray.size())

        assertTrue(node.asJsonObject.has("campaigns"))
        assertTrue(node.asJsonObject.get("campaigns").isJsonArray)
        assertEquals(0, node.asJsonObject.get("campaigns").asJsonArray.size())

        // Ensure no error logs were emitted
        val hasError = loggedLevels.any { it == LogLevelEnum.ERROR }
        assertTrue("No error logs should be emitted", !hasError)
    }

    @Test
    fun `getSettings handles empty features with no error logs`() {
        // Override client for this test to focus on empty features behavior
        val mockClient = object : NetworkClientInterface {
            override fun GET(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data = "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{}}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    statusCode = 200
                    data = "{}"
                }
            }
        }
        NetworkManager.attachClient(mockClient)

        val options = VWOInitOptions().apply {
            sdkKey = "dummy-sdk-key"
            accountId = 123456
            cachedSettingsExpiryTime = 0
        }

        val settingsManager = SettingsManager(options)

        val settingsJson = settingsManager.getSettings(false)
        assertNotNull("Settings should not be null", settingsJson)

        val node = VWOClient.objectMapper.readTree(settingsJson!!)

        // features should be an empty array
        assertTrue(node.asJsonObject.has("features"))
        assertTrue(node.asJsonObject.get("features").isJsonArray)
        assertEquals(0, node.asJsonObject.get("features").asJsonArray.size())

        // campaigns should be an empty array
        assertTrue(node.asJsonObject.has("campaigns"))
        assertTrue(node.asJsonObject.get("campaigns").isJsonArray)
        assertEquals(0, node.asJsonObject.get("campaigns").asJsonArray.size())

        // Ensure no error logs were emitted
        val hasError = loggedLevels.any { it == LogLevelEnum.ERROR }
        assertTrue("No error logs should be emitted", !hasError)
    }

    @Test
    fun `getSettings preserves additional key and remains valid`() {
        // Override client for this test to include additional key
        val mockClient = object : NetworkClientInterface {
            override fun GET(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data = "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{},\"extraKey\":\"testValue\"}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    statusCode = 200
                    data = "{}"
                }
            }
        }
        NetworkManager.attachClient(mockClient)

        val options = VWOInitOptions().apply {
            sdkKey = "dummy-sdk-key"
            accountId = 123456
            cachedSettingsExpiryTime = 0
        }

        val settingsManager = SettingsManager(options)

        val settingsJson = settingsManager.getSettings(false)
        assertNotNull("Settings should not be null", settingsJson)

        val node = VWOClient.objectMapper.readTree(settingsJson!!)

        // extraKey must be preserved
        assertTrue(node.asJsonObject.has("extraKey"))
        assertEquals("testValue", node.asJsonObject.get("extraKey").asString)

        // Ensure no error logs were emitted
        val hasError = loggedLevels.any { it == LogLevelEnum.ERROR }
        assertTrue("No error logs should be emitted", !hasError)
    }

    @Test
    fun `getSettings converts non-empty campaigns object into array`() {
        // Mock with campaigns as a non-empty object
        val mockClient = object : NetworkClientInterface {
            override fun GET(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data = "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{\"c1\":{\"id\":1}},\"extraKey\":\"testValue\"}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(request: RequestModel): ResponseModel {
                return ResponseModel().apply {
                    statusCode = 200
                    data = "{}"
                }
            }
        }
        NetworkManager.attachClient(mockClient)

        val options = VWOInitOptions().apply {
            sdkKey = "dummy-sdk-key"
            accountId = 123456
            cachedSettingsExpiryTime = 0
        }

        val settingsManager = SettingsManager(options)
        val settingsJson = settingsManager.getSettings(false)
        assertNotNull("Settings should not be null", settingsJson)

        val node = VWOClient.objectMapper.readTree(settingsJson!!)

        // campaigns must be converted to an array even if object had entries
        assertTrue(node.asJsonObject.has("campaigns"))
        assertTrue(node.asJsonObject.get("campaigns").isJsonArray)
        assertEquals(0, node.asJsonObject.get("campaigns").asJsonArray.size())

        // Ensure no error logs were emitted
        val hasError = loggedLevels.any { it == LogLevelEnum.ERROR }
        assertTrue("No error logs should be emitted", !hasError)
    }
}


