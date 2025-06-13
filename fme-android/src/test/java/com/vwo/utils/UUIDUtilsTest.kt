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

import org.junit.Test
import org.junit.Assert.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

class UUIDUtilsTest {

    @Test
    fun testGetUUID() {
        val userId = "testUser123"
        val accountId = "123456"
        
        val uuid1 = UUIDUtils.getUUID(userId, accountId)
        val uuid2 = UUIDUtils.getUUID(userId, accountId)
        
        // Same inputs should generate same UUID
        assertEquals("Same inputs should generate same UUID", uuid1, uuid2)
        
        // UUID should be 32 characters (without dashes) and uppercase
        assertEquals("UUID should be 32 characters", 32, uuid1.length)
        assertTrue("UUID should be uppercase", uuid1 == uuid1.uppercase())
        assertTrue("UUID should contain only hex characters", uuid1.matches(Regex("[0-9A-F]+")))
    }

    @Test
    fun testGetRandomUUID() {
        val sdkKey = "test-sdk-key"
        
        val uuid1 = UUIDUtils.getRandomUUID(sdkKey)
        val uuid2 = UUIDUtils.getRandomUUID(sdkKey)
        
        // Random UUIDs should be different even with same input
        assertNotEquals("Random UUIDs should be different", uuid1, uuid2)
        
        // Should be valid UUID format
        assertTrue("Should be valid UUID format", uuid1.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun testGenerateUUID() {
        val name = "test-name"
        val namespace = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
        
        val uuid1 = UUIDUtils.generateUUID(name, namespace)
        val uuid2 = UUIDUtils.generateUUID(name, namespace)
        
        // Same inputs should generate same UUID
        assertEquals("Same inputs should generate same UUID", uuid1, uuid2)
        assertNotNull("UUID should not be null", uuid1)
    }

    /**
     * Test that verifies SHA-1 hashing produces consistent results
     * This ensures our MessageDigest implementation is working correctly
     */
    @Test
    fun testSHA1HashConsistency() {
        val testCases = listOf(
            "hello world",
            "VWO SDK",
            "",
            "123456789",
            "special chars: !@#$%^&*()",
            "unicode: 你好世界",
            "long string: " + "x".repeat(1000)
        )

        for (testString in testCases) {
            val inputBytes = testString.toByteArray(StandardCharsets.UTF_8)
            
            // Test Java's MessageDigest implementation consistency
            val hash1 = MessageDigest.getInstance("SHA-1").digest(inputBytes)
            val hash2 = MessageDigest.getInstance("SHA-1").digest(inputBytes)
            
            // Same input should produce same hash
            assertArrayEquals(
                "SHA-1 hashing should be consistent for: '$testString'",
                hash1,
                hash2
            )
            
            // Hash should be 20 bytes (SHA-1 standard)
            assertEquals("SHA-1 hash should be 20 bytes", 20, hash1.size)
        }
    }

    /**
     * Test the complete user UUID generation (getUUID method) for consistency
     */
    @Test
    fun testUserUUIDConsistency() {
        val testUsers = listOf(
            Pair("user123", "account456"),
            Pair("test@email.com", "12345"),
            Pair("用户测试", "账户测试"),
            Pair("", ""),
            Pair(null, null)
        )

        for ((userId, accountId) in testUsers) {
            val uuid1 = UUIDUtils.getUUID(userId, accountId)
            val uuid2 = UUIDUtils.getUUID(userId, accountId)
            
            // Same inputs should always generate same UUID
            assertEquals(
                "getUUID should be deterministic for userId='$userId', accountId='$accountId'",
                uuid1, 
                uuid2
            )
            
            // UUID should be valid format (32 hex chars, uppercase)
            if (uuid1.isNotEmpty()) {
                assertEquals("UUID should be 32 characters", 32, uuid1.length)
                assertTrue("UUID should be uppercase", uuid1 == uuid1.uppercase())
                assertTrue("UUID should contain only hex characters", uuid1.matches(Regex("[0-9A-F]+")))
            }
        }
    }

    /**
     * Test UUID v5 compliance
     * Ensures our UUID generation follows RFC 4122 specification
     */
    @Test
    fun testUUIDv5Compliance() {
        val name = "test"
        val namespace = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
        val uuid = UUIDUtils.generateUUID(name, namespace)
        
        assertNotNull("UUID should not be null", uuid)
        
        // Convert to string and check version
        val uuidString = uuid.toString()
        
        // UUID v5 should have version 5 in the third group, first character
        val versionChar = uuidString.split("-")[2][0]
        assertEquals("UUID should be version 5", '5', versionChar)
        
        // Variant should be 10 (bits 6-7 of octet 8)
        val variantChar = uuidString.split("-")[3][0]
        assertTrue("UUID variant should be RFC 4122 compliant", 
            variantChar in listOf('8', '9', 'a', 'b'))
    }

    // Helper functions (copied from UUIDUtils for testing)
    private fun toBytes(uuid: UUID): ByteArray {
        val bytes = ByteArray(16)
        val msb = uuid.mostSignificantBits
        val lsb = uuid.leastSignificantBits

        for (i in 0..7) {
            bytes[i] = (msb ushr (8 * (7 - i))).toByte()
            bytes[8 + i] = (lsb ushr (8 * (7 - i))).toByte()
        }

        return bytes
    }

    private fun fromBytes(bytes: ByteArray): UUID {
        var msb: Long = 0
        var lsb: Long = 0

        for (i in 0..7) {
            msb = msb shl 8 or (bytes[i].toLong() and 0xff)
        }

        for (i in 8..15) {
            lsb = lsb shl 8 or (bytes[i].toLong() and 0xff)
        }

        return UUID(msb, lsb)
    }
} 