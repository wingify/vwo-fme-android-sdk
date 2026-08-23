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
package com.vwo.models

import com.wingify.WingifyClient
import com.wingify.models.Storage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StorageTest {

    private lateinit var storage: Storage

    @Before
    fun setup() {
        storage = Storage()
    }

    @Test
    fun `test default values`() {
        assertNull(storage.featureKey)
        assertNull(storage.user)
        assertNull(storage.rolloutId)
        assertNull(storage.rolloutKey)
        assertNull(storage.rolloutVariationId)
        assertNull(storage.experimentId)
        assertNull(storage.experimentKey)
        assertNull(storage.experimentVariationId)
        assertNull(storage.decisionExpiryTime)
    }

    @Test
    fun `test setting and getting featureKey`() {
        val testFeatureKey = "test_feature_key"
        storage.featureKey = testFeatureKey
        assertEquals(testFeatureKey, storage.featureKey)
    }

    @Test
    fun `test setting and getting user`() {
        val testUser = "test_user"
        storage.user = testUser
        assertEquals(testUser, storage.user)
    }

    @Test
    fun `test setting and getting rolloutId`() {
        val testRolloutId = 123
        storage.rolloutId = testRolloutId
        assertEquals(testRolloutId, storage.rolloutId)
    }

    @Test
    fun `test setting and getting rolloutKey`() {
        val testRolloutKey = "test_rollout_key"
        storage.rolloutKey = testRolloutKey
        assertEquals(testRolloutKey, storage.rolloutKey)
    }

    @Test
    fun `test setting and getting rolloutVariationId`() {
        val testRolloutVariationId = 456
        storage.rolloutVariationId = testRolloutVariationId
        assertEquals(testRolloutVariationId, storage.rolloutVariationId)
    }

    @Test
    fun `test setting and getting experimentId`() {
        val testExperimentId = 789
        storage.experimentId = testExperimentId
        assertEquals(testExperimentId, storage.experimentId)
    }

    @Test
    fun `test setting and getting experimentKey`() {
        val testExperimentKey = "test_experiment_key"
        storage.experimentKey = testExperimentKey
        assertEquals(testExperimentKey, storage.experimentKey)
    }

    @Test
    fun `test setting and getting experimentVariationId`() {
        val testExperimentVariationId = 101112
        storage.experimentVariationId = testExperimentVariationId
        assertEquals(testExperimentVariationId, storage.experimentVariationId)
    }

    @Test
    fun `test setting and getting decisionExpiryTime`() {
        val testExpiryTime = System.currentTimeMillis() + 60000L
        storage.decisionExpiryTime = testExpiryTime
        assertEquals(testExpiryTime, storage.decisionExpiryTime)
    }

    @Test
    fun `test isDecisionExpired returns false when decisionExpiryTime is null`() {
        storage.decisionExpiryTime = null
        assertFalse(storage.isDecisionExpired())
    }

    @Test
    fun `test isDecisionExpired returns false when decisionExpiryTime is zero`() {
        storage.decisionExpiryTime = 0L
        assertFalse(storage.isDecisionExpired())
    }

    @Test
    fun `test isDecisionExpired returns false when decisionExpiryTime is negative`() {
        storage.decisionExpiryTime = -1L
        assertFalse(storage.isDecisionExpired())
    }

    @Test
    fun `test isDecisionExpired returns false when decisionExpiryTime is in the future`() {
        storage.decisionExpiryTime = System.currentTimeMillis() + 60000L
        assertFalse(storage.isDecisionExpired())
    }

    @Test
    fun `test isDecisionExpired returns true when decisionExpiryTime is in the past`() {
        storage.decisionExpiryTime = System.currentTimeMillis() - 1000L
        assertTrue(storage.isDecisionExpired())
    }

    @Test
    fun `test setting and getting all properties`() {
        val testFeatureKey = "test_feature_key"
        val testUser = "test_user"
        val testRolloutId = 123
        val testRolloutKey = "test_rollout_key"
        val testRolloutVariationId = 456
        val testExperimentId = 789
        val testExperimentKey = "test_experiment_key"
        val testExperimentVariationId = 101112
        val testDecisionExpiryTime = System.currentTimeMillis() + 60000L

        storage.apply {
            featureKey = testFeatureKey
            user = testUser
            rolloutId = testRolloutId
            rolloutKey = testRolloutKey
            rolloutVariationId = testRolloutVariationId
            experimentId = testExperimentId
            experimentKey = testExperimentKey
            experimentVariationId = testExperimentVariationId
            decisionExpiryTime = testDecisionExpiryTime
        }

        assertEquals(testFeatureKey, storage.featureKey)
        assertEquals(testUser, storage.user)
        assertEquals(testRolloutId, storage.rolloutId)
        assertEquals(testRolloutKey, storage.rolloutKey)
        assertEquals(testRolloutVariationId, storage.rolloutVariationId)
        assertEquals(testExperimentId, storage.experimentId)
        assertEquals(testExperimentKey, storage.experimentKey)
        assertEquals(testExperimentVariationId, storage.experimentVariationId)
        assertEquals(testDecisionExpiryTime, storage.decisionExpiryTime)
    }

    @Test
    fun `parses holdout id lists written as raw JSON arrays`() {
        // MobileDefaultStorage persists holdoutIds / notInHoldoutIds as raw arrays.
        val json = """
            {
              "rolloutKey": "r1",
              "rolloutId": 10,
              "rolloutVariationId": 1,
              "holdoutIds": [],
              "notInHoldoutIds": [100, 200]
            }
        """.trimIndent()

        val parsed = WingifyClient.objectMapper.readValue(json, Storage::class.java)

        assertEquals(emptyList<Int>(), parsed.holdoutIds?.values)
        assertEquals(listOf(100, 200), parsed.notInHoldoutIds?.values)
        assertEquals("r1", parsed.rolloutKey)
        assertEquals(10, parsed.rolloutId)
    }

    @Test
    fun `parses holdout id lists written as values wrapper objects`() {
        val json = """
            {
              "holdoutIds": {"values": [11]},
              "notInHoldoutIds": {"values": [22, 33]}
            }
        """.trimIndent()

        val parsed = WingifyClient.objectMapper.readValue(json, Storage::class.java)

        assertEquals(listOf(11), parsed.holdoutIds?.values)
        assertEquals(listOf(22, 33), parsed.notInHoldoutIds?.values)
    }

    @Test
    fun `parses holdout id lists from historical myArrayList wrapper`() {
        val json = """
            {
              "holdoutIds": {"myArrayList": []},
              "notInHoldoutIds": {"myArrayList": [100, 200]}
            }
        """.trimIndent()

        val parsed = WingifyClient.objectMapper.readValue(json, Storage::class.java)

        assertEquals(emptyList<Int>(), parsed.holdoutIds?.values)
        assertEquals(listOf(100, 200), parsed.notInHoldoutIds?.values)
    }

    @Test
    fun `round-trips storage map the way GetFlagAPI reads MobileDefaultStorage`() {
        // After JSONObject.toMap() unwraps arrays, GetFlagAPI does:
        // writeValueAsString(map) -> readValue(Storage)
        val map = mapOf(
            "rolloutKey" to "r1",
            "rolloutId" to 10,
            "rolloutVariationId" to 1,
            "holdoutIds" to emptyList<Int>(),
            "notInHoldoutIds" to listOf(100, 200),
        )
        val intermediate = WingifyClient.objectMapper.writeValueAsString(map)
        val parsed = WingifyClient.objectMapper.readValue(intermediate, Storage::class.java)

        assertEquals("r1", parsed.rolloutKey)
        assertEquals(10, parsed.rolloutId)
        assertEquals(listOf(100, 200), parsed.notInHoldoutIds?.values)
        assertEquals(emptyList<Int>(), parsed.holdoutIds?.values)
    }

    @Test
    fun `serializes holdout id lists as raw JSON arrays`() {
        val storage = Storage().apply {
            holdoutIds = Storage.JsonArrayWrapper().apply { values = listOf(1, 2) }
            notInHoldoutIds = Storage.JsonArrayWrapper().apply { values = emptyList() }
        }

        val json = WingifyClient.objectMapper.writeValueAsString(storage)

        assertTrue(json.contains("\"holdoutIds\":[1,2]"))
        assertTrue(json.contains("\"notInHoldoutIds\":[]"))
        assertFalse(json.contains("\"values\""))
        assertFalse(json.contains("\"myArrayList\""))
    }

    @Test
    fun `GetFlagAPI read path parses all three holdout cache formats after toMap`() {
        // Simulates MobileDefaultStorage.get() after JSONObject.toMap() unwrap,
        // then GetFlagAPI: writeValueAsString(map) -> readValue(Storage).

        // Format 1: raw arrays -> List
        assertHoldoutIdsViaStorageReadPath(
            storageMap = mapOf(
                "holdoutIds" to emptyList<Int>(),
                "notInHoldoutIds" to listOf(100, 200),
            ),
            expectedHoldoutIds = emptyList(),
            expectedNotInHoldoutIds = listOf(100, 200),
        )

        // Format 2: {"values":[...]} -> Map("values" to List)
        assertHoldoutIdsViaStorageReadPath(
            storageMap = mapOf(
                "holdoutIds" to mapOf("values" to listOf(11)),
                "notInHoldoutIds" to mapOf("values" to listOf(22, 33)),
            ),
            expectedHoldoutIds = listOf(11),
            expectedNotInHoldoutIds = listOf(22, 33),
        )

        // Format 3: {"myArrayList":[...]} -> Map("myArrayList" to List)
        assertHoldoutIdsViaStorageReadPath(
            storageMap = mapOf(
                "holdoutIds" to mapOf("myArrayList" to emptyList<Int>()),
                "notInHoldoutIds" to mapOf("myArrayList" to listOf(100, 200)),
            ),
            expectedHoldoutIds = emptyList(),
            expectedNotInHoldoutIds = listOf(100, 200),
        )
    }

    /**
     * Mirrors GetFlagAPI / HoldoutGroupService after MobileDefaultStorage.get():
     * storage map -> writeValueAsString -> readValue(Storage)
     */
    private fun assertHoldoutIdsViaStorageReadPath(
        storageMap: Map<String, Any>,
        expectedHoldoutIds: List<Int>,
        expectedNotInHoldoutIds: List<Int>,
    ) {
        val storageMapAsString = WingifyClient.objectMapper.writeValueAsString(storageMap)
        val parsed = WingifyClient.objectMapper.readValue(storageMapAsString, Storage::class.java)

        assertEquals(expectedHoldoutIds, parsed.holdoutIds?.values)
        assertEquals(expectedNotInHoldoutIds, parsed.notInHoldoutIds?.values)
    }
}