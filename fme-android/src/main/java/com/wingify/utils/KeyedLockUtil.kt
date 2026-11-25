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
     * Lock keys the calling thread currently holds.
     *
     * Thread-local so a nested acquire on the same thread can re-enter [withLock].
     * Mutated only while the corresponding monitor is held.
     */
    @PublishedApi
    internal val heldLockKeys = ThreadLocal.withInitial { mutableSetOf<String>() }

    /**
     * Returns the mutex for [key], creating one if needed.
     */
    @PublishedApi
    internal fun lockFor(key: String): Any =
        locks.computeIfAbsent(key) { Any() }

    /**
     * Whether the calling thread already holds the lock for [key].
     *
     * Other threads always see false for keys they do not hold.
     *
     * @param key Lock key to check.
     * @return True when this thread acquired [key] and has not released it.
     */
    @PublishedApi
    internal fun isLockHeldByCurrentThread(key: String): Boolean =
        heldLockKeys.get().contains(key)

    /**
     * Builds a multi-account-safe lock key aligned with MobileDefaultStorage identity.
     */
    fun flagLockKey(accountId: Int, sdkKey: String, featureKey: String, userId: String?): String =
        "$accountId|$sdkKey|$featureKey|${userId.orEmpty()}"

    /**
     * Runs [block] while holding the lock for [key].
     *
     * Reentrant on the calling thread: if [key] is already held, [block] runs without taking the
     * monitor again. A different key on the same thread nests. Callers that take more than one key
     * must acquire them in one consistent order, or two threads can deadlock.
     *
     * @param key Key whose critical section is being protected.
     * @param block Critical section to execute under the lock.
     * @return The result of [block].
     */
    inline fun <T> withLock(key: String, block: () -> T): T {
        if (isLockHeldByCurrentThread(key)) {
            return block()
        }
        synchronized(lockFor(key)) {
            heldLockKeys.get().add(key)
            try {
                return block()
            } finally {
                heldLockKeys.get().remove(key)
            }
        }
    }
}
