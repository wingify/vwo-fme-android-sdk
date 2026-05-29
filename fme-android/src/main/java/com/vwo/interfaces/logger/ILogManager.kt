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
package com.vwo.interfaces.logger

/**
 * Interface for managing log operations.
 *
 * @deprecated Use [com.wingify.interfaces.logger.ILogManager] for new integrations.
 */
@Deprecated(
    message = "Use com.wingify.interfaces.logger.ILogManager instead",
    replaceWith = ReplaceWith("ILogManager", "com.wingify.interfaces.logger.ILogManager"),
)
interface ILogManager : com.wingify.interfaces.logger.ILogManager
