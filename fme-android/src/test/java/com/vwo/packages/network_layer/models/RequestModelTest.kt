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

package com.vwo.packages.network_layer.models

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RequestModelTest {

    @Test
    fun `test RequestModel with minimal parameters`() {
        val requestModel = RequestModel(
            url = "example.com",
            method = null,
            path = null,
            query = null,
            body = null,
            headers = null,
            scheme = null,
            port = 80
        )

        val options = requestModel.options

        assertEquals("example.com", options["hostname"])
        assertEquals("GET", options["method"])
        assertEquals("http", options["scheme"])
        assertFalse(options.containsKey("port"))
        assertTrue(options.containsKey("headers"))
        assertFalse(options.containsKey("body"))
        assertFalse(options.containsKey("path"))
    }

    @Test
    fun `test RequestModel with all parameters`() {
        val headers = mutableMapOf("Authorization" to "Bearer token")
        val query = mutableMapOf("param1" to "value1", "param2" to "value2")
        val body = mapOf("key" to "value")

        val requestModel = RequestModel(
            url = "example.com",
            method = "POST",
            path = "/api/v1",
            query = query,
            body = body,
            headers = headers,
            scheme = "https",
            port = 443
        )

        val options = requestModel.options

        assertEquals("example.com", options["hostname"])
        assertEquals("POST", options["method"])
        assertEquals("https", options["scheme"])
        assertEquals(443, options["port"])
        assertEquals(headers, options["headers"])
        assertEquals(body, options["body"])
        assertEquals("/api/v1?param1=value1&param2=value2", options["path"])
    }

    @Test
    fun `test RequestModel with custom port`() {
        val requestModel = RequestModel(
            url = "example.com",
            method = null,
            path = null,
            query = null,
            body = null,
            headers = null,
            scheme = null,
            port = 8080
        )

        val options = requestModel.options

        assertEquals(8080, options["port"])
    }

    @Test
    fun `test RequestModel with body sets Content-Type header`() {
        val headers = mutableMapOf<String, String>()
        val body = mapOf("key" to "value")

        val requestModel = RequestModel(
            url = "example.com",
            method = "POST",
            path = null,
            query = null,
            body = body,
            headers = headers,
            scheme = null,
            port = 80
        )

        val options = requestModel.options
        val updatedHeaders = options["headers"] as? MutableMap<String, String>

        assertNotNull(updatedHeaders)
        assertEquals("application/json", updatedHeaders?.get("Content-Type"))
        assertEquals(body, options["body"])
    }

    @Test
    fun `test RequestModel with timeout`() {
        val requestModel = RequestModel(
            url = "example.com",
            method = null,
            path = null,
            query = null,
            body = null,
            headers = null,
            scheme = null,
            port = 80
        )
        requestModel.timeout = 5000

        val options = requestModel.options

        assertEquals(5000, options["timeout"])
    }

    @Test
    fun `test RequestModel with empty query parameters`() {
        val requestModel = RequestModel(
            url = "example.com",
            method = null,
            path = "/api",
            query = mutableMapOf(),
            body = null,
            headers = null,
            scheme = null,
            port = 80
        )

        val options = requestModel.options

        assertEquals("/api", options["path"])
    }

    @Test
    fun `test RequestModel with custom expectedResponseType`() {
        val requestModel = RequestModel(
            url = "example.com",
            method = null,
            path = null,
            query = null,
            body = null,
            headers = null,
            scheme = null,
            port = 80,
            expectedResponseType = "text/plain"
        )

        assertEquals("text/plain", requestModel.expectedResponseType)
    }

    @Test
    fun `test RequestModel with null values`() {
        val requestModel = RequestModel(
            url = null,
            method = null,
            path = null,
            query = null,
            body = null,
            headers = null,
            scheme = null,
            port = 80
        )

        val options = requestModel.options

        assertNull(options["hostname"])
        assertEquals("GET", options["method"])
        assertEquals("http", options["scheme"])
        assertTrue(options.containsKey("headers"))
        assertFalse(options.containsKey("body"))
        assertFalse(options.containsKey("path"))
    }
} 