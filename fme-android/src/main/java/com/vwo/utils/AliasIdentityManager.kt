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
package com.vwo.utils

import com.vwo.models.user.VWOUserContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.storage.LocalStorageController
import com.vwo.providers.StorageProvider
import com.vwo.services.AliasApiService
import com.vwo.services.LoggerService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Manages user identity aliasing functionality for VWO SDK.
 * 
 * This class handles the mapping between user IDs and alias IDs, providing
 * both synchronous and asynchronous methods to retrieve canonical user IDs
 * based on various user contexts. It also manages local storage of identity
 * mappings and communicates with the alias API service.
 */
class AliasIdentityManager {

    object Options {

        /**
         * Flag to enable/disable aliasing functionality.
         */
        var isAliasingEnabled: Boolean = false

        /**
         * Flag indicating if the gateway is configured.
         */
        var isGatewaySet: Boolean = false

    }

    private val ID_NOT_FOUND = null

    private val JSON_PREFIX = "[{"

    private val KEY_USER_ID = "userId"
    private val KEY_ALIAS_ID = "aliasId"

    private val KEY_IDENTITY_STORE = "vwo_identity_store_final"

    private val aliasApiService by lazy { AliasApiService() }

    /**
     * Sets an alias mapping between a user ID and an alias ID.
     * 
     * This method makes an asynchronous API call to establish the relationship
     * between the provided user ID and alias ID. Upon successful completion,
     * it fetches updated alias mappings from the server.
     * 
     * @param userId The user identifier to be aliased
     * @param aliasId The alias identifier to associate with the user ID
     */
    fun setAlias(userId: String, aliasId: String) {

        ioThreadAsync(callback = {
            val response = aliasApiService.setAlias(userId, aliasId)

            if (response?.statusCode != 200) {
                // the request was not successful.
                val errorMap =
                    mapOf<String?, String?>("err" to "Status CODE: ${response?.statusCode}")
                LoggerService.log(LogLevelEnum.ERROR, "SET_ALIAS_ERROR", errorMap)
                return@ioThreadAsync
            }

            // GET updated values for all alias
            maybeGetAllMappedIdAliasFromServer(
                userIdFromContext = getAllSavedAliasAsJsonArray(aliasId)
            )

        }, exceptionDuringProcessing = {

            val errorMap = mapOf<String?, String?>("err" to it.message)
            LoggerService.log(LogLevelEnum.ERROR, "ALIAS_NETWORK_SDK_ERROR", errorMap)
        })

    }

    /**
     * Retrieves the alias-aware user ID synchronously.
     * 
     * This method blocks the calling thread until the alias resolution is complete.
     * It internally calls the asynchronous version and waits for the result.
     * 
     * @param vwoUserContext The user context containing identification information
     * @return The canonical user ID if found, null otherwise
     */
    fun maybeGetAliasAwareUserIdSync(vwoUserContext: VWOUserContext?): String? {
        return runBlocking { getAliasLinkedUserId(vwoUserContext = vwoUserContext) }
    }

    /**
     * Retrieves the canonical user ID based on the provided user context.
     * 
     * This is an asynchronous method that first checks local storage for existing
     * mappings. If not found locally, it makes a network request to fetch updated
     * mappings from the server.
     * 
     * @param vwoUserContext The user context containing identification information
     * @return The canonical user ID if found, null otherwise
     */
    private suspend fun getAliasLinkedUserId(
        vwoUserContext: VWOUserContext?
    ): String? {

        val userContextId = vwoUserContext?.getIdBasedOnSpecificCondition() ?: return ID_NOT_FOUND

        // if found locally SKIP network call
        val id = getCanonicalIdFor(userContextId)
        if (ID_NOT_FOUND != id) return id

        // check if server has the updated values; fetch then store it
        val isSuccess = requestFromGatewayIfNotFoundLocally(vwoUserContext)
        val canonicalId = when (isSuccess) {
            true -> getCanonicalIdFor(userContextId) // query the local storage once again after we get the response from server
            else -> ID_NOT_FOUND
        }
        return canonicalId
    }

