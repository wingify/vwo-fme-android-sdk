/*
 * Copyright (c) 2024-2025 Wingify Software Pvt. Ltd.
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
package com.vwo.models.schemas

import com.vwo.models.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SettingsSchemaTest {

    private lateinit var settingsSchema: SettingsSchema
    private lateinit var validSettings: Settings
    private lateinit var validCampaign: Campaign
    private lateinit var validVariation: Variation
    private lateinit var validFeature: Feature
    private lateinit var validMetric: Metric
    private lateinit var validRule: Rule
    private lateinit var validVariable: Variable

    @Before
    fun setup() {
        settingsSchema = SettingsSchema()
        
        // Setup valid variable
        validVariable = Variable().apply {
            id = 1
            type = "string"
            key = "test_key"
            value = "test_value"
        }

        // Setup valid variation
        validVariation = Variation().apply {
            id = 1
            name = "Test Variation"
            variables = listOf(validVariable)
        }

        // Setup valid campaign
        validCampaign = Campaign().apply {
            id = 1
            type = "FLAG_TEST"
            key = "test_campaign"
            status = "RUNNING"
            name = "Test Campaign"
            variations = listOf(validVariation)
        }

        // Setup valid metric
        validMetric = Metric().apply {
            id = 1
            type = "REVENUE"
            identifier = "test_metric"
        }

        // Setup valid rule
        validRule = Rule().apply {
            type = "FLAG_TEST"
            ruleKey = "test_rule"
            campaignId = 1
        }

        // Setup valid feature
        validFeature = Feature().apply {
            id = 1
            key = "test_feature"
            status = "ON"
            name = "Test Feature"
            type = "FEATURE_FLAG"
            metrics = listOf(validMetric)
            rules = listOf(validRule)
            variables = listOf(validVariable)
        }

        // Setup valid settings
        validSettings = Settings().apply {
            version = 1
            accountId = 12345
            campaigns = listOf(validCampaign)
            features = listOf(validFeature)
        }
    }

    @Test
    fun `test isSettingsValid with valid settings returns true`() {
        // Act & Assert
        assertTrue(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with null settings returns false`() {
        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(null))
    }

    @Test
    fun `test isSettingsValid with null version returns false`() {
        // Arrange
        validSettings.version = null

        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with null accountId returns false`() {
        // Arrange
        validSettings.accountId = null

        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with null campaigns returns false`() {
        // Arrange
        validSettings.campaigns = null

        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with empty campaigns returns false`() {
        // Arrange
        validSettings.campaigns = emptyList()

        // Act & Assert
        assertTrue(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with invalid campaign returns false`() {
        // Arrange
        validCampaign.id = null
        validSettings.campaigns = listOf(validCampaign)

        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with invalid variation returns false`() {
        // Arrange
        validVariation.id = null
        validCampaign.variations = listOf(validVariation)
        validSettings.campaigns = listOf(validCampaign)

        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with invalid variable returns false`() {
        // Arrange
        validVariable.id = null
        validVariation.variables = listOf(validVariable)
        validCampaign.variations = listOf(validVariation)
        validSettings.campaigns = listOf(validCampaign)

        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with invalid feature returns false`() {
        // Arrange
        validFeature.id = null
        validSettings.features = listOf(validFeature)

        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with invalid metric returns false`() {
        // Arrange
        validMetric.id = null
        validFeature.metrics = listOf(validMetric)
        validSettings.features = listOf(validFeature)

        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(validSettings))
    }

    @Test
    fun `test isSettingsValid with invalid rule returns false`() {
        // Arrange
        validRule.type = null
        validFeature.rules = listOf(validRule)
        validSettings.features = listOf(validFeature)

        // Act & Assert
        assertFalse(settingsSchema.isSettingsValid(validSettings))
    }
} 