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
package com.vwo.packages.logger.core

import androidx.annotation.VisibleForTesting
import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.Logger
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.logger.enums.LogLevelNumberEnum
import java.util.Locale

/**
 * Manages log transports and logs messages to them.
 *
 * This class is responsible for managing a list of log transports and logging messages to them based on the configured log level.
 */
class LogTransportManager(private val config: Map<String, Any>) : Logger(), LogTransport {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val transports: MutableList<LogTransport> = ArrayList()

    /**
     * Adds a log transport to the manager.
     *
     * @param transport The log transport to add.
     */
    fun addTransport(transport: LogTransport?) {
        if (transport != null)
            transports.add(transport)
    }

    /**
     * Checks if a log message should be logged based on the transport level and configuration level.
     *
     * @param transportLevel The log level of the transport.
     * @param configLevel The log level specified in the configuration.
     * @return True if the message should be logged, false otherwise.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun shouldLog(transportLevel: String, configLevel: String): Boolean {
        val targetLevel =
            LogLevelNumberEnum.valueOf(transportLevel.uppercase(Locale.getDefault())).level
        val desiredLevel =
            LogLevelNumberEnum.valueOf(configLevel.uppercase(Locale.getDefault())).level
        return targetLevel >= desiredLevel
    }

    /**
     * Logs a trace message.
     *
     * @param message The message to log.
     */
    override fun trace(message: String?) {
        log(LogLevelEnum.TRACE, message)
    }

    /**
     * Logs a debug message.
     *
     * @param message The message to log.
     */
    override fun debug(message: String?) {
        log(LogLevelEnum.DEBUG, message)
    }

    /**
     * Logs an info message.
     *
     * @param message The message to log.
     */
    override fun info(message: String?) {
        log(LogLevelEnum.INFO, message)
    }

    /**
     * Logs a warning message.
     *
     * @param message The message to log.
     */
    override fun warn(message: String?) {
        log(LogLevelEnum.WARN, message)
    }

    /**
     * Logs an error message.
     ** @param message The message to log.
     */
    override fun error(message: String?) {
        log(LogLevelEnum.ERROR, message)
    }

    /**
     * Logs a message with the specified log level.
     *
     * @param level The log level.
     * @param message The message to log.
     */
    override fun log(level: LogLevelEnum, message: String?) {
        for (transport in transports) {
            val levelString = (LogManager.instance?.level ?: LogLevelEnum.ERROR).toString()
            if (shouldLog(level.name, levelString)) {
                transport.log(level, message)
            }
        }
    }
}
