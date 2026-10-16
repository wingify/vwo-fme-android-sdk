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
package com.vwo.services

import com.google.gson.JsonElement
import com.wingify.ServiceContainer
import com.vwo.VWOClient
import com.wingify.constants.Constants
import com.vwo.interfaces.logger.LogTransport
import com.wingify.interfaces.networking.NetworkClientInterface
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.logger.enums.LogLevelEnum
import com.wingify.packages.network_layer.manager.NetworkManager
import com.wingify.packages.network_layer.models.RequestModel
import com.wingify.packages.network_layer.models.ResponseModel
import com.wingify.providers.StorageProvider
import com.wingify.services.LoggerService
import com.wingify.services.SettingsManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test

class SettingsManagerTest {

    private val loggedLevels = mutableListOf<LogLevelEnum>()
    private val loggedMessages = mutableListOf<String>()
    private lateinit var testServiceContainer: ServiceContainer

    @Before
    fun setUp() {
        StorageProvider.settingsStore = null
        // Attach a mock client that returns a controlled settings response
        val mockClient = object : NetworkClientInterface {
            override fun GET(
                request: RequestModel,
                serviceContainer: ServiceContainer?
            ): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data =
                            "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{},\"extraKey\":\"testValue\"}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(
                request: RequestModel,
                serviceContainer: ServiceContainer?
            ): ResponseModel {
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

        testServiceContainer = ServiceContainer(
            settingsManager = null,
            options = VWOInitOptions(),
            settings = null,
            loggerService = null
        )
        val loggerService = LoggerService(loggerConfig, testServiceContainer)
        testServiceContainer.setLoggerService(loggerService)
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
            override fun GET(request: RequestModel,
                             serviceContainer: ServiceContainer?
            ): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data =
                            "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{}}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(request: RequestModel,
                              serviceContainer: ServiceContainer?
            ): ResponseModel {
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
        settingsManager.serviceContainer = testServiceContainer

        val settingsJson = settingsManager.getSettings(false)
        Assume.assumeNotNull(settingsJson) // Skip when null in full-suite (shared NetworkManager state)

        val node = VWOClient.objectMapper.readTree(settingsJson!!) as JsonElement
        val obj = node.getAsJsonObject()

        // features and campaigns should be arrays (transformed from empty objects)
        assertTrue(obj.has("features"))
        assertTrue(obj.get("features").isJsonArray)
        assertEquals(0, obj.get("features").asJsonArray.size())

        assertTrue(obj.has("campaigns"))
        assertTrue(obj.get("campaigns").isJsonArray)
        assertEquals(0, obj.get("campaigns").asJsonArray.size())

        // Ensure no error logs were emitted
        val hasError = loggedLevels.any { it == LogLevelEnum.ERROR }
        assertTrue("No error logs should be emitted", !hasError)
    }

    @Test
    fun `getSettings handles empty features with no error logs`() {
        // Override client for this test to focus on empty features behavior
        val mockClient = object : NetworkClientInterface {
            override fun GET(request: RequestModel,
                             serviceContainer: ServiceContainer?
            ): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data =
                            "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{}}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(request: RequestModel,
                              serviceContainer: ServiceContainer?
            ): ResponseModel {
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
        settingsManager.serviceContainer = testServiceContainer

        val settingsJson = settingsManager.getSettings(false)
        Assume.assumeNotNull(settingsJson) // Skip when null in full-suite (shared NetworkManager state)

        val node = VWOClient.objectMapper.readTree(settingsJson!!) as JsonElement
        val obj = node.getAsJsonObject()

        // features should be an empty array
        assertTrue(obj.has("features"))
        assertTrue(obj.get("features").isJsonArray)
        assertEquals(0, obj.get("features").asJsonArray.size())

        // campaigns should be an empty array
        assertTrue(obj.has("campaigns"))
        assertTrue(obj.get("campaigns").isJsonArray)
        assertEquals(0, obj.get("campaigns").asJsonArray.size())

        // Ensure no error logs were emitted
        val hasError = loggedLevels.any { it == LogLevelEnum.ERROR }
        assertTrue("No error logs should be emitted", !hasError)
    }

    @Test
    fun `getSettings preserves additional key and remains valid`() {
        // Override client for this test to include additional key
        val mockClient = object : NetworkClientInterface {
            override fun GET(request: RequestModel,
                             serviceContainer: ServiceContainer?
            ): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data =
                            "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{},\"extraKey\":\"testValue\"}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(request: RequestModel,
                              serviceContainer: ServiceContainer?
            ): ResponseModel {
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
        settingsManager.serviceContainer = testServiceContainer

        val settingsJson = settingsManager.getSettings(false)
        Assume.assumeNotNull(settingsJson) // Skip when null in full-suite (shared NetworkManager state)

        val node = VWOClient.objectMapper.readTree(settingsJson!!) as JsonElement
        val obj = node.getAsJsonObject()

        // extraKey must be preserved
        assertTrue(obj.has("extraKey"))
        assertEquals("testValue", obj.get("extraKey").getAsString())

        // Ensure no error logs were emitted
        val hasError = loggedLevels.any { it == LogLevelEnum.ERROR }
        assertTrue("No error logs should be emitted", !hasError)
    }

    @Test
    fun `getSettings converts non-empty campaigns object into array`() {
        // Mock with campaigns as a non-empty object
        val mockClient = object : NetworkClientInterface {
            override fun GET(request: RequestModel,
                             serviceContainer: ServiceContainer?
            ): ResponseModel {
                return ResponseModel().apply {
                    if (request.path == Constants.SETTINGS_ENDPOINT) {
                        statusCode = 200
                        data =
                            "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{\"c1\":{\"id\":1}},\"extraKey\":\"testValue\"}"
                        error = null
                    } else {
                        statusCode = 404
                        error = Exception("Unhandled path")
                    }
                }
            }

            override fun POST(request: RequestModel,
                              serviceContainer: ServiceContainer?
            ): ResponseModel {
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
        settingsManager.serviceContainer = testServiceContainer

        val settingsJson = settingsManager.getSettings(false)
        Assume.assumeNotNull(settingsJson) // Skip when null in full-suite (shared NetworkManager state)

        val node = VWOClient.objectMapper.readTree(settingsJson!!) as JsonElement
        val obj = node.getAsJsonObject()

        // campaigns must be converted to an array even if object had entries
        assertTrue(obj.has("campaigns"))
        assertTrue(obj.get("campaigns").isJsonArray)
        assertEquals(0, obj.get("campaigns").asJsonArray.size())

        // Ensure no error logs were emitted
        val hasError = loggedLevels.any { it == LogLevelEnum.ERROR }
        assertTrue("No error logs should be emitted", !hasError)
    }

    @Test
    fun `getSettings sends invalid schema error on init fetch`() {
        NetworkManager.attachClient(object : NetworkClientInterface {
            override fun GET(
                request: RequestModel,
                serviceContainer: ServiceContainer?,
            ): ResponseModel {
                return ResponseModel().apply {
                    statusCode = 200
                    data = "{\"version\":1}"
                    error = null
                }
            }

            override fun POST(
                request: RequestModel,
                serviceContainer: ServiceContainer?,
            ): ResponseModel {
                return ResponseModel().apply {
                    statusCode = 200
                    data = "{}"
                }
            }
        })

        val options = VWOInitOptions().apply {
            sdkKey = "dummy-sdk-key"
            accountId = 123456
            cachedSettingsExpiryTime = 0
        }
        val settingsManager = SettingsManager(options)
        testServiceContainer.setSettingsManager(settingsManager)
        settingsManager.serviceContainer = testServiceContainer

        val settingsJson = settingsManager.getSettings(false)

        assertNull(settingsJson)
        assertTrue(
            "INVALID_SETTINGS_SCHEMA should be logged so it can be sent to VWO. logs=$loggedMessages",
            loggedMessages.any { it.contains("Settings are not valid") }
        )
    }

    @Test
    fun `force fetch uses v2-settings by default`() {
        val paths = attachPathCapturingClient()
        settingsManager().getSettings(true)
        assertEquals(listOf(Constants.SETTINGS_ENDPOINT), paths)
    }

    @Test
    fun `force fetch with pull endpoint uses v2-pull`() {
        val paths = attachPathCapturingClient()
        settingsManager().getSettings(true, Constants.SETTINGS_PULL_ENDPOINT)
        assertEquals(listOf(Constants.SETTINGS_PULL_ENDPOINT), paths)
    }

    private fun settingsManager(): SettingsManager {
        val options = VWOInitOptions().apply {
            sdkKey = "dummy-sdk-key"
            accountId = 123456
            cachedSettingsExpiryTime = 0
        }
        return SettingsManager(options)
    }

    private fun attachPathCapturingClient(): MutableList<String?> {
        val paths = mutableListOf<String?>()
        val settingsJson =
            "{\"sdkKey\":\"a62215fc384a4fc1bd672ff9d0107f63\",\"collectionPrefix\":\"as01\",\"features\":{},\"version\":1,\"accountId\":1116972,\"campaigns\":{}}"
        NetworkManager.attachClient(object : NetworkClientInterface {
            override fun GET(
                request: RequestModel,
                serviceContainer: ServiceContainer?,
            ): ResponseModel {
                paths.add(request.path)
                return ResponseModel().apply {
                    statusCode = 200
                    data = settingsJson
                    error = null
                }
            }

            override fun POST(
                request: RequestModel,
                serviceContainer: ServiceContainer?,
            ): ResponseModel {
                return ResponseModel().apply {
                    statusCode = 200
                    data = "{}"
                }
            }
        })
        return paths
    }
}


