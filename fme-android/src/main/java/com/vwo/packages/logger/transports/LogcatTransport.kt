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

import android.util.Log
import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.enums.LogLevelEnum

/**
 * Logs messages to Logcat.
 *
 * This class implements a log transport that sends log messages to Logcat, the Android logging
 * system. It filters messages based on the configured log level.
 */
class LogcatTransport(private val level: LogLevelEnum) : LogTransport {
    private val tag = "Vwo-fme-android"

    /**
     * Logs a verbose message to Logcat.*
     * @param tag The log tag.
     * @param message The message to log.
     */
    fun trace(tag: String, message: String) {
        Log.v(tag, message)
    }

    /**
     * Logs a debug message to Logcat.
     *
     * @param tag The log tag.
     * @param message The message to log.
     */
    fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    /**
     * Logs an info message to Logcat.
     *
     * @param tag The log tag.
     * @param message The message to log.
     */
    fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    /**
     * Logs a warning message to Logcat.
     *
     * @param tag The log tag.
     * @param message The message to log.
     */
    fun warn(tag: String, message: String) {
        Log.w(tag, message)
    }

    /**
     * Logs an error message to Logcat.
     *
     * @param tag The log tag.
     * @param message The message to log.
     */
    fun error(tag: String, message: String) {
        Log.e(tag, message)
    }

    /**
     * Logs a message to Logcat with the specified log level.
     *
     * @param level The log level.
     * @param message The message to log.
     */
    override fun log(level: LogLevelEnum, message: String?) {
        if (message == null) return

        if (this.level.ordinal <= level.ordinal) {
            when (level) {
                LogLevelEnum.TRACE -> trace(tag, message)
                LogLevelEnum.ERROR -> error(tag, message)
                LogLevelEnum.DEBUG -> debug(tag, message)
                LogLevelEnum.WARN -> warn(tag, message)
                else -> info(tag, message)
            }
        }
    }
}