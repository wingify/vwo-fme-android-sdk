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
package com.wingify.utils

import com.wingify.ServiceContainer
import com.wingify.models.user.WingifyInitOptions
import com.wingify.models.user.WingifyUserContext
import com.vwo.packages.logger.enums.LogLevelEnum

/**
 * Utility class for managing user IDs and device IDs.
 *
 * This class provides methods to get the effective user ID based on the
 * VWO initialization options, returning either the provided userId or
 * a generated device ID as fallback.
 */
object UserIdUtil {

    private val deviceIdUtil: DeviceIdUtil = DeviceIdUtil()

    /**
     * Gets the effective user ID based on the context and initialization options.
     *
     * @param userContext The VWO user context
     * @param options The VWO initialization options
     * @param deviceIdUtil Optional DeviceIdUtil instance for testing
     * @return The effective user ID (either provided userId or generated deviceId), or null if neither is available
     */
    fun getUserId(
        userContext: WingifyUserContext?,
        options: WingifyInitOptions?,
        serviceContainer: ServiceContainer,
        deviceIdUtil: DeviceIdUtil = this.deviceIdUtil
    ): String? {

        if (options?.isAliasingEnabled == true) {

            if (options.gatewayService.isNotEmpty()) {

                val userId =
                    AliasIdentityManager(serviceContainer).maybeGetAliasAwareUserIdSync(userContext)
                if (userContext != null && !userId.isNullOrEmpty()) {
                    // try to use the ID from gateway, if not found fallback to existing logic
                    return userId
                }
            } else {

                serviceContainer.getLoggerService()
                    ?.log(LogLevelEnum.ERROR, "INVALID_GATEWAY_URL", null)
            }
        }

        if (!userContext?.id.isNullOrEmpty()) {
            serviceContainer.getLoggerService()
                ?.log(LogLevelEnum.INFO, "USER_ID_INFO", mapOf("id" to userContext?.id))
            return userContext?.id
        }

        // If no user ID is provided and device ID is enabled in context, generate device ID
        if (userContext?.shouldUseDeviceIdAsUserId == true && options?.context != null) {
            val deviceId = deviceIdUtil.getDeviceId(options.context!!)
            serviceContainer.getLoggerService()
                ?.log(LogLevelEnum.INFO, "USER_ID_INFO", mapOf("id" to deviceId))
            return deviceId
        }

        // Return null if no user ID is available and device ID is not enabled
        serviceContainer.getLoggerService()?.log(LogLevelEnum.INFO, "USER_ID_NULL", emptyMap())
        return null
    }

    /**
     * Checks if an effective user ID is available based on the context and options.
     *
     * @param userContext The VWO user context
     * @param options The VWO initialization options
     * @param deviceIdUtil Optional DeviceIdUtil instance for testing
     * @return True if an effective user ID is available, false otherwise
     */
    fun isUserIdAvailable(
        userContext: WingifyUserContext?,
        options: WingifyInitOptions?,
        deviceIdUtil: DeviceIdUtil = this.deviceIdUtil,
        serviceContainer: ServiceContainer
    ): Boolean {
        return getUserId(userContext, options, serviceContainer, deviceIdUtil) != null
    }
} 