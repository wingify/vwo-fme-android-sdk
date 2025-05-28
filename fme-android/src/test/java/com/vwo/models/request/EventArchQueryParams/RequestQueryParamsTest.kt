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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RequestQueryParamsTest {

    private lateinit var requestQueryParams: RequestQueryParams
    private val eventName = "test_event"
    private val accountId = "12345"
    private val environment = "test_env"
    private val visitorUserAgent = "Mozilla/5.0"
    private val visitorIp = "192.168.1.1"
    private val url = "https://test.com"

    @Before
    fun setup() {
        requestQueryParams = RequestQueryParams(
            en = eventName,
            a = accountId,
            env = environment,
            visitor_ua = visitorUserAgent,
            visitor_ip = visitorIp,
            url = url
        )
    }

    @Test
    fun `test RequestQueryParams initialization`() {
        // Assert
        assertNotNull(requestQueryParams)
    }

    @Test
    fun `test queryParams contains correct event name`() {
        // Assert
        assertEquals(eventName, requestQueryParams.queryParams["en"])
    }

    @Test
    fun `test queryParams contains correct account ID`() {
        // Assert
        assertEquals(accountId, requestQueryParams.queryParams["a"])
    }

    @Test
    fun `test queryParams contains correct environment`() {
        // Assert
        assertEquals(environment, requestQueryParams.queryParams["env"])
    }

    @Test
    fun `test queryParams contains correct visitor user agent`() {
        // Assert
        assertEquals(visitorUserAgent, requestQueryParams.queryParams["visitor_ua"])
    }

    @Test
    fun `test queryParams contains correct visitor IP`() {
        // Assert
        assertEquals(visitorIp, requestQueryParams.queryParams["visitor_ip"])
    }

    @Test
    fun `test queryParams contains correct platform`() {
        // Assert
        assertEquals("FS", requestQueryParams.queryParams["p"])
    }

    @Test
    fun `test queryParams contains eTime`() {
        // Assert
        assertNotNull(requestQueryParams.queryParams["eTime"])
    }

    @Test
    fun `test queryParams contains random value`() {
        // Assert
        assertNotNull(requestQueryParams.queryParams["random"])
    }

    @Test
    fun `test queryParams with null visitor user agent`() {
        // Arrange
        val params = RequestQueryParams(
            en = eventName,
            a = accountId,
            env = environment,
            visitor_ua = null,
            visitor_ip = visitorIp,
            url = url
        )

        // Assert
        assertNull(params.queryParams["visitor_ua"])
    }

    @Test
    fun `test queryParams with null visitor IP`() {
        // Arrange
        val params = RequestQueryParams(
            en = eventName,
            a = accountId,
            env = environment,
            visitor_ua = visitorUserAgent,
            visitor_ip = null,
            url = url
        )

        // Assert
        assertNull(params.queryParams["visitor_ip"])
    }

    @Test
    fun `test queryParams with empty event name`() {
        // Arrange
        val emptyEventName = ""
        val params = RequestQueryParams(
            en = emptyEventName,
            a = accountId,
            env = environment,
            visitor_ua = visitorUserAgent,
            visitor_ip = visitorIp,
            url = url
        )

        // Assert
        assertEquals(emptyEventName, params.queryParams["en"])
    }

    @Test
    fun `test queryParams with empty account ID`() {
        // Arrange
        val emptyAccountId = ""
        val params = RequestQueryParams(
            en = eventName,
            a = emptyAccountId,
            env = environment,
            visitor_ua = visitorUserAgent,
            visitor_ip = visitorIp,
            url = url
        )

        // Assert
        assertEquals(emptyAccountId, params.queryParams["a"])
    }

    @Test
    fun `test queryParams with empty environment`() {
        // Arrange
        val emptyEnvironment = ""
        val params = RequestQueryParams(
            en = eventName,
            a = accountId,
            env = emptyEnvironment,
            visitor_ua = visitorUserAgent,
            visitor_ip = visitorIp,
            url = url
        )

        // Assert
        assertEquals(emptyEnvironment, params.queryParams["env"])
    }
} 