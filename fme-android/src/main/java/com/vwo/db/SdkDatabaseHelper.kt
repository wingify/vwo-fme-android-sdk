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
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * The name of the database file.
 */
private const val DATABASE_NAME = "fme_sdk.db"

/**
 * The current version of the database schema.
 */
private const val DATABASE_VERSION = 1

/**
 * A helper class for managing the FME SDK database.
 *
 * This class handles database creation, upgrades, and provides access to the database.
 *
 * @param context The application context.
 */
class SdkDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION) {

    /**
     * Called when the database is created for the first time.
     *
     * This method is responsible for creating the database tables.
     *
     * @param db The database instance.
     */
    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
            $COLUMN_ID_KEY INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SDK_KEY TEXT,
                $COLUMN_ACCOUNT_ID INTEGER,
                $COLUMN_PAYLOAD TEXT
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    /**
     * Called when the database needs to be upgraded.
     *
     * This method is responsible for migrating data from the old schema to the new schema.
     *
     * @param db The database instance.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}
