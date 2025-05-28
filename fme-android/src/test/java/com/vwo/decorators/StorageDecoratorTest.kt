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
package com.vwo.decorators

import com.vwo.models.user.VWOUserContext
import com.vwo.services.StorageService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.ArgumentMatchers.any

@RunWith(MockitoJUnitRunner::class)
class StorageDecoratorTest {

    @Mock
    private lateinit var mockStorageService: StorageService

    private lateinit var storageDecorator: StorageDecorator

    @Before
    fun setup() {
        storageDecorator = StorageDecorator()
    }

    @Test
    fun `test getFeatureFromStorage returns data from storage service`() {
        // Arrange
        val featureKey = "test_feature"
        val context = VWOUserContext().apply { id = "user123" }
        val expectedData = mapOf("key" to "value")
        `when`(mockStorageService.getDataInStorage(featureKey, context)).thenReturn(expectedData)

        // Act
        val result = storageDecorator.getFeatureFromStorage(featureKey, context, mockStorageService)

        // Assert
        assertEquals(expectedData, result)
        verify(mockStorageService).getDataInStorage(featureKey, context)
    }

    @Test
    fun `test setDataInStorage with valid experiment data returns variation`() {
        // Arrange
        val data = mapOf(
            "featureKey" to "test_feature",
            "userId" to "user123",
            "experimentKey" to "exp123",
            "experimentVariationId" to 1
        )

        // Act
        val result = storageDecorator.setDataInStorage(data, mockStorageService)

        // Assert
        assertNotNull(result)
        verify(mockStorageService).setDataInStorage(data)
    }

    @Test
    fun `test setDataInStorage with null featureKey returns null`() {
        // Arrange
        val data = mapOf(
            "userId" to "user123",
            "experimentKey" to "exp123",
            "experimentVariationId" to 1
        )

        // Act
        val result = storageDecorator.setDataInStorage(data, mockStorageService)

        // Assert
        assertNull(result)
        verify(mockStorageService, never()).setDataInStorage(anyMap())
    }

    @Test
    fun `test setDataInStorage with empty featureKey returns null`() {
        // Arrange
        val data = mapOf(
            "featureKey" to "",
            "userId" to "user123",
            "experimentKey" to "exp123",
            "experimentVariationId" to 1
        )

        // Act
        val result = storageDecorator.setDataInStorage(data, mockStorageService)

        // Assert
        assertNull(result)
        verify(mockStorageService, never()).setDataInStorage(anyMap())
    }

    @Test
    fun `test setDataInStorage with null userId returns null`() {
        // Arrange
        val data = mapOf(
            "featureKey" to "test_feature",
            "experimentKey" to "exp123",
            "experimentVariationId" to 1
        )

        // Act
        val result = storageDecorator.setDataInStorage(data, mockStorageService)

        // Assert
        assertNull(result)
        verify(mockStorageService, never()).setDataInStorage(anyMap())
    }

    @Test
    fun `test setDataInStorage with empty userId returns null`() {
        // Arrange
        val data = mapOf(
            "featureKey" to "test_feature",
            "userId" to "",
            "experimentKey" to "exp123",
            "experimentVariationId" to 1
        )

        // Act
        val result = storageDecorator.setDataInStorage(data, mockStorageService)

        // Assert
        assertNull(result)
        verify(mockStorageService, never()).setDataInStorage(anyMap())
    }

    @Test
    fun `test setDataInStorage with rolloutKey but no experimentKey or rolloutVariationId returns null`() {
        // Arrange
        val data = mapOf(
            "featureKey" to "test_feature",
            "userId" to "user123",
            "rolloutKey" to "rollout123"
        )

        // Act
        val result = storageDecorator.setDataInStorage(data, mockStorageService)

        // Assert
        assertNull(result)
        verify(mockStorageService, never()).setDataInStorage(anyMap())
    }

    @Test
    fun `test setDataInStorage with experimentKey but no experimentVariationId returns null`() {
        // Arrange
        val data = mapOf(
            "featureKey" to "test_feature",
            "userId" to "user123",
            "experimentKey" to "exp123"
        )

        // Act
        val result = storageDecorator.setDataInStorage(data, mockStorageService)

        // Assert
        assertNull(result)
        verify(mockStorageService, never()).setDataInStorage(anyMap())
    }

    @Test
    fun `test setDataInStorage with valid rollout data returns variation`() {
        // Arrange
        val data = mapOf(
            "featureKey" to "test_feature",
            "userId" to "user123",
            "rolloutKey" to "rollout123",
            "experimentKey" to "exp123",
            "rolloutVariationId" to 1,
            "experimentVariationId" to 1
        )

        // Act
        val result = storageDecorator.setDataInStorage(data, mockStorageService)

        // Assert
        assertNotNull(result)
        verify(mockStorageService).setDataInStorage(data)
    }

    @Test
    fun `test setDataInStorage with rolloutKey and experimentKey but no rolloutVariationId returns null`() {
        // Arrange
        val data = mapOf(
            "featureKey" to "test_feature",
            "userId" to "user123",
            "rolloutKey" to "rollout123",
            "experimentKey" to "exp123"
        )

        // Act
        val result = storageDecorator.setDataInStorage(data, mockStorageService)

        // Assert
        assertNull(result)
        verify(mockStorageService, never()).setDataInStorage(anyMap())
    }
}