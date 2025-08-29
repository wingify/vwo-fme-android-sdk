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
package com.vwo.models.request

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Custom Gson serializer and deserializer for Props class
 * to handle additionalProperties flattening behavior similar to Jackson's @JsonAnyGetter/@JsonAnySetter
 */
class PropsSerializer : JsonSerializer<Props>, JsonDeserializer<Props> {

    override fun serialize(src: Props, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        
        // Manually serialize each known field using reflection or accessors
        // Note: Since fields are private, we need to use reflection or public methods
        
        // For now, let's manually serialize the public/accessible fields
        src.variation?.let { jsonObject.addProperty("variation", it) }
        src.id?.let { jsonObject.addProperty("id", it) }
        
        // Use reflection to access private fields with SerializedName
        val fields = src.javaClass.declaredFields
        for (field in fields) {
            field.isAccessible = true
            
            // Skip the additionalProperties field explicitly by name
            if (field.name == "additionalProperties") {
                continue
            }
            
            val serializedName = field.getAnnotation(com.google.gson.annotations.SerializedName::class.java)
            val fieldName = serializedName?.value ?: field.name
            
            val value = field.get(src)
            if (value != null) {
                jsonObject.add(fieldName, context.serialize(value))
            }
        }
        
        // Add additional properties at root level (flattening behavior)
        val additionalProperties = src.getAdditionalProperties()
        for ((key, value) in additionalProperties) {
            jsonObject.add(key, context.serialize(value))
        }
        
        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Props {
        val jsonObject = json.asJsonObject
        val props = Props()
        
        // Set known properties
        jsonObject.get("vwo_sdkName")?.let { 
            if (!it.isJsonNull) props.setSdkName(it.asString)
        }
        jsonObject.get("vwo_sdkVersion")?.let { 
            if (!it.isJsonNull) props.setSdkVersion(it.asString)
        }
        jsonObject.get("vwo_envKey")?.let { 
            if (!it.isJsonNull) props.setEnvKey(it.asString)
        }
        jsonObject.get("variation")?.let { 
            if (!it.isJsonNull) props.variation = it.asString
        }
        jsonObject.get("id")?.let { 
            if (!it.isJsonNull) props.id = it.asInt
        }
        jsonObject.get("isFirst")?.let { 
            if (!it.isJsonNull) props.setFirst(it.asInt)
        }
        jsonObject.get("isMII")?.let { 
            if (!it.isJsonNull) props.setIsMII(it.asBoolean)
        }
        jsonObject.get("isCustomEvent")?.let { 
            if (!it.isJsonNull) props.setCustomEvent(it.asBoolean)
        }
        jsonObject.get("product")?.let { 
            if (!it.isJsonNull) props.setProduct(it.asString)
        }
        jsonObject.get("data")?.let { 
            if (!it.isJsonNull) {
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                props.setData(context.deserialize(it, mapType))
            }
        }
        jsonObject.get("vwoMeta")?.let { 
            if (!it.isJsonNull) {
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                props.setVwoMeta(context.deserialize(it, mapType))
            }
        }

        // Collect unknown properties into additionalProperties
        val knownKeys = setOf("vwo_sdkName", "vwo_sdkVersion", "vwo_envKey", "variation", "id", 
                             "isFirst", "isMII", "isCustomEvent", "product", "data", "vwoMeta")
        val additionalProperties = mutableMapOf<String, Any>()
        
        for ((key, value) in jsonObject.entrySet()) {
            if (key !in knownKeys) {
                additionalProperties[key] = context.deserialize<Any>(value, Any::class.java)
            }
        }
        
        if (additionalProperties.isNotEmpty()) {
            props.setAdditionalProperties(additionalProperties)
        }
        
        return props
    }
} 