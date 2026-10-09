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
package com.wingify.models.user

import com.vwo.models.user.GetFlag as VWOGetFlag

/**
 * Immutable collection of evaluated feature flags keyed by feature key.
 */
class FlagCollection internal constructor(
    private val flags: Map<String, VWOGetFlag>,
    private val fallbackContext: WingifyUserContext,
) {
    /**
     * Returns the evaluated flag for [featureKey], or a disabled flag when the key is absent.
     *
     * @param featureKey Feature key to look up.
     * @return Wrapped [GetFlag] for the key.
     */
    fun get(featureKey: String): GetFlag = GetFlag.wrap(getRaw(featureKey))

    /**
     * @return The set of feature keys present in this collection.
     */
    fun keys(): Set<String> = flags.keys

    /**
     * @return The number of evaluated flags in this collection.
     */
    fun size(): Int = flags.size

    /**
     * @return An iterator over feature-key to [GetFlag] entries.
     */
    fun iterator(): Iterator<Map.Entry<String, GetFlag>> {
        return object : Iterator<Map.Entry<String, GetFlag>> {
            private val iterator = flags.entries.iterator()

            override fun hasNext(): Boolean = iterator.hasNext()

            override fun next(): Map.Entry<String, GetFlag> {
                val entry = iterator.next()
                return object : Map.Entry<String, GetFlag> {
                    override val key: String = entry.key
                    override val value: GetFlag = GetFlag.wrap(entry.value)
                }
            }
        }
    }

    /**
     * Returns the underlying VWO [com.vwo.models.user.GetFlag] for [featureKey],
     * or a disabled flag when the key is absent.
     */
    internal fun getRaw(featureKey: String): com.vwo.models.user.GetFlag {
        return flags[featureKey]
            ?: com.vwo.models.user.GetFlag(fallbackContext).apply { setIsEnabled(false) }
    }

    /**
     * @return An iterator over the underlying VWO [com.vwo.models.user.GetFlag] entries.
     */
    internal fun rawIterator(): Iterator<Map.Entry<String, com.vwo.models.user.GetFlag>> =
        flags.entries.iterator()

    internal companion object {
        /**
         * Creates an immutable [FlagCollection] from the given flag map.
         *
         * @param flags Evaluated flags keyed by feature key.
         * @param fallbackContext Context used to build disabled flags for missing keys.
         */
        fun create(
            flags: Map<String, com.vwo.models.user.GetFlag>,
            fallbackContext: WingifyUserContext,
        ): FlagCollection = FlagCollection(flags.toMap(), fallbackContext)

        /**
         * Builds a collection with an explicitly disabled flag for each requested key.
         * Used on validation/execution failure so [keys]/[size]/ and [iterator] still
         * reflect the caller's requested set.
         *
         * @param flagNames Feature keys to include as disabled flags, or null/empty for an empty collection.
         * @param fallbackContext Context used to build the disabled [com.vwo.models.user.GetFlag] instances.
         */
        fun createDisabled(
            flagNames: Array<String>?,
            fallbackContext: WingifyUserContext,
        ): FlagCollection {
            val keys = flagNames?.filter { it.isNotBlank() }?.distinct().orEmpty()
            if (keys.isEmpty()) {
                return create(emptyMap(), fallbackContext)
            }
            val flags = keys.associateWith { key ->
                com.vwo.models.user.GetFlag(fallbackContext).apply { setIsEnabled(false) }
            }
            return create(flags, fallbackContext)
        }
    }
}
