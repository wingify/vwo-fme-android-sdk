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
package com.vwo.utils

import com.wingify.utils.ForceListOperands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Recursive `not` split (Node `_forceOnSegmentsOnly` / collect Off).
 *
 * Off-only: `{ "not": { "or": [Off] } }`
 * Mixed:    `{ "and": [ { "or": [On] }, { "not": { "or": [Off] } } ] }`
 * Nested:   `{ "or": [ { "not": Off }, { "or": [On] } ] }`
 */
class ForceListOperandsTest {

    private val onA = orUser("on-a")
    private val onB = orUser("on-b")
    private val offA = orUser("off-a")
    private val offB = orUser("off-b")

    @Test
    fun `off of null is null`() {
        assertNull(ForceListOperands.off(null))
    }

    @Test
    fun `off of On-only or is null`() {
        assertNull(ForceListOperands.off(onA))
    }

    @Test
    fun `off unwraps top-level not to inner or`() {
        splitEquals(not(offA), off = offA, on = null)
    }

    @Test
    fun `off and on split a mixed and of one On and one not`() {
        splitEquals(and(onA, not(offA)), off = offA, on = and(onA))
    }

    @Test
    fun `not first then On in and splits the same as On first`() {
        splitEquals(and(not(offA), onA), off = offA, on = and(onA))
    }

    @Test
    fun `backend not-first and with comma user lists splits Off and On`() {
        val offUsers =
            "D5F4524B731F524E9C006DFFC0611F78,BA1E16C5E6E35CF6A361C682F3C8EFE6,01567088402F510B857833195B721D9E"
        val onUsers =
            "86D73008B18F5B7C9B8308673E5CF6B1,A91DDF233995553D930C26069F211254,B45B98F02A9956D49FAE9F3701DDF2AD"
        val dsl = mapOf(
            "and" to listOf(
                mapOf(
                    "not" to mapOf(
                        "or" to listOf(mapOf("user" to offUsers))
                    )
                ),
                mapOf(
                    "or" to listOf(mapOf("user" to onUsers))
                )
            )
        )
        splitEquals(
            dsl,
            off = mapOf("or" to listOf(mapOf("user" to offUsers))),
            on = mapOf("and" to listOf(mapOf("or" to listOf(mapOf("user" to onUsers)))))
        )
    }

    @Test
    fun `two top-level nots with one On become or of both Off inners`() {
        splitEquals(
            and(onA, not(offA), not(offB)),
            off = mapOf("or" to listOf(offA, offB)),
            on = and(onA)
        )
    }

    @Test
    fun `two Ons and two nots keep both Ons and or both Offs`() {
        splitEquals(
            and(onA, not(offA), onB, not(offB)),
            off = mapOf("or" to listOf(offA, offB)),
            on = and(onA, onB)
        )
    }

    @Test
    fun `and of only nots has no On operand`() {
        splitEquals(
            and(not(offA), not(offB)),
            off = mapOf("or" to listOf(offA, offB)),
            on = null
        )
    }

    @Test
    fun `On-only is unchanged and has no Off operand`() {
        splitEquals(onA, off = null, on = onA)
    }

    @Test
    fun `not nested inside or is Force Off and stripped from On`() {
        splitEquals(
            mapOf("or" to listOf(not(offA), onA)),
            off = offA,
            on = mapOf("or" to listOf(onA))
        )
    }

    @Test
    fun `or of only not has no On operand`() {
        splitEquals(
            mapOf("or" to listOf(not(offA))),
            off = offA,
            on = null
        )
    }

    @Test
    fun `off skips non-map and children and still unwraps top-level not`() {
        val off = ForceListOperands.off(
            mapOf("and" to listOf("skip", 1, not(offA), null))
        )
        assertEquals(offA, off)
        assertNoNot(off)
    }

    private fun splitEquals(
        dsl: Map<String, Any>,
        off: Map<String, Any>?,
        on: Map<String, Any>?
    ) {
        val offOperand = ForceListOperands.off(dsl)
        val onOperand = ForceListOperands.on(dsl)
        assertEquals(off, offOperand)
        assertEquals(on, onOperand)
        assertNoNot(offOperand)
        assertNoNot(onOperand)
    }

    private fun orUser(id: String) = mapOf(
        "or" to listOf(mapOf("user" to id))
    )

    private fun not(inner: Map<String, Any>) = mapOf("not" to inner)

    private fun and(vararg nodes: Map<String, Any>) = mapOf("and" to nodes.toList())

    private fun assertNoNot(node: Map<String, Any>?) {
        assertFalse("unwrapped operand must not contain not: $node", containsNot(node))
    }

    private fun containsNot(value: Any?): Boolean = when (value) {
        is Map<*, *> -> value.containsKey("not") || value.values.any { containsNot(it) }
        is List<*> -> value.any { containsNot(it) }
        else -> false
    }
}
