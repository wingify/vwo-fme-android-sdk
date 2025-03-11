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

package com.vwo.models.user

import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.LoggerService
import java.util.Calendar

/**
 * Configuration object for managing FME session data.
 * This object provides methods for setting and retrieving session information,
 * as well as generating session IDs.
 */
object FMEConfig {

    /**
     * Internal storage for the current FME session data.
     */
    private var sessionData: FmeSession? = null

    private val sessionIdKey = "sessionId"

    /**
     * This property is a flag used to determine if the Mobile Insights (MI) SDK
     * is linked and integrated with the current environment. It plays a crucial
     * role in the FME<>MI SDK integration.
     *
     * Specifically, this flag is used to set the isMII property in the event data
     * for the "vwoVariationShown" event. Such a configuration helps coordinate
     * session data sharing between various SDKs, ensuring that both
     * FME and MI SDKs correctly manage and interpret session-related information.
     */
    internal var isMISdkLinked = false

    /**
     * Sets the session data for the current FME session.
     *
     * @param sessionData The FmeSession object containing session information.
     */
    @JvmStatic
    fun setSessionData(sessionData: Map<String, Any>) {

        isMISdkLinked = false
        if (sessionData.isEmpty()) {
            LoggerService.log(LogLevelEnum.ERROR, "Session data cannot be empty.")
            return
        } else if (!sessionData.containsKey(sessionIdKey)) {
            LoggerService.log(LogLevelEnum.ERROR, "Session data must contain 'sessionId' key.")
            return
        }

        val sessionIdValue = sessionData["sessionId"]
        if (sessionIdValue !is Long) {
            LoggerService.log(LogLevelEnum.ERROR, "'sessionId' value must be a Long.")
            return
        }
        if (sessionIdValue <= 0) {
            LoggerService.log(LogLevelEnum.ERROR, "'sessionId' value must be a positive number.")
            return
        }
        this.sessionData = FmeSession(sessionIdValue)
        isMISdkLinked = true
    }

    /**
     * Generates a session ID for the event.
     * If a session ID is already present in the session data, it will be returned.
     * Otherwise, a new session ID is generated based on the current timestamp.
     *
     * @return The session ID.
     */
    internal fun generateSessionId(): Long {
        val sessionId = sessionData?.sessionId
        if (sessionId != null && sessionId != 0L) {
            return sessionId
        }

        return Calendar.getInstance().timeInMillis / 1000
    }
}