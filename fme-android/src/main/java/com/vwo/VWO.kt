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
package com.vwo

import com.vwo.models.user.VWOInitOptions
import com.vwo.utils.LogMessageUtil.buildMessage

class VWO
/**
 * Constructor for the VWO class.
 * Initializes a new instance of VWO with the provided options.
 * @param options - Configuration options for the VWO instance.
 */
    (settings: String?, options: VWOInitOptions?) : VWOClient(settings, options) {
    companion object {
        private var vwoBuilder: VWOBuilder? = null
        private var instance: VWO? = null

        /**
         * Sets the singleton instance of VWO.
         * Configures and builds the VWO instance using the provided options.
         * @param options - Configuration options for setting up VWO.
         * @return A CompletableFuture resolving to the configured VWO instance.
         */
        private fun setInstance(options: VWOInitOptions?): VWO {
            if (options!!.vwoBuilder != null) {
                vwoBuilder = options.vwoBuilder
            } else {
                vwoBuilder = VWOBuilder(options)
            }
            vwoBuilder
                .setLogger() // Sets up logging for debugging and monitoring.
                .setSettingsManager() // Sets the settings manager for configuration management.
                .setStorage() // Configures storage for data persistence.
                .setNetworkManager() // Configures network management for API communication.
                .setSegmentation() // Sets up segmentation for targeted functionality.
                .initPolling() // Initializes the polling mechanism for fetching settings.

            val settings = vwoBuilder!!.getSettings(false)
            val vwoInstance = VWO(settings, options)

            // Set VWOClient instance in VWOBuilder
            vwoBuilder!!.setVWOClient(vwoInstance)
            return vwoInstance
        }

        /**
         * Gets the singleton instance of VWO.
         * @return The singleton instance of VWO.
         */
        fun getInstance(): Any? {
            return instance
        }

        fun init(options: VWOInitOptions?): VWO? {
            if (options?.sdkKey == null || options.sdkKey!!.isEmpty()) {
                val message = buildMessage(
                    "SDK key is required to initialize VWO. Please provide the sdkKey in the options.",
                    null
                )
                System.err.println(message)
            }

            if (options?.accountId == null || options.accountId.toString().isEmpty()) {
                val message = buildMessage(
                    "Account ID is required to initialize VWO. Please provide the accountId in the options.",
                    null
                )
                System.err.println(message)
            }

            instance = setInstance(options)
            return instance
        }
    }
}

