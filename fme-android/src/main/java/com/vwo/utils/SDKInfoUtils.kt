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
import android.content.pm.PackageInfo
import android.os.Build

private const val platformAndroid = "ad"
private const val platformFlutter = "fl"
private const val platformReact = "rn"
const val valueUnknown = "Unknown"

object SDKInfoUtils {

    fun getSDKName(): String {
        return when {
            SDKMetaUtil.sdkName.contains("android", ignoreCase = true) -> platformAndroid
            SDKMetaUtil.sdkName.contains("flutter", ignoreCase = true) -> platformFlutter
            else -> platformReact
        }
    }
    
    fun getSDKVersion(): String = SDKMetaUtil.sdkVersion
    
    fun getPlatform(): String = platformAndroid
    
    fun getOSVersion(): String = Build.VERSION.RELEASE
    
    fun getOSDetails(): String = "$platformAndroid ${Build.VERSION.RELEASE}"//Android 12

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: valueUnknown
        } catch (e: Exception) {
            valueUnknown
        }
    }
    
    fun getLanguageVersion(): String {
        // Java version note: System.getProperty("java.version") returns "0" in Android
        // Android Doesn't Use a Standard JVM: Android applications don't run on a standard Java
        // Virtual Machine (JVM) like desktop or server Java applications. Historically, they used
        // the Dalvik Virtual Machine (DVM), and since Android 5.0 (Lollipop), they primarily use
        // the Android Runtime (ART).
        return "Kotlin ${KotlinVersion.CURRENT}"
    }
} 