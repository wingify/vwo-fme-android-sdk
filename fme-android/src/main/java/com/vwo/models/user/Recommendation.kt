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

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vwo.api.TrackEventAPI.createAndSendImpressionForTrack
import com.vwo.enums.EventEnum
import com.vwo.interfaces.IVwoListener
import com.vwo.models.RecommendedProduct
import com.vwo.utils.RecommendationUtil.getRecommendation
import com.vwo.utils.SettingsUtil
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread


class Recommendation(val recommendationBlock: Int, private val context: VWOUserContext) {
    /**
     * This method is used to get recommendations based on the options passed.
     *
     * @param options Map<String, Object> containing the options to be passed to the recommendation API
     * @param listener IVwoListener interface to handle the success and failure of the recommendation API
     *
     * @return JSONObject containing the recommendations and the recommendationBlock
     */
    fun getRecommendations(options: Map<String, Any>, category:String?, productIds:List<Int>?, listener:IVwoListener) {
        thread(start = true) {
            try {
                val eventType = options["pageType"] as? String ?: ""
                val recommendations = getRecommendation(
                    recommendationBlock,
                    context.id!!,
                    eventType,
                    category,
                    productIds
                )
                // Add configId to the recommendations
                recommendations.put("recommendationBlock", recommendationBlock)

                var results = recommendations.optJSONArray("results")
                // if recommendations is an empty object or if the results array is empty, then return the recommendations as is
                if (results == null) {
                    recommendations.put("results", JSONArray())
                }

                // Send recommendationShown event
                results = recommendations.getJSONArray("results")
                if (results?.length() != 0) {
                    SettingsUtil.settingsFile?.let {

                        val inputProducts = (options["productIds"] as? String) ?: ""
                        val data = mapOf<String, Any>(
                            "recommendation_block" to recommendationBlock.toString(),
                            "input_products" to inputProducts,
                            "recommended_products" to getRecommendedProductIds(results),
                            "page_type" to (options["pageType"] as? String ?: "")
                        )

                        val eventName = EventEnum.VWO_RECOMMENDATION_SHOWN.value
                        createAndSendImpressionForTrack(it, eventName, context, data)
                    }
                }

                listener.onSuccess(recommendations)
            } catch (e: Exception) {
                listener.onFailure(e.message ?: e.toString())
            }
            return@thread
        }
    }

    private fun getRecommendedProductIds(results: JSONArray?): String {
        val resultString = results.toString()
        val listType = object : TypeToken<List<RecommendedProduct>>() {}.type
        val products:List<RecommendedProduct> = Gson().fromJson(resultString, listType)
        return products.joinToString(separator = ",") { it.id }
    }

    fun getRecommendationWidget(featureFlag: GetFlag, overrideVariables:Map<String, Any>, callback:IVwoListener){
        try {
            val displayConfig = featureFlag.getRecommendationDisplayConfig("recommendationBlock")
            if (displayConfig.isNullOrEmpty()) {
                callback.onFailure("Invalid displayConfig")
                return
            }
            val result = JSONObject(displayConfig)
            for ((key, value) in overrideVariables) {
                if (result.has(key)) {
                    result.put(key, value)
                }
            }
            callback.onSuccess(result)
        } catch (e:Exception) {
            callback.onFailure(e.message?:e.toString())
        }
    }

    override fun toString(): String {
        return recommendationBlock.toString()
    }
}
