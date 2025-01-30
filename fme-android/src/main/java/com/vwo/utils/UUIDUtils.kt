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

import com.google.common.hash.Hashing
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

object UUIDUtils {

    // Define the DNS and URL namespaces for UUID v5
    private val DNS_NAMESPACE = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
    private val URL_NAMESPACE = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")

    // Define the SEED_URL constant
    private const val SEED_URL = "https://vwo.com"

    /**
     * Generates a random UUID based on an API key.
     * @param sdkKey The API key used to generate a namespace for the UUID.
     * @return A random UUID string.
     */
    fun getRandomUUID(sdkKey: String): String {
        // Generate a namespace based on the API key using DNS namespace
        val namespace = generateUUID(sdkKey, DNS_NAMESPACE)
        // Generate a random UUID (UUIDv4)
        val randomUUID = UUID.randomUUID()
        // Generate a UUIDv5 using the random UUID and the namespace
        val uuidv5 = generateUUID(randomUUID.toString(), namespace)

        return uuidv5.toString()
    }

    /**
     * Generates a UUID for a user based on their userId and accountId.
     * @param userId The user's ID.
     * @param accountId The account ID associated with the user.
     * @return A UUID string formatted without dashes and in uppercase.
     */
    fun getUUID(userId: String?, accountId: String?): String {
        // Generate a namespace UUID based on SEED_URL using URL namespace
        val VWO_NAMESPACE = generateUUID(SEED_URL, URL_NAMESPACE)
        // Ensure userId and accountId are strings
        val userIdStr = userId ?: ""
        val accountIdStr = accountId ?: ""
        // Generate a namespace UUID based on the accountId
        val userIdNamespace = generateUUID(accountIdStr, VWO_NAMESPACE)
        // Generate a UUID based on the userId and the previously generated namespace
        val uuidForUserIdAccountId = generateUUID(userIdStr, userIdNamespace)

        // Remove all dashes from the UUID and convert it to uppercase
        val desiredUuid = uuidForUserIdAccountId.toString().replace("-".toRegex(), "").uppercase(
            Locale.getDefault()
        )
        return desiredUuid
    }

    /**
     * Helper function to generate a UUID v5 based on a name and a namespace.
     * @param name The name from which to generate the UUID.
     * @param namespace The namespace used to generate the UUID.
     * @return A UUID.
     */
    fun generateUUID(name: String?, namespace: UUID?): UUID? {
        if (name == null || namespace == null) {
            return null
        }

        val namespaceBytes = toBytes(namespace)
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        val combined = ByteArray(namespaceBytes.size + nameBytes.size)
        System.arraycopy(namespaceBytes, 0, combined, 0, namespaceBytes.size)
        System.arraycopy(nameBytes, 0, combined, namespaceBytes.size, nameBytes.size)

        val hash = Hashing.sha1().hashBytes(combined).asBytes()

        // Set version to 5 (name-based using SHA-1)
        hash[6] = (hash[6].toInt() and 0x0f).toByte() // Clear version
        hash[6] = (hash[6].toInt() or 0x50).toByte() // Set to version 5
        hash[8] = (hash[8].toInt() and 0x3f).toByte() // Clear variant
        hash[8] = (hash[8].toInt() or 0x80).toByte() // Set to IETF variant

        return fromBytes(hash)
    }

    /**
     * Helper function to convert a UUID to a byte array.
     * @param uuid The UUID to convert.
     * @return A byte array.
     */
    private fun toBytes(uuid: UUID): ByteArray {
        val bytes = ByteArray(16)
        val msb = uuid.mostSignificantBits
        val lsb = uuid.leastSignificantBits

        // Convert the most significant bits and least significant bits to bytes
        for (i in 0..7) {
            bytes[i] = (msb ushr (8 * (7 - i))).toByte()
            bytes[8 + i] = (lsb ushr (8 * (7 - i))).toByte()
        }

        return bytes
    }

    /**
     * Helper function to convert a byte array to a UUID.
     * @param bytes The byte array to convert.
     * @return A UUID.
     */
    private fun fromBytes(bytes: ByteArray): UUID {
        var msb: Long = 0
        var lsb: Long = 0

        // Convert the most significant bits and least significant bits to longs
        for (i in 0..7) {
            msb = msb shl 8 or (bytes[i].toLong() and 0xff)
        }

        for (i in 8..15) {
            lsb = lsb shl 8 or (bytes[i].toLong() and 0xff)
        }

        return UUID(msb, lsb)
    }
}
