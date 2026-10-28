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

import com.wingify.constants.Constants.DEVICE_TYPE_MOBILE
import com.wingify.constants.Constants.DEVICE_TYPE_TV
import com.wingify.constants.Constants.USER_AGENT_MOBILE_PRODUCTS
import com.wingify.constants.Constants.USER_AGENT_PRODUCT

/**
 * Builds an RFC 9110 User-Agent.
 *
 * Shape: `[USER_AGENT_PRODUCT]_<platform>/<sdkVersion> (<platform> <osRelease>)`.
 * [DEVICE_TYPE_MOBILE] and a blank device type append [USER_AGENT_MOBILE_PRODUCTS]. Tablet and television keep the base value.
 *
 * @param platform SDK platform token, such as `Android`.
 * @param sdkVersion SDK version placed in the product token.
 * @param osRelease Operating-system release placed inside the comment.
 * @param deviceType Value from [DeviceInfo.getDeviceType].
 * @return The User-Agent string for this device.
 */
internal fun formatUserAgent(
    platform: String,
    sdkVersion: String,
    osRelease: String,
    deviceType: String,
): String {
    val agent = "${USER_AGENT_PRODUCT}_$platform/$sdkVersion ($platform $osRelease)"
    return if (deviceType.isBlank() || deviceType == DEVICE_TYPE_MOBILE) {
        "$agent $USER_AGENT_MOBILE_PRODUCTS"
    } else {
        agent
    }
}

/**
 * Log key in `warn-messages.json` for an unsupported Android TV device.
 *
 * @param deviceType Device type from [DeviceInfo.getDeviceType].
 * @return `ANDROID_TV_UNSUPPORTED` for [DEVICE_TYPE_TV], otherwise null.
 */
internal fun unsupportedDeviceWarning(deviceType: String): String? {
    return if (deviceType == DEVICE_TYPE_TV) "ANDROID_TV_UNSUPPORTED" else null
}