    /**
     * Saves the alias mapping data to local storage.
     * 
     * Converts the provided map of alias ID to user ID mappings into a JSON array
     * format and stores it in the local storage using the identity store key.
     * 
     * @param map A mutable map containing alias ID to user ID mappings
     */
    private fun saveMutableMapToLocalStorage(map: MutableMap<String, String>) {

        val localStorage = getLocalStorageController() ?: return

        val finalJsonArray = JSONArray()
        map.forEach {
            val item = JSONObject().apply {
                put(KEY_ALIAS_ID, it.key)
                put(KEY_USER_ID, it.value)
            }
            finalJsonArray.put(item)
        }

        localStorage.saveString(KEY_IDENTITY_STORE, finalJsonArray.toString())
    }

    /**
     * Retrieves locally stored alias mappings as a mutable map.
     * 
     * Reads the JSON array from local storage and converts it back to a map
     * where keys are alias IDs and values are user IDs.
     * 
     * @return A mutable map containing alias ID to user ID mappings, empty map if no data exists
     */
    private fun getLocallyStoredValuesAsMutableMap(): MutableMap<String, String> {
        val arrFromLocalStorage = getLocalJsonArray() ?: return mutableMapOf()
        val keyValueMap = mutableMapOf<String, String>()
        for (index in 0 until arrFromLocalStorage.length()) {
            val obj = arrFromLocalStorage.getJSONObject(index)
            keyValueMap[obj.getString(KEY_ALIAS_ID)] = obj.getString(KEY_USER_ID)
        }
        return keyValueMap
    }

    /**
     * Retrieves the canonical user ID for a given user context ID.
     * 
     * Looks up the provided user context ID in the locally stored mappings
     * to find its corresponding canonical user ID.
     * 
     * @param userContextId The user context ID to look up
     * @return The canonical user ID if found, null otherwise
     */
    private fun getCanonicalIdFor(userContextId: String): String? {
        return getLocallyStoredValuesAsMutableMap()[userContextId]
    }

    /**
     * Creates a JSON array string containing all saved alias IDs.
     * 
     * Retrieves all locally stored alias IDs and optionally adds a specific
     * alias ID to the array before converting to JSON string format.
     * 
     * @param aliasId Optional alias ID to include in the array
     * @return JSON array string containing all alias IDs
     */
    private fun getAllSavedAliasAsJsonArray(aliasId: String? = null): String {
        val arr = JSONArray()
        getLocallyStoredValuesAsMutableMap().forEach { arr.put(it.key) }
        aliasId?.let { arr.put(it) }
        return arr.toString()
    }

    /**
     * Retrieves the JSON array from local storage.
     * 
     * Reads the identity store data from local storage and parses it as a JSON array.
     * Returns an empty JSON array if no data exists or if the data is invalid.
     * 
     * @return JSONArray containing the stored identity mappings, or null if storage is unavailable
     */
    private fun getLocalJsonArray(): JSONArray? {
        val localStorage = getLocalStorageController() ?: return null
        val jsonStr = localStorage.getString(KEY_IDENTITY_STORE)
        return if (jsonStr.isNotBlank()) JSONArray(jsonStr) else JSONArray()
    }

    /**
     * Executes a callback function asynchronously on the IO dispatcher.
     * 
     * Launches a coroutine on the IO thread pool to execute the provided callback.
     * Handles exceptions during execution and calls the exception handler if provided.
     * 
     * @param callback The suspend function to execute asynchronously
     * @param exceptionDuringProcessing Function to handle exceptions during execution
     */
    private fun ioThreadAsync(
        callback: suspend () -> Unit,
        exceptionDuringProcessing: (Throwable) -> Unit
    ) {
        val err = CoroutineExceptionHandler { _, throwable ->
            exceptionDuringProcessing(throwable)
        }
        CoroutineScope(Dispatchers.IO + err).launch {
            try {
                callback()
            } catch (exception: Exception) {
                exceptionDuringProcessing(exception)
            }
        }
    }

