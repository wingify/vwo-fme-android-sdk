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
package com.vwo.utils

import com.wingify.models.Feature
import com.wingify.models.Groups
import com.wingify.models.Rule
import com.wingify.models.Settings
import com.wingify.utils.GsonUtil
import com.wingify.utils.MegGroupPartitionUtil
import com.wingify.utils.SettingsUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MegGroupPartitionUtilTest {

    private lateinit var megSettings: Settings
    private lateinit var basicSettings: Settings
    private lateinit var stressSettings: Settings

    @Before
    fun setup() {
        val reader = DummySettingsReader()
        megSettings = GsonUtil.gson.fromJson(
            reader.settingsMap["MEG_CAMPAIGN_RANDOM_ALGO_SETTINGS"],
            Settings::class.java,
        )
        SettingsUtil.processSettings(megSettings)
        basicSettings = GsonUtil.gson.fromJson(
            reader.settingsMap["SETTINGS_WITH_DIFFERENT_SALT"],
            Settings::class.java,
        )
        SettingsUtil.processSettings(basicSettings)
        stressSettings = GetFlagsStressTestSettingsFactory.build()
    }

    @Test
    fun `independent flags are single-key batches`() {
        val batches = MegGroupPartitionUtil.getFeatureFlagBatches(
            listOf("feature1", "feature2"),
            basicSettings,
        )
        assertEquals(2, batches.size)
        assertTrue(batches.all { it.keys.size == 1 })
    }

    @Test
    fun `meg-linked requested keys are grouped together`() {
        val batches = MegGroupPartitionUtil.getFeatureFlagBatches(
            listOf("feature1"),
            megSettings,
        )
        assertEquals(1, batches.size)
        assertEquals(listOf("feature1"), batches[0].keys)
    }

    /**
     * Full 12-flag stress partition:
     * - 8 independent/salt keys -> 8 single-key batches (parallel-eligible).
     * - MEG random pair -> 1 two-key batch (sequential inside batch).
     * - MEG advance pair -> 1 two-key batch (sequential inside batch).
     */
    @Test
    fun `stress settings partition separates independent and meg-linked flags`() {
        val allKeys = GetFlagsStressTestSettingsFactory.ALL_FLAG_KEYS
        val batches = MegGroupPartitionUtil.getFeatureFlagBatches(allKeys, stressSettings)

        assertEquals(12, allKeys.size)
        assertEquals(10, batches.size)

        val independentAndSalt = GetFlagsStressTestSettingsFactory.INDEPENDENT_FLAG_KEYS +
            GetFlagsStressTestSettingsFactory.SALT_FLAG_KEYS
        independentAndSalt.forEach { key ->
            val batch = batches.single { key in it.keys }
            assertEquals(
                "Independent flag $key must be evaluated in its own batch",
                listOf(key),
                batch.keys,
            )
        }

        val megRandomBatch = batches.single {
            GetFlagsStressTestSettingsFactory.MEG_RANDOM_FLAG_KEYS[0] in it.keys
        }
        assertEquals(
            GetFlagsStressTestSettingsFactory.MEG_RANDOM_FLAG_KEYS,
            megRandomBatch.keys,
        )

        val megAdvanceBatch = batches.single {
            GetFlagsStressTestSettingsFactory.MEG_ADVANCE_FLAG_KEYS[0] in it.keys
        }
        assertEquals(
            GetFlagsStressTestSettingsFactory.MEG_ADVANCE_FLAG_KEYS,
            megAdvanceBatch.keys,
        )
    }

    /**
     * When a key is MEG-linked via campaignGroups but the group has no campaigns
     * (so featureKeys lookup is empty), the key must still be evaluated as a
     * single-key batch instead of being silently dropped.
     */
    @Test
    fun `meg-linked key with empty group featureKeys falls back to single-key batch`() {
        val settings = Settings().apply {
            features = listOf(
                Feature().apply {
                    key = "orphaned_meg_flag"
                    rules = listOf(
                        Rule().apply {
                            campaignId = 101
                            type = "FLAG_TESTING"
                        },
                    )
                },
            )
            campaignGroups = mapOf("101" to 1)
            groups = mapOf(
                "1" to Groups().apply {
                    name = "empty-group"
                    campaigns = emptyList()
                },
            )
        }

        val batches = MegGroupPartitionUtil.getFeatureFlagBatches(
            listOf("orphaned_meg_flag"),
            settings,
        )

        assertEquals(1, batches.size)
        assertEquals(listOf("orphaned_meg_flag"), batches[0].keys)
    }
}
