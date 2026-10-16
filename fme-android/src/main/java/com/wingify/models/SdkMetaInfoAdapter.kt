/*
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
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

package com.wingify.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/**
 * Deserializes [SdkMetaInfo] control timestamps from numeric or string JSON values,
 * matching Node / iOS wire contracts.
 */
class SdkMetaInfoAdapter : JsonDeserializer<SdkMetaInfo> {

    /**
     * Deserializes [SdkMetaInfo] from JSON, accepting numeric or string timestamps
     * for panic and refresh to match Node / iOS wire contracts.
     *
     * @param json JSON object, or null.
     * @param typeOfT Target type (unused).
     * @param context Gson deserialization context (unused).
     * @return Parsed [SdkMetaInfo], or null when [json] is not an object.
     */
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): SdkMetaInfo? {
        if (json == null || !json.isJsonObject) return null
        val obj = json.asJsonObject
        val wasInitializedEarlier = obj.get("wasInitializedEarlier")?.takeIf { it.isJsonPrimitive }
            ?.asBoolean ?: false
        return SdkMetaInfo(
            wasInitializedEarlier = wasInitializedEarlier,
            panic = parseFlexibleLong(obj.get("panic")),
            refresh = parseFlexibleLong(obj.get("refresh")),
        )
    }
}

/** Accepts numeric or string JSON timestamps (iOS Settings decoder). */
internal class FlexibleLongAdapter : JsonDeserializer<Long> {
    /**
     * Deserializes a JSON number or numeric string into a [Long].
     *
     * @param json JSON primitive, or null.
     * @param typeOfT Target type (unused).
     * @param context Gson deserialization context (unused).
     * @return Parsed [Long], or null when missing or invalid.
     */
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): Long? = parseFlexibleLong(json)
}

/**
 * Parses a JSON number or numeric string into a [Long]; null for missing, null, or invalid values.
 *
 * @param element JSON primitive, or null.
 * @return Parsed [Long], or null when [element] is missing or not a numeric primitive.
 */
internal fun parseFlexibleLong(element: JsonElement?): Long? {
    if (element == null || element.isJsonNull || !element.isJsonPrimitive) return null
    val primitive = element.asJsonPrimitive
    if (primitive.isNumber) return primitive.asLong
    if (primitive.isString) return primitive.asString.trim().toLongOrNull()
    return null
}
