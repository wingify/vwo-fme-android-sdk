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
package com.vwo.utils

import android.content.Context
import android.os.Build
import com.vwo.constants.Constants.APP_VERSION
import com.vwo.constants.Constants.DEVICE_MODEL
import com.vwo.constants.Constants.LOCALE
import com.vwo.constants.Constants.MANUFACTURER
import com.vwo.constants.Constants.OS_VERSION
import java.util.Locale

class DeviceInfo {

    /**
     * Gets the version name of the host application.
     * e.g., "1.0.3"
     */
    fun getApplicationVersion(context: Context): String {
        return SDKInfoUtils.getAppVersion(context)
    }

    /**
     * Gets the Android OS version.
     * e.g., "11"
     */
    fun getOsVersion(): String {
        return Build.VERSION.RELEASE ?: valueUnknown
    }

    /**
     * Gets the device manufacturer.
     * e.g., "Google"
     */
    fun getManufacturer(): String {
        return Build.MANUFACTURER ?: valueUnknown
    }

    /**
     * Gets the device model.
     * e.g., "Pixel 5"
     */
    fun getDeviceModel(): String {
        return Build.MODEL ?: valueUnknown
    }

    /**
     * Gets the current device locale.
     * e.g., "en-US"
     */
    fun getLocale(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Locale.getDefault(Locale.Category.DISPLAY).toLanguageTag()
        } else {
            Locale.getDefault().toString().replace("_", "-")
        }
    }

    /**
     * Gathers all specified device details into a Map.
     */
    fun getAllDeviceDetails(context: Context): Map<String, String> {
        return mapOf(
            APP_VERSION to getApplicationVersion(context),
            OS_VERSION to getOsVersion(),
            MANUFACTURER to getManufacturer(),
            DEVICE_MODEL to getDeviceModel(),
            LOCALE to getLocale()
        )
    }
}