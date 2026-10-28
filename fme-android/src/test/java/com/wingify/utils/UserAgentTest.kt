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

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import com.wingify.constants.Constants.DEVICE_TYPE_MOBILE
import com.wingify.constants.Constants.DEVICE_TYPE_TABLET
import com.wingify.constants.Constants.DEVICE_TYPE_TV
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserAgentTest {

    @Test
    fun mobileAppendsMobileSafari() {
        val agent = formatUserAgent(
            platform = "Android",
            sdkVersion = "1.76.0",
            osRelease = "14",
            deviceType = DEVICE_TYPE_MOBILE,
        )

        assertEquals("VWO_FME_Android/1.76.0 (Android 14) Mobile Safari", agent)
    }

    @Test
    fun tabletOmitsMobileSafari() {
        val agent = formatUserAgent(
            platform = "Android",
            sdkVersion = "1.76.0",
            osRelease = "14",
            deviceType = DEVICE_TYPE_TABLET,
        )

        assertEquals("VWO_FME_Android/1.76.0 (Android 14)", agent)
    }

    @Test
    fun televisionOmitsMobileSafari() {
        val agent = formatUserAgent(
            platform = "Android",
            sdkVersion = "1.76.0",
            osRelease = "14",
            deviceType = DEVICE_TYPE_TV,
        )

        assertEquals("VWO_FME_Android/1.76.0 (Android 14)", agent)
    }

    @Test
    fun televisionWarnsUnsupported() {
        val key = unsupportedDeviceWarning(DEVICE_TYPE_TV)
        val warnMessages = javaClass.classLoader!!
            .getResource("assets/warn-messages.json")!!
            .readText()

        assertEquals("ANDROID_TV_UNSUPPORTED", key)
        assertTrue(warnMessages.contains("\"ANDROID_TV_UNSUPPORTED\": \"Android TV is unsupported. Post-segmentation will classify this device as a tablet/others.\""))
    }

    @Test
    fun mobileDoesNotWarn() {
        assertNull(unsupportedDeviceWarning(DEVICE_TYPE_MOBILE))
    }

    @Test
    fun tabletDoesNotWarn() {
        assertNull(unsupportedDeviceWarning(DEVICE_TYPE_TABLET))
    }

    @Test
    fun blankDeviceTypeUsesPhoneUserAgent() {
        val agent = formatUserAgent(
            platform = "Android",
            sdkVersion = "1.76.0",
            osRelease = "14",
            deviceType = "",
        )

        assertEquals("VWO_FME_Android/1.76.0 (Android 14) Mobile Safari", agent)
    }

    @Test
    fun nullContextLogsDeviceContextMissing() {
        val errors = javaClass.classLoader!!
            .getResource("assets/error-messages.json")!!
            .readText()

        assertEquals("DEVICE_CONTEXT_MISSING", deviceTypeLookupError(null))
        assertTrue(
            errors.contains(
                "\"DEVICE_CONTEXT_MISSING\": \"Context is null. The User-Agent classifies this device as a phone, and device-type segmentation does not match.\""
            )
        )
    }

    @Test
    fun nullResourcesLogsDeviceResourcesMissing() {
        val context = object : ContextWrapper(null) {
            override fun getResources(): Resources? = null
        }
        val errors = javaClass.classLoader!!
            .getResource("assets/error-messages.json")!!
            .readText()

        assertEquals("DEVICE_RESOURCES_MISSING", deviceTypeLookupError(context))
        assertEquals("", DeviceInfo().getDeviceType(context))
        assertTrue(
            errors.contains(
                "\"DEVICE_RESOURCES_MISSING\": \"Context resources are null. The User-Agent classifies this device as a phone, and device-type segmentation does not match.\""
            )
        )
    }
}
