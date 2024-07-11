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
package com.vwo.packages.segmentation_evaluator.core

import com.fasterxml.jackson.databind.JsonNode
import com.vwo.constants.Constants
import com.vwo.models.Feature
import com.vwo.models.Settings
import com.vwo.packages.logger.enums.LogLevelEnum

class SegmentationManager {
    private var evaluator: SegmentEvaluator? = null

    fun attachEvaluator(segmentEvaluator: SegmentEvaluator?) {
        this.evaluator = segmentEvaluator
    }

    fun attachEvaluator() {
        this.evaluator = SegmentEvaluator()
    }

    /**
     * This method sets the contextual data required for segmentation.
     * @param settings  SettingsModel object containing the account settings.
     * @param feature   FeatureModel object containing the feature settings.
     * @param context   VWOContext object containing the user context.
     */
    fun setContextualData(settings: Settings, feature: Feature, context: VWOContext) {
        this.attachEvaluator()
        evaluator.context = context
        evaluator.settings = settings
        evaluator.feature = feature

        // If gateway service is required and the base URL is not the default one, fetch the data from the gateway service
        if (feature.getIsGatewayServiceRequired() && !UrlService.getBaseUrl()
                .contains(Constants.HOST_NAME) && (context.vwo == null)
        ) {
            val queryParams: MutableMap<String, String> = HashMap()
            if ((context.userAgent == null || context.userAgent.isEmpty()) && (context.ipAddress == null || context.ipAddress.isEmpty())) {
                return
            }
            if (context.userAgent != null) {
                queryParams["userAgent"] = context.userAgent
            }

            if (context.ipAddress != null) {
                queryParams["ipAddress"] = context.ipAddress
            }

            try {
                val params: Map<String, String> = GatewayServiceUtil.getQueryParams(queryParams)
                val _vwo: String =
                    GatewayServiceUtil.getFromGatewayService(params, UrlEnum.GET_USER_DATA.url)
                val gatewayServiceModel: GatewayService =
                    VWOClient.objectMapper.readValue(_vwo, GatewayService::class.java)
                context.vwo = gatewayServiceModel
            } catch (err: Exception) {
                LoggerService.log(
                    LogLevelEnum.ERROR,
                    "Error in setting contextual data for segmentation. Got error: $err"
                )
            }
        }
    }

    /**
     * This method validates the segmentation for the given DSL and properties.
     * @param dsl     Object containing the segmentation DSL.
     * @param properties  Map containing the properties required for segmentation.
     * @return  Boolean value indicating whether the segmentation is valid or not.
     */
    fun validateSegmentation(dsl: Any, properties: Map<String?, Any?>?): Boolean {
        try {
            val dslNodes: JsonNode = if (dsl is String) VWOClient.objectMapper.readValue(
                dsl.toString(),
                JsonNode::class.java
            ) else VWOClient.objectMapper.valueToTree(dsl)
            return evaluator.isSegmentationValid(dslNodes, properties)
        } catch (exception: Exception) {
            LoggerService.log(
                LogLevelEnum.ERROR,
                "Exception occurred validate segmentation " + exception.message
            )
            return false
        }
    }

    companion object {
        @JvmStatic
        var instance: SegmentationManager? = null
            get() {
                if (field == null) {
                    field = SegmentationManager()
                }
                return field
            }
            private set
    }
}
