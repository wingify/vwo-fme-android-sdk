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
package com.vwo.utils

import com.vwo.constants.Constants
import com.vwo.constants.Constants.AUTH_TOKEN
import com.vwo.enums.UrlEnum
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.services.LoggerService.Companion.log
import com.vwo.services.SettingsManager.Companion.instance
import org.json.JSONObject


object RecommendationUtil {

    var request :RequestModel?=null
    /**
     * This method is used to get recommendations based on the options passed.
     * @param configId The configId for which recommendation is to be fetched
     * @return The recommendations
     */
    fun getRecommendation(
        configId: Int,
        userId: String,
        eventType: String,
        category: String?,
        productIds: List<Int>?
    ): JSONObject {
        try {
            // set the query parameters
            val queryParams: MutableMap<String, String> = HashMap()
            queryParams["accountId"] = instance?.accountId.toString()

            // create payload for the request
            val array = ArrayList<Map<String, Any>>()
            productIds?.forEach {
                val productMap: MutableMap<String, Any> = HashMap()
                productMap["id"] = it
                array.add(productMap)
            }

            val payload: MutableMap<String, Any> = LinkedHashMap()
            payload["configId"] = configId.toString()
            payload["eventType"] = eventType
            payload["products"] = array
            payload["visitorId"] = userId
            if (productIds != null && category != null) {
                val variables: MutableMap<String, Any> = LinkedHashMap()
                variables["product_id"] = productIds[0]
                variables["category"] = category
                payload["variables"] = variables
            }

            NetworkManager.attachClient()
            val header = mutableMapOf("authToken" to AUTH_TOKEN)
            val request = RequestModel(
                Constants.VWO_APP_URL,
                "POST",
                UrlEnum.GENERATE_RECOMMENDATION.url,
                queryParams,
                payload,
                header,
                instance?.protocol,
                0
            )
            val map = mapOf<String?, String?>(
                "accountId" to instance?.accountId.toString(),
                "userId" to userId
            )
            log(LogLevelEnum.DEBUG, "IMPRESSION_FOR_GET_RECOMMENDATION", map)
            val responseModel = NetworkManager.post(request)
            val jsonObject = if (responseModel?.statusCode == 200) {
                JSONObject(responseModel.data?:"{}")
            } else {
                val map = mutableMapOf<String?, String?>()
                map["err"]= responseModel?.error.toString()
                log(LogLevelEnum.ERROR, "RECOMMENDATION_ERROR",map)
                JSONObject()
            }
            return jsonObject
        } catch (e: Exception) {
            val map = mapOf<String?,String?>("err" to e.message)
            log(LogLevelEnum.ERROR, "RECOMMENDATION_ERROR", map)
            return JSONObject()
        }
    }
}
