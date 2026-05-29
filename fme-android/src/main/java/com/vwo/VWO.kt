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

import com.vwo.interfaces.IVwoInitCallback
import com.vwo.interfaces.IVwoListener
import com.vwo.models.user.VWOInitOptions
import com.vwo.models.user.VWOUserContext
import com.wingify.Wingify
import com.wingify.WingifyBuilder
import com.wingify.models.user.WingifyInitOptions
import kotlin.concurrent.thread

/**
 * VWO (Visual Website Optimizer) is a powerful A/B testing and experimentation platform.
 *
 * @deprecated Use [com.wingify.Wingify] for new integrations. This type remains available
 * for backward compatibility.
 */
@Deprecated(
    message = "Use com.wingify.Wingify instead",
    replaceWith = ReplaceWith("Wingify", "com.wingify.Wingify"),
)
class VWO private constructor(
    settings: String?,
    options: WingifyInitOptions,
    wingifyBuilder: WingifyBuilder,
) : VWOClient(settings, options, wingifyBuilder) {

    companion object {
        @Deprecated(
            message = "Use com.wingify.Wingify.init instead",
            replaceWith = ReplaceWith(
                "Wingify.init(options, initListener)",
                "com.wingify.Wingify",
            ),
        )
        @JvmStatic
        fun init(options: VWOInitOptions, initListener: IVwoInitCallback) {
            Wingify.initWithFactory(
                options,
                factory = { settings, initOptions, builder -> VWO(settings, initOptions, builder) },
                onSuccess = { client, message ->
                    initListener.vwoInitSuccess(
                        client as VWO,
                        message.replace("Wingify", "VWO"),
                    )
                },
                onFailure = { message ->
                    initListener.vwoInitFailed(message.replace("Wingify", "VWO"))
                },
            )
        }

        @Deprecated(
            message = "Use com.wingify.Wingify.getInstance instead",
            replaceWith = ReplaceWith(
                "Wingify.getInstance(accountId, sdkKey)",
                "com.wingify.Wingify",
            ),
        )
        @JvmStatic
        fun getInstance(accountId: Int?, sdkKey: String?): VWO? {
            return Wingify.getClientInstance(accountId, sdkKey) as? VWO
        }

        @Deprecated(
            message = "Use com.wingify.Wingify.clearInstance instead",
            replaceWith = ReplaceWith(
                "Wingify.clearInstance(accountId, sdkKey)",
                "com.wingify.Wingify",
            ),
        )
        @JvmStatic
        fun clearInstance(accountId: Int?, sdkKey: String?) {
            Wingify.clearInstance(accountId, sdkKey)
        }

        @Deprecated(
            message = "Use com.wingify.Wingify.clearAllInstances instead",
            replaceWith = ReplaceWith(
                "Wingify.clearAllInstances()",
                "com.wingify.Wingify",
            ),
        )
        @JvmStatic
        fun clearAllInstances() {
            Wingify.clearAllInstances()
        }
    }

    fun getFlag(featureKey: String, context: VWOUserContext, listener: IVwoListener) {
        thread(start = true) {
            try {
                val flag = super.getFlag(featureKey, context)
                listener.onSuccess(flag)
            } catch (e: Exception) {
                listener.onFailure(e.message ?: e.toString())
            }
        }
    }
}
