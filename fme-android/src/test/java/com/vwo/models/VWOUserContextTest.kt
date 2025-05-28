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
package com.vwo.models

import com.vwo.models.user.VWOUserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class VWOUserContextTest {

    private lateinit var userContext: VWOUserContext

    @Before
    fun setup() {
        userContext = VWOUserContext()
    }

    @Test
    fun `test default values`() {
        assertNull(userContext.id)
        assertNotNull(userContext.customVariables)
    }

    @Test
    fun `test setting and getting userId`() {
        val testUserId = "test_user_123"
        userContext.id = testUserId
        assertEquals(testUserId, userContext.id)
    }

    @Test
    fun `test setting and getting customVariables`() {
        val testCustomVariables = mutableMapOf<String,Any>(
            "key1" to "value1",
            "key2" to "value2"
        )
        userContext.customVariables = testCustomVariables
        assertEquals(testCustomVariables, userContext.customVariables)
    }

    @Test
    fun `test setting and getting all properties`() {
        val testCustomVariables = mutableMapOf<String,Any>(
            "key1" to "value1",
            "key2" to "value2"
        )

        userContext.apply {
            id = "test_user_123"
            customVariables = testCustomVariables
        }

        assertEquals("test_user_123", userContext.id)
        assertEquals(testCustomVariables, userContext.customVariables)
    }
} 