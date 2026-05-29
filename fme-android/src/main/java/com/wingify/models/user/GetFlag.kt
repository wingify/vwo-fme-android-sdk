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
package com.wingify.models.user

/**
 * Feature flag result for the Wingify SDK.
 */
class GetFlag private constructor(
    private val delegate: com.vwo.models.user.GetFlag,
) {
    val context: WingifyUserContext = delegate.context

    fun isEnabled(): Boolean = delegate.isEnabled()

    fun getVariable(key: String?, defaultValue: Any): Any = delegate.getVariable(key, defaultValue)

    fun getRecommendationDisplayConfig(key: String?): Map<String, Any>? =
        delegate.getRecommendationDisplayConfig(key)

    fun getVariables(): List<Map<String, Any>> = delegate.getVariables()

    internal companion object {
        fun wrap(flag: com.vwo.models.user.GetFlag): GetFlag = GetFlag(flag)
    }
}
