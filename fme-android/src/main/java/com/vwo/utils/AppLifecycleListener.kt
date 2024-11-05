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

package com.vwo.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.network_layer.client.ApiCallRepeater
import com.vwo.services.LoggerService

/**
 * A class that listens to the application's activity lifecycle events.*
 * This class implements the `Application.ActivityLifecycleCallbacks` interface
 * and provides methods to start and stop listening to activity lifecycle events.
 * It also keeps track of the number of active activities and triggers actions
 * based on the activity lifecycle state.
 */
class AppLifecycleListener : Application.ActivityLifecycleCallbacks {

    /**
     * The number of active activities.
     */
    private var activityCounter = 0

    /**
     * Starts listening to activity lifecycle events.
     *
     * @param application The application instance to register the lifecycle callbacks with.
     */
    fun start(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * Stops listening to activity lifecycle events.
     *
     * @param application The application instance to unregister the lifecycle callbacks from.
     */
    fun stop(application: Application) {
        application.unregisterActivityLifecycleCallbacks(this)
    }

    /**
     * Called when an activity is created.
     *
     * @param activity The activity that was created.
     * @param savedInstanceState If the activity is being re-created after
     * previously being destroyed, then this Bundle contains the data it most
     * recently supplied in [onSaveInstanceState].
     * **Note: Otherwise it is null.**
     */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    /**
     * Called when an activity is started.
     *
     * @param activity The activity that was started.
     */
    override fun onActivityStarted(activity: Activity) {
        activityCounter++
        LoggerService.log(LogLevelEnum.INFO, "APP_LIFECYCLE_EVENT $activityCounter")
    }

    /**
     * Called when an activity is resumed.
     *
     * @param activity The activity that was resumed.
     */
    override fun onActivityResumed(activity: Activity) {
    }

    /**
     * Called whenan activity is paused.
     *
     * @param activity The activity that was paused.
     */
    override fun onActivityPaused(activity: Activity) {
    }

    /**
     * Called when an activity is stopped. This is used to trigger the API call repeater when app
     * is finished off all activities.
     *
     * @param activity The activity that was stopped.
     */
    override fun onActivityStopped(activity: Activity) {
        activityCounter--
        if (activityCounter <= 0) {
            ApiCallRepeater.start()
        }
        LoggerService.log(LogLevelEnum.INFO, "APP_LIFECYCLE_EVENT $activityCounter")
    }

    /**
     * Called to retrieve per-instance state from an activity before being killed
     * so that the state can be restored in [onActivityCreated] or
     * [onRestoreInstanceState] (the [Bundle] populated by this method
     * will be passed to both).
     *
     * @param activity The activity that was stopped.
     * @param outState Bundle in which to place your saved state.
     */
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    /**
     * Called when an activity is destroyed.
     *
     * @param activity The activity that was destroyed.
     */
    override fun onActivityDestroyed(activity: Activity) {
    }
}