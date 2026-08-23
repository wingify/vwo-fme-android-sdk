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
package com.wingify.models

/**
 * Sampling category for error log keys used when sending `vwo_sdkDebug` events.
 *
 * - [ALWAYS_SEND]: The event is sent every time with no sampling applied.
 * - [SAMPLED]: The event is subject to the debug sampling rate from DaCDN settings.
 */
enum class ErrorLogSamplingCategory {
    /** Keys not listed for sampling; always reported to the server. */
    ALWAYS_SEND,

    /** High-volume keys that may be dropped based on the configured sampling percentage. */
    SAMPLED
}

/**
 * Manually maintained error log keys for `vwo_sdkDebug` sampling.
 *
 * Only [sampled] keys are listed explicitly. Any other key is treated as always-send.
 */
object ErrorLogSamplingKeys {

    /**
     * Error keys subject to debug-event sampling before being sent to the server.
     *
     * Targets high-volume client errors that can fire on every flag/event call.
     */
    val sampled: Set<String> = setOf(
        "EVENT_NOT_FOUND",
        "FEATURE_NOT_FOUND",
        "FEATURE_NOT_FOUND_WITH_ID",
        "INIT_OPTIONS_ERROR"
    )

    /**
     * Resolves the sampling category for a given error log key.
     *
     * Keys in [sampled] return [ErrorLogSamplingCategory.SAMPLED];
     * all other non-null keys return [ErrorLogSamplingCategory.ALWAYS_SEND].
     *
     * @param key The error message key (e.g. the `msg_t` field on debug events).
     * @return [ErrorLogSamplingCategory.ALWAYS_SEND], [ErrorLogSamplingCategory.SAMPLED],
     * or null when the key is missing.
     */
    fun category(key: String?): ErrorLogSamplingCategory? {
        if (key == null) return null
        return if (sampled.contains(key)) {
            ErrorLogSamplingCategory.SAMPLED
        } else {
            ErrorLogSamplingCategory.ALWAYS_SEND
        }
    }

    /**
     * Returns whether the given error key is subject to debug-event sampling.
     *
     * A key is sampled only when it is listed in [sampled].
     *
     * @param key The error message key from the debug event payload.
     * @return true when the key is in [sampled]; false for always-send keys or a missing key.
     */
    fun isSampled(key: String?): Boolean {
        if (key == null) return false
        return sampled.contains(key)
    }
}
