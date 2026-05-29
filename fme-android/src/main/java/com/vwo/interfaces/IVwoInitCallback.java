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
package com.vwo.interfaces;

import com.vwo.VWO;

import org.jetbrains.annotations.NotNull;

/**
 * Callback interface to receive notifications about the VWO initialization process.
 *
 * @deprecated Use {@link com.wingify.interfaces.IWingifyInitCallback} for new integrations.
 * <p>
 * <b>Usage:</b>
 * <p>
 * <b>Kotlin:</b>
 * <p><pre>
 *    val callback = object : IVwoInitCallback {
 *       override fun vwoInitSuccess(message: String) {
 *           println("VWO initialization successful: $message")
 *       }
 *
 *       override fun vwoInitFailed(message: String) {
 *           println("VWO initialization failed: $message")
 *       }
 *    }
 *    </pre>
 * </p>
 * <b>Java:</b>
 * <p><pre>
 *    IVwoInitCallback callback = new IVwoInitCallback() {
 *
 *       public void vwoInitSuccess(String message) {
 *           System.out.println("VWO initialization successful: " + message);
 *       }
 *
 *       public void vwoInitFailed(String message) {
 *           System.out.println("VWO initialization failed: " + message);
 *       }
 *    };
 * </pre>
 * </p>
 */
@Deprecated
public interface IVwoInitCallback {
    /**
     * Called when VWO initialization is successful.
     * @param message String message
     */
    void vwoInitSuccess(@NotNull VWO vwo, @NotNull String message);

    /**
     * Called when VWO initialization is failed.
     * <p>
     * Most common reason for failure is device unable to connect to Internet.
     *
     * @param message String message
     */
    void vwoInitFailed(@NotNull String message);
}