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
class SettingsQueryParamsTest {

    private lateinit var settingsQueryParams: SettingsQueryParams
    private val sdkKey = "test-sdk-key"
    private val random = "0.123456789"
    private val accountId = "12345"

    @Before
    fun setup() {
        settingsQueryParams = SettingsQueryParams(sdkKey, random, accountId)
    }

    @Test
    fun `test SettingsQueryParams initialization`() {
        // Assert
        assertNotNull(settingsQueryParams)
    }

    @Test
    fun `test queryParams contains correct SDK key`() {
        // Assert
        assertEquals(sdkKey, settingsQueryParams.queryParams["i"])
    }

    @Test
    fun `test queryParams contains correct random value`() {
        // Assert
        assertEquals(random, settingsQueryParams.queryParams["r"])
    }

    @Test
    fun `test queryParams contains correct account ID`() {
        // Assert
        assertEquals(accountId, settingsQueryParams.queryParams["a"])
    }

    @Test
    fun `test queryParams contains all required parameters`() {
        // Assert
        assertEquals(3, settingsQueryParams.queryParams.size)
        assertNotNull(settingsQueryParams.queryParams["i"])
        assertNotNull(settingsQueryParams.queryParams["r"])
        assertNotNull(settingsQueryParams.queryParams["a"])
    }

    @Test
    fun `test queryParams with empty SDK key`() {
        // Arrange
        val emptySdkKey = ""
        val params = SettingsQueryParams(emptySdkKey, random, accountId)

        // Assert
        assertEquals(emptySdkKey, params.queryParams["i"])
    }

    @Test
    fun `test queryParams with empty random value`() {
        // Arrange
        val emptyRandom = ""
        val params = SettingsQueryParams(sdkKey, emptyRandom, accountId)

        // Assert
        assertEquals(emptyRandom, params.queryParams["r"])
    }

    @Test
    fun `test queryParams with empty account ID`() {
        // Arrange
        val emptyAccountId = ""
        val params = SettingsQueryParams(sdkKey, random, emptyAccountId)

        // Assert
        assertEquals(emptyAccountId, params.queryParams["a"])
    }
} 