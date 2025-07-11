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
import com.vwo.models.user.VWOUserContext
import com.vwo.models.user.VWOInitOptions
import com.vwo.providers.StorageProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.mockito.kotlin.mock
import org.mockito.MockitoAnnotations
import java.lang.ref.WeakReference

@RunWith(MockitoJUnitRunner::class)
class UserIdUtilTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockDeviceIdUtil: DeviceIdUtil

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `getUserId should return user ID when provided`() {
        // Arrange
        val context = VWOUserContext()
        context.id = "user123"
        context.shouldUseDeviceIdAsUserId = true
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val userId = UserIdUtil.getUserId(context, options)

        // Assert
        assertEquals("user123", userId)
    }

    @Test
    fun `getUserId should return device ID when user ID is null and device ID is enabled`() {
        // Arrange
        val context = VWOUserContext()
        context.id = null
        context.shouldUseDeviceIdAsUserId = true
        val options = VWOInitOptions()
        options.context = mockContext
        
        whenever(mockDeviceIdUtil.getDeviceId(mockContext)).thenReturn("device_123456")

        // Act
        val userId = UserIdUtil.getUserId(context, options, mockDeviceIdUtil)

        // Assert
        assertEquals("device_123456", userId)
    }

    @Test
    fun `getUserId should return device ID when user ID is empty and device ID is enabled`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.shouldUseDeviceIdAsUserId = true
        val options = VWOInitOptions()
        options.context = mockContext
        
        whenever(mockDeviceIdUtil.getDeviceId(mockContext)).thenReturn("device_789012")

        // Act
        val userId = UserIdUtil.getUserId(context, options, mockDeviceIdUtil)

        // Assert
        assertEquals("device_789012", userId)
    }

    @Test
    fun `getUserId should return null when user ID is null and device ID is disabled`() {
        // Arrange
        val context = VWOUserContext()
        context.id = null
        context.shouldUseDeviceIdAsUserId = false
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val userId = UserIdUtil.getUserId(context, options)

        // Assert
        assertNull(userId)
    }

    @Test
    fun `getUserId should return null when user ID is empty and device ID is disabled`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.shouldUseDeviceIdAsUserId = false
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val userId = UserIdUtil.getUserId(context, options)

        // Assert
        assertNull(userId)
    }

    @Test
    fun `getUserId should return null when context is null`() {
        // Arrange
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val userId = UserIdUtil.getUserId(null, options)

        // Assert
        assertNull(userId)
    }

    @Test
    fun `getUserId should return null when options is null`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""

        // Act
        val userId = UserIdUtil.getUserId(context, null)

        // Assert
        assertNull(userId)
    }

    @Test
    fun `getUserId should return null when device ID generation fails`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.shouldUseDeviceIdAsUserId = true
        val options = VWOInitOptions()
        options.context = mockContext
        
        whenever(mockDeviceIdUtil.getDeviceId(mockContext)).thenReturn(null)

        // Act
        val userId = UserIdUtil.getUserId(context, options, mockDeviceIdUtil)

        // Assert
        assertNull(userId)
    }

    @Test
    fun `isUserIdAvailable should return true when user ID is provided`() {
        // Arrange
        val context = VWOUserContext()
        context.id = "user123"
        context.shouldUseDeviceIdAsUserId = false
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val isAvailable = UserIdUtil.isUserIdAvailable(context, options)

        // Assert
        assertTrue(isAvailable)
    }

    @Test
    fun `isUserIdAvailable should return true when device ID is available`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.shouldUseDeviceIdAsUserId = true
        val options = VWOInitOptions()
        options.context = mockContext
        
        whenever(mockDeviceIdUtil.getDeviceId(mockContext)).thenReturn("device_123456")

        // Act
        val isAvailable = UserIdUtil.isUserIdAvailable(context, options, mockDeviceIdUtil)

        // Assert
        assertTrue(isAvailable)
    }

    @Test
    fun `isUserIdAvailable should return false when no user ID is available`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.shouldUseDeviceIdAsUserId = false
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val isAvailable = UserIdUtil.isUserIdAvailable(context, options)

        // Assert
        assertFalse(isAvailable)
    }
} 