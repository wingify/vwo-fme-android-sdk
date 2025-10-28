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

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.vwo.constants.Constants
import com.vwo.db.SdkDatabaseHelper
import com.vwo.models.OfflineEventData
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.LoggerService
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

        return try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_SDK_KEY, sdkKey)
                put(COLUMN_ACCOUNT_ID, accountId)
                put(COLUMN_PAYLOAD, payload)
            }
            val rowId = db.insertWithOnConflict(
                TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE
            )
            rowId != -1L
        } catch (e: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "DATABASE_ERROR", mapOf(Constants.ERR to e.message))
            false
        }
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
        val offlineEventDataList = mutableListOf<OfflineEventData>()
        val db = dbHelper.readableDatabase

        var cursor: Cursor? = null
        try {
            cursor = db.query(
                TABLE_NAME,
                null,  // Select all columns
                "$COLUMN_SDK_KEY = ? AND $COLUMN_ACCOUNT_ID = ?",
                arrayOf(sdkKey, accountId.toString()),
                null,
                null,
                null
            )
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID_KEY))
                val payload = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PAYLOAD))
                offlineEventDataList.add(OfflineEventData(id, sdkKey, accountId, payload))
            }
        } catch (e: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "DATABASE_ERROR", mapOf(Constants.ERR to e.message))
        } finally {
            cursor?.close()
        }
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
        return try {
            val db = dbHelper.writableDatabase
            val rowsDeleted = db.delete(TABLE_NAME, "$COLUMN_SDK_KEY = ?", arrayOf(sdkKey))
            rowsDeleted > 0
        } catch (e: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "DATABASE_ERROR", mapOf(Constants.ERR to e.message))
            false
        }
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
        var cursor: Cursor? = null
        val sdkKeys = mutableListOf<OfflineEventData>()
        try {
            val db = dbHelper.readableDatabase
            val query = "SELECT DISTINCT $COLUMN_ACCOUNT_ID, $COLUMN_SDK_KEY FROM $TABLE_NAME"

            cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                do {
                    val sdkKey = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SDK_KEY))
                    val accountId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID))

                    sdkKeys.add(OfflineEventData(0, sdkKey, accountId, ""))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "DATABASE_ERROR", mapOf(Constants.ERR to e.message))
        } finally {
            cursor?.close()
        }
        return sdkKeys
    }

    /**
     * Deletes a row from the database by ID.
     *
     * @param id The ID of the row to delete.
     * @return `true` if the row was deleted successfully, `false` otherwise.
     */
    fun deleteData(id: Long): Boolean {
        return try {
            val db = dbHelper.writableDatabase
            val whereClause = "$COLUMN_ID_KEY = ?"
            val whereArgs = arrayOf(id.toString())
            val rowCount = db.delete(TABLE_NAME, whereClause, whereArgs)
            rowCount != 0
        } catch (e: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "DATABASE_ERROR", mapOf(Constants.ERR to e.message))
            false
        }
    }

    /**
     * Retrieves the total number of entries in the database table.
     *
     * This function queries the database to count the total number of rows in the specified table.
     * It returns the count as an integer value.
     *
     * @return The total number of entries in the database table.
     */
    fun getEntryCount(): Int {
        var cursor: Cursor? = null
        return try {
            val db = dbHelper.readableDatabase

            cursor = db.query(
                TABLE_NAME,
                arrayOf("COUNT(*)"),
                null,
                null,
                null,
                null,
                null
            )

            var entryCount = 0
            if (cursor != null && cursor.moveToFirst()) {
                entryCount = cursor.getInt(0)
                cursor.close()
            }

            entryCount
        } catch (e: Exception) {
            LoggerService.log(LogLevelEnum.ERROR, "DATABASE_ERROR", mapOf(Constants.ERR to e.message))
            0
        } finally {
            cursor?.close()
        }
    }
}
