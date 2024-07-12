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
package com.vwo.constants

object Constants {
    const val PLATFORM: String = "server"

    const val MAX_TRAFFIC_PERCENT: Int = 100
    const val MAX_TRAFFIC_VALUE: Int = 10000
    const val STATUS_RUNNING: String = "RUNNING"

    const val SEED_VALUE: Int = 1
    const val MAX_EVENTS_PER_REQUEST: Int = 5000
    const val DEFAULT_REQUEST_TIME_INTERVAL: Long = 600 // 10 * 60(secs) = 600 secs i.e. 10 minutes
    const val DEFAULT_EVENTS_PER_REQUEST: Int = 100
    const val SDK_NAME: String = "vwo-fme-android-sdk"
    const val SETTINGS_EXPIRY: Long = 10000000
    const val SETTINGS_TIMEOUT: Long = 50000

    const val HOST_NAME: String = "dev.visualwebsiteoptimizer.com"
    const val SETTINGS_ENDPOINT: String = "/server-side/v2-settings"

    const val VWO_FS_ENVIRONMENT: String = "vwo_fs_environment"
    const val HTTPS_PROTOCOL: String = "https"

    const val RANDOM_ALGO: Int = 1
}