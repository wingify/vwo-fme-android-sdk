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
package com.vwo.models.request.EventArchQueryParams

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EventBatchQueryParamsTest {

    private lateinit var eventBatchQueryParams: EventBatchQueryParams
    private val sdkKey = "test-sdk-key"
    private val accountId = "12345"

    @Before
    fun setup() {
        eventBatchQueryParams = EventBatchQueryParams(sdkKey, accountId)
    }

    @Test
    fun `test EventBatchQueryParams initialization`() {
        // Assert
        assertNotNull(eventBatchQueryParams)
    }

    @Test
    fun `test queryParams contains correct SDK key`() {
        // Assert
        assertEquals(sdkKey, eventBatchQueryParams.queryParams["i"])
        assertEquals(sdkKey, eventBatchQueryParams.queryParams["env"])
    }

    @Test
    fun `test queryParams contains correct account ID`() {
        // Assert
        assertEquals(accountId, eventBatchQueryParams.queryParams["a"])
    }

    @Test
    fun `test queryParams contains all required parameters`() {
        // Assert
        assertEquals(3, eventBatchQueryParams.queryParams.size)
        assertNotNull(eventBatchQueryParams.queryParams["i"])
        assertNotNull(eventBatchQueryParams.queryParams["env"])
        assertNotNull(eventBatchQueryParams.queryParams["a"])
    }

    @Test
    fun `test queryParams with empty SDK key`() {
        // Arrange
        val emptySdkKey = ""
        val params = EventBatchQueryParams(emptySdkKey, accountId)

        // Assert
        assertEquals(emptySdkKey, params.queryParams["i"])
        assertEquals(emptySdkKey, params.queryParams["env"])
    }

    @Test
    fun `test queryParams with empty account ID`() {
        // Arrange
        val emptyAccountId = ""
        val params = EventBatchQueryParams(sdkKey, emptyAccountId)

        // Assert
        assertEquals(emptyAccountId, params.queryParams["a"])
    }
} 