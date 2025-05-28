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
class ImpactCampaignTest {

    private lateinit var impactCampaign: ImpactCampaign

    @Before
    fun setup() {
        impactCampaign = ImpactCampaign()
    }

    @Test
    fun `test default values`() {
        assertNull(impactCampaign.campaignId)
        assertNull(impactCampaign.type)
    }

    @Test
    fun `test setting and getting id`() {
        val testId = 123
        impactCampaign.campaignId = testId
        assertEquals(testId, impactCampaign.campaignId)
    }

    @Test
    fun `test setting and getting name`() {
        val testName = "Test Campaign"
        impactCampaign.type = testName
        assertEquals(testName, impactCampaign.type)
    }

    @Test
    fun `test setting and getting type`() {
        val testType = "VISUAL_AB"
        impactCampaign.type = testType
        assertEquals(testType, impactCampaign.type)
    }

    @Test
    fun `test setting and getting all properties`() {
        impactCampaign.apply {
            campaignId = 123
            type = "VISUAL_AB"
        }

        assertEquals(123, impactCampaign.campaignId)
        assertEquals("VISUAL_AB", impactCampaign.type)
    }
} 