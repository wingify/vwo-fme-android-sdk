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
package com.vwo.packages.logger

import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.enums.LogLevelEnum
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun formatMessage(level: LogLevelEnum?, message: String?): String {
        return String.format(
            "[%s]: %s %s %s",
            getFormattedLevel(level!!.name),
            getFormattedPrefix(prefix),
            formattedDateTime,
            message
        )
    }

    private fun getFormattedPrefix(prefix: String): String {
        return String.format(
            "%s%s%s",
            AnsiColorEnum.BOLD,
            AnsiColorEnum.GREEN,
            prefix + AnsiColorEnum.RESET
        )
    }

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

    private val formattedDateTime: String
        get() = dateTimeFormat.format(Date())
}
