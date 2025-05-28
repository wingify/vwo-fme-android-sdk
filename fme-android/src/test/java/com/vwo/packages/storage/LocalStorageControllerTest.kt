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
class LocalStorageControllerTest {

    private lateinit var context: Context
    private lateinit var localStorageController: LocalStorageController

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        localStorageController = LocalStorageController(context)
    }

    @Test
    fun testSaveString() {
        val key = "testKey"
        val value = "testValue"
        localStorageController.saveString(key, value)
        val result = localStorageController.getString(key)
        assertEquals(value, result)
    }

    @Test
    fun testGetString() {
        val key = "testKey"
        val value = "testValue"
        localStorageController.saveString(key, value)
        val result = localStorageController.getString(key)
        assertEquals(value, result)
    }

    @Test
    fun testSaveLong() {
        val key = "testKey"
        val value = 123456789L
        localStorageController.saveLong(key, value)
        val result = localStorageController.getLong(key)
        assertEquals(value, result)
    }

    @Test
    fun testGetLong() {
        val key = "testKey"
        val value = 123456789L
        localStorageController.saveLong(key, value)
        val result = localStorageController.getLong(key)
        assertEquals(value, result)
    }

    @Test
    fun testSaveBoolean() {
        val key = "testKey"
        val value = true
        localStorageController.saveBoolean(key, value)
        val result = localStorageController.getBoolean(key)
        assertEquals(value, result)
    }

    @Test
    fun testGetBoolean() {
        val key = "testKey"
        val value = true
        localStorageController.saveBoolean(key, value)
        val result = localStorageController.getBoolean(key)
        assertEquals(value, result)
    }

    @Test
    fun testClearData() {
        val key = "testKey"
        val value = "testValue"
        localStorageController.saveString(key, value)
        localStorageController.clearData(key)
        val result = localStorageController.getString(key)
        assertEquals("", result)
    }
} 