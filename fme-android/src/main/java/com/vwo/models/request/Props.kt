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

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a collection of properties associated with an event or entity.
 *
 * This class is used to store various properties, includingSDK information,
 * variation details, custom event flags, and additional dynamic properties.
 */
class Props {
    @JsonProperty("vwo_sdkName")
    private var vwo_sdkName: String? = null

    @JsonProperty("vwo_sdkVersion")
    private var vwo_sdkVersion: String? = null

    @JsonProperty("vwo_envKey")
    private var vwo_envKey: String? = null
    var variation: String? = null
    var id: Int? = null

    @JsonProperty("isFirst")
    private var isFirst: Int? = null

    @JsonProperty("isMII")
    private var isMII: Boolean = false

    @JsonProperty("isCustomEvent")
    private var isCustomEvent: Boolean? = null

    @JsonProperty("product")
    private var product: String? = null

    @JsonProperty("data")
    private var data: Map<String, Any>? = null

    @JsonProperty("vwoMeta")
    private var vwoMeta: Map<String, Any>? = null

    @JsonIgnore
    private var additionalProperties: Map<String, Any> = HashMap()

    @JsonProperty("vwo_sdkName")
    fun setSdkName(sdkName: String?) {
        this.vwo_sdkName = sdkName
    }

    @JsonProperty("vwo_sdkVersion")
    fun setSdkVersion(sdkVersion: String?) {
        this.vwo_sdkVersion = sdkVersion
    }

    fun setIsFirst(isFirst: Int?) {
        this.isFirst = isFirst
    }

    fun setIsMii(isMii: Boolean) {
        this.isMII = isMii
    }

    fun setIsCustomEvent(isCustomEvent: Boolean?) {
        this.isCustomEvent = isCustomEvent
    }

    @JsonProperty("vwo_envKey")
    fun setEnvKey(vwo_envKey: String?) {
        this.vwo_envKey = vwo_envKey
    }

    @JsonAnyGetter
    fun getAdditionalProperties(): Map<String, *> {
        return additionalProperties
    }

    @JsonAnySetter
    fun setAdditionalProperties(additionalProperties: Map<String, Any>) {
        this.additionalProperties = additionalProperties
    }

    fun setProduct(product: String?) {
        this.product = product
    }

    fun setData(data: Map<String, Any>?) {
        this.data = data
    }

    fun setUsageStats(stats: Map<String, Any>) {
        this.vwoMeta = stats
    }
}
