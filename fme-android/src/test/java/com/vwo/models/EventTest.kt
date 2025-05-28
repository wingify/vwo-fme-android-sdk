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

import com.vwo.models.request.Event
import com.vwo.models.request.Props
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EventTest {

    private lateinit var event: Event

    @Before
    fun setup() {
        event = Event()
    }

    @Test
    fun `test default values`() {
        assertNull(event.name)
        assertNull(event.props)
        assertNull(event.time)
    }

    @Test
    fun `test setting and getting name`() {
        val testName = "Test Event"
        event.name = testName
        assertEquals(testName, event.name)
    }

    @Test
    fun `test setting and getting props`() {
        val testProps = Props().apply {
            setSdkName("test_sdk")
            setSdkVersion("1.0.0")
            setEnvKey("test_env")
        }
        event.props = testProps
        assertEquals(testProps, event.props)
    }

    @Test
    fun `test setting and getting time`() {
        val testTime = 1234567890L
        event.time = testTime
        assertEquals(testTime, event.time)
    }

    @Test
    fun `test setting and getting all properties`() {
        val testProps = Props().apply {
            setSdkName("test_sdk")
            setSdkVersion("1.0.0")
            setEnvKey("test_env")
        }
        val testTime = 1234567890L

        event.apply {
            name = "Test Event"
            props = testProps
            time = testTime
        }

        assertEquals("Test Event", event.name)
        assertEquals(testProps, event.props)
        assertEquals(testTime, event.time)
    }
} 