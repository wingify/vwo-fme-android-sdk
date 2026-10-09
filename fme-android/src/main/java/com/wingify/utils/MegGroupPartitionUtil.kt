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
package com.wingify.utils

import com.wingify.models.Settings

/**
 * Partitions requested feature keys into evaluation batches for getFlags().
 * Flags sharing a MEG group are batched together for sequential evaluation;
 * independent flags are evaluated as single-key batches eligible for parallelism.
 */
object MegGroupPartitionUtil {

    /**
     * A set of feature keys that must be evaluated sequentially within one worker.
     *
     * @property keys Feature keys belonging to this batch (MEG-linked or a single independent key).
     */
    data class EvaluationBatch(val keys: List<String>)

    /**
     * Partitions [requestedKeys] into evaluation batches for parallel getFlags().
     *
     * @param requestedKeys Distinct feature keys requested by the caller.
     * @param settings Account settings used to resolve MEG group membership.
     * @return Batches where MEG-linked keys share a batch and independent keys are alone.
     */
    fun getFeatureFlagBatches(
        requestedKeys: List<String>,
        settings: Settings
    ): List<EvaluationBatch> {
        if (requestedKeys.isEmpty()) return emptyList()

        val batches = mutableListOf<EvaluationBatch>()
        val assigned = mutableSetOf<String>()
        val megUtil = MegUtil()

        for (key in requestedKeys) {
            if (key in assigned) continue

            val groups = CampaignUtil.findGroupsFeaturePartOf(settings, key)
            val groupId = groups.firstOrNull()?.get("groupId")?.toIntOrNull()

            if (groupId != null) {
                @Suppress("UNCHECKED_CAST")
                val megFeatureKeys =
                    megUtil.getFeatureKeysFromGroup(settings, groupId)["featureKeys"]
                            as? List<String>
                        ?: emptyList()
                val keysInBatch = requestedKeys.filter { it in megFeatureKeys && it !in assigned }
                // MEG group lookup can miss the current key (empty/mismatched featureKeys).
                // Always evaluate at least this key so it is never silently dropped.
                val batchKeys = if (keysInBatch.isNotEmpty()) keysInBatch else listOf(key)
                batches.add(EvaluationBatch(batchKeys))
                assigned.addAll(batchKeys)
            } else {
                batches.add(EvaluationBatch(listOf(key)))
                assigned.add(key)
            }
        }

        return batches
    }
}
