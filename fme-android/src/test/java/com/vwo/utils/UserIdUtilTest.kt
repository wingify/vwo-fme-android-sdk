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
    fun `getEffectiveUserId should return user ID when provided`() {
        // Arrange
        val context = VWOUserContext()
        context.id = "user123"
        context.enableDeviceId = true
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val effectiveUserId = UserIdUtil.getEffectiveUserId(context, options)

        // Assert
        assertEquals("user123", effectiveUserId)
    }

    @Test
    fun `getEffectiveUserId should return device ID when user ID is null and device ID is enabled`() {
        // Arrange
        val context = VWOUserContext()
        context.id = null
        context.enableDeviceId = true
        val options = VWOInitOptions()
        options.context = mockContext
        
        whenever(mockDeviceIdUtil.getDeviceId(mockContext)).thenReturn("device_123456")

        // Act
        val effectiveUserId = UserIdUtil.getEffectiveUserId(context, options, mockDeviceIdUtil)

        // Assert
        assertEquals("device_123456", effectiveUserId)
    }

    @Test
    fun `getEffectiveUserId should return device ID when user ID is empty and device ID is enabled`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.enableDeviceId = true
        val options = VWOInitOptions()
        options.context = mockContext
        
        whenever(mockDeviceIdUtil.getDeviceId(mockContext)).thenReturn("device_789012")

        // Act
        val effectiveUserId = UserIdUtil.getEffectiveUserId(context, options, mockDeviceIdUtil)

        // Assert
        assertEquals("device_789012", effectiveUserId)
    }

    @Test
    fun `getEffectiveUserId should return null when user ID is null and device ID is disabled`() {
        // Arrange
        val context = VWOUserContext()
        context.id = null
        context.enableDeviceId = false
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val effectiveUserId = UserIdUtil.getEffectiveUserId(context, options)

        // Assert
        assertNull(effectiveUserId)
    }

    @Test
    fun `getEffectiveUserId should return null when user ID is empty and device ID is disabled`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.enableDeviceId = false
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val effectiveUserId = UserIdUtil.getEffectiveUserId(context, options)

        // Assert
        assertNull(effectiveUserId)
    }

    @Test
    fun `getEffectiveUserId should return null when context is null`() {
        // Arrange
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val effectiveUserId = UserIdUtil.getEffectiveUserId(null, options)

        // Assert
        assertNull(effectiveUserId)
    }

    @Test
    fun `getEffectiveUserId should return null when options is null`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""

        // Act
        val effectiveUserId = UserIdUtil.getEffectiveUserId(context, null)

        // Assert
        assertNull(effectiveUserId)
    }

    @Test
    fun `getEffectiveUserId should return null when device ID generation fails`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.enableDeviceId = true
        val options = VWOInitOptions()
        options.context = mockContext
        
        whenever(mockDeviceIdUtil.getDeviceId(mockContext)).thenReturn(null)

        // Act
        val effectiveUserId = UserIdUtil.getEffectiveUserId(context, options, mockDeviceIdUtil)

        // Assert
        assertNull(effectiveUserId)
    }

    @Test
    fun `isEffectiveUserIdAvailable should return true when user ID is provided`() {
        // Arrange
        val context = VWOUserContext()
        context.id = "user123"
        context.enableDeviceId = false
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val isAvailable = UserIdUtil.isEffectiveUserIdAvailable(context, options)

        // Assert
        assertTrue(isAvailable)
    }

    @Test
    fun `isEffectiveUserIdAvailable should return true when device ID is available`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.enableDeviceId = true
        val options = VWOInitOptions()
        options.context = mockContext
        
        whenever(mockDeviceIdUtil.getDeviceId(mockContext)).thenReturn("device_123456")

        // Act
        val isAvailable = UserIdUtil.isEffectiveUserIdAvailable(context, options, mockDeviceIdUtil)

        // Assert
        assertTrue(isAvailable)
    }

    @Test
    fun `isEffectiveUserIdAvailable should return false when no user ID is available`() {
        // Arrange
        val context = VWOUserContext()
        context.id = ""
        context.enableDeviceId = false
        val options = VWOInitOptions()
        options.context = mockContext

        // Act
        val isAvailable = UserIdUtil.isEffectiveUserIdAvailable(context, options)

        // Assert
        assertFalse(isAvailable)
    }
} 