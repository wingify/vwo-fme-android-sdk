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
package com.vwo.constants

object Constants {
    const val defaultString: String = ""
    const val PLATFORM: String = "android"

    const val MAX_TRAFFIC_PERCENT: Int = 100
    const val MAX_TRAFFIC_VALUE: Int = 10000
    const val STATUS_RUNNING: String = "RUNNING"

    const val SEED_VALUE: Int = 1
    const val MAX_EVENTS_PER_REQUEST: Int = 5000
    const val DEFAULT_REQUEST_TIME_INTERVAL: Long = 600 // 10 * 60(secs) = 600 secs i.e. 10 minutes
    const val DEFAULT_EVENTS_PER_REQUEST: Int = 100
    const val SDK_NAME: String = "vwo-fme-android-sdk"
    const val SETTINGS_TIMEOUT: Long = 50000

    const val HOST_NAME: String = "dev.visualwebsiteoptimizer.com"
    const val SETTINGS_ENDPOINT: String = "/server-side/v2-settings"
    const val EVENT_BATCH_ENDPOINT: String = "/server-side/batch-events-v2"

    const val VWO_FS_ENVIRONMENT: String = "vwo_fs_environment"
    const val HTTPS_PROTOCOL: String = "https"
    const val VWO_META_MEG_KEY: String = "_vwo_meta_meg_"

    const val VWO_APP_URL: String = "app.vwo.com"

    const val RANDOM_ALGO: Int = 1

    const val AUTH_TOKEN = ""
    const val RETRY_DELAY = 1000L
    const val MAX_RETRY_ATTEMPTS = 3

    const val BATCH_UPLOADER_INITIAL_DELAY = 1L //minutes
    const val BATCH_UPLOADER_RETRY_INTERVAL = 3L //minutes

    const val DEFAULT_BATCH_UPLOAD_INTERVAL = 3 * 60 * 1000L //3 minutes in milliseconds
}
