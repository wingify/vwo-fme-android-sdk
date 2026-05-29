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
package com.vwo.utils

import com.wingify.utils.LogMessageUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogMessageUtilTest {

    @Test
    fun `applySdkBranding returns null when message is null`() {
        assertNull(LogMessageUtil.applySdkBranding(null, true))
    }

    @Test
    fun `applySdkBranding leaves message unchanged when branding is disabled`() {
        val message = "[ERROR]: VWO-SDK _vwo_meta"
        assertEquals(message, LogMessageUtil.applySdkBranding(message, false))
    }

    @Test
    fun `applySdkBranding replaces VWO case insensitively`() {
        assertEquals(
            "[ERROR]: Wingify-SDK Options",
            LogMessageUtil.applySdkBranding("[ERROR]: VWO-SDK Options", true)
        )
        assertEquals(
            "Wingify settings and Wingify Gateway",
            LogMessageUtil.applySdkBranding("vwo settings and VWO Gateway", true)
        )
    }

    @Test
    fun `applySdkBranding does not replace vwo identifiers with underscore`() {
        assertEquals(
            "Key _vwo_meta is invalid",
            LogMessageUtil.applySdkBranding("Key _vwo_meta is invalid", true)
        )
        assertEquals(
            "prefix vwo_suffix unchanged",
            LogMessageUtil.applySdkBranding("prefix vwo_suffix unchanged", true)
        )
        assertEquals(
            "Segment key _vwoUserId missing",
            LogMessageUtil.applySdkBranding("Segment key _vwoUserId missing", true)
        )
        assertEquals(
            "Impression built for vwo_variationShown event",
            LogMessageUtil.applySdkBranding(
                "Impression built for vwo_variationShown event",
                true,
            )
        )
        assertEquals(
            "vwo_sdkName=Wingify-fme-android-sdk",
            LogMessageUtil.applySdkBranding("vwo_sdkName=vwo-fme-android-sdk", true)
        )
    }

    @Test
    fun `applySdkBranding does not replace vwo in domains`() {
        assertEquals(
            "Gateway host app.vwo.com unreachable",
            LogMessageUtil.applySdkBranding("Gateway host app.vwo.com unreachable", true)
        )
        assertEquals(
            "https://vwo.com seed",
            LogMessageUtil.applySdkBranding("https://vwo.com seed", true)
        )
    }

    @Test
    fun `applySdkBranding does not replace VWO enum style constants`() {
        assertEquals(
            "Event VWO_ERROR payload invalid",
            LogMessageUtil.applySdkBranding("Event VWO_ERROR payload invalid", true)
        )
    }
}
