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

package com.vwo.packages.logger

import com.vwo.packages.logger.enums.LogLevelEnum
import org.junit.Assert.*
import org.junit.Test
import java.lang.IllegalArgumentException

class LogLevelTest {
    @Test
    fun testLogLevelOrder() {
        // Test that log levels are ordered correctly
        assertTrue(LogLevelEnum.DEBUG.ordinal < LogLevelEnum.INFO.ordinal)
        assertTrue(LogLevelEnum.INFO.ordinal < LogLevelEnum.WARN.ordinal)
        assertTrue(LogLevelEnum.WARN.ordinal < LogLevelEnum.ERROR.ordinal)
    }

    @Test
    fun testLogLevelFromString() {
        assertEquals(LogLevelEnum.DEBUG, LogLevelEnum.valueOf("DEBUG"))
        assertEquals(LogLevelEnum.INFO, LogLevelEnum.valueOf("INFO"))
        assertEquals(LogLevelEnum.WARN, LogLevelEnum.valueOf("WARN"))
        assertEquals(LogLevelEnum.ERROR, LogLevelEnum.valueOf("ERROR"))

        // Test case insensitivity
        assertThrows(IllegalArgumentException::class.java) {
            LogLevelEnum.valueOf("debug")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LogLevelEnum.valueOf("info")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LogLevelEnum.valueOf("warn")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LogLevelEnum.valueOf("unknown")
        }
    }

    @Test
    fun testLogLevelToString() {
        assertEquals("DEBUG", LogLevelEnum.DEBUG.toString())
        assertEquals("INFO", LogLevelEnum.INFO.toString())
        assertEquals("WARN", LogLevelEnum.WARN.toString())
        assertEquals("ERROR", LogLevelEnum.ERROR.toString())
    }
}