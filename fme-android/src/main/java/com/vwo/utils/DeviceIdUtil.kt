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
package com.vwo.utils

import android.content.Context
import android.provider.Settings
import com.vwo.providers.StorageProvider

/**
 * Utility class for generating and managing device IDs.
 * 
 * This class provides methods to generate a persistent device ID that remains
 * consistent across app uninstalls/reinstalls using Android ID.
 */
class DeviceIdUtil {
    
    /**
     * Generates a device ID using Android ID.
     * This ID persists across app uninstalls/reinstalls but may change
     * on factory resets or when the user changes their Google account.
     * 
     * @param context The Android context
     * @return A device ID string, or null if context is not available
     */
    fun getDeviceId(context: Context?): String? {
        if (context == null) {
            return null
        }
        
        try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            
            return if (androidId != null && androidId.isNotEmpty()) {
                androidId
            } else {
                null
            }
        } catch (e: Exception) {
            // Return null if we can't get the Android ID
            return null
        }
    }
    
    /**
     * Gets the device ID from the current context stored in StorageProvider.
     * 
     * @return A device ID string, or null if context is not available
     */
    fun getDeviceId(): String? {
        val context = StorageProvider.contextRef.get()
        return getDeviceId(context)
    }
} 