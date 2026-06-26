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

import com.wingify.models.Settings
import com.wingify.utils.GsonUtil
import com.wingify.utils.UserTrackingUsageUtil
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserTrackingUsageUtil {

    @Test
    fun `isEnabled is false when user tracking flag omitted from settings JSON`() {
        val json = """{"accountId": 1, "version": 1}"""
        val parsed = GsonUtil.gson.fromJson(json, Settings::class.java)

        assertFalse(UserTrackingUsageUtil.isUsageTrackingEnabled(parsed))
    }

    @Test
    fun `isEnabled is true only when user tracking flag is explicitly true in settings JSON`() {
        val enabled = GsonUtil.gson.fromJson("""{"isMAU": true}""", Settings::class.java)
        val disabled = GsonUtil.gson.fromJson("""{"isMAU": false}""", Settings::class.java)

        assertTrue(UserTrackingUsageUtil.isUsageTrackingEnabled(enabled))
        assertFalse(UserTrackingUsageUtil.isUsageTrackingEnabled(disabled))
    }

}
