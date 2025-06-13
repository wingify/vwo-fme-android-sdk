package com.vwo.utils

import com.google.common.hash.Hashing
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
     * CRITICAL COMPARISON TEST: Guava vs Java MessageDigest
     * This test ensures our replacement produces identical results to the original Guava implementation
     * Run this test whenever you need to verify compatibility
     */
    @Test
    fun testGuavaVsJavaHashingComparison() {
        val testCases = listOf(
            "hello world",
            "VWO SDK",
            "",
            "123456789",
            "special chars: !@#$%^&*()",
            "unicode: 你好世界",
            "long string: " + "x".repeat(1000),
            "mixed: Abc123!@#测试"
        )

        for (testString in testCases) {
            val inputBytes = testString.toByteArray(StandardCharsets.UTF_8)
            
            // Guava's implementation (ORIGINAL)
            @Suppress("DEPRECATION") // We know it's deprecated, that's why we replaced it
            val guavaHash = Hashing.sha1().hashBytes(inputBytes).asBytes()
            
            // Java's implementation (CURRENT)
            val javaHash = MessageDigest.getInstance("SHA-1").digest(inputBytes)
            
            // They should produce identical results
            assertArrayEquals(
                "Guava and Java MessageDigest should produce identical SHA-1 hashes for: '$testString'",
                guavaHash,
                javaHash
            )
        }
    }

    /**
     * END-TO-END COMPARISON TEST: Complete UUID generation pipeline
     * This tests the complete generateUUID function with both implementations
     * Run this to verify complete pipeline compatibility
     */
    @Test
    fun testCompleteUUIDGenerationComparison() {
        val testCases = listOf(
            Pair("test-user-123", "account-456"),
            Pair("", ""),
            Pair("user with spaces", "account with spaces"),
            Pair("特殊用户", "特殊账户"),
            Pair("very-long-user-id-" + "x".repeat(100), "very-long-account-id-" + "y".repeat(100)),
            Pair("null-test", null),
            Pair(null, "null-test")
        )

        for ((name, _) in testCases) {
            val namespace = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
            
            // Generate UUID using current (Java MessageDigest) implementation
            val currentUuid = UUIDUtils.generateUUID(name, namespace)
            
            // Generate UUID using Guava implementation (for comparison)
            val guavaUuid = generateUUIDWithGuava(name, namespace)
            
            // They should produce identical UUIDs
            assertEquals(
                "UUID generation should be identical between Guava and Java implementations for name='$name'",
                guavaUuid,
                currentUuid
            )
        }
    }

    /**
     * USER UUID COMPARISON TEST: Compare complete getUUID method
     * This tests the full user UUID generation pipeline
     */
    @Test
    fun testUserUUIDPipelineComparison() {
        val testUsers = listOf(
            Pair("user123", "account456"),
            Pair("test@email.com", "12345"),
            Pair("用户测试", "账户测试"),
            Pair("", ""),
            Pair(null, null),
            Pair("user", null),
            Pair(null, "account")
        )

        for ((userId, accountId) in testUsers) {
            // Current implementation 
            val currentUuid = UUIDUtils.getUUID(userId, accountId)
            
            // Guava-based implementation (for comparison)
            val guavaBasedUuid = getUserUUIDWithGuava(userId, accountId)
            
            // Should be identical
            assertEquals(
                "User UUID should be identical between implementations for userId='$userId', accountId='$accountId'",
                guavaBasedUuid,
                currentUuid
            )
        }
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

    // ========================================
    // HELPER FUNCTIONS FOR GUAVA COMPARISON
    // ========================================

    /**
     * Helper function to generate UUID using Guava's hashing (for comparison)
     * This is exactly how the OLD implementation worked
     */
    @Suppress("DEPRECATION")
    private fun generateUUIDWithGuava(name: String?, namespace: UUID?): UUID? {
        if (name == null || namespace == null) {
            return null
        }

        val namespaceBytes = toBytes(namespace)
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        val combined = ByteArray(namespaceBytes.size + nameBytes.size)
        System.arraycopy(namespaceBytes, 0, combined, 0, namespaceBytes.size)
        System.arraycopy(nameBytes, 0, combined, namespaceBytes.size, nameBytes.size)

        // OLD: Using Guava's Hashing
        val hash = Hashing.sha1().hashBytes(combined).asBytes()

        // Set version to 5 (name-based using SHA-1)
        hash[6] = (hash[6].toInt() and 0x0f).toByte() // Clear version
        hash[6] = (hash[6].toInt() or 0x50).toByte() // Set to version 5
        hash[8] = (hash[8].toInt() and 0x3f).toByte() // Clear variant
        hash[8] = (hash[8].toInt() or 0x80).toByte() // Set to IETF variant

        return fromBytes(hash)
    }

    /**
     * Helper function to generate User UUID using Guava-based pipeline (for comparison)
     */
    private fun getUserUUIDWithGuava(userId: String?, accountId: String?): String {
        // Constants from UUIDUtils
        val SEED_URL = "https://vwo.com"
        val URL_NAMESPACE = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")
        
        // Generate VWO namespace using Guava
        val VWO_NAMESPACE = generateUUIDWithGuava(SEED_URL, URL_NAMESPACE)
        
        // Ensure userId and accountId are strings
        val userIdStr = userId ?: ""
        val accountIdStr = accountId ?: ""
        
        // Generate namespace UUID based on the accountId using Guava
        val userIdNamespace = generateUUIDWithGuava(accountIdStr, VWO_NAMESPACE)
        
        // Generate UUID based on the userId and the previously generated namespace using Guava
        val uuidForUserIdAccountId = generateUUIDWithGuava(userIdStr, userIdNamespace)

        // Remove all dashes from the UUID and convert it to uppercase
        val desiredUuid = uuidForUserIdAccountId.toString().replace("-".toRegex(), "").uppercase()
        return desiredUuid
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