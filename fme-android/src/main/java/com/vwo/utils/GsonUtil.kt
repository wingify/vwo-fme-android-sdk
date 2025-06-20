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

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.vwo.models.request.Props
import com.vwo.models.request.PropsSerializer

/**
 * Utility object for providing a consistent Gson instance throughout the SDK.
 *
 * This object provides a shared Gson instance configured with custom serializers
 * to ensure consistent JSON serialization behavior across the entire SDK.
 */
object GsonUtil {
    
    /**
     * A shared Gson instance configured with custom serializers for VWO types.
     * This instance includes:
     * - PropsSerializer for handling additionalProperties flattening in Props objects
     * 
     * Use this instance instead of creating new Gson() instances to ensure consistent
     * serialization behavior throughout the SDK.
     */
    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Props::class.java, PropsSerializer())
        .create()
} 