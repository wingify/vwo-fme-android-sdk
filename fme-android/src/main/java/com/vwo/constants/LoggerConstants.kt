/*
 * Copyright (c) 2024 Wingify Software Pvt. Ltd.
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

package com.vwo.constants

const val debugMessages = "{\n" +
        "  \"API_CALLED\": \"API called: {apiName}\",\n" +
        "  \"SERVICE_INITIALIZED\": \"VWO {service} initialized while creating an instance of SDK\",\n" +
        "\n" +
        "  \"EXPERIMENTS_EVALUATION_WHEN_ROLLOUT_PASSED\": \"Rollout rule got passed for user {userId}. Hence, evaluating experiments\",\n" +
        "  \"EXPERIMENTS_EVALUATION_WHEN_NO_ROLLOUT_PRESENT\": \"No Rollout rules present for the feature. Hence, checking experiment rules\",\n" +
        "\n" +
        "  \"USER_BUCKET_TO_VARIATION\": \"User ID: {userId} for experiment: {campaignKey} having percent traffic: {percentTraffic} got bucket-value as {bucketValue} and hash-value as {hashValue}\",\n" +
        "\n" +
        "  \"IMPRESSION_FOR_TRACK_USER\": \"Impression built for vwo_variationShown event for Account ID:{accountId}, User ID:{userId}, and experiment ID:{campaignId}\",\n" +
        "  \"IMPRESSION_FOR_TRACK_GOAL\": \"Impression built for {eventName} event for Account ID:{accountId}, and user ID:{userId}\",\n" +
        "  \"IMPRESSION_FOR_SYNC_VISITOR_PROP\": \"Impression built for {eventName} event for Account ID:{accountId}, and user ID:{userId}\"\n" +
        "}"
const val errorMessages = "{\n" +
        "  \"INIT_OPTIONS_ERROR\": \"[ERROR]: VWO-SDK {date} Options should be of type object\",\n" +
        "  \"INIT_OPTIONS_SDK_KEY_ERROR\": \"[ERROR]: VWO-SDK {date} Please provide the sdkKey in the options and should be a of type string\",\n" +
        "  \"INIT_OPTIONS_ACCOUNT_ID_ERROR\": \"[ERROR]: VWO-SDK {date} Please provide VWO account ID in the options and should be a of type string|number\",\n" +
        "\n" +
        "  \"INIT_OPTIONS_INVALID\": \"Invalid {key} passed in options. Should be of type: {correctType} and greater than equal to 1000\",\n" +
        "\n" +
        "  \"SETTINGS_FETCH_ERROR\": \"Settings could not be fetched. Error: {err}\",\n" +
        "  \"SETTINGS_SCHEMA_INVALID\": \"Settings are not valid. Failed schema validation\",\n" +
        "\n" +
        "  \"POLLING_FETCH_SETTINGS_FAILED\": \"Error while fetching VWO settings with polling\",\n" +
        "\n" +
        "  \"API_THROW_ERROR\": \"API - {apiName} failed to execute. Trace - {err}\",\n" +
        "  \"API_INVALID_PARAM\": \"{key} passed to {apiName} API is not of valid type. Got type: {type}, should be: {correctType}\",\n" +
        "  \"API_SETTING_INVALID\": \"Settings are not valid. Contact VWO Support\",\n" +
        "  \"API_CONTEXT_INVALID\": \"Context should be an object and must contain a mandatory key - id, which is User ID\",\n" +
        "\n" +
        "  \"FEATURE_NOT_FOUND\": \"Feature not found for the key {featureKey}\",\n" +
        "  \"EVENT_NOT_FOUND\": \"Event {eventName} not found in any of the features metrics\",\n" +
        "\n" +
        "  \"STORED_DATA_ERROR\": \"Error in getting data from storage. Error: {err}\",\n" +
        "\n" +
        "  \"GATEWAY_URL_ERROR\": \"Please provide a valid URL for VWO Gateway Service\",\n" +
        "\n" +
        "  \"NETWORK_CALL_FAILED\": \"Error occurred while sending {method} request. Error: {err}\"\n" +
        "}"

const val infoMessage = "{\n" +
        "  \"ON_READY_ALREADY_RESOLVED\": \"[INFO]: VWO-SDK {date} {apiName} already resolved\",\n" +
        "  \"ON_READY_SETTINGS_FAILED\": \"[INFO]: VWO-SDK {date} VWO settings could not be fetched\",\n" +
        "\n" +
        "  \"POLLING_SET_SETTINGS\": \"There's a change in settings from the last settings fetched. Hence, instantiating a new VWO client internally\",\n" +
        "  \"POLLING_NO_CHANGE_IN_SETTINGS\": \"No change in settings with the last settings fetched. Hence, not instantiating new VWO client\",\n" +
        "\n" +
        "  \"SETTINGS_FETCH_SUCCESS\": \"Settings fetched successfully\",\n" +
        "\n" +
        "  \"CLIENT_INITIALIZED\": \"VWO Client initialized\",\n" +
        "\n" +
        "  \"STORED_VARIATION_FOUND\": \"Variation {variationKey} found in storage for the user {userId} for the {experimentType} experiment:{experimentKey}\",\n" +
        "\n" +
        "  \"USER_PART_OF_CAMPAIGN\": \"User ID:{userId} is {notPart} part of experiment: {campaignKey}\",\n" +
        "  \"SEGMENTATION_SKIP\": \"For userId:{userId} of experiment:{campaignKey}, segments was missing. Hence, skipping segmentation\",\n" +
        "  \"SEGMENTATION_STATUS\": \"Segmentation {status} for userId:{userId} of experiment:{campaignKey}\",\n" +
        "\n" +
        "  \"USER_CAMPAIGN_BUCKET_INFO\": \"User ID:{userId} for experiment:{campaignKey} {status}\",\n" +
        "\n" +
        "  \"WHITELISTING_SKIP\": \"Whitelisting is not used for experiment:{campaignKey}, hence skipping evaluating whitelisting {variation} for User ID:{userId}\",\n" +
        "  \"WHITELISTING_STATUS\": \"User ID:{userId} for experiment:{campaignKey} {status} whitelisting {variationString}\",\n" +
        "\n" +
        "  \"VARIATION_RANGE_ALLOCATION\": \"Variation:{variationKey} of experiment:{campaignKey} having weight:{variationWeight} got bucketing range: ({startRange} - {endRange})\",\n" +
        "\n" +
        "  \"IMPACT_ANALYSIS\": \"Sending data for Impact Campaign for the user {userId}\",\n" +
        "\n" +
        "  \"MEG_SKIP_ROLLOUT_EVALUATE_EXPERIMENTS\": \"No rollout rule found for feature:{featureKey}. Hence, evaluating experiments\",\n" +
        "  \"MEG_CAMPAIGN_FOUND_IN_STORAGE\": \"Campaign {campaignKey} found in storage for user ID:{userId}\",\n" +
        "  \"MEG_CAMPAIGN_ELIGIBLE\": \"Campaign {campaignKey} is eligible for user ID:{userId}\",\n" +
        "  \"MEG_WINNER_CAMPAIGN\": \"MEG: Campaign {campaignKey} is the winner for group {groupId} for user ID:{userId} {algo}\"\n" +
        "}"
const val traceMessage = "{}"
const val warnMessage = "{}"

val logTemplates = mapOf(
    "debug-messages.json" to debugMessages,
    "info-messages.json" to infoMessage,
    "trace-messages.json" to traceMessage,
    "warn-messages.json" to warnMessage,
    "error-messages.json" to errorMessages
)