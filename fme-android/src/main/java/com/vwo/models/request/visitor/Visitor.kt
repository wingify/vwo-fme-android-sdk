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
package com.vwo.models.request.visitor

import com.google.gson.annotations.SerializedName

/**
 * Represents a visitor and their associated properties.
 *
 * This class allows storing and retrieving custom properties associated with a visitor.
 */
class Visitor {
    /**
     * A mutable map to store visitor properties.
     *
     * This property is optional and can be set using the `setProps` method.
     */
    @SerializedName("props")
    var props: MutableMap<String, Any>? = null
        private set

    /**
     * Sets the properties for the visitor.
     *
     * @param props A mutable map of properties to associate with the visitor.
     * @return The `Visitor` instance (for method chaining).
     */
    fun setProps(props: MutableMap<String, Any>?): Visitor {
        this.props = props
        return this
    }
}
