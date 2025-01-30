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
package com.vwo.packages.decision_maker

import com.github.eprst.murmur3.MurmurHash3
import kotlin.math.floor
import kotlin.math.pow

class DecisionMaker {
    /**
     * Generates a bucket value for a user by hashing the user ID with murmurHash
     * and scaling it down to a specified maximum value.
     *
     * @param hashValue The hash value generated after hashing
     * @param maxValue The maximum value up to which the hash value needs to be scaled
     * @param multiplier Multiplier to adjust the scale in case the traffic allocation is less than 100
     * @return The bucket value of the user
     */
    fun generateBucketValue(hashValue: Long, maxValue: Int, multiplier: Int): Int {
        val ratio: Double =
            hashValue.toDouble() / 2.0.pow(32.0) // Calculate the ratio of the hash value to the maximum hash value
        val multipliedValue =
            (maxValue * ratio + 1) * multiplier // Apply the multiplier after scaling the hash value
        return floor(multipliedValue).toInt() // Floor the value to get an integer bucket value
    }

    /**
     * Generates a bucket value for a user by hashing the user ID with murmurHash
     * and scaling it down to a specified maximum value.
     *
     * @param hashValue The hash value generated after hashing
     * @param maxValue The maximum value up to which the hash value needs to be scaled
     */
    fun generateBucketValue(hashValue: Long, maxValue: Int): Int {
        val multiplier = 1
        val ratio: Double =
            hashValue.toDouble() / 2.0.pow(32.0) // Calculate the ratio of the hash value to the maximum hash value
        val multipliedValue =
            (maxValue * ratio + 1) * multiplier // Apply the multiplier after scaling the hash value
        return floor(multipliedValue).toInt() // Floor the value to get an integer bucket value
    }

    /**
     * Validates the user ID and generates a bucket value for the user by hashing the user ID with murmurHash
     * and scaling it down.
     *
     * @param userId The unique ID assigned to the user
     * @param maxValue The maximum value for bucket scaling (default is 100)
     * @return The bucket value allotted to the user (between 1 and maxValue)
     */
    fun getBucketValueForUser(userId: String?, maxValue: Int): Int {
        require(!(userId == null || userId.isEmpty())) { "User ID cannot be null or empty" }
        val hashValue = generateHashValue(userId) // Generate the hash value using murmurHash
        return generateBucketValue(
            hashValue,
            maxValue,
            1
        ) // Generate the bucket value using the hash value (default multiplier)
    }

    /**
     * Calculates the bucket value for a given user ID.
     *
     * This function generates a bucket value within a specified range based on the user ID. The
     * bucket value is determined using a hashing algorithm and can be used for various purposes
     * like user segmentation or feature rollout.
     *
     * @param userId The ID of the user.
     * @return The calculated bucket value for the user.
     * @throws IllegalArgumentException If the user ID isnull or empty.
     */
    fun getBucketValueForUser(userId: String?): Int {
        val maxValue = 100
        require(!(userId == null || userId.isEmpty())) { "User ID cannot be null or empty" }
        val hashValue = generateHashValue(userId) // Generate the hash value using murmurHash
        return generateBucketValue(
            hashValue,
            maxValue,
            1
        ) // Generate the bucket value using the hash value (default multiplier)
    }

    /**
     * Calculates the bucket value for a given string and optional multiplier and maximum value.
     *
     * @param str The string to hash
     * @param multiplier Multiplier to adjust the scale (default is 1)
     * @param maxValue Maximum value for bucket scaling (default is 10000)
     * @return The calculated bucket value
     */
    fun calculateBucketValue(str: String, multiplier: Int, maxValue: Int): Int {
        val hashValue = generateHashValue(str) // Generate the hash value for the string

        return generateBucketValue(
            hashValue,
            maxValue,
            multiplier
        ) // Generate and return the bucket value
    }

    /**
     * Calculates the bucket value for a given string.
     *
     * This function generates a bucket value within a specified range based on the input string.
     * The bucket value is determined using a hashing algorithm and can be used for various
     * purposes like consistent hashing or data partitioning.
     *
     * @param str The input string.
     * @return The calculated bucket value for the string.
     */
    fun calculateBucketValue(str: String): Int {
        val multiplier = 1
        val maxValue = 10000
        val hashValue = generateHashValue(str) // Generate the hash value for the string

        return generateBucketValue(
            hashValue,
            maxValue,
            multiplier
        ) // Generate and return the bucket value
    }

    /**
     * Generates a hash value for a given key using murmurHash.
     *
     * @param hashKey The key to hash
     * @return The generated hash value
     */
    fun generateHashValue(hashKey: String): Long {
        /**
         * Took reference from StackOverflow (https://stackoverflow.com/) to:
         * Convert the int to unsigned long value
         * Author - Mysticial (https://stackoverflow.com/users/922184/mysticial)
         * Source - https://stackoverflow.com/questions/9578639/best-way-to-convert-a-signed-integer-to-an-unsigned-long
         */
        val murmurHash: Int =
            MurmurHash3.murmurhash3_x86_32(hashKey.toByteArray(), 0, hashKey.length, SEED_VALUE)
        val signedMurmurHash = (murmurHash.toLong() and 0xFFFFFFFFL)

        return signedMurmurHash
    }

    companion object {
        private const val SEED_VALUE = 1 // Seed value for the hash function
        const val MAX_TRAFFIC_VALUE: Int = 10000 // Maximum traffic value used as a default scale
        const val MAX_CAMPAIGN_VALUE: Int = 100
    }
}
