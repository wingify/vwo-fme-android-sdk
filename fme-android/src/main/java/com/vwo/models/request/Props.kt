/**
 * Copyright 2024 Wingify Software Pvt. Ltd.
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

    @JsonProperty("isCustomEvent")
    private var isCustomEvent: Boolean? = null

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
}
