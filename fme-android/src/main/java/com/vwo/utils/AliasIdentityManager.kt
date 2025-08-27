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

class AliasIdentityManager {

    object Options {

        var isAliasingEnabled: Boolean = false

    }

    private val ID_NOT_FOUND = null

    private val JSON_PREFIX = "[{"

    private val KEY_USER_ID = "userId"
    private val KEY_ALIAS_ID = "aliasId"

    private val KEY_IDENTITY_STORE = "vwo_identity_store_final"

    private val aliasApiService by lazy { AliasApiService() }

    // the aliasId is the userId ( after logging in ) -> is the actual user id after login ; or something that we might get at later stage
    // userId is tempId -> the id that was passed during the VWOUserContext init phase
    // the {alias} will be linked with the {idPassedOnInit}
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
     * Blocks the { Thread } on which this method is being invoked until the processing is complete.
     *
     * @param vwoUserContext the context
     */
    fun maybeGetAliasAwareUserIdSync(vwoUserContext: VWOUserContext?): String? {
        return runBlocking { getAliasLinkedUserId(vwoUserContext = vwoUserContext) }
    }

    /**
     * Get the id based on the passed [vwoUserContext]'s values. This call is an Async call, will
     * not block the { Thread } which invoked this.
     *
     * @param vwoUserContext - the context
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

    private fun getLocallyStoredValuesAsMutableMap(): MutableMap<String, String> {
        val arrFromLocalStorage = getLocalJsonArray() ?: return mutableMapOf()
        val keyValueMap = mutableMapOf<String, String>()
        for (index in 0 until arrFromLocalStorage.length()) {
            val obj = arrFromLocalStorage.getJSONObject(index)
            keyValueMap[obj.getString(KEY_ALIAS_ID)] = obj.getString(KEY_USER_ID)
        }
        return keyValueMap
    }

    private fun getCanonicalIdFor(userContextId: String): String? {
        return getLocallyStoredValuesAsMutableMap()[userContextId]
    }

    private fun getAllSavedAliasAsJsonArray(aliasId: String? = null): String {
        val arr = JSONArray()
        getLocallyStoredValuesAsMutableMap().forEach { arr.put(it.key) }
        aliasId?.let { arr.put(it) }
        return arr.toString()
    }

    private fun getLocalJsonArray(): JSONArray? {
        val localStorage = getLocalStorageController() ?: return null
        val jsonStr = localStorage.getString(KEY_IDENTITY_STORE)
        return if (jsonStr.isNotBlank()) JSONArray(jsonStr) else JSONArray()
    }

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

    private fun getLocalStorageController(): LocalStorageController? {
        val ctx = StorageProvider.contextRef.get() ?: return null
        return LocalStorageController(ctx)
    }

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
     * Send request to the gateway and fetch the id that'll be used for getFlag and etc.
     *
     * @param vwoUserContext        - the user context object
     * @return [Boolean] true if call success, else false
     */
    private suspend fun requestFromGatewayIfNotFoundLocally(vwoUserContext: VWOUserContext): Boolean {

        val userIdFromContext = vwoUserContext.getIdBasedOnSpecificCondition() ?: return false

        val locallySavedCanonicalId = getCanonicalIdFor(userIdFromContext)
        if (locallySavedCanonicalId != null) return false

        return maybeGetAllMappedIdAliasFromServer(userIdFromContext = "[\"$userIdFromContext\"]")
    }

}