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

package com.vwo.services

import SdkDataManager
import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.vwo.constants.Constants.BATCH_UPLOADER_INITIAL_DELAY
import com.vwo.constants.Constants.BATCH_UPLOADER_RETRY_INTERVAL
import com.vwo.packages.network_layer.manager.BatchManager
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.providers.StorageProvider
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * A class responsible for scheduling periodic data uploads using WorkManager.
 */
class PeriodicDataUploader {

    private val workName = "VwoUploader"

    /**
     * Enqueues a unique work request for data upload.
     *
     * If a job with the same name is already in the queue, the existing job is kept,
     * and a new job is not added.
     *
     * @param context The application context.
     */
    fun enqueue(context: Context) {
        val job = createJob()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, job)
    }

    /**
     * Creates a [OneTimeWorkRequest] for data upload with network constraints and backoff criteria.
     *
     * @return The created [OneTimeWorkRequest].
     */
    private fun createJob(): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setInitialDelay(BATCH_UPLOADER_INITIAL_DELAY, TimeUnit.MINUTES)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                BATCH_UPLOADER_RETRY_INTERVAL,
                TimeUnit.MINUTES
            )
            .build()
        return uploadRequest
    }

    /**
     * Checks if the data upload work is finished.
     *
     * @param context The application context.
     * @return `true` if the work is finished, `false` otherwise.
     */
    fun isFinished(context: Context): Boolean {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(workName)
            .isDone
    }
}

/**
 * A [Worker] that performs the actual data upload task.
 */
class UploadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    /**
     * Performs the data upload work.
     *
     * @return [Result.success] if the upload was successful, [Result.retry] if it should be retried,
     * or [Result.failure] if it failed permanently.
     */
    override suspend fun doWork(): Result {
        try {
            if (StorageProvider.contextRef.get() == null)
                StorageProvider.contextRef = WeakReference(applicationContext)
            if (StorageProvider.sdkDataManager == null)
                StorageProvider.sdkDataManager = SdkDataManager(applicationContext)
            if (BatchManager.sdkDataManager == null)
                BatchManager.sdkDataManager = StorageProvider.sdkDataManager
            if (NetworkManager.config == null || NetworkManager.client == null)
                NetworkManager.attachClient()

            // This delay is added for the case below:
            // Sometimes work gets triggered as soon as wifi icon is tapped. If device was
            // disconnected earlier, it can not communicate immediately as WIFI or mobile data is
            // still unstable. So the work is retried and delayed by few more minutes until OS
            // triggers it again. Adding delay will give some time to network to get it stabilized
            // and then our work will start.
            val delayToWaitForNetwork: Long = 10 * 1000
            delay(delayToWaitForNetwork)
            val isFinished = BatchManager.start()
            if (!isFinished) return Result.retry()
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}