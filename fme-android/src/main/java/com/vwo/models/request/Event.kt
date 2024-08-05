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
package com.vwo.models.request

/**
 * Represents an event with associated properties, name, and timestamp.
 *
 * This class is used to encapsulate information about events that occur within the application.
 */
class Event {
    /**
     * Custom properties associated with the event.
     */
    var props: Props? = null
    /**
     * The name of the event.
     */
    var name: String? = null
    /**
     * The timestamp of when the event occurred (in milliseconds).
     */
    var time: Long? = null
}
