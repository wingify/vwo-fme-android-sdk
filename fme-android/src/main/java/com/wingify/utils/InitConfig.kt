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

import com.wingify.models.user.WingifyInitOptions
import com.wingify.packages.storage.MobileDefaultStorage

/**
 * Builds the `initConfig` object attached to the SDK init event.
 *
 * Property names match the Node SDK (`IVWOOptions` + `sdkName` overlay) so the
 * dashboard can read the same keys across platforms. Interface fields are sent
 * as booleans because they are not JSON-serializable.
 */
internal object InitConfig {

    fun from(options: WingifyInitOptions): Map<String, Any> {
        val initConfig = mutableMapOf<String, Any?>()
        initConfig["accountId"] = options.accountId
        initConfig["sdkKey"] = options.sdkKey
        initConfig["pollInterval"] = options.pollInterval
        initConfig["sdkName"] = SDKMetaUtil.sdkName
        initConfig["sdkVersion"] = SDKMetaUtil.sdkVersion
        initConfig["isUsageStatsDisabled"] = options.isUsageStatsDisabled
        initConfig["isAliasingEnabled"] = options.isAliasingEnabled
        initConfig["shouldTriggerIntegrationCallbackAlways"] =
            options.shouldTriggerIntegrationCallbackAlways

        if (options.cachedSettingsExpiryTime > 0) {
            initConfig["cachedSettingsExpiryTime"] = options.cachedSettingsExpiryTime
        }
        if (options.cachedDecisionExpiryTime > 0) {
            initConfig["cachedDecisionExpiryTime"] = options.cachedDecisionExpiryTime
        }
        if (options.batchMinSize > 0) {
            initConfig["batchMinSize"] = options.batchMinSize
        }
        if (options.batchUploadTimeInterval > 0) {
            initConfig["batchUploadTimeInterval"] = options.batchUploadTimeInterval
        }
        if (options.gatewayService.isNotEmpty()) {
            initConfig["gatewayService"] = options.gatewayService
        }
        if (options._vwo_meta.isNotEmpty()) {
            initConfig["_vwo_meta"] = options._vwo_meta
        }
        loggerConfig(options.logger)?.let { initConfig["logger"] = it }

        if (options.networkClientInterface != null) initConfig["network"] = true
        if (options.segmentEvaluator != null) initConfig["segmentation"] = true
        if (options.integrations != null) initConfig["integrations"] = true
        if (options.wingifyBuilder != null) initConfig["vwoBuilder"] = true
        if (options.storage != null && options.storage !is MobileDefaultStorage) {
            initConfig["storage"] = true
        }

        return initConfig.filterValues { it != null }.mapValues { it.value as Any }
    }

    private fun loggerConfig(logger: Map<String, Any>): Map<String, Any>? {
        if (logger.isEmpty()) return null
        val out = mutableMapOf<String, Any>()
        logger["level"]?.let { out["level"] = it }
        logger["prefix"]?.let { out["prefix"] = it }
        if (logger.containsKey("transport") || logger.containsKey("transports")) {
            out["transports"] = true
        }
        return out.takeIf { it.isNotEmpty() }
    }
}
