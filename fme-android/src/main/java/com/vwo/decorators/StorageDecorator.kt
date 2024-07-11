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
package com.vwo.decorators

import com.vwo.interfaces.storage.IStorageDecorator
import com.vwo.models.Variation
import com.vwo.models.user.VWOContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.services.LoggerService.Companion.log
import com.vwo.services.StorageService

class StorageDecorator : IStorageDecorator {
    override fun getFeatureFromStorage(
        featureKey: String?,
        context: VWOContext?,
        storageService: StorageService?
    ): Map<String, Any>? {
        return storageService!!.getDataInStorage(featureKey, context!!)
    }

    override fun setDataInStorage(
        data: Map<String?, Any?>?,
        storageService: StorageService?
    ): Variation? {
        val featureKey = data!!["featureKey"] as String?
        val userId = data["user"].toString()

        if (featureKey == null || featureKey.isEmpty()) {
            log(LogLevelEnum.ERROR, "STORING_DATA_ERROR", object : HashMap<String?, String?>() {
                init {
                    put("key", "featureKey")
                }
            })
            return null
        }

        if (userId == null || userId.isEmpty()) {
            log(LogLevelEnum.ERROR, "STORING_DATA_ERROR", object : HashMap<String?, String?>() {
                init {
                    put("key", "Context or Context.id")
                }
            })
            return null
        }

        val rolloutKey = data["rolloutKey"] as String?
        val experimentKey = data["experimentKey"] as String?
        val rolloutVariationId = data["rolloutVariationId"] as Int?
        val experimentVariationId = data["experimentVariationId"] as Int?

        if (rolloutKey != null && !rolloutKey.isEmpty() && experimentKey == null && rolloutVariationId == null) {
            log(LogLevelEnum.ERROR, "STORING_DATA_ERROR", object : HashMap<String?, String?>() {
                init {
                    put("key", "Variation:(rolloutKey, experimentKey or rolloutVariationId)")
                }
            })
            return null
        }

        if (experimentKey != null && !experimentKey.isEmpty() && experimentVariationId == null) {
            log(LogLevelEnum.ERROR, "STORING_DATA_ERROR", object : HashMap<String?, String?>() {
                init {
                    put("key", "Variation:(experimentKey or rolloutVariationId)")
                }
            })
            return null
        }

        storageService!!.setDataInStorage(data)

        return Variation() // Assuming you need to return a new VariationModel instance.
    }
}
