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
package com.vwo.packages.logger.core

import com.vwo.constants.Constants
import com.vwo.enums.EventEnum
import com.vwo.interfaces.logger.ILogManager
import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.Logger
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.logger.transports.LogcatTransport
import com.vwo.utils.NetworkUtil
import com.vwo.utils.SDKMetaUtil.sdkVersion
import com.vwo.utils.LogMessageUtil.buildMessage
import com.vwo.utils.sendDebugEventToVWO
import com.vwo.enums.DebuggerCategoryEnum
import com.vwo.services.LoggerService.Companion.errorMessages
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Manages logging configurations and transports.
 *
 * This class is responsible for initializing and managing the logging system,including configuring loglevels, transports, and formatting. It provides methods for adding transports, retrieving log details, and logging messages at various levels.
 */
class LogManager(override val config: Map<String, Any>) : Logger(), ILogManager {

    override val transportManager: LogTransportManager = LogTransportManager(config)

    override val name: String = config["name"] as? String ?: "VWO Logger"

    override val requestId: String = UUID.randomUUID().toString()

    override val level: LogLevelEnum by lazy {
        val defaultLogLevel = LogLevelEnum.ERROR
        try {
            val level = (config["level"] as? String?)?.uppercase(Locale.getDefault())
            level?.let { LogLevelEnum.valueOf(it) } ?: defaultLogLevel
        } catch (e: Exception) {
            defaultLogLevel
        }
    }

    private val transports: List<Map<String, Any>> = ArrayList()

    override val prefix: String = (config["prefix"] as? String) ?: "VWO-SDK"

    val dateTimeForm = SimpleDateFormat(
        (config["dateTimeFormat"] as? String) ?: "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        Locale.getDefault()
    )

    private val storedMessages = mutableSetOf<String>()

    init {
        handleTransports()
        instance = this
    }

    /**
     * Initializes and configures log transports.
     *
     * If a list of transports is provided in the configuration, it adds them.Otherwise, it adds a default `LogcatTransport`.
     */
    private fun handleTransports() {
        val transportList = config["transports"] as? List<Map<String, Any>>
        if (!transportList.isNullOrEmpty()) {
            addTransports(transportList)
        } else {
            val defaultTransport = LogcatTransport(level)
            val defaultTransportMap = mutableMapOf<String, Any>()
            defaultTransportMap["defaultTransport"] = defaultTransport
            addTransport(defaultTransportMap)
        }
    }

    /**
     * Adds a single log transport.
     *
     * @param transport A map containing the transport instance.
     */
    override fun addTransport(transport: Map<String, Any>) {
        transportManager.addTransport(transport["defaultTransport"] as LogTransport?)
    }

    /**
     * Adds multiple log transports.
     *
     * @param transports A list of maps, each containing a transport instance.
     */
    override fun addTransports(transports: List<Map<String, Any>>) {
        for (transport in transports) {
            addTransport(transport)
        }
    }

    /**
     * Retrieves the date and time format used for logging.
     *
     * @return The date and time format string.
     */
    override fun getDateTimeFormat(): String {
        return dateTimeForm.toPattern()
    }

    /**
     * Retrieves the list of transports.
     *
     * @return The list of transports.
     */
    override fun getTransports(): List<Map<String, Any>> {
        return transports
    }

    /**
     * Retrieves a specific transport.
     *
     * @return The transport or null if not found.
     */
    override fun getTransport(): Map<String, Any>? {
        // This method needs more context, currently returning null.
        return null
    }

    /**
     * Logs a trace message.
     *
     * @param message The message to log.
     */
    override fun trace(message: String?) {
        transportManager.trace(message)
    }

    /**
     * Logs a debug message.
     *
     * @param message The message to log.
     */
    override fun debug(message: String?) {
        transportManager.debug(message)
    }

    /**
     * Logs an info message.
     *
     * @param message The message to log.
     */
    override fun info(message: String?) {
        transportManager.info(message)
    }

    /**
     * Logs a warning message.
     *
     * @param message The message to log.
     */
    override fun warn(message: String?) {
        transportManager.warn(message)
    }

    /**
     * Logs an error message.
     *
     * @param message The message to log.
     */
    override fun error(message: String?) {
        transportManager.error(message)
    }

    /**
     * Middleware method that stores error in DebuggerService and logs it.
     * @param key The template string for the error message.
     * @param data Data to be used in the template.
     * @param debugData Additional debug data to be sent.
     * @param shouldSendToVWO Whether to send the error to VWO.
     */
    override fun errorLog(
        key: String,
        data: Map<String, Any>?,
        debugData: Map<String, Any>?,
        shouldSendToVWO: Boolean
    ) {

        val message = buildMessage(errorMessages[key], data as Map<String?, Any?>?)
        error(message)
        
        if (shouldSendToVWO) {
            val debugEventProps = mutableMapOf<String, Any>()
            debugData?.let { debugEventProps.putAll(it)}
            data?.let { debugEventProps.putAll(it)}
            debugEventProps["msg_t"] = key
            debugEventProps["lt"] = com.vwo.enums.LogLevelEnum.ERROR.name
            debugEventProps["cg"] = DebuggerCategoryEnum.ERROR.key
            debugEventProps["msg"] = message ?: ""

            sendDebugEventToVWO(debugEventProps)
        }
    }

    companion object {
        @JvmStatic
        var instance: LogManager? = null
            private set
    }
}
