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
import com.vwo.constants.Constants
import com.vwo.providers.StorageProvider
import com.vwo.models.Settings
import com.vwo.models.schemas.SettingsSchema
import com.vwo.models.user.VWOInitOptions
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.utils.NetworkUtil
import java.net.URL

/**
 * Manages settings for the VWO SDK.
 *
 * This class is responsible for handling and managing the settings required for the VWO SDK to
 * operate correctly. It initializes and stores settings based on the provided initialization
 * options.
 */
class SettingsManager(options: VWOInitOptions) {
    val sdkKey = options.sdkKey
    val accountId = options.accountId

    private val cachedSettingsExpiryInterval = options.cachedSettingsExpiryTime
    private val networkTimeout = Constants.SETTINGS_TIMEOUT.toInt()
    var hostname: String

    @JvmField
    var port: Int = 0

    @JvmField
    var protocol: String = Constants.HTTPS_PROTOCOL
    var isGatewayServiceProvided: Boolean = false

    init {
        if (options.gatewayService.isNotEmpty()) {
            isGatewayServiceProvided = true
            try {
                val parsedUrl: URL
                val gatewayServiceUrl = options.gatewayService["url"].toString()
                val gatewayServiceProtocol = options.gatewayService["protocol"]
                val gatewayServicePort = options.gatewayService["port"]
                parsedUrl =
                    if (gatewayServiceUrl.startsWith("http://") || gatewayServiceUrl.startsWith("https://")) {
                        URL(gatewayServiceUrl)
                    } else if (gatewayServiceProtocol != null
                        && gatewayServiceProtocol.toString().isNotEmpty()
                    ) {
                        URL("$gatewayServiceProtocol://$gatewayServiceUrl")
                    } else {
                        URL("https://$gatewayServiceUrl")
                    }
                this.hostname = parsedUrl.host
                this.protocol = parsedUrl.protocol
                if (parsedUrl.port != -1) {
                    this.port = parsedUrl.port
                } else if (gatewayServicePort != null && !gatewayServicePort.toString().isEmpty()) {
                    this.port = gatewayServicePort.toString().toInt()
                }
            } catch (e: Exception) {
                LoggerService.log(
                    LogLevelEnum.ERROR,
                    "Error occurred while parsing gateway service URL: " + e.message
                )
                this.hostname = Constants.HOST_NAME
            }
        } else {
            this.hostname = Constants.HOST_NAME
        }
        instance = this
    }

    /**
     * Fetches settings from the server
     */
    private fun fetchFromCacheOrServer(): String? {
        try {
            var responseString: String?

            if (canUseCachedSettings()) {
                responseString = getCachedSetting()
                if (responseString.isNullOrEmpty()) {
                    responseString = fetchAndCacheServerSettings()
                }
            } else {
                responseString = fetchSettings()
                if (!responseString.isNullOrEmpty()) {
                    updateSettingsCache(responseString)
                } else if (cachedSettingsAllowed()) {
                    //Return settings even if it is expired - SDK should work as long as there is setting in cache.
                    responseString = getCachedSetting()
                }
            }
            return responseString
        } catch (e: Exception) {
            val map = mapOf<String?, String?>("err" to e.toString())
            LoggerService.log(LogLevelEnum.ERROR, "SETTINGS_FETCH_ERROR", map)
        }
        return null
    }

    private fun cachedSettingsAllowed() = cachedSettingsExpiryInterval != 0

    private fun fetchAndCacheServerSettings(): String? {
        val response: String? = fetchSettings()
        if (response != null) {
            updateSettingsCache(response)
        }
        return response
    }

    private fun updateSettingsCache(responseString: String) {
        StorageProvider.settingsStore?.saveSettings(responseString)
        val expiryTime = System.currentTimeMillis() + cachedSettingsExpiryInterval
        StorageProvider.settingsStore?.saveSettingsExpiry(expiryTime)
    }

    private fun getCachedSetting(): String? {
        return StorageProvider.settingsStore?.getSettings()
    }

    private fun canUseCachedSettings(): Boolean {
        if (cachedSettingsExpiryInterval == 0) return false
        return isCachedSettingValid()
    }

    private fun isCachedSettingValid(): Boolean {
        val expiryTime = StorageProvider.settingsStore?.getSettingsExpiry() ?: -1
        return expiryTime != -1L && System.currentTimeMillis() <= expiryTime
    }

    /**
     * Fetches settings from the server
     * @return settings
     */
    private fun fetchSettings(): String? {
        require(!(sdkKey == null || accountId == null)) { "SDK Key and Account ID are required to fetch settings. Aborting!" }

        val options = NetworkUtil().getSettingsPath(sdkKey, accountId)
        options["api-version"] = "3"

        if (NetworkManager.config?.developmentMode != true) {
            options["s"] = "prod"
        }

        try {
            val request = RequestModel(
                hostname,
                "GET",
                Constants.SETTINGS_ENDPOINT,
                options,
                null,
                null,
                this.protocol,
                port
            )
            request.timeout = networkTimeout

            val response = NetworkManager.get(request)
            if (response?.statusCode != 200) {
                LoggerService.log(
                    LogLevelEnum.ERROR,
                    "SETTINGS_FETCH_ERROR",
                    object : HashMap<String?, String?>() {
                        init {
                            put("err", response?.error.toString())
                        }
                    })
                return null
            }
            return response.data
        } catch (e: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "SETTINGS_FETCH_ERROR",
                object : HashMap<String?, String?>() {
                    init {
                        put("err", e.toString())
                    }
                })
            return null
        }
    }

    /**
     * Fetches settings from the server
     * @param forceFetch forceFetch, if pooling - true, else - false
     * @return settings
     */
    fun getSettings(forceFetch: Boolean): String? {
        if (forceFetch) {
            LoggerService.log(LogLevelEnum.INFO, "Settings: Fetched from: ServerPoll")
            return fetchAndCacheServerSettings()
        } else {
            try {
                val settings = fetchFromCacheOrServer()
                if (settings == null) {
                    LoggerService.log(LogLevelEnum.ERROR, "SETTINGS_SCHEMA_INVALID", null)
                    return null
                }
                val settingsValid = SettingsSchema().isSettingsValid(
                    VWOClient.objectMapper.readValue(settings, Settings::class.java)
                )
                if (settingsValid) {
                    return settings
                } else {
                    LoggerService.log(LogLevelEnum.ERROR, "SETTINGS_SCHEMA_INVALID", null)
                    return null
                }
            } catch (e: Exception) {
                return null
            }
        }
    }

    companion object {
        @JvmStatic
        var instance: SettingsManager? = null
            private set
    }
}
