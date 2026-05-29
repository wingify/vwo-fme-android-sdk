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
package com.wingify.interfaces.logger

import com.wingify.packages.logger.core.LogTransportManager
import com.vwo.packages.logger.enums.LogLevelEnum

/**
 * Interface for managing log operations.
 */
interface ILogManager {
    val transportManager: LogTransportManager?

    val config: Map<String, Any>

    val name: String?

    val requestId: String?

    val level: LogLevelEnum?

    val prefix: String?

    fun getDateTimeFormat(): String?

    fun getTransport(): Map<String, Any>?

    fun getTransports(): List<Map<String, Any>>

    fun addTransport(transport: Map<String, Any>)

    fun addTransports(transports: List<Map<String, Any>>)

    fun errorLog(
        key: String,
        data: Map<String, Any>?,
        debugData: Map<String, Any>?,
        shouldSendToVWO: Boolean,
    )
}
