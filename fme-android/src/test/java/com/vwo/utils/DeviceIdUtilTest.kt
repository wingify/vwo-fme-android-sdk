/**
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vwo.utils

import android.content.Context
import android.provider.Settings
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import android.content.ContentResolver
import org.mockito.MockitoAnnotations
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic

@RunWith(MockitoJUnitRunner::class)
class DeviceIdUtilTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockContentResolver: ContentResolver
    
    private lateinit var deviceIdUtil: DeviceIdUtil

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        deviceIdUtil = DeviceIdUtil()
    }

    @Test
    fun `getDeviceId should return device ID when Android ID is available`() {
        // Arrange
        val androidId = "test_android_id_123"
        mockStatic(Settings.Secure::class.java).use { mockedStatic ->
            whenever(Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID))
                .thenReturn(androidId)

            // Act
            val deviceId = deviceIdUtil.getDeviceId(mockContext)

            // Assert
            assertNotNull(deviceId)
            assertTrue(deviceId!!.startsWith("device_"))
            assertEquals(23, deviceId.length) // "device_" + 16 chars hash
        }
    }

    @Test
    fun `getDeviceId should return null when context is null`() {
        // Act
        val deviceId = deviceIdUtil.getDeviceId(null)

        // Assert
        assertNull(deviceId)
    }

    @Test
    fun `getDeviceId should return null when Android ID is null`() {
        // Arrange
        mockStatic(Settings.Secure::class.java).use { mockedStatic ->
            whenever(Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID))
                .thenReturn(null)

            // Act
            val deviceId = deviceIdUtil.getDeviceId(mockContext)

            // Assert
            assertNull(deviceId)
        }
    }

    @Test
    fun `getDeviceId should return null when Android ID is empty`() {
        // Arrange
        mockStatic(Settings.Secure::class.java).use { mockedStatic ->
            whenever(Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID))
                .thenReturn("")

            // Act
            val deviceId = deviceIdUtil.getDeviceId(mockContext)

            // Assert
            assertNull(deviceId)
        }
    }

    @Test
    fun `getDeviceId should return same ID for same Android ID`() {
        // Arrange
        val androidId = "consistent_android_id"
        mockStatic(Settings.Secure::class.java).use { mockedStatic ->
            whenever(Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID))
                .thenReturn(androidId)

            // Act
            val deviceId1 = deviceIdUtil.getDeviceId(mockContext)
            val deviceId2 = deviceIdUtil.getDeviceId(mockContext)

            // Assert
            assertEquals(deviceId1, deviceId2)
        }
    }

    @Test
    fun `getDeviceId should return different IDs for different Android IDs`() {
        // Arrange
        val androidId1 = "android_id_1"
        val androidId2 = "android_id_2"
        
        mockStatic(Settings.Secure::class.java).use { mockedStatic ->
            whenever(Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID))
                .thenReturn(androidId1)
            val deviceId1 = deviceIdUtil.getDeviceId(mockContext)
            
            whenever(Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID))
                .thenReturn(androidId2)
            val deviceId2 = deviceIdUtil.getDeviceId(mockContext)

            // Assert
            assertNotEquals(deviceId1, deviceId2)
        }
    }

    @Test
    fun `getDeviceId should handle exception gracefully`() {
        // Arrange
        mockStatic(Settings.Secure::class.java).use { mockedStatic ->
            whenever(Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID))
                .thenThrow(RuntimeException("Test exception"))

            // Act
            val deviceId = deviceIdUtil.getDeviceId(mockContext)

            // Assert
            assertNull(deviceId)
        }
    }
} 