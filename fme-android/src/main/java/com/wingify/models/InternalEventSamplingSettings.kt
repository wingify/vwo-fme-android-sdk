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

import com.google.gson.annotations.SerializedName

/**
 * Platform-specific sampling rates received from DaCDN settings.
 *
 * Each platform (`server`, `client`, `serverless`) has its own sampling percentage (0–100).
 * The Android SDK reads the `client` value.
 */
class PlatformSamplingRates {
    /** Sampling rate for server-side SDKs. */
    @SerializedName("server")
    var server: Double? = null

    /** Sampling rate for client SDKs (used by Android). */
    @SerializedName("client")
    var client: Double? = null

    /** Sampling rate for serverless SDKs. */
    @SerializedName("serverless")
    var serverless: Double? = null
}

/**
 * Sampling rates grouped by internal event category.
 *
 * Maps to the `sampling` object in DaCDN settings:
 * `{ usage: { server, client, serverless }, debug: { server, client, serverless } }`
 */
class EventCategorySamplingRates {
    /** Sampling rates for `vwo_sdkUsageStats`. */
    @SerializedName("usage")
    var usage: PlatformSamplingRates? = null

    /** Sampling rates for sampled `vwo_sdkDebug` error events. */
    @SerializedName("debug")
    var debug: PlatformSamplingRates? = null
}

/**
 * Flags indicating whether sampling should always be applied per platform.
 *
 * Maps to the `alwaysApplySampling` object in DaCDN settings.
 * The Android SDK reads `client`. When true, configured sampling rates are applied;
 * when false or missing, events are always sent without sampling.
 */
class AlwaysApplySampling {
    /** Whether sampling is always applied for server-side SDKs. */
    @SerializedName("server")
    var server: Boolean? = null

    /** Whether sampling is always applied for client SDKs. */
    @SerializedName("client")
    var client: Boolean? = null

    /** Whether sampling is always applied for serverless SDKs. */
    @SerializedName("serverless")
    var serverless: Boolean? = null
}
