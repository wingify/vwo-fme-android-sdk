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
package com.vwo.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class GroupsTest {

    private lateinit var groups: Groups

    @Before
    fun setup() {
        groups = Groups()
    }

    @Test
    fun `test default values`() {
        assertNull(groups.name)
        assertNull(groups.campaigns)
        assertEquals(ArrayList<String>(), groups.p)
        assertEquals(mutableMapOf<String, Double>(), groups.wt)
        assertEquals(1, groups.getEt())
    }

    @Test
    fun `test setEt and getEt`() {
        // Test setting valid experiment type
        groups.setEt(2)
        assertEquals(2, groups.getEt())

        // Test setting null experiment type
        groups.setEt(1)
        assertEquals(1, groups.getEt())

        // Test setting empty experiment type
        groups.setEt(0)
        assertEquals(0, groups.getEt())
    }

    @Test
    fun `test setting and getting name`() {
        val testName = "Test Group"
        groups.name = testName
        assertEquals(testName, groups.name)
    }

    @Test
    fun `test setting and getting campaigns`() {
        val testCampaigns = listOf("campaign1", "campaign2")
        groups.campaigns = testCampaigns
        assertEquals(testCampaigns, groups.campaigns)
    }

    @Test
    fun `test setting and getting priority list`() {
        val testPriority = mutableListOf("priority1", "priority2")
        groups.p = testPriority
        assertEquals(testPriority, groups.p)
    }

    @Test
    fun `test setting and getting weight map`() {
        val testWeight = mutableMapOf("campaign1" to 0.5, "campaign2" to 0.5)
        groups.wt = testWeight
        assertEquals(testWeight, groups.wt)
    }

    @Test
    fun `test setting and getting all properties`() {
        groups.apply {
            name = "Test Group"
            campaigns = listOf("campaign1", "campaign2")
            p = mutableListOf("priority1", "priority2")
            wt = mutableMapOf("campaign1" to 0.5, "campaign2" to 0.5)
            setEt(2)
        }

        assertEquals("Test Group", groups.name)
        assertEquals(listOf("campaign1", "campaign2"), groups.campaigns)
        assertEquals(mutableListOf("priority1", "priority2"), groups.p)
        assertEquals(mutableMapOf("campaign1" to 0.5, "campaign2" to 0.5), groups.wt)
        assertEquals(2, groups.getEt())
    }
} 