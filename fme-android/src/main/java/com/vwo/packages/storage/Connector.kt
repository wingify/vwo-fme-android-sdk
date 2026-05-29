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
package com.vwo.packages.storage

/**
 * An abstract class representing a connector for data storage and retrieval.
 *
 * @deprecated Use [com.wingify.packages.storage.Connector] for new integrations.
 */
@Deprecated(
    message = "Use com.wingify.packages.storage.Connector instead",
    replaceWith = ReplaceWith("Connector", "com.wingify.packages.storage.Connector"),
)
abstract class Connector : com.wingify.packages.storage.Connector()
