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
package com.vwo.testcases

import com.google.gson.annotations.SerializedName
import com.vwo.models.Storage

class Expectation {
    @SerializedName("isEnabled")
    var isEnabled: Boolean? = null
    @SerializedName("intVariable")
    var intVariable: Int? = null
    @SerializedName("stringVariable")
    var stringVariable: String? = null
    @SerializedName("floatVariable")
    var floatVariable: Double? = null
    @SerializedName("booleanVariable")
    var booleanVariable: Boolean? = null
    @SerializedName("jsonVariable")
    var jsonVariable: Map<String, Any>? = null
    @SerializedName("storageData")
    var storageData: Storage? = null

    var shouldReturnSameVariation: Boolean? = null
}
