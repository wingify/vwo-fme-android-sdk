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

import COLUMN_ACCOUNT_ID
import COLUMN_ID_KEY
import COLUMN_PAYLOAD
import COLUMN_SDK_KEY
import TABLE_NAME
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SdkDatabaseHelperTest {

    private lateinit var dbHelper: SdkDatabaseHelper
    private lateinit var db: SQLiteDatabase

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        dbHelper = SdkDatabaseHelper(context)
        db = dbHelper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
        dbHelper.close()
    }

    @Test
    fun `database is created with correct name`() {
        assertEquals("fme_sdk.db", dbHelper.databaseName)
    }

    @Test
    fun `onCreate creates table with correct schema`() {
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        val columnNames = cursor.columnNames

        assertTrue(columnNames.contains(COLUMN_ID_KEY))
        assertTrue(columnNames.contains(COLUMN_SDK_KEY))
        assertTrue(columnNames.contains(COLUMN_ACCOUNT_ID))
        assertTrue(columnNames.contains(COLUMN_PAYLOAD))

        cursor.close()
    }

    @Test
    fun `onUpgrade handles version changes gracefully`() {
        // This test verifies that onUpgrade doesn't throw any exceptions
        dbHelper.onUpgrade(db, 1, 2)
    }

    @Test
    fun `table can store and retrieve data`() {
        val sdkKey = "test-sdk-key"
        val accountId = 1234
        val payload = "test-payload"

        // Insert test data
        val values = ContentValues().apply {
            put(COLUMN_SDK_KEY, sdkKey)
            put(COLUMN_ACCOUNT_ID, accountId.toLong())
            put(COLUMN_PAYLOAD, payload)
        }
        db.insert(TABLE_NAME, null, values)

        // Query the data
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_SDK_KEY = ? AND $COLUMN_ACCOUNT_ID = ?",
            arrayOf(sdkKey, accountId.toString()),
            null,
            null,
            null
        )

        assertTrue(cursor.moveToFirst())
        assertEquals(sdkKey, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SDK_KEY)))
        assertEquals(accountId.toLong(), cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID)))
        assertEquals(payload, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAYLOAD)))

        cursor.close()
    }

    @Test
    fun `table handles null values correctly`() {
        val values = ContentValues().apply {
            putNull(COLUMN_SDK_KEY)
            putNull(COLUMN_ACCOUNT_ID)
            putNull(COLUMN_PAYLOAD)
        }
        val rowId = db.insert(TABLE_NAME, null, values)
        assertTrue(rowId > 0)

        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_ID_KEY = ?",
            arrayOf(rowId.toString()),
            null,
            null,
            null
        )

        assertTrue(cursor.moveToFirst())
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_SDK_KEY)))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID)))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_PAYLOAD)))

        cursor.close()
    }

    @Test
    fun `table enforces primary key constraint`() {
        val values = ContentValues().apply {
            put(COLUMN_SDK_KEY, "test-sdk-key")
            put(COLUMN_ACCOUNT_ID, 1234L)
            put(COLUMN_PAYLOAD, "test-payload")
        }

        // First insert should succeed
        val rowId1 = db.insert(TABLE_NAME, null, values)
        assertTrue(rowId1 > 0)

        // Second insert with same values should also succeed (no unique constraint on these columns)
        val rowId2 = db.insert(TABLE_NAME, null, values)
        assertTrue(rowId2 > 0)
        assertTrue(rowId2 != rowId1)
    }
} 