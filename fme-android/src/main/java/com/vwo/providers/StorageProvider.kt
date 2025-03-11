/*
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vwo.providers

import SdkDataManager
import android.content.Context
import com.vwo.packages.storage.GatewayResponseStore
import com.vwo.packages.storage.SettingsStore
import java.lang.ref.WeakReference

/**
 * An object that provides access to various components and data stores.
 * This object acts as a central point for accessing instances of `SettingsStore`,
 * `RequestStore`, and the application context. It uses weak references to avoid
 * memory leaks.
 */
internal object StorageProvider {

    /**
     * The instance of `SettingsStore` used to manage settingsdata.
     */
    var settingsStore: SettingsStore? = null

    /**
     * The instance of `GatewayResponseStore` used to manage gateway data chaching.
     */
    var gatewayStore: GatewayResponseStore? = null

    /**
     * A weak reference to the application context.
     */
    var contextRef: WeakReference<Context> = WeakReference(null)

    var sdkDataManager: SdkDataManager? = null

    /** User Agent*/
    internal lateinit var userAgent :String

    /**
     * Ip address
     * Ip address will be empty as Mobile app can not get the ip address.
     */
    internal var ipAddress: String = ""
}