    /**
     * Retrieves the local storage controller instance.
     * 
     * Gets the application context from the storage provider and creates
     * a LocalStorageController instance for data persistence operations.
     * 
     * @return LocalStorageController instance if context is available, null otherwise
     */
    private fun getLocalStorageController(): LocalStorageController? {
        val ctx = StorageProvider.contextRef.get() ?: return null
        return LocalStorageController(ctx)
    }

    /**
     * Fetches alias mappings from the server for a given user ID.
     * 
     * Makes an asynchronous API call to retrieve alias mappings associated with
     * the provided user ID. Returns a pair containing success status and response data.
     * 
     * @param userId The user ID to fetch alias mappings for
     * @return Pair<Boolean, String> where first element indicates success and second contains response data or error message
     */
    private suspend fun getAliasMappingFromServer(userId: String) =
        suspendCoroutine<Pair<Boolean, String>> { cont ->

            ioThreadAsync(callback = {
                val response = aliasApiService.getAlias(userId)

                if (response?.statusCode != 200) {

                    val errorMap =
                        mapOf<String?, String?>("err" to "Status code: ${response?.statusCode}")
                    LoggerService.log(LogLevelEnum.ERROR, "GET_ALIAS_ERROR", errorMap)
                    cont.resume(Pair(false, errorMap["err"] ?: ""))
                    return@ioThreadAsync
                }

                response.data?.let { cont.resume(Pair(true, it)) } ?: kotlin.run {

                    val errorMap = mapOf<String?, String?>("err" to "Invalid data error.")
                    LoggerService.log(LogLevelEnum.ERROR, "GET_ALIAS_ERROR", errorMap)
                    cont.resume(Pair(false, errorMap["err"] ?: ""))
                }
            }, exceptionDuringProcessing = { ex ->

                val errorMap = mapOf<String?, String?>("err" to "${ex.message}")
                LoggerService.log(LogLevelEnum.ERROR, "ALIAS_NETWORK_SDK_ERROR", errorMap)

                cont.resume(Pair(false, ("Exception: ${ex.message}")))
            })
        }

    /**
     * Fetches and updates all mapped ID aliases from the server.
     * 
     * Retrieves alias mappings from the server for the provided user IDs,
     * merges them with existing local mappings, and saves the updated data
     * to local storage.
     * 
     * @param userIdFromContext JSON array string containing user IDs to fetch mappings for
     * @return true if the operation was successful, false otherwise
     */
    private suspend fun maybeGetAllMappedIdAliasFromServer(userIdFromContext: String): Boolean {

        val result = getAliasMappingFromServer(userIdFromContext)

        if (!result.first) return false

        val json = result.second
        if (json.startsWith(JSON_PREFIX)) {

            val mapped = getLocallyStoredValuesAsMutableMap()

            // as per discussion on 21 Aug, 2025
            val responseArray = JSONArray(json)
            for (index in 0 until responseArray.length()) {
                val responseItem = responseArray.getJSONObject(index)
                val key = responseItem.getString(KEY_ALIAS_ID)
                val value = responseItem.getString(KEY_USER_ID)
                mapped[key] = value
            }

            saveMutableMapToLocalStorage(mapped)

            return true
        }

        return false // because the data was not JSON
    }

    /**
     * Requests alias mappings from the gateway if not found locally.
     * 
     * Checks if the canonical ID for the given user context exists locally.
     * If not found, makes a request to the gateway to fetch updated mappings
     * for the user ID.
     * 
     * @param vwoUserContext The user context containing identification information
     * @return true if the gateway request was successful, false otherwise
     */
    private suspend fun requestFromGatewayIfNotFoundLocally(vwoUserContext: VWOUserContext): Boolean {

        val userIdFromContext = vwoUserContext.getIdBasedOnSpecificCondition() ?: return false

        val locallySavedCanonicalId = getCanonicalIdFor(userIdFromContext)
        if (locallySavedCanonicalId != null) return false

        return maybeGetAllMappedIdAliasFromServer(userIdFromContext = "[\"$userIdFromContext\"]")
    }

}