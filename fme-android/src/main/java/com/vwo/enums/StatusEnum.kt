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
package com.vwo.enums

/**
 * Enumeration representing different status values.
 *
 * This enum defines constants for representing the status of an operation or process,
 * typically indicating success or failure. Each status is associated with a specific string value.
 */
enum class StatusEnum(val status: String) {
    /**
     * Status indicating successful completion.
     */
    PASSED("passed"),
    /**
     * Status indicating failure.
     */
    FAILED("failed")
}
