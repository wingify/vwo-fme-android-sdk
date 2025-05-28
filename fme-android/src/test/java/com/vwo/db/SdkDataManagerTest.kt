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
package com.vwo.db

import SdkDataManager
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.vwo.models.OfflineEventData
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SdkDataManagerTest {

    private lateinit var context: Context
    private lateinit var sdkDataManager: SdkDataManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        sdkDataManager = SdkDataManager(context)
    }

    @Test
    fun testSaveSdkData() {
        val sdkKey = "testSdkKey"
        val accountId = 123
        val payload = "testPayload"
        val result = sdkDataManager.saveSdkData(sdkKey, accountId, payload)
        assertTrue(result)
    }

    @Test
    fun testGetSdkData() {
        val sdkKey = "testSdkKey"
        val accountId = 123L
        val payload = "testPayload"
        sdkDataManager.saveSdkData(sdkKey, accountId.toInt(), payload)
        val result = sdkDataManager.getSdkData(accountId, sdkKey)
        assertFalse(result.isEmpty())
        assertEquals(payload, result[0].payload)
    }

    @Test
    fun testDeleteSdkData() {
        val sdkKey = "testSdkKey"
        val accountId = 123
        val payload = "testPayload"
        sdkDataManager.saveSdkData(sdkKey, accountId, payload)
        val result = sdkDataManager.deleteSdkData(sdkKey)
        assertTrue(result)
    }

    @Test
    fun testGetDistinctSdkKeys() {
        val sdkKey = "testSdkKey"
        val accountId = 123
        val payload = "testPayload"
        sdkDataManager.saveSdkData(sdkKey, accountId, payload)
        val result = sdkDataManager.getDistinctSdkKeys()
        assertFalse(result.isEmpty())
        assertEquals(sdkKey, result[0].sdkKey)
    }

    @Test
    fun testDeleteData() {
        val sdkKey = "testSdkKey"
        val accountId = 123
        val payload = "testPayload"
        sdkDataManager.saveSdkData(sdkKey, accountId, payload)
        val result = sdkDataManager.deleteData(1L)
        assertTrue(result)
    }

    @Test
    fun testGetEntryCount() {
        val sdkKey = "testSdkKey"
        val accountId = 123
        val payload = "testPayload"
        sdkDataManager.saveSdkData(sdkKey, accountId, payload)
        val result = sdkDataManager.getEntryCount()
        assertEquals(1, result)
    }
} 