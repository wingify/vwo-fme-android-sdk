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
package com.vwo.utils

import java.util.Date
import java.util.function.Function

object DataTypeUtil {
    fun isObject(`val`: Any?): Boolean {
        // return val != null && !(val instanceof Object[]) && !(val instanceof Function) && !(val instanceof String) && !(val instanceof RegExp) && !(val instanceof Promise) && !(val instanceof Date);
        return `val` != null && !(`val` is Array<*> && `val`.isArrayOf<Any>()) && `val` !is Function<*, *> && `val` !is String && `val` !is Date
    }

    fun isArray(`val`: Any?): Boolean {
        return `val` is Array<*> && `val`.isArrayOf<Any>()
    }

    fun isNull(`val`: Any?): Boolean {
        return `val` == null
    }

    fun isUndefined(`val`: Any?): Boolean {
        return `val` == null
    }

    fun isDefined(`val`: Any?): Boolean {
        return `val` != null
    }

    fun isNumber(`val`: Any?): Boolean {
        return `val` is Number
    }

    fun isInteger(`val`: Any?): Boolean {
        return `val` is Int
    }

    fun isString(`val`: Any?): Boolean {
        return `val` is String
    }

    fun isBoolean(`val`: Any?): Boolean {
        return `val` is Boolean
    }

    fun isNaN(`val`: Any?): Boolean {
        return `val` is Double && `val`.isNaN()
    }

    fun isDate(`val`: Any?): Boolean {
        return `val` is Date
    }

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
}
