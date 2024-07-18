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

class ConsoleTransport(private val level: LogLevelEnum) : Logger(), LogTransport {
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

    override fun log(level: LogLevelEnum, message: String?) {
        if (this.level.ordinal <= level.ordinal) {
            println(message)
        }
    }
}
