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
package com.wingify.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

/**
 * Represents stored data for a VWO user.
 *
 * This class encapsulates information about a VWO user's assigned variations and rollout
 * information, which is typically persisted in storage.
 */
class Storage {
    var featureKey: String? = null
    var user: String? = null
    var rolloutId: Int? = null
    var rolloutKey: String? = null
    var rolloutVariationId: Int? = null
    var experimentId: Int? = null
    var experimentKey: String? = null
    var experimentVariationId: Int? = null

    var customVariables: Map<String, Any>? = null
    var variationTargetingVariables: Map<String, Any>? = null

    @JsonAdapter(JsonArrayWrapper.Adapter::class)
    var holdoutIds: JsonArrayWrapper? = null

    @JsonAdapter(JsonArrayWrapper.Adapter::class)
    var notInHoldoutIds: JsonArrayWrapper? = null

    /**
     * Holds a list of integer IDs that MobileDefaultStorage persists as a raw JSON array
     * (e.g. `[1,2]`), while older / intermediate forms may use `{"values":[1,2]}`.
     */
    class JsonArrayWrapper {
        var values: List<Int>? = null

        class Adapter : JsonDeserializer<JsonArrayWrapper>, JsonSerializer<JsonArrayWrapper> {
            override fun deserialize(
                json: JsonElement?,
                typeOfT: Type?,
                context: JsonDeserializationContext?,
            ): JsonArrayWrapper {
                val wrapper = JsonArrayWrapper()
                if (json == null || json.isJsonNull) {
                    return wrapper
                }
                wrapper.values = when {
                    json.isJsonArray -> json.asJsonArray.mapNotNull { element ->
                        when {
                            element.isJsonNull -> null
                            element.isJsonPrimitive -> element.asInt
                            else -> null
                        }
                    }
                    json.isJsonObject -> {
                        // Prefer {"values":[...]} (legacy wrapper). Also accept
                        // {"myArrayList":[...]} which Gson emitted historically when
                        // org.json.JSONArray leaked into the serialization map.
                        val obj = json.asJsonObject
                        val valuesElement = when {
                            obj.has("values") -> obj.get("values")
                            obj.has("myArrayList") -> obj.get("myArrayList")
                            else -> null
                        }
                        if (valuesElement == null || valuesElement.isJsonNull) {
                            null
                        } else if (valuesElement.isJsonArray) {
                            valuesElement.asJsonArray.mapNotNull { element ->
                                when {
                                    element.isJsonNull -> null
                                    element.isJsonPrimitive -> element.asInt
                                    else -> null
                                }
                            }
                        } else {
                            null
                        }
                    }
                    else -> null
                }
                return wrapper
            }

            override fun serialize(
                src: JsonArrayWrapper?,
                typeOfSrc: Type?,
                context: JsonSerializationContext?,
            ): JsonElement {
                if (src == null) return JsonNull.INSTANCE
                // Persist as a raw array to match MobileDefaultStorage write format.
                return context?.serialize(src.values ?: emptyList<Int>())
                    ?: JsonNull.INSTANCE
            }
        }
    }

    var decisionExpiryTime: Long? = null

    /**
     * Checks whether this stored decision has expired.
     *
     * A decision is considered expired when its [decisionExpiryTime] is a positive timestamp
     * that falls before the current time. If the expiry is null or non-positive, the decision
     * is treated as valid indefinitely (for backward compatibility).
     *
     * @return `true` if the decision has expired, `false` otherwise.
     */
    fun isDecisionExpired(): Boolean {
        val expiry = decisionExpiryTime ?: return false
        if (expiry <= 0L) return false
        return System.currentTimeMillis() > expiry
    }

    fun isDecisionExpiryTimeFoundInStorage(): Boolean {
        return decisionExpiryTime != null
    }

}
