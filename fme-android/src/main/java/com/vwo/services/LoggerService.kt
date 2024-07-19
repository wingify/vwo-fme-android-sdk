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
package com.vwo.services

import com.vwo.VWOClient
import com.vwo.packages.logger.core.LogManager
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.utils.DataTypeUtil.filterStringMap
import com.vwo.utils.LogMessageUtil
import com.vwo.utils.NetworkUtil

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
            val values = VWOClient.objectMapper.readValue(inputStream, MutableMap::class.java)
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
    }
}
