/*
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
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
package com.wingify.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.wingify.constants.Constants.APP_VERSION
import com.wingify.constants.Constants.DEVICE_MODEL
import com.wingify.constants.Constants.DEVICE_TYPE_MOBILE
import com.wingify.constants.Constants.DEVICE_TYPE_TABLET
import com.wingify.constants.Constants.DEVICE_TYPE_TV
import com.wingify.constants.Constants.LOCALE
import com.wingify.constants.Constants.MANUFACTURER
import com.wingify.constants.Constants.OS_VERSION
import java.util.Locale

private const val TABLET_SMALLEST_WIDTH_DP = 600

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
     * Pre-segmentation device type: mobile, tablet, or tv.
     *
     * Returns an empty string when [context] has no resources, so init can continue.
     * Callers log [deviceTypeLookupError] in that case.
     */
    fun getDeviceType(context: Context): String {
        val config = context.resources?.configuration ?: return ""
        return when (config.uiMode and Configuration.UI_MODE_TYPE_MASK) {
            Configuration.UI_MODE_TYPE_TELEVISION -> DEVICE_TYPE_TV
            else -> if (config.smallestScreenWidthDp >= TABLET_SMALLEST_WIDTH_DP) {
                DEVICE_TYPE_TABLET
            } else {
                DEVICE_TYPE_MOBILE
            }
        }
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

/**
 * Log key in `error-messages.json` when the device type cannot be read.
 *
 * @return `DEVICE_CONTEXT_MISSING` or `DEVICE_RESOURCES_MISSING`, otherwise null.
 */
internal fun deviceTypeLookupError(context: Context?): String? = when {
    context == null -> "DEVICE_CONTEXT_MISSING"
    context.resources == null -> "DEVICE_RESOURCES_MISSING"
    else -> null
}