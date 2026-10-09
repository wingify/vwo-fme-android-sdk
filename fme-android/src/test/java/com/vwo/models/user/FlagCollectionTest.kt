/*
 * Copyright (c) 2024-2026 Wingify Software Pvt. Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.vwo.models.user

import com.wingify.models.user.WingifyUserContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FlagCollectionTest {

    private lateinit var context: WingifyUserContext
    private lateinit var enabledFlag: GetFlag
    private lateinit var collection: FlagCollection

    @Before
    fun setup() {
        context = WingifyUserContext().apply { id = "user-1" }
        enabledFlag = GetFlag(context).apply { setIsEnabled(true) }
        collection = FlagCollection.wrap(
            com.wingify.models.user.FlagCollection.create(
                mapOf("feature_a" to enabledFlag),
                context,
            )
        )
    }

    @Test
    fun `get returns stored flag`() {
        assertTrue(collection.get("feature_a").isEnabled())
    }

    @Test
    fun `get returns disabled flag for missing key`() {
        assertFalse(collection.get("missing").isEnabled())
    }

    @Test
    fun `keys size and iterator`() {
        assertEquals(setOf("feature_a"), collection.keys())
        assertEquals(1, collection.size())

        val entries = collection.iterator().asSequence().toList()
        assertEquals(1, entries.size)
        assertEquals("feature_a", entries[0].key)
        assertTrue(entries[0].value.isEnabled())
    }

    @Test
    fun `createDisabled includes requested keys as disabled flags`() {
        val disabled = com.wingify.models.user.FlagCollection.createDisabled(
            arrayOf("flag_a", "flag_b", "flag_a", ""),
            context,
        )
        val wrapped = FlagCollection.wrap(disabled)

        assertEquals(setOf("flag_a", "flag_b"), wrapped.keys())
        assertEquals(2, wrapped.size())
        assertFalse(wrapped.get("flag_a").isEnabled())
        assertFalse(wrapped.get("flag_b").isEnabled())

        val entries = wrapped.iterator().asSequence().toList()
        assertEquals(2, entries.size)
        assertTrue(entries.all { !it.value.isEnabled() })
    }

    @Test
    fun `createDisabled with null or empty names returns empty collection`() {
        val nullNames = com.wingify.models.user.FlagCollection.createDisabled(null, context)
        assertEquals(0, nullNames.size())
        assertTrue(nullNames.keys().isEmpty())

        val emptyNames = com.wingify.models.user.FlagCollection.createDisabled(emptyArray(), context)
        assertEquals(0, emptyNames.size())
        assertTrue(emptyNames.keys().isEmpty())
    }
}
