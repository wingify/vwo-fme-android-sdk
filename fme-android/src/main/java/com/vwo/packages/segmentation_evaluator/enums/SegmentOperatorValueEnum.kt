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
package com.vwo.packages.segmentation_evaluator.enums

/**
 * Values for segment operators.
 *
 * This enum defines string values associated with different types of segment operators. These values can beused for identifying and processing specific operators within segmentation rules.
 */
enum class SegmentOperatorValueEnum(val value: String) {
    AND("and"),
    NOT("not"),
    OR("or"),
    CUSTOM_VARIABLE("custom_variable"),
    USER("user"),
    COUNTRY("country"),
    REGION("region"),
    CITY("city"),
    OPERATING_SYSTEM("os"),
    DEVICE_TYPE("device_type"),
    BROWSER_AGENT("browser_string"),
    UA("ua"),
    DEVICE("device"),
    FEATURE_ID("featureId");

    companion object {
        /**
         * Retrieves the enum constant for the given value.
         *
         * @param value The string value representing the operator.
         * @return The corresponding enum constant.
         * @throws IllegalArgumentException if no enum constant with the given value exists.
         */
        fun fromValue(value: String): SegmentOperatorValueEnum {
            for (operator in values()) {
                if (operator.value == value) {
                    return operator
                }
            }
            throw IllegalArgumentException("No enum constant with value $value")
        }
    }
}