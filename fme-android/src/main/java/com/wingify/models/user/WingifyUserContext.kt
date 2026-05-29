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
import com.wingify.utils.DeviceIdUtil
import com.wingify.utils.UUIDUtils.getUUID

/**
 * User context for Wingify SDK operations.
 *
 * Encapsulates information about a user, including their ID, custom variables,
 * variation targeting variables, and bucketing options.
 */
open class WingifyUserContext {

    var id: String? = null

    var customVariables: MutableMap<String, Any> = HashMap()

    var postSegmentationVariables: List<String>? = null

    var variationTargetingVariables: MutableMap<String, Any> = HashMap()

    internal var sessionId: Long = FMEConfig.generateSessionId()

    var vwo: GatewayService? = null

    /**
     * Use device ID as user ID when user ID is not provided.
     * When enabled, the SDK will generate a persistent device ID and use it as the user ID.
     * This option is useful when explicit user identification is not available.
     */
    var shouldUseDeviceIdAsUserId: Boolean = false

    /**
     * Custom bucketing seed to control variation assignment independently of userId.
     *
     * When isCustomBucketingSeed is enabled in [WingifyInitOptions] and this value is provided,
     * the SDK will use this seed for bucketing decisions instead of userId.
     *
     * Use cases:
     * - Household consistency: Use familyId to assign same variation to all family members
     * - Account-level testing: Use accountId for all users under same account
     * - Device consistency: Use deviceId for same variation across user's devices
     * - Session-based testing: Use sessionId to maintain consistency within a session
     */
    var bucketingSeed: String? = null

    internal fun getUuid(serviceContainer: ServiceContainer): String {
        return getUUID(
            this.id.toString(),
            serviceContainer.getSettingsManager()?.accountId?.toString(),
        )
    }

    /**
     * @return The qualified id from either [id] or a generated device id when [shouldUseDeviceIdAsUserId] is enabled.
     */
    fun getIdBasedOnSpecificCondition(): String? {
        if (id != null) return id
        return if (shouldUseDeviceIdAsUserId) DeviceIdUtil().getDeviceId() else id
    }
}
