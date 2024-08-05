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
package com.vwo.interfaces.logger


import com.vwo.packages.logger.core.LogTransportManager
import com.vwo.packages.logger.enums.LogLevelEnum

/**
 * Interface for managing log operations.
 *
 * This interface defines methods for configuring and interacting with log transports,
 * as well as for retrieving log-related information such as the current log level,
 * prefix, and date-time format.
 */
interface ILogManager {
    /**
     * The log transport manager responsible for handling log delivery.
     */
    val transportManager: LogTransportManager?

    /**
     * Configuration settings for the log manager.
     */
    val config: Map<String, Any>

    /**
     * The name of the log manager.
     */
    val name: String?

    /**
     * The unique request ID associated with the log manager.
     */
    val requestId: String?

    /**
     * The current log level.
     */
    val level: LogLevelEnum?

    /**
     * The prefix to be added to log messages.
     */
    val prefix: String?

    /**
     * Retrieves the date-time format used for log messages.
     *
     * @return The date-time format string.
     */
    fun getDateTimeFormat(): String?

    /**
     * Retrieves the primary log transport.
     *
     * @return A map representing the transport configuration.
     */
    fun getTransport(): Map<String, Any>?

    /**
     * Retrieves a list of all configured log transports.
     *
     * @return A list of maps, each representing a transport configuration.
     */
    fun getTransports(): List<Map<String, Any>>

    /**
     * Adds a new log transport.
     *
     * @param transport A map representing the transport configuration.
     */
    fun addTransport(transport: Map<String, Any>)

    /**
     * Adds multiple log transports.
     *
     * @param transports A list of maps, each representing a transport configuration.
     */
    fun addTransports(transports: List<Map<String, Any>>)
}