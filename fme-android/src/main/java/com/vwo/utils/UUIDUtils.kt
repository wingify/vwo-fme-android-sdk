/**
 * Copyright 2024 Wingify Software Pvt. Ltd.
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

import java.util.Locale
import java.util.UUID

object UUIDUtils {
    /**
     * Generates a random UUID based on an API key.
     * @param apiKey The API key used to generate a namespace for the UUID.
     * @return A random UUID string.
     */
    fun getRandomUUID(apiKey: String): String {
        // Generate a namespace based on the API key using DNS namespace
        val namespace = UUID.nameUUIDFromBytes(apiKey.toByteArray())
        // Generate a random UUID using the namespace derived from the API key
        val randomUUID = UUID.randomUUID()
        return UUID(namespace.mostSignificantBits, randomUUID.leastSignificantBits).toString()
    }

    /**
     * Generates a UUID for a user based on their userId and accountId.
     * @param userId The user's ID.
     * @param accountId The account ID associated with the user.
     * @return A UUID string formatted without dashes and in uppercase.
     */
    fun getUUID(userId: String?, accountId: String?): String {
        val VWO_NAMESPACE = UUID.nameUUIDFromBytes("https://vwo.com".toByteArray())
        // Generate a namespace UUID based on the accountId
        val userIdNamespace = generateUUID(accountId, VWO_NAMESPACE.toString())
        // Generate a UUID based on the userId and the previously generated namespace
        val uuidForUserIdAccountId = generateUUID(userId, userIdNamespace.toString())
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
     * @return A UUID string or null if inputs are invalid.
     */
    private fun generateUUID(name: String?, namespace: String?): UUID? {
        // Check for valid input to prevent errors
        if (name == null || namespace == null) {
            return null
        }
        // Generate and return the UUID v5
        return UUID.nameUUIDFromBytes(name.toByteArray())
    }
}
