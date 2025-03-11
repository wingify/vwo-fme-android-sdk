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

package com.vwo.packages.storage

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

/**
 * An abstract class that provides methods for accessing and managing local storage.*
 * This class uses SharedPreferences to store and retrieve data. It provides methods
 * for getting and saving strings and longs, as well as clearing data for specific keys.
 */
internal open class LocalStorageController(context: Context) {

    /**
     * The name of the Shared Preferences file.
     */
    private val sharedPrefs = "vwo_fme_shared_prefs"

    /**
     * The SharedPreferences instance.
     */
    private val sharedPreferences = context.getSharedPreferences(sharedPrefs, MODE_PRIVATE)

    /**
     * Retrieves a string value from the storage.
     *
     * @param key The key associated with the string value.
     * @return The string value if found, or an empty string otherwise.
     */
    fun getString(key: String): String {
        return sharedPreferences.getString(key,"") ?: ""
    }

    /**
     * Saves a string value to the storage.
     *
     * @param key The key associated with the string value.
     * @param value The string value to be saved.
     */
    fun saveString(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    /**
     * Retrieves a long value from the storage.
     *
     * @param key The key associated with the long value.
     * @return The long value if found, or -1 otherwise.
     */
    fun getLong(key: String): Long {
        return sharedPreferences.getLong(key, -1)
    }

    /**
     * Saves a long value to the storage.
     *
     * @param key The key associated with the long value.
     * @param value The long value to be saved.
     */
    fun saveLong(key: String, value: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(key,value)
        editor.apply()
    }

    /**
     * Retrieves a Boolean value from the storage.
     *
     * @param key The key associated with the Boolean value.
     * @return The Boolean value if found, or null otherwise.
     */
    fun getBoolean(key: String): Boolean? {
        return if (sharedPreferences.contains(key)) {
            sharedPreferences.getBoolean(key, false) // Default value is ignored since we check for key existence
        } else {
            null // Return null if the key does not exist
        }
    }

    /**
     * Saves a Boolean value to the storage.
     *
     * @param key The key associated with the Boolean value.
     * @param value The Boolean value to be saved.
     */
    fun saveBoolean(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key,value)
        editor.apply()
    }

    /**
     * Clears the data associated with a specific key.
     *
     * @param key The key associated with the data to be cleared.
     */
    fun clearData(key: String) {
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.remove(key)
        editor.apply()
    }
}