/**
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
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
package com.vwo.models.user

import com.wingify.models.user.FlagCollection as WingifyFlagCollection
import com.vwo.models.user.FlagCollection as VWOFlagCollection

/**
 * Batch feature flag evaluation result for the VWO SDK.
 *
 * Thin wrapper around [com.wingify.models.user.FlagCollection].
 */
class FlagCollection private constructor(
    private val flagCollection: WingifyFlagCollection,
) {
    /**
     * Returns the evaluated flag for [featureKey], or a disabled flag when the key is absent.
     *
     * @param featureKey Feature key to look up.
     * @return [GetFlag] for the key.
     */
    fun get(featureKey: String): GetFlag = flagCollection.getRaw(featureKey)

    /**
     * @return The set of feature keys present in this collection.
     */
    fun keys(): Set<String> = flagCollection.keys()

    /**
     * @return The number of evaluated flags in this collection.
     */
    fun size(): Int = flagCollection.size()

    /**
     * @return An iterator over feature-key to [GetFlag] entries.
     */
    fun iterator(): Iterator<Map.Entry<String, GetFlag>> = flagCollection.rawIterator()

    internal companion object {
        /**
         * Wraps a Wingify [WingifyFlagCollection] for the VWO public API.
         */
        fun wrap(collection: WingifyFlagCollection): VWOFlagCollection =
            VWOFlagCollection(collection)
    }
}
