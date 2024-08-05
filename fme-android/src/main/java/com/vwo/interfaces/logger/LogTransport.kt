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
package com.vwo.interfaces.logger

import com.vwo.packages.logger.enums.LogLevelEnum

/**
 * Interface for log transport mechanisms.
 *
 * This interface defines a single method, `log`, which is responsible for
 * transporting log messages to their destination.
 */
interface LogTransport {
    /**
     * Logs a message with the specified log level.
     *
     * @param level The log level of the message.
     * @param message The log message to be transported.
     */
    fun log(level: LogLevelEnum, message: String?)
}