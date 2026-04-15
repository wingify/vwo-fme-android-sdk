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
package com.vwo.models

/**
 * Represents stored data for a VWO user.
 *
 * This class encapsulates information about a VWO user's assigned variations and rollout
 * information, which is typically persisted in storage.
 */
class Storage {
    var featureKey: String? = null
    var user: String? = null
    var rolloutId: Int? = null
    var rolloutKey: String? = null
    var rolloutVariationId: Int? = null
    var experimentId: Int? = null
    var experimentKey: String? = null
    var experimentVariationId: Int? = null
    var holdoutIds: JsonArrayWrapper? = null
    var notInHoldoutIds: JsonArrayWrapper? = null

    // ---
    // MAJOR WORKAROUND because Gson and Jackson is being used simultaneously,
    // other solutions requires some refactoring and might cause side effects in existing behavior
    class JsonArrayWrapper {
        var values: List<Int>? = null
    }

    var decisionExpiryTime: Long? = null

    /**
     * Checks whether this stored decision has expired.
     *
     * A decision is considered expired when its [decisionExpiryTime] is a positive timestamp
     * that falls before the current time. If the expiry is null or non-positive, the decision
     * is treated as valid indefinitely (for backward compatibility).
     *
     * @return `true` if the decision has expired, `false` otherwise.
     */
    fun isDecisionExpired(): Boolean {
        val expiry = decisionExpiryTime ?: return false
        if (expiry <= 0L) return false
        return System.currentTimeMillis() > expiry
    }

    fun isDecisionExpiryTimeFoundInStorage(): Boolean {
        return decisionExpiryTime != null
    }

}
