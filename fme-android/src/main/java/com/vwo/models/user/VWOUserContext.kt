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
package com.vwo.models.user

/**
 * Represents the context of a VWO user.
 *
 * This class encapsulates information about a user in the context of VWO, including their ID, user agent, IP address, custom variables, and variation targeting variables.
 */
class VWOUserContext {
    var id: String? = null
    var customVariables: MutableMap<String, Any> = HashMap()

    var variationTargetingVariables: MutableMap<String, Any> = HashMap()

    var vwo: GatewayService? = null
    
    /**
     * Enable device ID generation when user ID is not provided.
     * When enabled, the SDK will generate a persistent device ID.
     * This option is useful when explicit user identification is not available.
     */
    var enableDeviceId: Boolean = false
}
