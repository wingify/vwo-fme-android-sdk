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

import com.google.gson.annotations.SerializedName

/**
 * Represents props for VWO requests.
 *
 * This class encapsulates information for request properties.
 */
class Props {
    @SerializedName("vwo_sdkName")
    private var vwo_sdkName: String? = null

    @SerializedName("vwo_sdkVersion")
    private var vwo_sdkVersion: String? = null

    @SerializedName("vwo_envKey")
    private var vwo_envKey: String? = null
    var variation: String? = null
    var id: Int? = null

    @SerializedName("isFirst")
    private var isFirst: Int? = null

    @SerializedName("isMII")
    private var isMII: Boolean = false

    @SerializedName("isCustomEvent")
    private var isCustomEvent: Boolean? = null

    @SerializedName("product")
    private var product: String? = null

    @SerializedName("data")
    private var data: Map<String, Any>? = null

    @SerializedName("vwoMeta")
    private var vwoMeta: Map<String, Any>? = null

    // Additional properties are handled by PropsSerializer
    // The custom serializer excludes this field and flattens its contents into the root JSON
    private var additionalProperties: Map<String, Any> = HashMap()

    fun setSdkName(sdkName: String?) {
        this.vwo_sdkName = sdkName
    }

    fun setSdkVersion(sdkVersion: String?) {
        this.vwo_sdkVersion = sdkVersion
    }

    fun setEnvKey(vwo_envKey: String?) {
        this.vwo_envKey = vwo_envKey
    }

    fun getAdditionalProperties(): Map<String, *> {
        return additionalProperties
    }

    fun setAdditionalProperties(additionalProperties: Map<String, Any>) {
        this.additionalProperties = additionalProperties
    }

    fun setFirst(first: Int?) {
        isFirst = first
    }

    fun setCustomEvent(customEvent: Boolean?) {
        isCustomEvent = customEvent
    }

    fun setIsMII(mII: Boolean) {
        isMII = mII
    }

    fun getIsMII(): Boolean {
        return isMII
    }

    fun setProduct(product: String?) {
        this.product = product
    }

    fun setData(data: Map<String, Any>?) {
        this.data = data
    }

    fun setVwoMeta(vwoMeta: Map<String, Any>?) {
        this.vwoMeta = vwoMeta
    }
}
