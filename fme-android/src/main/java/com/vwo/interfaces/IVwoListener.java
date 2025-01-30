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
package com.vwo.interfaces;

import org.jetbrains.annotations.NotNull;

/**
 * Callback interface to receive notifications about the operation in VWO.
 */
public interface IVwoListener {
    /**
     * Called when VWO action is successful.
     * @param data Additional data
     */
    void onSuccess(Object data);

    /**
     * Called when VWO action is failed.
     * <p>
     * Most common reason for failure is device unable to connect to Internet.
     *
     * @param message String message
     */
    void onFailure(@NotNull String message);
}