package com.vwo

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