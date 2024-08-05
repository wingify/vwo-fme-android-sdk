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
package com.vwo.services

import com.vwo.interfaces.integration.IntegrationCallback

/**
 * Manages integration hooks and callbacks.
 *
 * This class is responsible for handling integration hooks and invoking the associated callback
 * methods to allow external systems to integrate with the application's functionality.
 */
class HooksManager(private val callback: IntegrationCallback?) {
    private var decision: Map<String, Any>? = null

    /**
     * Executes the callback
     *
     * @param properties Properties from the callback
     */
    fun execute(properties: Map<String, Any>?) {
        if (this.callback != null && properties!=null) {
            callback.execute(properties)
        }
    }

    /**
     * Sets properties to the decision object
     *
     * @param properties Properties to set
     */
    fun set(properties: Map<String, Any>?) {
        if (this.callback != null) {
            this.decision = properties
        }
    }

    /**
     * Retrieves the decision object
     *
     * @return The decision object
     */
    fun get(): Map<String, Any>? {
        return this.decision
    }
}
