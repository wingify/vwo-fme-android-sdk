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
 * Represents an impact campaign in VWO.
 *
 * This class encapsulates information about a VWO impact campaign, including its campaign ID and type.
 */
class ImpactCampaign {
    @SerializedName("campaignId")
    var campaignId: Int? = null

    @SerializedName("type")
    var type: String? = null
}
