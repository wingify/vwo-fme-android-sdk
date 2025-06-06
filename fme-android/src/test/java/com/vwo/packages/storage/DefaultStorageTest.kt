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
class DefaultStorageTest {

    private lateinit var context: Context
    private lateinit var defaultStorage: DefaultStorage

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        defaultStorage = DefaultStorage(context)
    }

    @Test
    fun testSaveFeatureKey() {
        val featureKey = "testFeatureKey"
        val feature = "testFeature"
        defaultStorage.saveFeatureKey(featureKey, feature)
        val result = defaultStorage.getFeatureKey(featureKey)
        assertEquals(feature, result)
    }

    @Test
    fun testGetFeatureKey() {
        val featureKey = "testFeatureKey"
        val feature = "testFeature"
        defaultStorage.saveFeatureKey(featureKey, feature)
        val result = defaultStorage.getFeatureKey(featureKey)
        assertEquals(feature, result)
    }

    @Test
    fun testClearFeatureKey() {
        val featureKey = "testFeatureKey"
        val feature = "testFeature"
        defaultStorage.saveFeatureKey(featureKey, feature)
        defaultStorage.clearFeatureKey(featureKey)
        val result = defaultStorage.getFeatureKey(featureKey)
        assertEquals("", result)
    }
}