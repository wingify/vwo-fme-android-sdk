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
package com.wingify.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * Per-key locks for safe read-modify-write operations during parallel evaluation.
 */
object KeyedLockUtil {
    private val locks = ConcurrentHashMap<String, Any>()

    /**
     * Returns the mutex for [key], creating one if needed.
     */
    private fun lockFor(key: String): Any =
        locks.computeIfAbsent(key) { Any() }

    /**
     * Runs [block] while holding the lock for [key].
     *
     * @param key Key whose critical section is being protected.
     * @param block Critical section to execute under the lock.
     * @return The result of [block].
     */
    fun <T> withLock(key: String, block: () -> T): T {
        synchronized(lockFor(key)) {
            return block()
        }
    }
}
