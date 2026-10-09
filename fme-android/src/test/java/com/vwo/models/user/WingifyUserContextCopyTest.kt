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
package com.vwo.models.user

import com.wingify.models.user.GatewayService
import com.wingify.models.user.WingifyUserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class WingifyUserContextCopyTest {

    @Test
    fun `clone isolates mutable maps`() {
        val original = WingifyUserContext().apply {
            id = "user-1"
            customVariables["tier"] = "gold"
            variationTargetingVariables["region"] = "US"
            postSegmentationVariables = listOf("age")
            bucketingSeed = "seed-1"
            vwo = GatewayService().apply {
                location = mapOf("country" to "IN")
                userAgent = mapOf("browser" to "Chrome")
            }
        }

        val copy = original.clone()
        copy.customVariables["tier"] = "silver"
        copy.variationTargetingVariables["region"] = "EU"

        assertEquals("gold", original.customVariables["tier"])
        assertEquals("US", original.variationTargetingVariables["region"])
        assertEquals("user-1", copy.id)
        assertEquals("seed-1", copy.bucketingSeed)
        assertNotSame(original.customVariables, copy.customVariables)
        assertNotSame(original.variationTargetingVariables, copy.variationTargetingVariables)
        assertEquals("IN", copy.vwo?.location?.get("country"))
    }
}
