/*
 * Copyright (c) 2024 Wingify Software Pvt. Ltd.
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

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.vwo.db.SdkDatabaseHelper
import com.vwo.models.OfflineEventData
import com.vwo.providers.StorageProvider
import com.vwo.services.PeriodicDataUploader

const val TABLE_NAME = "fme_events"
const val COLUMN_ID_KEY = "id"
const val COLUMN_SDK_KEY = "sdkKey"
const val COLUMN_ACCOUNT_ID = "accountId"
const val COLUMN_PAYLOAD = "payload"

/**
 * A class responsible for managing SDK data interactions with the database.
 *
 * This class provides methods for saving, retrieving, and deleting SDK data.
 *
 * @param context The application context.
 */
class SdkDataManager(context: Context) {

    private val dbHelper = SdkDatabaseHelper(context)

    /**
     *  Saves or updates SDK data in the database.
     *
     * This method inserts or updates a row in the database with the provided SDK key, account ID, and payload.
     * If a row with the same SDK key and account ID already exists, it will be replaced.
     *
     * @param sdkKey The SDK key.
     * @param accountId The account ID.
     * @param payload The data payload.
     * @return `true` if the data was saved or updated successfully, `false` otherwise.
     */
    fun saveSdkData(sdkKey: String?, accountId: Int?, payload: String): Boolean {
        if (sdkKey.isNullOrEmpty() || accountId == null) return false

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SDK_KEY, sdkKey)
            put(COLUMN_ACCOUNT_ID, accountId)
            put(COLUMN_PAYLOAD, payload)
        }
        val rowId = db.insertWithOnConflict(
            TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
        db.close()
        StorageProvider.contextRef.get()?.let { PeriodicDataUploader().enqueue(it) }
        return rowId != -1L
    }

    /**
     * Retrieves SDK data from the database by SDK key and account ID.
     *
     * This method queries the database for rows matching the provided SDK key and account ID.
     * It returns a list of `OfflineEventData` objects representing the retrieved data.
     *
     * @param accountId The account ID.
     * @param sdkKey The SDK key.
     * @return A list of `OfflineEventData` objects.
     */
    fun getSdkData(accountId: Long, sdkKey: String): List<OfflineEventData> {
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.query(
            TABLE_NAME,
            null,  // Select all columns
            "$COLUMN_SDK_KEY = ? AND $COLUMN_ACCOUNT_ID = ?",
            arrayOf(sdkKey, accountId.toString()),
            null,
            null,
            null
        )
        val offlineEventDataList = mutableListOf<OfflineEventData>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID_KEY))
            val accountId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID))
            val payload = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAYLOAD))
            offlineEventDataList.add(OfflineEventData(id, sdkKey, accountId, payload))
        }
        cursor.close()
        db.close()
        return offlineEventDataList
    }

    /**
     * Deletes SDK data from the database by SDK key.
     *
     * This method deletes all rows from the database that match the provided SDK key.
     *
     * @param sdkKey The SDK key.
     * @return `true` if at least one row was deleted, `false` otherwise.
     */
    fun deleteSdkData(sdkKey: String): Boolean {
        val db = dbHelper.writableDatabase
        val rowsDeleted = db.delete(
            TABLE_NAME,
            "$COLUMN_SDK_KEY = ?",
            arrayOf(sdkKey)
        )
        db.close()
        return rowsDeleted > 0
    }

    /**
     * Retrieves a list of distinct SDK keys from the database.
     *
     * This method queries the database for all distinct SDK keys and returns them as a list of
     * `OfflineEventData` objects. The payload field in these objects will be empty.
     *
     * @return A list of `OfflineEventData` objects representing the distinct SDK keys.
     */
    fun getDistinctSdkKeys(): List<OfflineEventData> {
        val db = dbHelper.readableDatabase
        val sdkKeys = mutableListOf<OfflineEventData>()
        val query = "SELECT DISTINCT $COLUMN_ACCOUNT_ID, $COLUMN_SDK_KEY FROM $TABLE_NAME"

        val cursor: Cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val sdkKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SDK_KEY))
                val accountId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID))

                sdkKeys.add(OfflineEventData(0, sdkKey, accountId, ""))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return sdkKeys
    }

    /**
     * Deletes a row from the database by ID.
     *
     * @param id The ID of the row to delete.
     * @return `true` if the row was deleted successfully, `false` otherwise.
     */
    fun deleteData(id: Long): Boolean {
        val db = dbHelper.writableDatabase
        val whereClause = "$COLUMN_ID_KEY = ?"
        val whereArgs = arrayOf(id.toString())
        val rowCount = db.delete(TABLE_NAME, whereClause, whereArgs)
        db.close()
        return rowCount!=0
    }
}
