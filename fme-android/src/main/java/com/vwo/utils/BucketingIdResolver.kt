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

/**
 * Utility object for resolving the bucketing ID.
 *
 * Custom bucketing seed is used when provided in context (non-null, non-empty).
 * Otherwise, falls back to userId for bucketing.
 */
object BucketingIdResolver {

    /**
     * Resolves the bucketing ID.
     *
     * @param userId The user ID from context
     * @param context The VWOUserContext containing bucketingSeed
     * @return The resolved bucketing ID - either bucketingSeed or userId
     */
    fun resolve(userId: String?, context: VWOUserContext?): String? {
        val customBucketingSeed = context?.bucketingSeed

        return if (!customBucketingSeed.isNullOrEmpty()) {
            customBucketingSeed
        } else {
            userId
        }
    }

    /**
     * Formats the user ID for logging purposes.
     * When custom bucketing seed is used, shows both userId and seed.
     *
     * @param userId The original user ID
     * @param bucketingId The resolved bucketing ID
     * @return Formatted string for logging: "userId (Seed: bucketingId)" or just "userId"
     */
    fun formatUserIdForLogging(userId: String?, bucketingId: String?): String {
        return if (bucketingId != null && userId != null && bucketingId != userId) {
            "$userId (Seed: $bucketingId)"
        } else {
            userId ?: ""
        }
    }

    /**
     * Checks if custom bucketing seed is being used.
     *
     * @param userId The user ID from context
     * @param bucketingId The resolved bucketing ID
     * @return true if custom seed is being used, false otherwise
     */
    fun isUsingCustomSeed(userId: String?, bucketingId: String?): Boolean {
        return bucketingId != null && userId != null && bucketingId != userId
    }
}
