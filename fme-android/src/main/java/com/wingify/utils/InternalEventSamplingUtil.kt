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

import com.wingify.constants.Constants
import com.wingify.models.Settings
import kotlin.random.Random

/**
 * Shared helpers for sampling internal SDK events based on DaCDN settings.
 *
 * Used by `vwo_sdkUsageStats` and sampled `vwo_sdkDebug` error events to decide
 * whether an event should be sent to the server.
 */
object InternalEventSamplingUtil {

    /** Default sampling rate (1%) applied when DaCDN does not provide a value. */
    private const val DEFAULT_SAMPLING_RATE: Double = 1.0

    /**
     * Returns whether an event qualifies to be sent for the given sampling percentage.
     *
     * Generates a random integer between 0 and 100 (inclusive). The event qualifies when
     * the random value is less than or equal to the configured sampling rate.
     * For example, at 20% sampling, values 0–20 qualify and 21–100 do not.
     *
     * @param samplingPercentage The sampling rate from DaCDN settings (0–100).
     * @param randomValue Optional fixed value for testing. When null, a random value is generated.
     * @return true if the event should be sent; false otherwise.
     */
    @JvmStatic
    @JvmOverloads
    fun doesQualifyForSampling(samplingPercentage: Double, randomValue: Int? = null): Boolean {
        val clampedRate = samplingPercentage.coerceIn(0.0, Constants.MAX_TRAFFIC_PERCENT.toDouble())
        if (clampedRate >= Constants.MAX_TRAFFIC_PERCENT.toDouble()) {
            return true
        }
        if (clampedRate <= 0) {
            return false
        }

        val value = randomValue ?: Random.nextInt(0, Constants.MAX_TRAFFIC_PERCENT + 1)
        return value <= clampedRate.toInt()
    }

    /**
     * Returns the client usage-stats sampling rate from DaCDN settings.
     *
     * Reads `sampling.usage.client` for the Android SDK.
     *
     * @param settings The processed account settings, if available.
     * @return The sampling percentage (0–100). Defaults to 1 when not provided.
     */
    @JvmStatic
    fun usageStatsSamplingRate(settings: Settings?): Double {
        return settings?.sampling?.usage?.client ?: DEFAULT_SAMPLING_RATE
    }

    /**
     * Returns whether sampling should be applied for the Android (client) SDK.
     *
     * Reads `alwaysApplySampling.client` from DaCDN settings.
     * When false or missing, events are always sent without sampling.
     *
     * @param settings The processed account settings, if available.
     * @return true when sampling must be applied; false to always send.
     */
    @JvmStatic
    fun shouldAlwaysApplySampling(settings: Settings?): Boolean {
        return settings?.alwaysApplySampling?.client ?: false
    }

    /**
     * Returns whether `vwo_sdkUsageStats` qualifies to be sent based on configured sampling.
     *
     * When `alwaysApplySampling.client` is false or missing, the event is always sent.
     * When true, the event is subject to `sampling.usage.client`.
     *
     * @param settings The processed account settings used to read the sampling rate.
     * @param randomValue Optional fixed value for testing. When null, a random value is generated.
     * @return true if the usage-stats event qualifies to be sent; false otherwise.
     */
    @JvmStatic
    @JvmOverloads
    fun isUsageStatsEventQualified(settings: Settings?, randomValue: Int? = null): Boolean {
        if (!shouldAlwaysApplySampling(settings)) {
            return true
        }
        return doesQualifyForSampling(
            samplingPercentage = usageStatsSamplingRate(settings),
            randomValue = randomValue
        )
    }

    /**
     * Returns the client debug-event sampling rate from DaCDN settings.
     *
     * Reads `sampling.debug.client` for the Android SDK.
     *
     * @param settings The processed account settings, if available.
     * @return The sampling percentage (0–100). Defaults to 1 when not provided.
     */
    @JvmStatic
    fun debugSamplingRate(settings: Settings?): Double {
        return settings?.sampling?.debug?.client ?: DEFAULT_SAMPLING_RATE
    }

    /**
     * Returns whether a sampled `vwo_sdkDebug` event qualifies to be sent.
     *
     * Only called for debug events whose `msg_t` is in [com.wingify.models.ErrorLogSamplingKeys.sampled].
     * When `alwaysApplySampling.client` is false or missing, the event is always sent.
     * When true, the event is subject to `sampling.debug.client`.
     *
     * @param settings The account settings used to read the debug sampling rate.
     * @param randomValue Optional fixed value for testing. When null, a random value is generated.
     * @return true if the debug event qualifies to be sent; false otherwise.
     */
    @JvmStatic
    @JvmOverloads
    fun isSampledDebugEventQualified(settings: Settings?, randomValue: Int? = null): Boolean {
        if (!shouldAlwaysApplySampling(settings)) {
            return true
        }
        return doesQualifyForSampling(
            samplingPercentage = debugSamplingRate(settings),
            randomValue = randomValue
        )
    }
}
