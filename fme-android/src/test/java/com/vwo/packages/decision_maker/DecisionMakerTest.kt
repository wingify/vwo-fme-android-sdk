/*
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
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
package com.vwo.packages.decision_maker

import com.wingify.packages.decision_maker.DecisionMaker
import org.junit.Assert.*

import org.junit.Before
import org.junit.Test

class DecisionMakerTest {

    @Before
    fun setUp() {
    }

    @Test
    fun generateHashValue() {
        assertEquals(3929804969, DecisionMaker().generateHashValue("Swapnil"))
        assertEquals(1364076727, DecisionMaker().generateHashValue(""))
        assertEquals(562546376, DecisionMaker().generateHashValue("VWO"))
        assertEquals(2360047679L, DecisionMaker().generateHashValue("key123"))

    }
    @Test
    fun generateHashValue_differentCase_returnsDifferentHash() {
        val input = "TestCase"
        val lowerCaseHash = DecisionMaker().generateHashValue(input.lowercase())
        val upperCaseHash = DecisionMaker().generateHashValue(input.uppercase())
        assertNotEquals(lowerCaseHash, upperCaseHash)
    }

    @Test
    fun generateHashValue_specialCharacters_returnsValidHash() {
        val input = "!@#$%^&*()"
        val hash = DecisionMaker().generateHashValue(input)
        assertNotEquals(0, hash)
    }

    @Test
    fun generateHashValue_unicodeCharacters_returnsValidHash() {
        val input = "你好世界"
        val hash = DecisionMaker().generateHashValue(input)
        assertNotEquals(0, hash)
    }

    @Test
    fun generateHashValue_longString_returnsValidHash() {
        val input = "This is a very long string to test the hash function with a large input."
        val hash = DecisionMaker().generateHashValue(input)
        // Add an assertion based on your expected behavior for long strings
        // For example, assert that the hash is within a certain range:
        assertEquals(880528217,hash)
    }

    @Test
    fun generateBucketValue() {
        val hashValue = 2147483647L // Example hash value
        val maxValue = 100
        val multiplier = 1
        val expectedBucketValue = Math.floor((maxValue * (hashValue.toDouble() / Math.pow(2.0, 32.0)) + 1) * multiplier).toInt()
        val bucketValue = DecisionMaker().generateBucketValue(hashValue, maxValue, multiplier)
        assertEquals(expectedBucketValue, bucketValue)
    }

    @Test
    fun getBucketValueForUser() {
        val userId = "user123"
        val maxValue = 100
        val mockHashValue = 123456789L // Mocked hash value

        // Create a spy of DecisionMaker
        val decisionMaker = org.mockito.Mockito.spy(DecisionMaker())

        // Mock the generateHashValue method to return the mockHashValue
        org.mockito.Mockito.`when`(decisionMaker.generateHashValue(userId)).thenReturn(mockHashValue)

        val expectedBucketValue = decisionMaker.generateBucketValue(mockHashValue, maxValue)
        val bucketValue = decisionMaker.getBucketValueForUser(userId, maxValue)

        assertEquals(expectedBucketValue, bucketValue)
    }

    @Test
    fun calculateBucketValue() {
        val str = "testString"
        val multiplier = 1
        val maxValue = 10000
        val mockHashValue = 987654321L // Mocked hash value

        // Create a spy of DecisionMaker
        val decisionMaker = org.mockito.Mockito.spy(DecisionMaker())

        // Mock the generateHashValue method to return the mockHashValue
        org.mockito.Mockito.`when`(decisionMaker.generateHashValue(str)).thenReturn(mockHashValue)

        val expectedBucketValue = decisionMaker.generateBucketValue(mockHashValue, maxValue, multiplier)
        val bucketValue = decisionMaker.calculateBucketValue(str, multiplier, maxValue)

        assertEquals(expectedBucketValue, bucketValue)
    }

    /**
     * Finds a user ID that falls in the bottom 5% holdout bucket (bucket value 1-5).
     * Run this test and check the console output for a ready-to-use user ID.
     *
     * Bucket key format matches holdout evaluation: {accountId}_{holdoutId}_{userId}
     */
    @Test
    fun findUserIdInBottom5PercentHoldoutBucket() {
        val accountId = 951881
        val holdoutId = 200
        val trafficPercent = 5

        val decisionMaker = DecisionMaker()
        val baseMillis = System.currentTimeMillis()

        var matchedUserId: String? = null
        var matchedBucketValue = -1

        for (i in 0 until 10_000) {
            val userId = "user_${baseMillis}_$i"
            val bucketKey = "${accountId}_${holdoutId}_$userId"
            val bucketValue = decisionMaker.getBucketValueForUser(bucketKey)

            if (bucketValue != 0 && bucketValue <= trafficPercent) {
                matchedUserId = userId
                matchedBucketValue = bucketValue
                break
            }
        }

        assertNotNull("Could not find a user in the bottom 5% bucket within 10,000 attempts", matchedUserId)
        assertTrue(matchedBucketValue in 1..trafficPercent)

        println("=== Holdout test user (bucket <= $trafficPercent%) ===")
        println("userId: $matchedUserId")
        println("bucketValue: $matchedBucketValue")
        println("bucketKey: ${accountId}_${holdoutId}_$matchedUserId")
    }
}