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
package com.vwo.interfaces.storage

import com.wingify.models.Variation
import com.vwo.models.user.VWOUserContext
import com.wingify.services.StorageService

/**
 * Interface for decorating storage operations.
 *
 * @deprecated Use [com.wingify.interfaces.storage.IStorageDecorator] for new integrations.
 */
@Deprecated(
    message = "Use com.wingify.interfaces.storage.IStorageDecorator instead",
    replaceWith = ReplaceWith("IStorageDecorator", "com.wingify.interfaces.storage.IStorageDecorator"),
)
interface IStorageDecorator {
    fun setDataInStorage(data: Map<String, Any>, storageService: StorageService): Variation?

    fun getFeatureFromStorage(
        featureKey: String,
        context: VWOUserContext,
        storageService: StorageService,
    ): Map<String, Any>?
}
