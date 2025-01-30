/*
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vwo.utils

import com.vwo.enums.UrlEnum
import com.vwo.models.user.VWOContext
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.services.SettingsManager.Companion.instance
import com.vwo.services.UrlService.baseUrl

/**
 * Utility class for handling events related operations, such as pre-segmentation.
 *
 * This class provides methods for evaluating DSLs against user context to determine if events
 * should be tracked before segmentation. It also includes helper functions for validating
 * event data and other related tasks.
 */
class EventsUtils {

    /**
     * Evaluates the given DSL against the provided context to determine if the event
     * should be tracked before segmentation.
     *
     * This function sends a POST request to the VWO server with the DSL and context
     * information. It returns `true` if the server responds with a status code of 200
     * and the response data contains "true" (case-insensitive), indicating that the
     * event should be tracked. Otherwise, it returns `false`.
     *
     * @param dsl The DSL to evaluate.
     * @param context The VWO context containing user information.
     * @return `true` if the event should be tracked before segmentation, `false` otherwise.
     */
    fun getEventsPreSegmentation(dsl: Any, context: VWOContext): Boolean {
        var result = false
        val queryParams: MutableMap<String, String> = HashMap()
        val accountId = instance?.accountId.toString()
        queryParams["uuid"] = UUIDUtils.getUUID(context.id, accountId)
        queryParams["accountId"] = accountId

        val body: MutableMap<String, Any> = LinkedHashMap()
        body["dsl"] = dsl
        val contextBody = mutableMapOf<String, Any>()
        contextBody["id"] = context.id ?: ""
        contextBody["customVariables"] = context.customVariables
        contextBody["ipAddress"] = context.ipAddress
        contextBody["userAgent"] = context.userAgent
        contextBody["variationTargetingVariables"] = context.variationTargetingVariables
        body["context"] = contextBody

        NetworkManager.attachClient()
        val request = RequestModel(
            baseUrl,
            "POST",
            UrlEnum.EVALUATE_DSL.url,
            queryParams,
            body,
            HashMap(),
            instance?.protocol,
            instance?.port ?: 0
        )
        val responseModel = NetworkManager.post(request)
        if (responseModel?.statusCode == 200) {
            if (responseModel.data?.contains("true", ignoreCase = true) == true) {
                result = true
            }
        }
        return result
    }
}