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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MetricTest {

    private lateinit var metric: Metric

    @Before
    fun setup() {
        metric = Metric()
    }

    @Test
    fun `test default values`() {
        assertNull(metric.mca)
        assertNull(metric.hashProps)
        assertNull(metric.identifier)
        assertNull(metric.id)
        assertNull(metric.type)
    }

    @Test
    fun `test setting and getting mca`() {
        val testMca = 123
        metric.mca = testMca
        assertEquals(testMca, metric.mca)
    }

    @Test
    fun `test setting and getting hashProps`() {
        val testHashProps = true
        metric.hashProps = testHashProps
        assertEquals(testHashProps, metric.hashProps)
    }

    @Test
    fun `test setting and getting identifier`() {
        val testIdentifier = "test_metric"
        metric.identifier = testIdentifier
        assertEquals(testIdentifier, metric.identifier)
    }

    @Test
    fun `test setting and getting id`() {
        val testId = 456
        metric.id = testId
        assertEquals(testId, metric.id)
    }

    @Test
    fun `test setting and getting type`() {
        val testType = "REVENUE"
        metric.type = testType
        assertEquals(testType, metric.type)
    }

    @Test
    fun `test setting and getting all properties`() {
        metric.apply {
            mca = 123
            hashProps = true
            identifier = "test_metric"
            id = 456
            type = "REVENUE"
        }

        assertEquals(123, metric.mca)
        assertEquals(true, metric.hashProps)
        assertEquals("test_metric", metric.identifier)
        assertEquals(456, metric.id)
        assertEquals("REVENUE", metric.type)
    }
} 