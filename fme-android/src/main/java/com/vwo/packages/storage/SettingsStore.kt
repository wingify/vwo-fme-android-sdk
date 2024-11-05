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

package com.vwo.packages.storage

import android.content.Context

/**
 * A class for managing the storage and retrieval of settings data.*
 * This class uses local storage to persist settings data and provides methods
 * for getting, saving, and clearing settings. It also manages the expiry time
 * of the settings data.
 */
internal class SettingsStore(context: Context) : LocalStorageController(context) {

    /*** The key used to store the settings data in local storage.
     */
    private val SETTINGS_KEY = "settings"

    /**
     * The key used to store the settings expiry time in local storage.
     */
    private val SETTINGS_EXPIRY_KEY = "settingsExpiry"

    /**
     * Retrieves the settings data from local storage.
     *
     * @return The settings data as a string.
     */
    fun getSettings(): String {
        return getString(SETTINGS_KEY)
    }

    /**
     * Saves the settings data to local storage.
     *
     * @param settings The settings data to be saved.
     */
    fun saveSettings(settings: String) {
        saveString(SETTINGS_KEY, settings)
    }

    /**
     * Clears the settings data from local storage.
     */
    fun clearSettings() {
        clearData(SETTINGS_KEY)
    }

    /**
     * Retrieves the settings expiry time from local storage.
     *
     * @return The settings expiry time as a long value.
     */
    fun getSettingsExpiry(): Long {
        return getLong(SETTINGS_EXPIRY_KEY)
    }

    /**
     * Saves the settings expiry time to local storage.
     *
     * @param expiryTime The settings expiry time to be saved.
     */
    fun saveSettingsExpiry(expiryTime: Long) {
        saveLong(SETTINGS_EXPIRY_KEY, expiryTime)
    }

    /**
     * Clears the settings expiry time from local storage.
     */
    fun clearSettingsExpiry() {
        clearData(SETTINGS_EXPIRY_KEY)
    }
}