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
package com.vwo.utils

import com.vwo.utils.NetworkUtil.Companion
import org.json.JSONObject
import java.util.Date
import java.util.function.Function

/**
 * Utility object for data type operations.
 *
 * This object provides helper methods for checking and determining the type of various data values. It offers functions to identify objects, arrays, null values, undefined values, numbers, strings, booleans, dates, functions, and more.
 */
object DataTypeUtil {
    /**
     * Checks if a value is an object.
     *
     * @param `val` The value to check.
     * @return `true` if the value is an object, `false` otherwise.
     */
    fun isObject(`val`: Any?): Boolean {
        // return val != null && !(val instanceof Object[]) && !(val instanceof Function) && !(val instanceof String) && !(val instanceof RegExp) && !(val instanceof Promise) && !(val instanceof Date);
        return `val` != null && !(`val` is Array<*> && `val`.isArrayOf<Any>()) && `val` !is Function<*, *> && `val` !is String && `val` !is Date
    }

    /**
     * Checks if a value is an array.
     *
     * @param `val` The value to check.
     * @return `true` if the value is an array, `false` otherwise.
     */
    fun isArray(`val`: Any?): Boolean {
        return `val` is Array<*> && `val`.isArrayOf<Any>()
    }

    /**
     * Checks if a value is null.
     *
     * @param `val` The value to check.
     * @return `true` if the value is null, `false` otherwise.
     */
    fun isNull(`val`: Any?): Boolean {
        return `val` == null
    }

    /**
     * Checks if a value is undefined.
     *
     * @param `val` The value to check.
     * @return `true` if the value is undefined, `false` otherwise.
     */
    fun isUndefined(`val`: Any?): Boolean {
        return `val` == null
    }

    /**
     * Checks if a value is defined.
     *
     * @param `val` The value to check.
     * @return `true` if the value is defined, `false` otherwise.
     */
    fun isDefined(`val`: Any?): Boolean {
        return `val` != null
    }

    /**
     * Checks if a value is a number.
     *
     * @param `val` The value to check.
     * @return `true` if the value is a number, `false` otherwise.
     */
    fun isNumber(`val`: Any?): Boolean {
        return `val` is Number
    }

    /**
     * Checks if a value is an integer.
     *
     * @param `val` The value to check.
     * @return `true` if the value is an integer, `false` otherwise.
     */
    @JvmStatic
    fun isInteger(`val`: Any?): Boolean {
        return `val` is Int
    }

    /**
     * Checks if a value is a string.
     *
     * @param `val` The value to check.
     * @return `true` if the value is a string, `false` otherwise.
     */
    @JvmStatic
    fun isString(`val`: Any?): Boolean {
        return `val` is String
    }

    /**
     * Checks if a value is a boolean.
     *
     * @param `val` The value to check.
     * @return `true` if the value is a boolean, `false` otherwise.
     */
    fun isBoolean(`val`: Any?): Boolean {
        return `val` is Boolean
    }

    /**
     * Checks if a value is NaN (Not a Number).
     *
     * @param `val` The value to check.
     * @return `true` if the value is NaN, `false` otherwise.
     */
    fun isNaN(`val`: Any?): Boolean {
        return `val` is Double && `val`.isNaN()
    }

    /**
     * Checks if a value is a date.
     *
     * @param `val` The value to check.
     * @return `true` if the value is a date, `false` otherwise.
     */
    fun isDate(`val`: Any?): Boolean {
        return `val` is Date
    }

    /**
     * Checks if a value is a function.
     *
     * @param `val` The value to check.
     * @return `true` if the value is a function, `false` otherwise.
     */
    fun isFunction(`val`: Any?): Boolean {
        return `val` is Function<*, *>
    }

    /* public static boolean isRegex(Object val) {
        return val instanceof RegExp;
    }

    public static boolean isPromise(Object val) {
        return val instanceof Promise;
    }
    */

    /**
     * Gets the type of a value as a string.
     *
     * @param `val` The value to check.
     * @return The type of the value as a string.
     */
    @JvmStatic
    fun getType(`val`: Any?): String {
        return if (isObject(`val`)) {
            "Object"
        } else if (isArray(`val`)) {
            "Array"
        } else if (isNull(`val`)) {
            "Null"
        } else if (isUndefined(`val`)) {
            "Undefined"
        } else if (isNaN(`val`)) {
            "NaN"
        } else if (isNumber(`val`)) {
            "Number"
        } else if (isString(`val`)) {
            "String"
        } else if (isBoolean(`val`)) {
            "Boolean"
        } else if (isDate(`val`)) {
            "Date"
            // } else if (isRegex(val)) {
            // return "Regex";
        } else if (isFunction(`val`)) {
            "Function"
            // } else if (isPromise(val)) {
            // return "Promise";
        } else if (isInteger(`val`)) {
            "Integer"
        } else {
            "Unknown Type"
        }
    }

    /**
     * Filters a map to include only string key-value pairs.
     *
     * @param originalMap The original map to filter.
     * @return A new map containing only string key-value pairs.
     */
    fun filterStringMap(originalMap: Map<*, *>): Map<String, String> {
        val cleanedMap: MutableMap<String, String> = mutableMapOf()

        for (entry in originalMap.entries) {
            var value = entry.value
            if (value is Map<*, *>) {
                // Recursively remove null values from nested maps
                value = NetworkUtil.removeNullValues(value)
            }
            if (value != null && entry.key is String && value is String) {
                cleanedMap[entry.key as String] = value
            }
        }

        return cleanedMap
    }
}

fun JSONObject.toMap(): MutableMap<String, Any> {
    val map = mutableMapOf<String, Any>()
    val keys = this.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = this.get(key)
        map[key] = value
    }
    return map
}