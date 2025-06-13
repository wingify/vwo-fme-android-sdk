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

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

/**
 * Compatibility layer for Jackson to Gson migration
 * Provides type aliases and compatibility functions
 */

// Type aliases for Jackson types
typealias JsonNode = JsonElement
typealias ObjectNode = JsonObject
typealias ArrayNode = JsonArray

// Exception compatibility
class JsonProcessingException(message: String, cause: Throwable? = null) : Exception(message, cause)

// Extension functions to provide Jackson-like API for Gson JsonElement
val JsonElement.isArray: Boolean
    get() = this.isJsonArray

val JsonElement.isObject: Boolean
    get() = this.isJsonObject

val JsonElement.isTextual: Boolean
    get() = this.isJsonPrimitive && this.asJsonPrimitive.isString

fun JsonElement.asText(): String {
    return if (this.isJsonPrimitive) {
        this.asJsonPrimitive.asString
    } else {
        this.toString()
    }
}

fun JsonElement.size(): Int {
    return when {
        this.isJsonArray -> this.asJsonArray.size()
        this.isJsonObject -> this.asJsonObject.size()
        else -> 0
    }
}

fun JsonElement.has(memberName: String): Boolean {
    return if (this.isJsonObject) {
        this.asJsonObject.has(memberName)
    } else {
        false
    }
}

fun JsonElement.get(memberName: String): JsonElement? {
    return if (this.isJsonObject) {
        this.asJsonObject.get(memberName)
    } else {
        null
    }
}

fun JsonElement.fieldNames(): Iterator<String> {
    return if (this.isJsonObject) {
        this.asJsonObject.keySet().iterator()
    } else {
        emptyList<String>().iterator()
    }
}

fun JsonElement.fields(): Iterator<Map.Entry<String, JsonElement>> {
    return if (this.isJsonObject) {
        this.asJsonObject.entrySet().iterator()
    } else {
        emptyMap<String, JsonElement>().entries.iterator()
    }
}

// Iterator support for JsonArray
operator fun JsonArray.iterator(): Iterator<JsonElement> {
    return this.asList().iterator()
}

// Iterator support for JsonElement (for array-like iteration)
operator fun JsonElement.iterator(): Iterator<JsonElement> {
    return if (this.isJsonArray) {
        this.asJsonArray.iterator()
    } else {
        emptyList<JsonElement>().iterator()
    }
} 