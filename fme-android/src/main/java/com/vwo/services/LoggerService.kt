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
package com.vwo.services

import com.vwo.VWOClient
import com.vwo.packages.logger.core.LogManager
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.utils.DataTypeUtil.filterStringMap
import com.vwo.utils.LogMessageUtil

/**
 * Provides logging functionality.
 *
 * This class is responsible for handling logging operations, allowing the application to record
 * events, messages,and errors for debugging and monitoring purposes.
 */
class LoggerService(config: Map<String, Any>) {

    init {
        // initialize the LogManager
        LogManager(config)

        // read the log files
        debugMessages = readLogFiles("assets/debug-messages.json")
        infoMessages = readLogFiles("assets/info-messages.json")
        errorMessages = readLogFiles("assets/error-messages.json")
        warningMessages = readLogFiles("assets/warn-messages.json")
        traceMessages = readLogFiles("assets/trace-messages.json")
    }

    /**
     * Reads the log files and returns the messages in a map.
     */
    private fun readLogFiles(fileName: String): Map<String, String> {
        try {
            val inputStream = this.javaClass.classLoader?.getResourceAsStream(fileName)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() }?:"{}"
            val values = VWOClient.objectMapper.readValue(jsonString, MutableMap::class.java)
            return filterStringMap(values)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return HashMap()
    }

    companion object {

        var debugMessages = emptyMap<String, String>()
        var errorMessages = emptyMap<String, String>()
        var infoMessages = emptyMap<String, String>()
        var warningMessages = emptyMap<String, String>()
        var traceMessages = emptyMap<String, String>()

        /**
         * Logs a message with the specified log level, key, and optional parameters.
         * @param level The log level for the message.
         * @param key The key of the message to log.
         * @param map Optional parameters to replace placeholders in the message.
         */
        fun log(level: LogLevelEnum, key: String, map: Map<String?, String?>?) {
            val logManager = LogManager.instance ?: return
            when (level) {
                LogLevelEnum.DEBUG -> logManager.debug(
                    LogMessageUtil.buildMessage(debugMessages[key], map)
                )

                LogLevelEnum.INFO -> logManager.info(
                    LogMessageUtil.buildMessage(infoMessages[key], map)
                )

                LogLevelEnum.TRACE -> logManager.trace(
                    LogMessageUtil.buildMessage(traceMessages[key], map)
                )

                LogLevelEnum.WARN -> logManager.warn(
                    LogMessageUtil.buildMessage(warningMessages[key], map)
                )

                else -> logManager.error(LogMessageUtil.buildMessage(errorMessages[key], map))
            }
        }

        /**
         * Logs a message with the specified log level and message string.
         *
         * @param level The log level for the message.
         * @param message The message string to log.
         */
        @JvmStatic
        fun log(level: LogLevelEnum?, message: String?) {
            val logManager = LogManager.instance ?: return
            when (level) {
                LogLevelEnum.DEBUG -> logManager.debug(message)
                LogLevelEnum.INFO -> logManager.info(message)
                LogLevelEnum.TRACE -> logManager.trace(message)
                LogLevelEnum.WARN -> logManager.warn(message)
                else -> logManager.error(message)
            }
        }

        /**
         * Middleware method that stores error in DebuggerService and logs it.
         * @param key The template string for the error message.
         * @param data Data to be used in the template.
         * @param debugData Additional debug data to be sent.
         * @param shouldSendToVWO Whether to send the error to VWO.
         */
        fun errorLog(
            key: String,
            data: Map<String, Any>? = null,
            debugData: Map<String, Any>? = null,
            shouldSendToVWO: Boolean= true
        ) {
            try {

                val logManager = LogManager.instance ?: return
                logManager.errorLog(key, data, debugData, shouldSendToVWO)
            } catch (e: Exception) {
                log(LogLevelEnum.DEBUG, "Got error while logging error $e")
            }
        }

        fun getMessage(level: LogLevelEnum, key: String, map: Map<String?, String?>): String? {
            return when (level) {
                LogLevelEnum.DEBUG -> LogMessageUtil.buildMessage(debugMessages[key], map)
                LogLevelEnum.INFO -> LogMessageUtil.buildMessage(infoMessages[key], map)
                LogLevelEnum.TRACE -> LogMessageUtil.buildMessage(traceMessages[key], map)
                LogLevelEnum.WARN -> LogMessageUtil.buildMessage(warningMessages[key], map)
                else -> LogMessageUtil.buildMessage(errorMessages[key], map)
            }
        }
    }
}