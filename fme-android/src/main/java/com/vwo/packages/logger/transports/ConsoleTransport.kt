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
package com.vwo.packages.logger.transports

import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.Logger
import com.vwo.packages.logger.enums.LogLevelEnum

/**
 * Logs messages to the console.
 *
 * This class implements a log transport that prints log messages to the console.It filters message
 * based on the configured log level.
 */
class ConsoleTransport(private val level: LogLevelEnum) : Logger(), LogTransport {
    /**
     * Logs a trace message to the console.
     *
     * @param message The message to log.
     */
    override fun trace(message: String?) {
        log(LogLevelEnum.TRACE, message)
    }

    /**
     * Logs a debug message to the console.
     *
     * @param message The message to log.
     */
    override fun debug(message: String?) {
        log(LogLevelEnum.DEBUG, message)
    }

    /**
     * Logs an info message to the console.
     *
     * @param message The message to log.
     */
    override fun info(message: String?) {
        log(LogLevelEnum.INFO, message)
    }

    /**
     * Logs a warning message to the console.
     *
     * @param message The message to log.
     */
    override fun warn(message: String?) {
        log(LogLevelEnum.WARN, message)
    }

    /**
     * Logs an error message to the console.
     *
     * @param message The message to log.
     */
    override fun error(message: String?) {
        log(LogLevelEnum.ERROR, message)
    }

    /**
     * Logs a message to the console with the specified log level.
     *
     * @param level The log level.
     * @param message The message to log.
     */
    override fun log(level: LogLevelEnum, message: String?) {
        if (this.level.ordinal <= level.ordinal) {
            println(message)
        }
    }
}
