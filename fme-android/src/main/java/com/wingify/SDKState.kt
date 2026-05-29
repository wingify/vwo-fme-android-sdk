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
package com.wingify

/**
 * Represents the current initialization state of the VWO SDK.
 * 
 * This enum is used to track and manage the lifecycle of the SDK initialization process,
 * preventing multiple concurrent initialization attempts and ensuring proper state management. 
 * Required to ensure only one instance is initialized at any given time.
 */
enum class SDKState(val message: String) {
    
    /**
     * SDK has not been initialized yet or initialization has failed.
     * VWO functionality is not available when the SDK is in this state.
     */
    NOT_INITIALIZED("VWO is not initialized."),
    
    /**
     * SDK initialization is currently in progress.
     * This state prevents multiple concurrent initialization attempts.
     */
    INITIALIZING("VWO is already initializing."),
    
    /**
     * SDK has been successfully initialized and is ready for use.
     * All VWO functionality is available when the SDK is in this state.
     */
    INITIALIZED("VWO has already initialized."),
}