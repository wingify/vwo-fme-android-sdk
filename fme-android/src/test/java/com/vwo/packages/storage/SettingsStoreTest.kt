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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsStoreTest {

    private lateinit var context: Context
    private lateinit var settingsStore: SettingsStore

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        settingsStore = SettingsStore(context)
    }

    @Test
    fun testSaveSettings() {
        val settings = "testSettings"
        settingsStore.saveSettings(settings)
        val result = settingsStore.getSettings()
        assertEquals(settings, result)
    }

    @Test
    fun testGetSettings() {
        val settings = "testSettings"
        settingsStore.saveSettings(settings)
        val result = settingsStore.getSettings()
        assertEquals(settings, result)
    }

    @Test
    fun testClearSettings() {
        val settings = "testSettings"
        settingsStore.saveSettings(settings)
        settingsStore.clearSettings()
        val result = settingsStore.getSettings()
        assertEquals("", result)
    }

    @Test
    fun testSaveSettingsExpiry() {
        val expiryTime = 123456789L
        settingsStore.saveSettingsExpiry(expiryTime)
        val result = settingsStore.getSettingsExpiry()
        assertEquals(expiryTime, result)
    }

    @Test
    fun testGetSettingsExpiry() {
        val expiryTime = 123456789L
        settingsStore.saveSettingsExpiry(expiryTime)
        val result = settingsStore.getSettingsExpiry()
        assertEquals(expiryTime, result)
    }

    @Test
    fun testClearSettingsExpiry() {
        val expiryTime = 123456789L
        settingsStore.saveSettingsExpiry(expiryTime)
        settingsStore.clearSettingsExpiry()
        val result = settingsStore.getSettingsExpiry()
        assertEquals(-1L, result)
    }
} 