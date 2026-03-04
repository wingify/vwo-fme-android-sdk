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
    const val PLATFORM: String = "Android"

    const val MAX_TRAFFIC_PERCENT: Int = 100
    const val MAX_TRAFFIC_VALUE: Int = 10000
    const val STATUS_RUNNING: String = "RUNNING"

    const val SEED_VALUE: Int = 1
    const val MAX_EVENTS_PER_REQUEST: Int = 5000
    const val DEFAULT_REQUEST_TIME_INTERVAL: Long = 600 // 10 * 60(secs) = 600 secs i.e. 10 minutes
    const val DEFAULT_EVENTS_PER_REQUEST: Int = 100
    const val SDK_NAME: String = "vwo-fme-android-sdk"
    const val PRODUCT_NAME: String = "fme"
    const val SETTINGS_TIMEOUT: Long = 50000

    const val HOST_NAME: String = "dev.visualwebsiteoptimizer.com"
    const val SETTINGS_ENDPOINT: String = "/server-side/v2-settings"
    const val EVENT_BATCH_ENDPOINT: String = "/server-side/batch-events-v2"

    const val VWO_FS_ENVIRONMENT: String = "vwo_fs_environment"
    const val HTTPS_PROTOCOL: String = "https"
    const val VWO_META_MEG_KEY: String = "_vwo_meta_meg_"
    const val VWO_META_HOLDOUT_KEY: String = "_vwo_meta_holdout_"

    const val VWO_APP_URL: String = "app.vwo.com"

    const val RANDOM_ALGO: Int = 1

    const val AUTH_TOKEN = ""
    const val RETRY_DELAY = 1000L
    const val MAX_RETRY_ATTEMPTS = 4

    const val BATCH_UPLOADER_INITIAL_DELAY = 1L //minutes
    const val BATCH_UPLOADER_RETRY_INTERVAL = 3L //minutes

    const val DEFAULT_BATCH_UPLOAD_INTERVAL = 3 * 60 * 1000L //3 minutes in milliseconds

    const val GATEWAY_USER_DATA_CACHE_DURATION = 60 * 60 * 1000L //1 hour in milliseconds
    const val GATEWAY_LIST_EVALUATION_CACHE_DURATION = 60 * 60 * 1000L //1 hour in milliseconds

    const val APP_VERSION = "vwo_av"
    const val OS_VERSION = "vwo_osv"
    const val MANUFACTURER = "vwo_mfr"
    const val DEVICE_MODEL = "vwo_dm"
    const val LOCALE = "vwo_loc"

    // Debugger constants
    const val V2_SETTINGS = "v2-settings"
    const val POLLING = "polling"
    const val MOBILE_STORAGE = "MobileDefaultStorage"
    const val FLAG_DECISION_GIVEN = "FLAG_DECISION_GIVEN"
    const val NETWORK_CALL_FAILURE_AFTER_MAX_RETRIES = "NETWORK_CALL_FAILURE_AFTER_MAX_RETRIES"
    const val NETWORK_CALL_SUCCESS_WITH_RETRIES = "NETWORK_CALL_SUCCESS_WITH_RETRIES"
    const val END_POINT = "endPoint"
    const val ERR = "err"
    const val FEATURE_KEY = "fk"

    const val VARIATION_KEY = "variationKey"
    const val USER_ID = "userId"
    const val KEY_EXPERIMENT_TYPE = "experimentType"
    const val KEY_EXPERIMENT_KEY = "experimentKey"

    const val IMPRESSION_NO_FEATURE_ID = -1

    const val SETTINGS_MAX_RETRY_ATTEMPTS = 1

    const val HTTP_STATUS_CODE_400 = 400

    const val HTTP_STATUS_CODE_401 = 401

    const val REGEX_REQUIRES_GATEWAY_SERVICE =
        "\\b(country|region|city|os|device_type|browser_string|ua)\\b"

    const val REGEX_SEGMENTATION_FULL =
        "$REGEX_REQUIRES_GATEWAY_SERVICE|\"custom_variable\"\\s*:\\s*\\{\\s*\"name\"\\s*:\\s*\"inlist\\([^)]*\\)\""

    const val KEY_DECISION_IS_USER_PART_OF_CAMPAIGN = "isUserPartOfCampaign"

    object Holdouts {

        const val VARIATION_IS_PART_OF_HOLDOUT = 1

        const val VARIATION_NOT_PART_OF_HOLDOUT = 2

        const val KEY_STORAGE_HOLDOUT_IDS = "holdoutIds"
        const val KEY_STORAGE_NOT_IN_HOLDOUT_IDS = "notInHoldoutIds"

    }

}
