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

import com.vwo.enums.EventEnum
import com.vwo.utils.NetworkUtil.Companion.getEventsBaseProperties
import com.vwo.utils.NetworkUtil.Companion.getDebuggerEventPayload
import com.vwo.utils.NetworkUtil.Companion.sendMessagingEvent

/**
 * Utility functions for handling debugger service operations including
 * filtering sensitive properties and extracting decision keys.
 */

/**
 * Extracts only the required fields from a decision object.
 * @param decisionObj The decision object to extract fields from
 * @return A map containing only rolloutKey and experimentKey if they exist
 */
fun extractDecisionKeys(decisionObj: Map<String, Any> = emptyMap()): Map<String, Any> {
    val extractedKeys = mutableMapOf<String, Any>()

    // Extract rolloutKey if present
    decisionObj["rolloutId"]?.let { rolloutId ->
        extractedKeys["rId"] = rolloutId
    }

    decisionObj["rolloutVariationId"]?.let { rolloutVariationId ->
        extractedKeys["rvId"] = rolloutVariationId
    }

    // Extract experimentKey if present
    decisionObj["experimentId"]?.let { experimentId ->
        extractedKeys["eId"] = experimentId
    }

    // Extract experimentVariationId if present
    decisionObj["experimentVariationId"]?.let { experimentVariationId ->
        extractedKeys["evId"] = experimentVariationId
    }

    return extractedKeys
}

/**
 * Sends a debug event to VWO.
 * @param eventProps The properties for the event.
 */
fun sendDebugEventToVWO(eventProps: Map<String, Any> = emptyMap()) {
    try {
        // create query parameters
        val properties = getEventsBaseProperties(EventEnum.VWO_DEBUGGER_EVENT.value, null, null)

        // create payload
        val payload = getDebuggerEventPayload(eventProps)

        // send event
        sendMessagingEvent(properties, payload, EventEnum.VWO_DEBUGGER_EVENT.value)
    } catch (e: Exception) {
        // Silently catch any exceptions to prevent disrupting normal flow
    }
}
