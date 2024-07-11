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

import com.vwo.interfaces.logger.ILogManager
import com.vwo.interfaces.logger.LogTransport
import com.vwo.packages.logger.Logger
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.logger.transports.ConsoleTransport
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class LogManager(override val config: Map<String, Any>) : Logger(), ILogManager {
    override val transportManager: LogTransportManager = LogTransportManager(config)
    override val name: String = config.getOrDefault("name", "VWO Logger") as String
    override val requestId: String = UUID.randomUUID().toString()
    override val level: LogLevelEnum =
        LogLevelEnum.valueOf(
            config.getOrDefault("level", LogLevelEnum.ERROR.name).toString().uppercase(
                Locale.getDefault()
            )
        )
    override val prefix: String = config.getOrDefault("prefix", "VWO-SDK") as String
    override val dateTimeFormat: SimpleDateFormat =
        SimpleDateFormat(
            config.getOrDefault(
                "dateTimeFormat",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
            ) as String
        )
    override val transports: List<Map<String, Any>> = ArrayList()

    init {
        handleTransports()
        instance = this
    }

    private fun handleTransports() {
        val transportList = config["transports"] as List<Map<String?, Any?>?>?
        if (transportList != null && !transportList.isEmpty()) {
            addTransports(transportList)
        } else {
            val defaultTransport = ConsoleTransport(level)
            val defaultTransportMap: MutableMap<String?, Any?> = HashMap()
            defaultTransportMap["defaultTransport"] = defaultTransport
            addTransport(defaultTransportMap)
        }
    }

    override fun addTransport(transport: Map<String?, Any?>?) {
        transportManager.addTransport(transport!!["defaultTransport"] as LogTransport?)
    }

    override fun addTransports(transportList: List<Map<String?, Any?>?>?) {
        for (transport in transportList!!) {
            addTransport(transport)
        }
    }

    override fun getDateTimeFormat(): String {
        return dateTimeFormat.toPattern()
    }

    override val transport: Map<String, Any>?
        get() =// This method needs more context, currently returning null.
            null

    override fun trace(message: String?) {
        transportManager.trace(message)
    }

    override fun debug(message: String?) {
        transportManager.debug(message)
    }

    override fun info(message: String?) {
        transportManager.info(message)
    }

    override fun warn(message: String?) {
        transportManager.warn(message)
    }

    override fun error(message: String?) {
        transportManager.error(message)
    }

    companion object {
        @JvmStatic
        var instance: LogManager? = null
            private set
    }
}
