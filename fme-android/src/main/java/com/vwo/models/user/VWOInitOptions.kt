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
package com.vwo.models.user

import com.wingify.models.user.WingifyInitOptions

/**
 * Represents initialization options for the VWO SDK.
 *
 * This class encapsulates various options that can be configured when initializing the VWO SDK,
 * such as the SDK key, account ID, integrations, logger, network client, segment evaluator,
 * storage, polling interval, and gateway service.
 *
 * @deprecated Use [com.wingify.models.user.WingifyInitOptions] for new integrations.
 */
@Deprecated(
    message = "Use com.wingify.models.user.WingifyInitOptions instead",
    replaceWith = ReplaceWith("WingifyInitOptions", "com.wingify.models.user.WingifyInitOptions"),
)
class VWOInitOptions : WingifyInitOptions() {
    init {
        markAsLegacyVwoSdk()
    }
}
