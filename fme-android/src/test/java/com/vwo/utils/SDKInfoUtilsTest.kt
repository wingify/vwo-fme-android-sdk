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

package com.vwo.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.robolectric.RuntimeEnvironment

@RunWith(MockitoJUnitRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class SDKInfoUtilsTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Mock
    private lateinit var mockPackageInfo: PackageInfo

    @Before
    fun setUp() {
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)
        `when`(mockPackageManager.getPackageInfo(mockContext.packageName, 0)).thenReturn(mockPackageInfo)
    }

    @Test
    fun `getSDKName returns android platform for android sdk`() {
        // Given
        SDKMetaUtil.sdkName = "vwo-fme-android-sdk"

        // When
        val result = SDKInfoUtils.getSDKName()

        // Then
        assertEquals("ad", result)
    }

    @Test
    fun `getSDKName returns flutter platform for flutter sdk`() {
        // Given
        SDKMetaUtil.sdkName = "vwo-fme-flutter-sdk"

        // When
        val result = SDKInfoUtils.getSDKName()

        // Then
        assertEquals("fl", result)
    }

    @Test
    fun `getSDKName returns react platform for react sdk`() {
        // Given
        SDKMetaUtil.sdkName = "vwo-fme-react-sdk"

        // When
        val result = SDKInfoUtils.getSDKName()

        // Then
        assertEquals("rn", result)
    }

    @Test
    fun `getSDKVersion returns correct version`() {
        // Given
        val expectedVersion = "1.0.0"
        SDKMetaUtil.sdkVersion = expectedVersion

        // When
        val result = SDKInfoUtils.getSDKVersion()

        // Then
        assertEquals(expectedVersion, result)
    }

    @Test
    fun `getPlatform returns android platform`() {
        // When
        val result = SDKInfoUtils.getPlatform()

        // Then
        assertEquals("ad", result)
    }

    @Test
    fun `getOSDetails returns correct platform and version`() {
        // When
        val result = SDKInfoUtils.getOSDetails()

        // Then
        assertEquals("ad ${Build.VERSION.RELEASE}", result)
    }

    @Test
    fun `getAppVersion returns correct version from context`() {
        // Given
        val expectedVersion = "1.0.0"
        mockPackageInfo.versionName = expectedVersion

        // When
        val result = SDKInfoUtils.getAppVersion(mockContext)

        // Then
        assertEquals(expectedVersion, result)
    }

    @Test
    fun `getAppVersion returns empty string when package info is not available`() {
        // Given
        `when`(mockPackageManager.getPackageInfo(mockContext.packageName, 0))
            .thenThrow(PackageManager.NameNotFoundException())

        // When
        val result = SDKInfoUtils.getAppVersion(mockContext)

        // Then
        assertEquals("", result)
    }

    @Test
    fun `getLanguageVersion returns kotlin version`() {
        // When
        val result = SDKInfoUtils.getLanguageVersion()

        // Then
        assertTrue(result.startsWith("Kotlin "))
        assertTrue(result.contains(KotlinVersion.CURRENT.toString()))
    }
} 