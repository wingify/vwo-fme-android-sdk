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
package com.vwo.models

import com.google.gson.annotations.SerializedName

/**
 * Represents VWO settings.
 *
 * This class serves as a container for various settings and configurations used by the VWO SDK.
 */
class Settings {
    @SerializedName("features")
    var features: List<Feature> = emptyList()

    @SerializedName("accountId")
    var accountId: Int? = null

    @SerializedName("groups")
    var groups: Map<String, Groups>? = null

    @SerializedName("campaignGroups")
    var campaignGroups: Map<String, Int>? = null

    @SerializedName("isNBv2")
    var isNBv2: Boolean = false

    @SerializedName("campaigns")
    var campaigns: List<Campaign>? = null

    @SerializedName("isNB")
    var isNB: Boolean = false

    @SerializedName("sdkKey")
    var sdkKey: String? = null

    @SerializedName("version")
    var version: Int? = null

    @SerializedName("collectionPrefix")
    var collectionPrefix: String? = null
}
