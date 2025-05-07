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
package com.vwo.models.user

import android.content.Context
import com.vwo.VWOBuilder
import com.vwo.constants.Constants
import com.vwo.interfaces.networking.NetworkClientInterface
import com.vwo.packages.segmentation_evaluator.evaluators.SegmentEvaluator
import com.vwo.packages.storage.Connector
import com.vwo.interfaces.integration.IntegrationCallback
import com.vwo.packages.storage.MobileDefaultStorage
import com.vwo.sdk.fme.BuildConfig

/**
 * Represents initialization options for the VWO SDK.
 *
 * This class encapsulates various options that can be configured when initializing theVWO SDK,
 * such as the SDK key, account ID, integrations, logger, network client, segment evaluator,
 * storage, polling interval, and gateway service.
 */
class VWOInitOptions {
    var sdkKey: String? = null
    var accountId: Int? = null
    var integrations: IntegrationCallback? = null
    var logger: Map<String, Any> = HashMap()
    var networkClientInterface: NetworkClientInterface? = null
    var segmentEvaluator: SegmentEvaluator? = null
    var storage: Connector = MobileDefaultStorage()
    var pollInterval: Int? = null
    var vwoBuilder: VWOBuilder? = null

    var gatewayService: Map<String, Any> = HashMap()

    /** Optional: Even though context is optional, it is required if you want to use features like cached settings, offline batch upload, etc.*/
    var context: Context? = null

    /** Optional: If this value is provided, SDK will keep using cached settings till this interval is valid.*/
    var cachedSettingsExpiryTime: Int = 0

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

    /** Optional: Minimum size of Batch to upload*/
    var batchMinSize = -1

    /** Optional: Batch upload time interval in milliseconds. Please specify at least few minutes*/
    var batchUploadTimeInterval: Long = -1L

    /**Optional: Usage stats should always be collected, don’t collect if `isUsageStatsDisabled` flag is true*/
    var isUsageStatsDisabled = false

    /**Internal meta data for VWO use.*/
    var _vwo_meta: Map<String, Any> = emptyMap()
}