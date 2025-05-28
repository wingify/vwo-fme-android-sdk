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
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OfflineEventDataTest {

    @Test
    fun testOfflineEventDataCreation() {
        val offlineEventData = OfflineEventData(
            id = 1L,
            sdkKey = "test_sdk_key",
            accountId = 123L,
            payload = "{\"key\":\"value\"}"
        )

        assertEquals(1L, offlineEventData.id)
        assertEquals("test_sdk_key", offlineEventData.sdkKey)
        assertEquals(123L, offlineEventData.accountId)
        assertEquals("{\"key\":\"value\"}", offlineEventData.payload)
    }

    @Test
    fun testOfflineEventDataEquality() {
        val offlineEventData1 = OfflineEventData(
            id = 1L,
            sdkKey = "test_sdk_key",
            accountId = 123L,
            payload = "{\"key\":\"value\"}"
        )
        val offlineEventData2 = OfflineEventData(
            id = 1L,
            sdkKey = "test_sdk_key",
            accountId = 123L,
            payload = "{\"key\":\"value\"}"
        )
        val offlineEventData3 = OfflineEventData(
            id = 2L, // Different ID
            sdkKey = "test_sdk_key",
            accountId = 123L,
            payload = "{\"key\":\"value\"}"
        )

        assertEquals(offlineEventData1, offlineEventData2)
        assertNotEquals(offlineEventData1, offlineEventData3)
    }

    @Test
    fun testOfflineEventDataHashCode() {
        val offlineEventData1 = OfflineEventData(
            id = 1L,
            sdkKey = "test_sdk_key",
            accountId = 123L,
            payload = "{\"key\":\"value\"}"
        )
        val offlineEventData2 = OfflineEventData(
            id = 1L,
            sdkKey = "test_sdk_key",
            accountId = 123L,
            payload = "{\"key\":\"value\"}"
        )

        assertEquals(offlineEventData1.hashCode(), offlineEventData2.hashCode())
    }

    @Test
    fun testOfflineEventDataToString() {
        val offlineEventData = OfflineEventData(
            id = 1L,
            sdkKey = "test_sdk_key",
            accountId = 123L,
            payload = "{\"key\":\"value\"}"
        )

        val expectedString = "OfflineEventData(id=1, sdkKey=test_sdk_key, accountId=123, payload={\"key\":\"value\"})"
        assertEquals(expectedString, offlineEventData.toString())
    }

    @Test
    fun testOfflineEventDataWithDifferentPayload() {
        val offlineEventData1 = OfflineEventData(
            id = 1L,
            sdkKey = "test_sdk_key",
            accountId = 123L,
            payload = "{\"key\":\"value1\"}"
        )
        val offlineEventData2 = OfflineEventData(
            id = 1L,
            sdkKey = "test_sdk_key",
            accountId = 123L,
            payload = "{\"key\":\"value2\"}"
        )

        assertNotEquals(offlineEventData1, offlineEventData2)
        assertNotEquals(offlineEventData1.hashCode(), offlineEventData2.hashCode())
    }
}