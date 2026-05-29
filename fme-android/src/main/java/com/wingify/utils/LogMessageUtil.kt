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
package com.wingify.utils

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Utility object for log message operations.
 *
 * This object provides helper methods for formatting and processing log messages, such as
 * constructing log messages with dynamic data, adding timestamps, or applying formatting rules.
 */
object LogMessageUtil {
    private val NARGS: Pattern = Pattern.compile("\\{([0-9a-zA-Z_]+)\\}")
    // Replace branding only for standalone "vwo"/"VWO", not internal identifiers or domains:
    // - skip when preceded by letter, digit, or '_' (e.g. _vwoUserId, _vwo_meta)
    // - skip when followed by '_', lowercase letter, or '.' (e.g. vwo_variationShown, vwo_meta, vwo.com)
    private val VWO_BRANDING_REGEX = Regex("(?i)(?<![A-Za-z0-9_])vwo(?!_|[a-z]|\\.)")

    /**
     * Constructs a message by replacing placeholders in a template with corresponding values from a data object.
     *
     * @param template The message template containing placeholders in the format {key}.
     * @param data     An object containing keys and values used to replace the placeholders in the template.
     * @return The constructed message with all placeholders replaced by their corresponding values from the data object.
     */
    fun buildMessage(template: String?, data: Map<String?, Any?>?): String? {
        if (template == null || data == null) {
            return template
        }
        try {
            val result = StringBuffer()
            val matcher = NARGS.matcher(template)
            while (matcher.find()) {
                val key = matcher.group(1)
                val value: Any? = data[key]
                if (value != null) {
                    // If the value is not null, replace the placeholder with its value
                    matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()))
                }
            }
            matcher.appendTail(result)
            return result.toString()
        } catch (e: Exception) {
            // Return the original template in case of an error
            return template
        }
    }

    /**
     * Replaces VWO branding with Wingify in log messages when the Wingify SDK mode is active.
     * Matching is case-insensitive. Skips internal identifiers (`_vwoUserId`, `vwo_variationShown`,
     * `vwo_sdkName`) and domains (`vwo.com`, `app.vwo.com`).
     */
    fun applySdkBranding(message: String?, useWingifyBranding: Boolean): String? {
        if (message == null || !useWingifyBranding) {
            return message
        }
        return VWO_BRANDING_REGEX.replace(message, "Wingify")
    }
}
