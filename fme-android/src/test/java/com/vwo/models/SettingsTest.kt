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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SettingsTest {

    private lateinit var settings: Settings

    @Before
    fun setup() {
        settings = Settings()
    }

    @Test
    fun `test default values`() {
        assertNull(settings.sdkKey)
        assertNull(settings.accountId)
        assertNull(settings.campaigns)
        assertNull(settings.groups)
        assertNull(settings.version)
        assertNull(settings.campaignGroups) // Added assertion for campaignGroups
        assertNull(settings.collectionPrefix) // Added assertion for collectionPrefix

        // Assert default values for non-nullable properties
        assertNotNull(settings.features) // features is initialized to emptyList()
        assertTrue(settings.features.isEmpty()) // features should be an empty list by default
        assertFalse(settings.isNBv2) // isNBv2 is initialized to false
        assertFalse(settings.isNB) // isNB is initialized to false
    }

    @Test
    fun `test setting and getting sdkKey`() {
        val testSdkKey = "test_sdk_key"
        settings.sdkKey = testSdkKey
        assertEquals(testSdkKey, settings.sdkKey)
    }

    @Test
    fun `test setting and getting accountId`() {
        val testAccountId = 123
        settings.accountId = testAccountId
        assertEquals(testAccountId, settings.accountId)
    }

    @Test
    fun `test setting and getting campaigns`() {
        val testCampaigns = listOf(
            Campaign().apply {
                id = 1
                name = "Test Campaign"
            }
        )
        settings.campaigns = testCampaigns
        assertEquals(testCampaigns, settings.campaigns)
    }

    @Test
    fun `test setting and getting groups`() {
        // The 'groups' property is a Map<String, Groups>, not a List<Groups>
        val testGroups = mapOf(
            "group1" to Groups().apply {
                name = "Test Group 1"
                campaigns = listOf("campaign1", "campaign2")
            },
            "group2" to Groups().apply {
                name = "Test Group 2"
                campaigns = listOf("campaign3")
            }
        )
        settings.groups = testGroups
        assertEquals(testGroups, settings.groups)
    }

    @Test
    fun `test setting and getting version`() {
        // The 'version' property is an Int?, not a String?
        val testVersion = 1
        settings.version = testVersion
        assertEquals(testVersion, settings.version)
    }

    @Test
    fun `test setting and getting campaignGroups`() { // Added test for campaignGroups
        val testCampaignGroups = mapOf(
            "campaign1" to 1,
            "campaign2" to 2
        )
        settings.campaignGroups = testCampaignGroups
        assertEquals(testCampaignGroups, settings.campaignGroups)
    }

    @Test
    fun `test setting and getting isNBv2`() { // Added test for isNBv2
        val testIsNBv2 = true
        settings.isNBv2 = testIsNBv2
        assertEquals(testIsNBv2, settings.isNBv2)
    }

    @Test
    fun `test setting and getting isNB`() { // Added test for isNB
        val testIsNB = true
        settings.isNB = testIsNB
        assertEquals(testIsNB, settings.isNB)
    }

    @Test
    fun `test setting and getting collectionPrefix`() { // Added test for collectionPrefix
        val testCollectionPrefix = "test_prefix"
        settings.collectionPrefix = testCollectionPrefix
        assertEquals(testCollectionPrefix, settings.collectionPrefix)
    }

    @Test
    fun `test setting and getting features`() { // Added test for features
        val testFeatures = listOf(
            Feature().apply {
                // Assuming Feature has some properties to set
                // For example: id = 1, key = "test_feature"
            }
        )
        settings.features = testFeatures
        assertEquals(testFeatures, settings.features)
    }


    @Test
    fun `test setting and getting all properties`() {
        val testCampaigns = listOf(
            Campaign().apply {
                id = 1
                name = "Test Campaign"
            }
        )
        val testGroups = mapOf( // Corrected type to Map
            "group1" to Groups().apply {
                name = "Test Group 1"
                campaigns = listOf("campaign1", "campaign2")
            }
        )
        val testCampaignGroups = mapOf( // Added campaignGroups
            "campaign1" to 1
        )
        val testFeatures = listOf( // Added features
            Feature().apply {
                // Assuming Feature has some properties to set
            }
        )


        settings.apply {
            sdkKey = "test_sdk_key"
            accountId = 123
            campaigns = testCampaigns
            groups = testGroups
            version = 1 // Corrected type to Int
            campaignGroups = testCampaignGroups // Added campaignGroups
            isNBv2 = true // Added isNBv2
            isNB = true // Added isNB
            collectionPrefix = "test_prefix" // Added collectionPrefix
            features = testFeatures // Added features
        }

        assertEquals("test_sdk_key", settings.sdkKey)
        assertEquals(123, settings.accountId)
        assertEquals(testCampaigns, settings.campaigns)
        assertEquals(testGroups, settings.groups)
        assertEquals(1, settings.version) // Corrected assertion type
        assertEquals(testCampaignGroups, settings.campaignGroups) // Added assertion
        assertEquals(true, settings.isNBv2) // Added assertion
        assertEquals(true, settings.isNB) // Added assertion
        assertEquals("test_prefix", settings.collectionPrefix) // Added assertion
        assertEquals(testFeatures, settings.features) // Added assertion
    }
}