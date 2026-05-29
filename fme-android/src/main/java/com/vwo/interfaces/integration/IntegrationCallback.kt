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
package com.vwo.interfaces.integration

/**
 * Callback interface for integration events.
 *
 * @deprecated Use [com.wingify.interfaces.integration.IntegrationCallback] for new integrations.
 */
@Deprecated(
    message = "Use com.wingify.interfaces.integration.IntegrationCallback instead",
    replaceWith = ReplaceWith(
        "IntegrationCallback",
        "com.wingify.interfaces.integration.IntegrationCallback",
    ),
)
interface IntegrationCallback : com.wingify.interfaces.integration.IntegrationCallback
