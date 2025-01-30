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

import com.vwo.constants.Constants.DEFAULT_BATCH_UPLOAD_INTERVAL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages the online batch upload process.
 * This object is responsible for periodically triggering the batch upload of events that have been stored offline.
 * It uses a coroutine to run the upload process in the background at a configurable interval.
 */
object OnlineBatchUploadManager {

    /**
     * The time interval, in milliseconds,between batch uploads.
     *
     * A value of -1 indicates that the default interval should be used.
     */
    var batchUploadTimeInterval: Long = -1L

    /**
     * The minimum number of events required to trigger a batch upload.
     *
     * A value of -1 indicates that there is no minimum size requirement.
     */
    var batchMinSize: Int = -1

    /**
     * Starts the online batch uploader.
     *
     * This function launches a coroutine that periodically triggers the batch upload process.
     * The interval between uploads is determined by the `batchUploadTimeInterval` property, or the
     * default interval if it is not set.
     */
    fun startBatchUploader() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val timeDelay = if (batchUploadTimeInterval > 0)
                    batchUploadTimeInterval
                else
                    DEFAULT_BATCH_UPLOAD_INTERVAL

                delay(timeDelay)
                BatchManager.start("Online time based batch uploader")
            }
        }
    }
}