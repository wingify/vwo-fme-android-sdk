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

import android.content.Context
import com.wingify.WingifyBuilder
import com.wingify.constants.Constants
import com.wingify.interfaces.integration.IntegrationCallback
import com.wingify.interfaces.networking.NetworkClientInterface
import com.wingify.packages.segmentation_evaluator.evaluators.SegmentEvaluator
import com.wingify.packages.storage.Connector
import com.vwo.sdk.fme.BuildConfig
import com.wingify.packages.storage.MobileDefaultStorage

/**
 * Initialization options for the Wingify SDK.
 *
 * This class encapsulates various options that can be configured when initializing the SDK,
 * such as the SDK key, account ID, integrations, logger, network client, segment evaluator,
 * storage, polling interval, and gateway service.
 */
open class WingifyInitOptions {
    var sdkKey: String? = null
    var accountId: Int? = null
    var integrations: IntegrationCallback? = null
    var logger: Map<String, Any> = HashMap()
    var networkClientInterface: NetworkClientInterface? = null
    var segmentEvaluator: SegmentEvaluator? = null
    var storage: Connector? = null
    var pollInterval: Int? = null
    var wingifyBuilder: WingifyBuilder? = null
    var isAliasingEnabled: Boolean = false

    var gatewayService: Map<String, Any> = HashMap()

    /**
     * Optional: Even though context is optional, it is required if you want to use features like
     * cached settings, offline batch upload, device ID generation, storage.
     */
    var context: Context? = null

    /**
     * Optional: If this value is provided, SDK will keep using cached settings till this interval
     * is valid.
     */
    var cachedSettingsExpiryTime: Int = 0

    /**
     * Maximum time (in milliseconds) for which a stored GetFlag decision remains valid.
     *
     * When a positive value is set, any cached decision older than this duration is treated
     * as expired and re-evaluated on the next GetFlag call. A value of `0` (the default)
     * means decisions never expire and remain valid indefinitely.
     */
    var cachedDecisionExpiryTime: Int = 0

    /**
     * The name of the SDK.
     *
     * This is used to identifying the SDK.
     *
     * @param sdkName The name of the SDK. **For hybrid SDKs only.**
     */
    var sdkName = Constants.SDK_NAME

    /**
     * The version of the SDK.
     *
     * This is used for identifying the SDK version.
     *
     * @param sdkVersion The version of the SDK. **For hybrid SDKs only.**
     */
    var sdkVersion = BuildConfig.SDK_VERSION

    /** Optional: Minimum size of Batch to upload */
    var batchMinSize = -1

    /** Optional: Batch upload time interval in milliseconds. Please specify at least few minutes */
    var batchUploadTimeInterval: Long = -1L

    /** Optional: Usage stats should always be collected, don't collect if `isUsageStatsDisabled` flag is true */
    var isUsageStatsDisabled = false

    /** Internal meta data for VWO use. */
    var _vwo_meta: Map<String, Any> = emptyMap()

    private var isWingifySDK = true

    internal val isWingifySDKActive: Boolean
        get() = isWingifySDK

    internal fun markAsLegacyVwoSdk() {
        isWingifySDK = false
    }

    init {
        if (storage == null) {
            storage = MobileDefaultStorage(this)
        }
    }
}
