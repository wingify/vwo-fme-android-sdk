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

import com.vwo.interfaces.IVwoInitCallback
import com.vwo.models.user.GetFlag
import com.vwo.models.user.VWOContext
import com.vwo.models.user.VWOInitOptions
import com.vwo.utils.LogMessageUtil.buildMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object VWO {
    private var instance: VWO? = null
    private var vwoClient: VWOClient? = null

    /**
     * Sets the singleton instance of VWO.
     * Configures and builds the VWO instance using the provided options.
     * @param options - Configuration options for setting up VWO.
     * @return A CompletableFuture resolving to the configured VWO instance.
     */
    private fun setInstance(options: VWOInitOptions): VWO {

        val vwoBuilder: VWOBuilder = options.vwoBuilder ?: VWOBuilder(options)
        vwoBuilder.setLogger() // Sets up logging for debugging and monitoring.
            .setSettingsManager() // Sets the settings manager for configuration management.
            .setStorage() // Configures storage for data persistence.
            .setNetworkManager() // Configures network management for API communication.
            .setSegmentation() // Sets up segmentation for targeted functionality.
            .initPolling() // Initializes the polling mechanism for fetching settings.

        val settings = vwoBuilder.getSettings(false)
        val vwoInstance = this
        settings?.let {
            vwoClient = VWOClient(it, options)
            // Set VWOClient instance in VWOBuilder
            vwoBuilder.setVWOClient(vwoClient)
        }
        return vwoInstance
    }

    /**
     * Gets the singleton instance of VWO.
     * @return The singleton instance of VWO.
     */
    @JvmStatic
    fun getInstance(): VWO? {
        return instance
    }

    @JvmStatic
    fun init(options: VWOInitOptions, initListener: IVwoInitCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            if (options.sdkKey.isNullOrEmpty()) {
                val message = "SDK key is required to initialize VWO. Please provide the sdkKey in " +
                        "the options."
                initListener.vwoInitFailed(message)
                return@launch
            }

            if (options.accountId == null) {
                val message = "Account ID is required to initialize VWO. Please provide the " +
                        "accountId in the options."
                initListener.vwoInitFailed(message)
                return@launch
            }

            instance = setInstance(options)
            instance?.let { initListener.vwoInitSuccess(it, "VWO initialized successfully") }
        }
    }

    /**
     * This method is used to update the settings
     * @param newSettings New settings to be updated
     */
    fun updateSettings(newSettings: String) {
        vwoClient?.updateSettings(newSettings)
    }

    /**
     * This method is used to get the flag value for the given feature key
     * @param featureKey Feature key for which the flag value is to be fetched
     * @param context User context
     * @return GetFlag object containing the flag values
     */
    fun getFlag(featureKey: String, ctx: VWOContext): GetFlag? {
        return vwoClient?.getFlag(featureKey, ctx)
    }

    /**
     * Overloaded function if event properties need to be passed
     * calls track method to track the event
     * @param eventName Event name to be tracked
     * @param context User context
     * @param eventProperties event properties to be sent for the event
     * @return Map containing the event name and its status
     */
    fun trackEvent(
        eventName: String,
        context: VWOContext,
        eventProperties: Map<String, Any>
    ): Map<String, Boolean>? {
        return vwoClient?.trackEvent(eventName, context, eventProperties)
    }

    /**
     * Overloaded function for no event properties
     * calls track method to track the event
     * @param eventName Event name to be tracked
     * @param context User context
     * @return Map containing the event name and its status
     */
    fun trackEvent(eventName: String, context: VWOContext): Map<String, Boolean>? {
        return vwoClient?.trackEvent(eventName, context)
    }

    /**
     * Sets an attribute for a user in the context provided.
     * This method validates the types of the inputs before proceeding with the API call.
     * @param attributeKey - The key of the attribute to set.
     * @param attributeValue - The value of the attribute to set.
     * @param context User context
     */
    fun setAttribute(attributeKey: String, attributeValue: String, context: VWOContext) {
        vwoClient?.setAttribute(attributeKey, attributeValue, context)
    }
}

