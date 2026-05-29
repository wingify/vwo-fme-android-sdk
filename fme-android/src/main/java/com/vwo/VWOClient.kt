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
package com.vwo

import com.wingify.WingifyBuilder
import com.wingify.WingifyClient
import com.wingify.models.user.WingifyInitOptions

/**
 * Client class for interacting with the VWO SDK.
 *
 * @deprecated Use [com.wingify.WingifyClient] for new integrations.
 */
@Deprecated(
    message = "Use com.wingify.WingifyClient instead",
    replaceWith = ReplaceWith("WingifyClient", "com.wingify.WingifyClient"),
)
open class VWOClient(
    settings: String?,
    options: WingifyInitOptions,
    wingifyBuilder: WingifyBuilder,
) : WingifyClient(settings, options, wingifyBuilder) {

    companion object {
        @JvmStatic
        val gson get() = WingifyClient.gson

        @JvmStatic
        val objectMapper get() = WingifyClient.objectMapper
    }
}
