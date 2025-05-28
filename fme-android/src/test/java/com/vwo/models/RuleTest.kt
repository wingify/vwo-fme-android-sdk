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
class RuleTest {

    private lateinit var rule: Rule

    @Before
    fun setup() {
        rule = Rule()
    }

    @Test
    fun `test default values`() {
        assertNull(rule.ruleKey) // Corrected property name
        assertNull(rule.variationId) // Corrected property name
        assertNull(rule.campaignId) // Corrected property name
        assertNull(rule.type)
        // Removed assertions for operator, operand, and rules as they are not in the provided Rule class
    }

    @Test
    fun `test setting and getting ruleKey`() { // Corrected test name
        val testRuleKey = "test_rule_key"
        rule.setStatus(testRuleKey) // Use the setStatus method
        assertEquals(testRuleKey, rule.ruleKey) // Assert on the ruleKey property
    }

    @Test
    fun `test setting and getting variationId`() { // Added test for variationId
        val testVariationId = 123
        rule.variationId = testVariationId
        assertEquals(testVariationId, rule.variationId)
    }

    @Test
    fun `test setting and getting campaignId`() { // Added test for campaignId
        val testCampaignId = 456
        rule.campaignId = testCampaignId
        assertEquals(testCampaignId, rule.campaignId)
    }

    @Test
    fun `test setting and getting type`() {
        val testType = "CUSTOM"
        rule.type = testType
        assertEquals(testType, rule.type)
    }

    @Test
    fun `test setting and getting all properties`() {
        val testRuleKey = "test_rule_key"
        val testVariationId = 123
        val testCampaignId = 456
        val testType = "CUSTOM"

        rule.apply {
            setStatus(testRuleKey) // Use the setStatus method
            variationId = testVariationId
            campaignId = testCampaignId
            type = testType
        }

        assertEquals(testRuleKey, rule.ruleKey) // Assert on the ruleKey property
        assertEquals(testVariationId, rule.variationId)
        assertEquals(testCampaignId, rule.campaignId)
        assertEquals(testType, rule.type)
        // Removed assertions for operator, operand, and rules
    }
}