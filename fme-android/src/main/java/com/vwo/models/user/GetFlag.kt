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

import com.vwo.constants.Constants
import com.vwo.constants.Constants.defaultString
import com.vwo.enums.VariableTypeEnum
import com.vwo.models.Variable
import org.json.JSONObject

/**
 * Represents a feature flag and its associated variables.
 *
 * This class encapsulates information about a feature flag, including its enabled status and a
 * list of variables with their values.
 */
class GetFlag(val context: VWOContext) {
    private var isEnabled: Boolean = false

    private var variables: List<Variable> = ArrayList<Variable>()

    fun isEnabled() = isEnabled

    fun setIsEnabled(value: Boolean) {
        isEnabled = value
    }

    /**
     * Sets the variables for the feature flag.
     *
     * @param variables The list of variables to associate with the feature flag.
     */
    fun setVariables(variables: List<Variable>) {
        this.variables = variables
    }

    val variablesValue: List<Variable>
        get() = variables

    /**
     * Retrieves the value of a specific variable by its key.
     *
     * @param key The key of the variable to retrieve.
     * @param defaultValue The default value to return if the variable is not found or its value is null.
     * @return The value of the variable if found, otherwise the default value.
     */
    fun getVariable(key: String?, defaultValue: Any): Any {
        for (variable in variablesValue) {
            if (variable.key.equals(key)) {
                if (variable.type.equals(VariableTypeEnum.RECOMMENDATION.value,true)) {
                    // Return a Recommendation Object if type is RECOMMENDATION
                    val value = variable.value.toString().toIntOrNull()?:0
                    return Recommendation(value, context)
                }

                return variable.value?:defaultValue
            }
        }
        return defaultValue
    }

    fun getRecommendationDisplayConfig(key: String?): Map<String, Any>? {
        for (variable in variablesValue) {
            if (variable.key.equals(key)) {
                if (variable.type.equals(VariableTypeEnum.RECOMMENDATION.value, true)) {
                    // Return a Recommendation Object if type is RECOMMENDATION
                    return variable.displayConfiguration as? Map<String, Any>
                }
            }
        }
        return null
    }

    /**
     * Retrieves the list of variables as a list of maps.
     *
     * @return The list of variables, where each variable is represented as a map with keys "key", "value", "type", and "id".
     */
    fun getVariables(): List<Map<String, Any>> {
        val result: MutableList<Map<String, Any>> = ArrayList()
        for (variable in variablesValue) {
            // Check if the variable's type is not "recommendation"
            if (!variable.type.equals(VariableTypeEnum.RECOMMENDATION.value,true)) {
                result.add(convertVariableModelToMap(variable))
            }
        }
        return result
    }

    /**
     * Converts a Variable object to a map representation.
     *
     * @param variableModel The Variable object to convert.
     * @return A map representation of the Variable object.
     */
    private fun convertVariableModelToMap(variableModel: Variable): Map<String, Any> {
        val map: MutableMap<String, Any> = HashMap()
        map["key"] = variableModel.key?:defaultString
        map["value"] = variableModel.value?:defaultString
        map["type"] = variableModel.type?:defaultString
        map["id"] = variableModel.id?:0
        return map
    }
}
