/*
 * Copyright (c) 2024 Wingify Software Pvt. Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vwo.packages.network_layer.manager

import SdkDataManager
import com.vwo.VWOClient
import com.vwo.models.OfflineEventData
import com.vwo.services.BatchUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A manager class responsible for handling batch operations foroffline events.
 *
 * This object manages the process of sending batches of offline events to the server.
 * It retrieves event data from the database, groups it into batches, and uploads them using the `BatchUploader`.
 * It also handles synchronization to ensure that only one batch upload operation is in progress ata time.
 */
internal object BatchManager {

    /**
     * The data manager used to access and manipulate offline event data in the database.
     */
    var sdkDataManager: SdkDataManager? = null

    private val batchUploader = BatchUploader()
    private val mutex = Mutex()

    /**
     * Starts the batch upload process.
     *
     * This function initiates the process of sending batches of offline events to the server.
     * It acquires a lock using a `Mutex` to ensure that only one batch upload operation is in progress at a time.* It then calls the `sendBatches()` function to perform the actual upload.
     *
     * @return `true` if all batches were uploaded successfully, `false` otherwise.
     */
    suspend fun start(): Boolean {
        // When using mutex.withLock, the coroutine is suspended until the lock is available, and
        // the thread remains free to execute other coroutines. This leads to better scalability in
        // coroutine-based programs.
        mutex.withLock {
            return sendBatches()
        }
    }

    /**
     * Sends batches of offline events to the server.
     *
     * This function retrieves event data from the database, groups it into batches, and uploads them using the `BatchUploader`.
     * It runs on the IO dispatcher to avoid blocking the main thread.
     *
     * @return `true` if all batches were uploaded successfully, `false` otherwise.
     */
    private suspend fun sendBatches(): Boolean = withContext(Dispatchers.IO) {
        var result = true
        try {

            val batches = getData()
            batches.forEach { batch ->
                //Upload one batch at a time, and wait till it finishes, then start second batch
                val firstItem = batch.first()
                val values = batch.map {
                    VWOClient.objectMapper.readValue(it.payload, MutableMap::class.java)
                }
                val isUploaded =
                    batchUploader.uploadBatch(firstItem.accountId, firstItem.sdkKey, values)
                if (isUploaded) {
                    removeStoredData(batch)
                }
                result = isUploaded && result
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result = false
        }
        result
    }

    /**
     * Removes stored data for a batch of events from the database.
     *
     * This function iterates through a list of `OfflineEventData` objects and deletes each event from the database.
     *
     * @param batch The list of `OfflineEventData` objects to remove.
     */
    private fun removeStoredData(batch: List<OfflineEventData>) {
        batch.forEach {
            val isDeleted = sdkDataManager?.deleteData(it.id)
        }
    }

    /**
     * Retrieves data for batches of offline events from the database.
     *
     * This function retrieves distinct SDK keys and account IDs from the database and then fetches all events associated with each combination.
     * It groups the events into batches based on their SDK key and account ID.
     *
     * @return A mutable list of lists of `OfflineEventData` objects, representing the batches of events.
     */
    private fun getData(): MutableList<List<OfflineEventData>> {
        val result = mutableListOf<List<OfflineEventData>>()
        sdkDataManager?.getDistinctSdkKeys()?.forEach {
            val batch = sdkDataManager?.getSdkData(it.accountId, it.sdkKey)
            if (batch?.isNotEmpty() == true) {
                result.add(batch)
            } else {
                println("No data found for ${it.accountId} and ${it.sdkKey}")
            }
        }
        return result
    }
}