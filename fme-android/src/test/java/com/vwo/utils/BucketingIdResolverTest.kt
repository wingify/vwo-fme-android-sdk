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

import com.vwo.models.user.VWOUserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for BucketingIdResolver.
 * Tests the two-level safety mechanism for custom bucketing seed.
 */
class BucketingIdResolverTest {

    @Test
    fun `resolve returns userId when bucketingSeed is not set`() {
        val userId = "user-123"
        val context = VWOUserContext().apply {
            id = userId
        }

        val result = BucketingIdResolver.resolve(userId, context)

        assertEquals(userId, result)
    }

    @Test
    fun `resolve returns userId when isCustomBucketingSeed is true but customBucketingSeed is null`() {
        val userId = "user-123"
        val context = VWOUserContext().apply {
            id = userId
            bucketingSeed = null
        }

        val result = BucketingIdResolver.resolve(userId, context)

        assertEquals(userId, result)
    }

    @Test
    fun `resolve returns userId when isCustomBucketingSeed is true but customBucketingSeed is empty`() {
        val userId = "user-123"
        val context = VWOUserContext().apply {
            id = userId
            bucketingSeed = ""
        }

        val result = BucketingIdResolver.resolve(userId, context)

        assertEquals(userId, result)
    }

    @Test
    fun `resolve returns customBucketingSeed when both conditions are met`() {
        val userId = "user-123"
        val customSeed = "custom-seed-456"
        val context = VWOUserContext().apply {
            id = userId
            bucketingSeed = customSeed
        }

        val result = BucketingIdResolver.resolve(userId, context)

        assertEquals(customSeed, result)
    }

    @Test
    fun `resolve returns userId when context is null`() {
        val userId = "user-123"

        val result = BucketingIdResolver.resolve(userId, null)

        assertEquals(userId, result)
    }

    @Test
    fun `resolve returns bucketingSeed when userId is null but seed is provided`() {
        val customSeed = "custom-seed-456"
        val context = VWOUserContext().apply {
            id = null
            bucketingSeed = customSeed
        }

        val result = BucketingIdResolver.resolve(null, context)

        assertEquals(customSeed, result)
    }

    @Test
    fun `formatUserIdForLogging returns formatted string when using custom seed`() {
        val userId = "user-123"
        val bucketingId = "custom-seed-456"

        val result = BucketingIdResolver.formatUserIdForLogging(userId, bucketingId)

        assertEquals("user-123 (Seed: custom-seed-456)", result)
    }

    @Test
    fun `formatUserIdForLogging returns userId when not using custom seed`() {
        val userId = "user-123"

        val result = BucketingIdResolver.formatUserIdForLogging(userId, userId)

        assertEquals(userId, result)
    }

    @Test
    fun `formatUserIdForLogging returns empty string when both are null`() {
        val result = BucketingIdResolver.formatUserIdForLogging(null, null)

        assertEquals("", result)
    }

    @Test
    fun `isUsingCustomSeed returns true when bucketingId differs from userId`() {
        val userId = "user-123"
        val bucketingId = "custom-seed-456"

        val result = BucketingIdResolver.isUsingCustomSeed(userId, bucketingId)

        assertTrue(result)
    }

    @Test
    fun `isUsingCustomSeed returns false when bucketingId equals userId`() {
        val userId = "user-123"

        val result = BucketingIdResolver.isUsingCustomSeed(userId, userId)

        assertFalse(result)
    }

    @Test
    fun `isUsingCustomSeed returns false when userId is null`() {
        val bucketingId = "custom-seed-456"

        val result = BucketingIdResolver.isUsingCustomSeed(null, bucketingId)

        assertFalse(result)
    }

    @Test
    fun `isUsingCustomSeed returns false when bucketingId is null`() {
        val userId = "user-123"

        val result = BucketingIdResolver.isUsingCustomSeed(userId, null)

        assertFalse(result)
    }
}
