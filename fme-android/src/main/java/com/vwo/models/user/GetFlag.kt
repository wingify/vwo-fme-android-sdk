/**
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
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

import com.wingify.constants.Constants.defaultString
import com.wingify.enums.VariableTypeEnum
import com.wingify.internal.toVwoUserContext
import com.wingify.models.Variable
import com.wingify.models.impression.ImpressionPayload
import com.wingify.models.user.WingifyUserContext
import com.wingify.utils.ImpressionExposureHelper

/**
 * Represents a feature flag and its associated variables.
 *
 * This class encapsulates information about a feature flag, including its enabled status and a
 * list of variables with their values.
 *
 * When evaluation used `fetchFlag` / `fetchFlags`, call [exposeResult] to enqueue the deferred
 * `vwo_variationShown` impression.
 * A later default [com.wingify.WingifyClient.getFlag] / [com.wingify.WingifyClient.getFlags]
 * does **not** auto-send when storage has an explicit `variationShownSent=false`; use
 * [exposeResult] instead.
 */
class GetFlag(context: WingifyUserContext) {
    val context: VWOUserContext = context.toVwoUserContext()
    private var isEnabled: Boolean = false

    private var variables: List<Variable> = ArrayList<Variable>()

    private val pendingImpressions = ImpressionPayload()
    private var exposeDeps: ImpressionExposureHelper.ExposeDeps? = null
    private var impressionAlreadySent: Boolean = false

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
     * Enqueues a deferred `vwo_variationShown` impression for this flag result.
     *
     * No-op when the impression was already sent, or when there is nothing to expose
     * (empty stash and nothing rebuildable from storage + impact).
     *
     * Default [com.wingify.WingifyClient.getFlag] does not auto-expose after a prior
     * `fetchFlag`/`fetchFlags` evaluation that left
     * `variationShownSent=false` in storage — call this explicitly.
     *
     * @return `true` when an impression was enqueued.
     */
    fun exposeResult(): Boolean = ImpressionExposureHelper.expose(this)

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

    /**
     * Stores dependencies so [exposeResult] can rebuild a deferred variationShown later.
     *
     * Snapshots [ImpressionExposureHelper.ExposeDeps.isEnabled] from this flag, ignoring
     * the value on [deps], so the impact variation matches the decision already returned.
     *
     * @param deps Settings, feature key, user, and services from the evaluation.
     */
    internal fun attachExposeDeps(deps: ImpressionExposureHelper.ExposeDeps) {
        exposeDeps = deps.copy(isEnabled = isEnabled)
    }

    /** Dependencies captured for a later [exposeResult], or null when none were attached. */
    internal fun getExposeDeps(): ImpressionExposureHelper.ExposeDeps? = exposeDeps

    /**
     * Merges [payload] into the deferred stash. Duplicate campaign and variation pairs are dropped.
     */
    internal fun mergePendingImpressions(payload: ImpressionPayload) {
        for (index in 0 until payload.size()) {
            val impression = payload.get(index)
            pendingImpressions.add(
                campaignId = impression.campaignId,
                variationId = impression.variationId,
                featureId = impression.featureId,
            )
        }
    }

    /** Copies the deferred stash and clears it. */
    internal fun consumePendingImpressions(): ImpressionPayload {
        val copy = ImpressionPayload()
        for (index in 0 until pendingImpressions.size()) {
            val impression = pendingImpressions.get(index)
            copy.add(
                campaignId = impression.campaignId,
                variationId = impression.variationId,
                featureId = impression.featureId,
            )
        }
        pendingImpressions.impressionList.clear()
        return copy
    }

    /** Marks variationShown as sent for this result and drops the deferred stash. */
    internal fun markImpressionSent() {
        impressionAlreadySent = true
        pendingImpressions.impressionList.clear()
    }

    /** Whether variationShown was already enqueued for this result. */
    internal fun isImpressionAlreadySent(): Boolean = impressionAlreadySent

    /** Whether the deferred stash still holds impressions for [exposeResult]. */
    internal fun hasPendingImpressions(): Boolean = pendingImpressions.hasValidData()
}
