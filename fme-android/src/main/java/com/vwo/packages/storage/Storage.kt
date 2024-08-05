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
package com.vwo.packages.storage


/**
 * Manages data storage and retrieval through a connector.
 *
 * This class provides a singleton instance for accessing and managing data storage operations. It
 * allows attaching a connector to interact with a specific data store and provides methods for
 * setting and retrieving data.
 */
class Storage {
    private var connector: Connector? = null

    /**
     * Attaches a connector to the storage instance.
     *
     * @param connectorInstanceThe connector instance to attach.
     * @return The attached connector instance.
     */
    fun attachConnector(connectorInstance: Connector?): Connector? {
        this.connector = connectorInstance
        return this.connector
    }

    /**
     * Retrieves the attached connector instance.
     *
     * @return The attached connector instance or null if no connector is attached.
     */
    fun getConnector(): Any? {
        return this.connector
    }

    companion object {
        /**
         * Retrieves the singleton instance of the storage class.
         * @return The singleton instance of the storage class.
         */
        @JvmStatic
        @get:Synchronized
        var instance: Storage? = null
            get() {
                if (field == null) {
                    field = Storage()
                }
                return field
            }
            private set
    }
}
