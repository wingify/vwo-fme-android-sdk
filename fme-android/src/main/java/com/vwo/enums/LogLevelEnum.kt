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
package com.vwo.enums

/**
 * Enumeration representing log levels.
 *
 * This enum defines constants for different log levels used for logging messages
 * within the application. Each log level is associated with a specific string value.
 */
enum class LogLevelEnum(val level: String) {
    /**
     * Log level for detailed tracing information.
     */
    TRACE("trace"),
    /**
     * Log level for debugging information.
     */
    DEBUG("debug"),
    /**
     * Log level for general informational messages.
     */
    INFO("info"),
    /**
     * Log level for warning messages.
     */
    WARN("warn"),
    /**
     * Log level for error messages.
     */
    ERROR("error")
}
