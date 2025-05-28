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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CampaignTest {

    private lateinit var campaign: Campaign
    private lateinit var sourceCampaign: Campaign

    @Before
    fun setup() {
        campaign = Campaign()
        sourceCampaign = Campaign()
    }

    @Test
    fun `test default values`() {
        assertFalse(campaign.isAlwaysCheckSegment!!)
        assertFalse(campaign.isUserListEnabled!!)
        assertNull(campaign.id)
        assertNull(campaign.segments)
        assertNull(campaign.segments_events)
        assertNull(campaign.ruleKey)
        assertNull(campaign.salt)
        assertNull(campaign.status)
        assertNull(campaign.percentTraffic)
        assertNull(campaign.key)
        assertNull(campaign.type)
        assertNull(campaign.name)
        assertFalse(campaign.isForcedVariationEnabled!!)
        assertNull(campaign.variations)
        assertEquals(0, campaign.startRangeVariation)
        assertEquals(0, campaign.endRangeVariation)
        assertNull(campaign.variables)
        assertEquals(0.0, campaign.weight, 0.0)
        assertFalse(campaign.isEventsDsl)
    }

    @Test
    fun `test setModelFromDictionary with all fields`() {
        // Setup source campaign with all fields
        sourceCampaign.apply {
            id = 123
            segments = mapOf("key" to "value")
            segments_events = listOf("event1", "event2")
            ruleKey = "test_rule"
            salt = "test_salt"
            status = "RUNNING"
            percentTraffic = 50
            key = "test_key"
            type = "TEST"
            name = "Test Campaign"
            isForcedVariationEnabled = true
            variations = listOf(Variation())
            startRangeVariation = 10
            endRangeVariation = 20
            variables = listOf(Variable())
            weight = 1.5
            isAlwaysCheckSegment = true
            isUserListEnabled = true
        }

        // Copy from source
        campaign.setModelFromDictionary(sourceCampaign)

        // Verify all fields are copied correctly
        assertEquals(sourceCampaign.id, campaign.id)
        assertEquals(sourceCampaign.segments, campaign.segments)
        assertEquals(sourceCampaign.segments_events, campaign.segments_events)
        assertEquals(sourceCampaign.ruleKey, campaign.ruleKey)
        assertEquals(sourceCampaign.salt, campaign.salt)
        assertEquals(sourceCampaign.status, campaign.status)
        assertEquals(sourceCampaign.percentTraffic, campaign.percentTraffic)
        assertEquals(sourceCampaign.key, campaign.key)
        assertEquals(sourceCampaign.type, campaign.type)
        assertEquals(sourceCampaign.name, campaign.name)
        assertEquals(sourceCampaign.isForcedVariationEnabled, campaign.isForcedVariationEnabled)
        assertEquals(sourceCampaign.variations, campaign.variations)
        assertEquals(sourceCampaign.startRangeVariation, campaign.startRangeVariation)
        assertEquals(sourceCampaign.endRangeVariation, campaign.endRangeVariation)
        assertEquals(sourceCampaign.variables, campaign.variables)
        assertEquals(sourceCampaign.weight, campaign.weight, 0.0)
        assertEquals(sourceCampaign.isAlwaysCheckSegment, campaign.isAlwaysCheckSegment)
        assertEquals(sourceCampaign.isUserListEnabled, campaign.isUserListEnabled)
    }

    @Test
    fun `test setModelFromDictionary with partial fields`() {
        // Setup source campaign with only some fields
        sourceCampaign.apply {
            id = 123
            key = "test_key"
            type = "TEST"
        }

        // Copy from source
        campaign.setModelFromDictionary(sourceCampaign)

        // Verify only specified fields are copied
        assertEquals(sourceCampaign.id, campaign.id)
        assertEquals(sourceCampaign.key, campaign.key)
        assertEquals(sourceCampaign.type, campaign.type)

        // Verify other fields remain unchanged
        assertNull(campaign.segments)
        assertNull(campaign.segments_events)
        assertNull(campaign.ruleKey)
        assertNull(campaign.salt)
        assertNull(campaign.status)
        assertNull(campaign.percentTraffic)
        assertNull(campaign.name)
        assertFalse(campaign.isForcedVariationEnabled!!)
        assertNull(campaign.variations)
        assertEquals(0, campaign.startRangeVariation)
        assertEquals(0, campaign.endRangeVariation)
        assertNull(campaign.variables)
        assertEquals(0.0, campaign.weight, 0.0)
        assertFalse(campaign.isAlwaysCheckSegment!!)
        assertFalse(campaign.isUserListEnabled!!)
    }

    @Test
    fun `test setModelFromDictionary with null source`() {
        // Copy from null source
        campaign.setModelFromDictionary(Campaign())

        // Verify all fields remain unchanged
        assertNull(campaign.id)
        assertNull(campaign.segments)
        assertNull(campaign.segments_events)
        assertNull(campaign.ruleKey)
        assertNull(campaign.salt)
        assertNull(campaign.status)
        assertNull(campaign.percentTraffic)
        assertNull(campaign.key)
        assertNull(campaign.type)
        assertNull(campaign.name)
        assertFalse(campaign.isForcedVariationEnabled!!)
        assertNull(campaign.variations)
        assertEquals(0, campaign.startRangeVariation)
        assertEquals(0, campaign.endRangeVariation)
        assertNull(campaign.variables)
        assertEquals(0.0, campaign.weight, 0.0)
        assertFalse(campaign.isAlwaysCheckSegment!!)
        assertFalse(campaign.isUserListEnabled!!)
    }
} 