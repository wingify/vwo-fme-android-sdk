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
package com.wingify.models.user

import com.wingify.ServiceContainer

/**
 * Configuration object for managing FME session data (Wingify SDK).
 *
 * Delegates to [com.vwo.models.user.FMEConfig].
 */
object FMEConfig {

    /**
     * Flag indicating whether the Mobile Insights (MI) SDK is linked.
     * Used when coordinating session data between FME and MI SDKs.
     */
    internal var isMISdkLinked: Boolean
        get() = com.vwo.models.user.FMEConfig.isMISdkLinked
        set(value) {
            com.vwo.models.user.FMEConfig.isMISdkLinked = value
        }

    /**
     * Sets the session data for the current FME session.
     *
     * @param sessionData Map containing session information; must include a positive Long `sessionId`.
     * @param serviceContainer Optional container used to log validation errors.
     */
    @JvmStatic
    fun setSessionData(sessionData: Map<String, Any>, serviceContainer: ServiceContainer? = null) {
        com.vwo.models.user.FMEConfig.setSessionData(sessionData, serviceContainer)
    }

    /**
     * Returns the configured session ID, or a timestamp-based ID when none is set.
     */
    internal fun generateSessionId(): Long = com.vwo.models.user.FMEConfig.generateSessionId()
}
