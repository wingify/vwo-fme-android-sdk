/**
 * Copyright 2024 Wingify Software Pvt. Ltd.
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

import com.vwo.VWOBuilder
import com.vwo.interfaces.networking.NetworkClientInterface
import com.vwo.packages.segmentation_evaluator.evaluators.SegmentEvaluator
import com.vwo.packages.storage.Connector
import com.vwo.interfaces.integration.IntegrationCallback

/**
 * Represents initialization options for the VWO SDK.
 *
 * This class encapsulates various options that can be configured when initializing theVWO SDK,
 * such as the SDK key, account ID, integrations, logger, network client, segment evaluator,
 * storage, polling interval, and gateway service.
 */
class VWOInitOptions {
    var sdkKey: String? = null
    var accountId: Int?=null
    var integrations: IntegrationCallback? = null
    var logger: Map<String, Any> = HashMap()
    var networkClientInterface: NetworkClientInterface? = null
    var segmentEvaluator: SegmentEvaluator? = null
    var storage: Connector? = null
    var pollInterval: Int? = null

    var vwoBuilder: VWOBuilder? = null

    var gatewayService: Map<String, Any> = HashMap()
}
