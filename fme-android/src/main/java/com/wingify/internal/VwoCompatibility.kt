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
package com.wingify.internal

import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.VWOUserContext
import com.wingify.interfaces.IWingifyListener
import com.wingify.models.user.GetFlag
import com.wingify.models.user.WingifyUserContext

/**
 * Internal adapters between Wingify public types and legacy [com.vwo] types.
 */

/**
 * Returns a [VWOUserContext] for legacy [com.vwo] APIs.
 */
internal fun WingifyUserContext.toVwoUserContext(): VWOUserContext {
    if (this is VWOUserContext) return this
    val vwo = VWOUserContext()
    vwo.id = id
    vwo.customVariables = HashMap(customVariables)
    vwo.postSegmentationVariables = postSegmentationVariables
    vwo.variationTargetingVariables = HashMap(variationTargetingVariables)
    vwo.sessionId = sessionId
    vwo.vwo = this.vwo
    vwo.shouldUseDeviceIdAsUserId = shouldUseDeviceIdAsUserId
    vwo.bucketingSeed = bucketingSeed
    return vwo
}

/**
 * Adapts an [IWingifyListener] to [IVwoListener], wrapping [GetFlag] on success callbacks.
 */
internal fun IWingifyListener.toVwoListener(): IVwoListener = object : IVwoListener {
    override fun onSuccess(data: Any) {
        val wrapped = (data as? com.vwo.models.user.GetFlag)?.let { GetFlag.wrap(it) } ?: data
        this@toVwoListener.onSuccess(wrapped)
    }

    override fun onFailure(message: String) {
        this@toVwoListener.onFailure(message)
    }
}
