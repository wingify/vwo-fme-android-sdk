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
package com.vwo.models.user

import com.vwo.models.Variable

class GetFlag {
    var isEnabled: Boolean = false
    private var variables: List<com.vwo.models.Variable> = ArrayList<com.vwo.models.Variable>()

    fun setVariables(variables: List<com.vwo.models.Variable>) {
        this.variables = variables
    }

    val variablesValue: List<Any>
        get() = variables

    // get specific value from variables given key
    fun getVariable(key: String?, defaultValue: Any): Any {
        for (variable in variablesValue) {
            if (variable.getKey().equals(key)) {
                return variable.getValue()
            }
        }
        return defaultValue
    }

    fun getVariables(): List<Map<String, Any>> {
        val result: MutableList<Map<String, Any>> = ArrayList()
        for (variable in variablesValue) {
            result.add(convertVariableModelToMap(variable))
        }
        return result
    }

    private fun convertVariableModelToMap(variableModel: com.vwo.models.Variable): Map<String, Any> {
        val map: MutableMap<String, Any> = HashMap()
        map["key"] = variableModel.getKey()
        map["value"] = variableModel.getValue()
        map["type"] = variableModel.getType()
        map["id"] = variableModel.getId()
        return map
    }
}
