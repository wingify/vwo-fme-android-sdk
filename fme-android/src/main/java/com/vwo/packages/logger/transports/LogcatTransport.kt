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
import com.vwo.packages.logger.Logger
import com.vwo.packages.logger.enums.LogLevelEnum

class LogcatTransport(private val level: LogLevelEnum) : LogTransport {
    fun trace(tag: String, message: String) {
        log(LogLevelEnum.TRACE, message) { tag, msg ->
            Log.v(tag, msg)
        }
    }

    fun debug(tag: String, message: String) {
        log(LogLevelEnum.DEBUG, message) { tag, msg ->
            Log.d(tag, msg)
        }
    }

    fun info(tag: String, message: String) {
        log(LogLevelEnum.INFO, message) { tag, msg ->
            Log.i(tag, msg)
        }
    }

    fun warn(tag: String, message: String) {
        log(LogLevelEnum.WARN, message) { tag, msg ->
            Log.w(tag, msg)
        }
    }

    fun error(tag: String, message: String) {
        log(LogLevelEnum.ERROR, message) { tag, msg ->
            Log.e(tag, msg)
        }
    }

    fun log(level: LogLevelEnum, message: String?, logFunction: (String, String) -> Unit) {
        if (message == null) return
        if (this.level.ordinal <= level.ordinal) {
            logFunction("Vwo", message)
        }
    }

    override fun log(level: LogLevelEnum, message: String?) {
        if (this.level.ordinal <= level.ordinal) {
            println(message)
        }
    }
}
