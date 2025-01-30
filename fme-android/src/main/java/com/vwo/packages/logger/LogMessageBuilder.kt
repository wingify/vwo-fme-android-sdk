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
package com.vwo.packages.logger

import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.enums.LogLevelEnum
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds formatted log messages.
 *
 * This class is responsible for constructing formatted log messages based on the provided configuration and log level. It includes functionalities for adding prefixes, timestamps, and color-coded log levels.
 */
class LogMessageBuilder(
    private val loggerConfig: Map<String, Any>,
    private val transport: LogTransport?
) {
    private val prefix = loggerConfig.getOrDefault("prefix", "VWO-SDK") as String
    private val dateTimeFormat =
        SimpleDateFormat(
            loggerConfig.getOrDefault(
                "dateTimeFormat",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
            ) as String
        )

    /**
     * Formats a log message with level, prefix, timestamp, and message.
     *
     * @param level The log level.
     * @param message The log message.
     * @return The formatted log message.
     */
    fun formatMessage(level: LogLevelEnum?, message: String?): String {
        return String.format(
            "[%s]: %s %s %s",
            getFormattedLevel(level!!.name),
            getFormattedPrefix(prefix),
            formattedDateTime,
            message
        )
    }

    /**
     * Formats the prefix with bold and green color.
     *
     * @param prefix The prefix string.
     * @return The formatted prefix.
     */
    private fun getFormattedPrefix(prefix: String): String {
        return String.format(
            "%s%s%s",
            AnsiColorEnum.BOLD,
            AnsiColorEnum.GREEN,
            prefix + AnsiColorEnum.RESET
        )
    }

    /**
     * Formats the log level with bold and specific color for each level.
     *
     * @param level The log level string.
     * @return The formatted log level.
     */
    private fun getFormattedLevel(level: String): String {
        val upperCaseLevel = level.uppercase(Locale.getDefault())
        return when (LogLevelEnum.valueOf(level.uppercase(Locale.getDefault()))) {
            LogLevelEnum.TRACE -> String.format(
                "%s%s%s",
                AnsiColorEnum.BOLD,
                AnsiColorEnum.WHITE,
                upperCaseLevel + AnsiColorEnum.RESET
            )

            LogLevelEnum.DEBUG -> String.format(
                "%s%s%s",
                AnsiColorEnum.BOLD,
                AnsiColorEnum.LIGHTBLUE,
                upperCaseLevel + AnsiColorEnum.RESET
            )

            LogLevelEnum.INFO -> String.format(
                "%s%s%s",
                AnsiColorEnum.BOLD,
                AnsiColorEnum.CYAN,
                upperCaseLevel + AnsiColorEnum.RESET
            )

            LogLevelEnum.WARN -> String.format(
                "%s%s%s",
                AnsiColorEnum.BOLD,
                AnsiColorEnum.YELLOW,
                upperCaseLevel + AnsiColorEnum.RESET
            )

            LogLevelEnum.ERROR -> String.format(
                "%s%s%s",
                AnsiColorEnum.BOLD,
                AnsiColorEnum.RED,
                upperCaseLevel + AnsiColorEnum.RESET
            )

            else -> level
        }
    }

    /**
     * Gets the formatted date and time.
     *
     * @return The formatted date and time string.
     */
    private val formattedDateTime: String
        get() = dateTimeFormat.format(Date())
}
