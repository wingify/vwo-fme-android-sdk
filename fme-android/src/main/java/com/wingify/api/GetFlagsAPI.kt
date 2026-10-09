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
package com.wingify.api

import com.vwo.models.user.GetFlag
import com.wingify.ServiceContainer
import com.wingify.constants.Constants
import com.wingify.models.Settings
import com.wingify.models.user.FlagCollection
import com.wingify.models.user.WingifyUserContext
import com.wingify.packages.network_layer.manager.BatchManager
import com.wingify.packages.segmentation_evaluator.core.SegmentationManager
import com.wingify.utils.MegGroupPartitionUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Evaluates multiple feature flags in parallel for [com.wingify.WingifyClient.getFlags].
 *
 * Flags that share a MEG group are evaluated sequentially within a batch; independent
 * flags run concurrently up to [Constants.MAX_CONCURRENT_FLAG_EVALUATIONS].
 */
object GetFlagsAPI {

    /**
     * Evaluates the requested feature flags and returns them as a [FlagCollection].
     *
     * @param flagNames Feature keys to evaluate, or null/empty to evaluate all flags from [settings].
     * @param settings Account settings used for evaluation.
     * @param context User context (resolved once; each flag gets an isolated copy).
     * @param serviceContainer Shared services for logging, storage, and batch uploads.
     * @return [FlagCollection] keyed by feature key.
     */
    fun getFlags(
        flagNames: Array<String>?,
        settings: Settings,
        context: WingifyUserContext,
        serviceContainer: ServiceContainer,
    ): FlagCollection {
        val keys = resolveFlagKeys(flagNames, settings)
        if (keys.isEmpty()) {
            return FlagCollection.create(emptyMap(), context)
        }

        val batches = MegGroupPartitionUtil.getFeatureFlagBatches(keys, settings)
        val results = mutableMapOf<String, GetFlag>()

        serviceContainer.beginDeferImmediateBatchUpload()
        try {
            runBlocking(Dispatchers.Default) {
                val semaphore = Semaphore(Constants.MAX_CONCURRENT_FLAG_EVALUATIONS)
                batches.map { batch ->
                    async {
                        semaphore.withPermit {
                            evaluateBatch(
                                batch,
                                settings,
                                context,
                                serviceContainer,
                                results,
                            )
                        }
                    }
                }.awaitAll()
            }
        } finally {
            // Flush only when the outermost nested defer ends so concurrent getFlags
            // calls cannot re-enable mid-batch uploads for each other.
            val shouldFlush = serviceContainer.endDeferImmediateBatchUpload()
            if (shouldFlush && serviceContainer.onlineBatchUploadManager.isBatchingDisabled()) {
                CoroutineScope(Dispatchers.IO).launch {
                    BatchManager.start("getFlags batch", serviceContainer)
                }
            }
        }

        return FlagCollection.create(results.toMap(), context)
    }

    /**
     * Resolves the distinct feature keys to evaluate.
     * When [flagNames] is null or empty, all feature keys from [settings] are used.
     */
    private fun resolveFlagKeys(flagNames: Array<String>?, settings: Settings): List<String> {
        return when {
            flagNames.isNullOrEmpty() -> settings.features.mapNotNull { it.key }.distinct()
            else -> flagNames.toList().distinct()
        }
    }

    /**
     * Evaluates every key in [batch] sequentially and writes results into [results].
     */
    private fun evaluateBatch(
        batch: MegGroupPartitionUtil.EvaluationBatch,
        settings: Settings,
        baseContext: WingifyUserContext,
        serviceContainer: ServiceContainer,
        results: MutableMap<String, GetFlag>,
    ) {
        for (key in batch.keys) {
            val flag = evaluateSingleFlag(key, settings, baseContext, serviceContainer)
            synchronized(results) {
                results[key] = flag
            }
        }
    }

    /**
     * Evaluates a single feature flag on a cloned context with a thread-local segmentation manager.
     */
    private fun evaluateSingleFlag(
        featureKey: String,
        settings: Settings,
        baseContext: WingifyUserContext,
        serviceContainer: ServiceContainer,
    ): GetFlag {
        val clonedContext = baseContext.clone()
        val evalSegmentationManager = SegmentationManager().apply {
            attachEvaluator(serviceContainer)
        }
        serviceContainer.setEvalSegmentationManager(evalSegmentationManager)
        return try {
            GetFlagAPI.getFlag(
                featureKey,
                settings,
                clonedContext,
                serviceContainer,
                serviceContainer.getHooksManager(),
            )
        } finally {
            serviceContainer.clearEvalSegmentationManager()
        }
    }
}
