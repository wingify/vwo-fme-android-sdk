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

/**
 * Splits [Variation.whitelistedSegments] into Force Off / Force On operands.
 *
 * Walks the whole DSL (same idea as Node `_collectRolloutForceOffNotOperands` /
 * `_forceOnSegmentsOnly`). Off collects every `not` inner; On drops every `not`
 * so inverted Off lists cannot Force On everyone else.
 */
internal object ForceListOperands {

    /**
     * Inner Off list(s). Several `not` nodes become one `or`.
     */
    fun off(segments: Map<String, Any>?): Map<String, Any>? {
        if (segments == null) return null
        val offs = ArrayList<Map<String, Any>>()
        collectNotInners(segments, offs)
        if (offs.isEmpty()) return null
        if (offs.size == 1) return offs[0]
        return mapOf("or" to offs)
    }

    /**
     * On list with every `not` removed. Null when nothing but Off remains.
     */
    @Suppress("UNCHECKED_CAST")
    fun on(segments: Map<String, Any>): Map<String, Any>? {
        return stripNot(segments) as? Map<String, Any>
    }

    private fun collectNotInners(node: Any?, outs: MutableList<Map<String, Any>>) {
        when (node) {
            is Map<*, *> -> {
                val inner = node["not"]
                if (inner is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    outs.add(inner as Map<String, Any>)
                }
                for ((key, value) in node) {
                    if (key != "not") collectNotInners(value, outs)
                }
            }
            is List<*> -> node.forEach { collectNotInners(it, outs) }
        }
    }

    private fun stripNot(node: Any?): Any? {
        when (node) {
            is List<*> -> {
                val kept = node.mapNotNull { stripNot(it) }
                return kept.ifEmpty { null }
            }
            is Map<*, *> -> {
                val result = LinkedHashMap<String, Any>()
                for ((rawKey, value) in node) {
                    val key = rawKey as? String ?: continue
                    if (key == "not") continue
                    val stripped = stripNot(value) ?: continue
                    result[key] = stripped
                }
                return result.takeIf { it.isNotEmpty() }
            }
            else -> return node
        }
    }
}
