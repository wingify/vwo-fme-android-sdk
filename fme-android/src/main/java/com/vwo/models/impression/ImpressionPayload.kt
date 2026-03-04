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
package com.vwo.models.impression

import com.vwo.constants.Constants.IMPRESSION_NO_FEATURE_ID

/**
 * Container for collecting and managing impression events before batch transmission.
 *
 * This class serves as an accumulator for [Impression] instances that track
 * campaign-variation pairs shown to users. It provides validation and retrieval
 * operations to ensure only valid impressions are processed and sent to VWO's
 * analytics system as a batch.
 *
 * The class maintains data integrity by:
 * - Storing impressions internally and providing controlled access
 * - Validating data before allowing batch transmission
 * - Preventing invalid or empty payloads from being processed
 *
 * **Usage Pattern:**
 * During feature flag evaluation, multiple campaigns and variations may be
 * evaluated and shown to a user. This class collects all such impressions
 * and sends them as a single batch operation to optimize network usage and
 * reduce latency.
 *
 * @property impressionList Internal storage for impression events awaiting transmission.
 *                          Defaults to an empty list for a fresh payload instance.
 */
data class ImpressionPayload(
    val impressionList: ArrayList<Impression> = ArrayList(),
) {

    /**
     * Retrieves the impression at the specified index.
     *
     * This method provides indexed access to stored impressions, typically used
     * during batch processing to iterate over collected impressions.
     *
     * @param index The zero-based index of the impression to retrieve.
     * @return The [Impression] instance at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of valid range.
     */
    fun get(index: Int): Impression {
        return impressionList[index]
    }

    /**
     * Adds a new impression event to the payload for batch transmission.
     *
     * This method creates an [Impression] instance from the provided campaign and
     * variation IDs and appends it to the internal collection. The impression will
     * be included when the payload is processed and sent to VWO's analytics system.
     *
     * **Note:** Both IDs are expected to be positive integers representing valid
     * campaign and variation identifiers. Zero values are used to indicate invalid
     * states in cases where a campaign or variation couldn't be determined.
     *
     * @param campaignId The unique identifier for the campaign associated with this impression breakdown.
     * @param variationId The unique identifier for the variation that was shown to the user.
     * @param featureId The feature id.
     */
    fun add(campaignId: Int, variationId: Int, featureId: Int = IMPRESSION_NO_FEATURE_ID) {
        val hasThisEntry = impressionList.any {
            it.campaignId == campaignId && it.variationId == variationId
        }
        if (!hasThisEntry) {
            impressionList.add(
                Impression(
                    campaignId = campaignId,
                    variationId = variationId,
                    featureId = featureId
                )
            )
        }
    }

    /**
     * Determines whether the payload contains impression data ready for processing.
     *
     * This method performs a simple validity check by verifying that at least one
     * impression has been added to the payload. A payload is considered valid when
     * it contains impressions that can be sent to the analytics system.
     *
     * **Use Case:** Call this method before attempting to send impressions to avoid
     * unnecessary network requests or batch processing operations on empty payloads.
     *
     * @return `true` if the payload contains one or more valid impressions, `false` otherwise.
     *
     * @see hasNoValidData for the inverse check
     * @see size for getting the count of impressions
     */
    fun hasValidData(): Boolean {
        return !hasNoValidData()
    }

    /**
     * Determines if the payload is empty and has nothing to process.
     *
     * This method checks whether any impressions have been added to the payload.
     * An empty payload indicates that no impressions were collected during the
     * current evaluation cycle, and thus no batch transmission is necessary.
     *
     * **Performance Consideration:** Using this check before batch operations
     * helps avoid unnecessary work when no impression events have occurred.
     *
     * @return `true` if the payload is empty (no impressions collected), `false` otherwise.
     *
     * @see hasValidData for the inverse check
     */
    fun hasNoValidData(): Boolean {
        return impressionList.isEmpty()
    }

    /**
     * Returns the number of impressions collected in the payload.
     *
     * This method provides the count of impressions that will be included when
     * the payload is processed and sent as a batch. The size can be used for
     * logging, metrics, or determining batch processing parameters.
     *
     * **Behavior:** Returns 0 if the payload has no valid data, otherwise returns
     * the actual count of impressions. This ensures that empty payloads always
     * report zero size, providing a consistent interface for validation checks.
     *
     * @return The number of impressions in the payload. Returns 0 if the payload is empty.
     *
     * @see hasValidData to check if the payload is non-empty before calling size()
     */
    fun size(): Int {
        return if (hasValidData()) impressionList.size else 0
    }
}