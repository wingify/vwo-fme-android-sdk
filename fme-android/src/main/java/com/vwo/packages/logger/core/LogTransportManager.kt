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
package com.vwo.packages.logger.core

import com.vwo.interfaces.logger.ILogManager.level
import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.LogMessageBuilder
import com.vwo.packages.logger.Logger
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.logger.enums.LogLevelNumberEnum
import java.util.Locale

class LogTransportManager(private val config: Map<String, Any>) : Logger(), LogTransport {
    private val transports: MutableList<LogTransport?> = ArrayList()

    fun addTransport(transport: LogTransport?) {
        transports.add(transport)
    }

    fun shouldLog(transportLevel: String, configLevel: String): Boolean {
        val targetLevel =
            LogLevelNumberEnum.valueOf(transportLevel.uppercase(Locale.getDefault())).level
        val desiredLevel =
            LogLevelNumberEnum.valueOf(configLevel.uppercase(Locale.getDefault())).level
        return targetLevel >= desiredLevel
    }

    override fun trace(message: String?) {
        log(LogLevelEnum.TRACE, message)
    }

    override fun debug(message: String?) {
        log(LogLevelEnum.DEBUG, message)
    }

    override fun info(message: String?) {
        log(LogLevelEnum.INFO, message)
    }

    override fun warn(message: String?) {
        log(LogLevelEnum.WARN, message)
    }

    override fun error(message: String?) {
        log(LogLevelEnum.ERROR, message)
    }

    override fun log(level: LogLevelEnum?, message: String?) {
        for (transport in transports) {
            val logMessageBuilder = LogMessageBuilder(config, transport)
            val formattedMessage = logMessageBuilder.formatMessage(level, message)
            if (shouldLog(level!!.name, LogManager.Companion.getInstance().level.toString())) {
                transport!!.log(level, formattedMessage)
            }
        }
    }
}
