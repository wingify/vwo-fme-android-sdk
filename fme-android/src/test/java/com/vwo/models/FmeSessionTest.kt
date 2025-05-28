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

import com.vwo.models.user.FmeSession
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class FmeSessionTest {

    private lateinit var fmeSession: FmeSession

    @Before
    fun setup() {
        fmeSession = FmeSession(12345L)
    }

    @Test
    fun `test sessionId value`() {
        assertEquals(12345L, fmeSession.sessionId)
    }

    @Test
    fun `test updating sessionId`() {
        val newSessionId = 67890L
        fmeSession = FmeSession(newSessionId)
        assertEquals(newSessionId, fmeSession.sessionId)
    }
} 