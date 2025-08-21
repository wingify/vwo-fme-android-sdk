package com.vwo.utils

import com.vwo.models.user.VWOUserContext
import com.vwo.packages.logger.enums.LogLevelEnum
import com.vwo.packages.storage.LocalStorageController
import com.vwo.providers.StorageProvider
import com.vwo.services.LoggerService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val ERROR_INVALID_DATA_FROM_SERVER = "Got invalid data from server, statusCode."

object AliasIdentityManager {

    private val ID_NOT_FOUND = null

    private const val KEY_IDENTITY_STORE = "vwo_identity_store_final"

    private const val KEY_USER_ID = "userId"
    private const val KEY_ALIAS_ID = "aliasId"

    init {
        LoggerService.log(
            LogLevelEnum.INFO, "NOTE: canonical (id = userId) used internally for getFlag()"
        )
    }

    private fun String?.log() = LoggerService.log(LogLevelEnum.INFO, "FINAL_NVN_CALL: $this")

    // the aliasId is the userId ( after logging in ) -> is the actual user id after login ; or something that we might get at later stage
    // userId is tempId -> the id that was passed during the VWOUserContext init phase
    // the {alias} will be linked with the {idPassedOnInit}
    fun setAlias(userId: String, aliasId: String) {

        ioThreadAsync(callback = {
            val response = NetworkUtil.AliasApiService.setAlias(userId, aliasId)
            ("got response back after invoking >> setAlias($userId, $aliasId) <<").log()

            if (response?.statusCode != 200) {
                // the request was not successful.
                ("[ ERROR IN REQUEST ]: error:${response?.error}, statusCode:${response?.statusCode}, headers:${response?.headers}, data:${response?.data}")
                return@ioThreadAsync
            }

            // GET for all alias

            maybeGetAllMappedIdAliasFromServer(
                userIdFromContext = getAllSavedAliasAsJsonArray()
            ) {

            }

            ("[ SUCCESS ] got back response from server, but we do not need what's being received ...").log()
        }, exceptionDuringProcessing = {
            it.printStackTrace()
            ("could not send alias to server SDK side error -> ${it.message}")
        })

        "alias won't be saved on local storage ... next call to get id example getFlag() will directly ask the server ...".log()
    }

    /**
     *
     * @param vwoUserContext        - the user context object
     * @param callback              - the optional callback that is invoked if required,
     *                                  true if call success, else false
     */
    fun requestFromGatewayIfNotFoundLocally(
        vwoUserContext: VWOUserContext,
        callback: ((Boolean) -> Unit)? = null
    ) {

        val userIdFromContext = vwoUserContext.maybeGetQualifyingId() ?: run {
            "[ STOP ] the passed user id is invalid ...".log()
            return
        }

        ("[ SEARCH ] checking local storage for $userIdFromContext's mapping ....").log()
        val locallySavedCanonicalId = maybeGetCanonicalIdFromLocalStorage(userIdFromContext)
        if (locallySavedCanonicalId != null) {

            ("[ FOUND ] mapping $userIdFromContext mapped to -> $locallySavedCanonicalId ...").log()
            return
        }

        ("did not find anything stored locally for $userIdFromContext; send request to server ...").log()
        maybeGetAllMappedIdAliasFromServer(
            userIdFromContext = userIdFromContext,
            callback = callback
        )

    }

    private fun maybeGetAllMappedIdAliasFromServer(
        userIdFromContext: String,
        callback: ((Boolean) -> Unit)?
    ) {
        getAliasMappingFromServer(
            userIdFromContext,
            success = { json ->

                if (json.startsWith("[{")) {

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
                } else {

                    // TODO remove this code because final code will be [{}] and not {} :D
                    // example response from server -> {"aliasId":"Scenario_1_LOGIN_USER_ID_1"}
                    val canonicalIdFromServer = JSONObject(json).getString(KEY_ALIAS_ID)
                    ("[ SERVER_RESPONSE ] -> $json").log()
                    ("[ USEFUL_INFO ] $userIdFromContext is mapped to -> { $canonicalIdFromServer }").log()

                    saveCanonicalIdLocallyAfterGetAlias(userIdFromContext, canonicalIdFromServer)
                }

                callback?.invoke(true)
            }, error = {
                callback?.invoke(false)

                it.log()
                ("error occurred $it").log()
            }
        )
    }

    private fun saveMutableMapToLocalStorage(map: MutableMap<String, String>) {

        val localStorage = getLocalStorageController() ?: kotlin.run {
            "[ ERROR ] getLocalStorageController() returned null ...".log()
            return
        }

        val finalJsonArray = JSONArray()
        map.forEach {
            val item = JSONObject().apply {
                put(KEY_ALIAS_ID, it.key)
                put(KEY_USER_ID, it.value)
            }
            finalJsonArray.put(item)
        }


        println("[ SAVE ] to be saved $finalJsonArray")
        localStorage.saveString(KEY_IDENTITY_STORE, finalJsonArray.toString())

    }

    private fun getLocallyStoredValuesAsMutableMap(): MutableMap<String, String> {
        val arrFromLocalStorage = getLocalJsonArray() ?: kotlin.run {
            "[ ERROR ] getLocalJsonArray() returned null ...".log()
            return mutableMapOf()
        }

        "locally saved entries count ${arrFromLocalStorage.length()} ...".log()
        val keyValueMap = mutableMapOf<String, String>()
        for (index in 0 until arrFromLocalStorage.length()) {
            val obj = arrFromLocalStorage.getJSONObject(index)
            keyValueMap[obj.getString(KEY_ALIAS_ID)] = obj.getString(KEY_USER_ID)
        }
        return keyValueMap
    }

    /**
     * Save the [canonicalIdFromServer] but only after a response is received from server. Never use
     * this to save anything from [setAlias] method.
     *
     * @param userIdFromContext     - the user id that was passed in [VWOUserContext]
     * @param canonicalIdFromServer - the id received after successful API call.
     */
    private fun saveCanonicalIdLocallyAfterGetAlias(
        userIdFromContext: String,
        canonicalIdFromServer: String
    ) {

        val mappedValues = getLocallyStoredValuesAsMutableMap()

        if (mappedValues.contains(userIdFromContext)) {
            "[ DUPLICATE ] key `$userIdFromContext` already exist and it is mapped to ${mappedValues[userIdFromContext]}".log()
            return
        }

        mappedValues[userIdFromContext] = canonicalIdFromServer

        saveMutableMapToLocalStorage(mappedValues)

        "[ LOCAL JSON ] updated local values -> ${getLocallyStoredValuesAsMutableMap()}".log()
    }

    // we get canonical id by comparing the alias / user id from context to associated ids
    private fun maybeGetCanonicalIdFromLocalStorage(userIdFromContext: String): String? {
        return getLocallyStoredValuesAsMutableMap()[userIdFromContext]
    }

    private fun getCanonicalIdFor(userContextId: String): String? {
        return getLocallyStoredValuesAsMutableMap()[userContextId]
    }

    /**
     * Blocks the { Thread } on which this method is being invoked until the processing is complete.
     *
     * @param vwoUserContext the context
     */
    fun maybeGetAliasAwareUserIdSync(
        vwoUserContext: VWOUserContext?,
    ): String? {
        val start = System.nanoTime()
        val result = runBlocking {
            maybeGetAliasAwareUserIdSuspend(vwoUserContext = vwoUserContext)
        }
        val end = System.nanoTime()
        val actualTime = TimeUnit.NANOSECONDS.toMillis((end - start))
        ("time taken for alias retrieval -> $actualTime").log()
        return result
    }

    /**
     * Get the id based on the passed [vwoUserContext]'s values. This call is an Async call, will
     * not block the { Thread } which invoked this.
     *
     * @param vwoUserContext - the context
     * @param callback       - the callback where invokee will get the id
     */
    fun maybeGetAliasAwareUserIdAsync(
        vwoUserContext: VWOUserContext?,
        callback: ((String?) -> Unit)? = null
    ) {

        "[ ASYNC ] init :: maybeGetAliasAwareUserIdAsync()".log()

        val userContextId = vwoUserContext?.maybeGetQualifyingId() ?: run {
            "[ ERROR ] cannot find user id ".log()
            callback?.invoke(ID_NOT_FOUND)
            return
        }

        // if found locally SKIP network call
        val id = getCanonicalIdFor(userContextId)
        if (ID_NOT_FOUND != id) {
            "[ SAVE API CALL ] found the user id in local storage $id".log()
            callback?.invoke(id)
            return
        }

        // check if server has the updated values; fetch then store it
        "[ SEARCH COMPLETE ] did not find any flag for $userContextId locally ... trying to get from server ...".log()
        requestFromGatewayIfNotFoundLocally(vwoUserContext) { isSuccess ->
            when (isSuccess) {
                // query the local storage once again after we get the response from server
                true -> {
                    val id = getCanonicalIdFor(userContextId)
                    "[ YES ] found id ...".log()
                    callback?.invoke(id)
                }

                else -> {
                    "[ NOPE ] could not find anything even on server ...".log()
                    callback?.invoke(ID_NOT_FOUND)
                }
            }

            "[ AFTER ] search locally :: if not found request server >> send request to server :: store it locally >> search locally :: return result ".log()
        }

    }

    /**
     * This method acts as the bridge between blocking call and the async call with the same
     * method name.
     *
     * @param vwoUserContext - the context
     */
    private suspend fun maybeGetAliasAwareUserIdSuspend(
        vwoUserContext: VWOUserContext?,
    ) = suspendCoroutine<String?> { continuation ->
        maybeGetAliasAwareUserIdAsync(vwoUserContext = vwoUserContext, callback = { id ->
            continuation.resume(id)
        })
    }

    private fun getLocalJsonArray(): JSONArray? {

        // NEW CHANGE: [{"key": "value"}, ..... and so on]
        val localStorage = getLocalStorageController() ?: return null
        val jsonStr = localStorage.getString(KEY_IDENTITY_STORE)
        return if (jsonStr.isNotBlank()) JSONArray(jsonStr) else JSONArray()
    }

    private fun getLocalStorageController(): LocalStorageController? {
        val ctx = StorageProvider.contextRef.get() ?: run {
            "[ ERROR ] StorageProvider.contextRef is null ...".log()
            return null
        }
        return LocalStorageController(ctx)
    }

    private fun getAliasMappingFromServer(
        userId: String,
        success: (String) -> Unit,
        error: (String) -> Unit
    ) {
        ioThreadAsync(callback = {
            val response = NetworkUtil.AliasApiService.getAlias(userId)
            if (response?.statusCode == 200) {
                response.data?.let { success(it) }
                    ?: error("$ERROR_INVALID_DATA_FROM_SERVER ${response.statusCode}")
            } else {
                error("statusCode: ${response?.statusCode}; cannot get expected response from server: ${response?.error}")
            }
        }, exceptionDuringProcessing = { ex ->
            error("SDK error while getAlias() -> ${ex.message}")
        })
    }

    private fun getAllSavedAliasAsJsonArray(): String {
        val arr = JSONArray()
        getLocallyStoredValuesAsMutableMap().forEach { arr.put(it.key) }
        return arr.toString()
    }

    private fun ioThreadAsync(
        callback: () -> Unit,
        exceptionDuringProcessing: (Throwable) -> Unit
    ) {
        val err = CoroutineExceptionHandler { coroutineContext, throwable ->
            ("SOMETHING HAPPENED: ${throwable.message}")
            exceptionDuringProcessing(throwable)
        }
        CoroutineScope(Dispatchers.IO + err).launch {
            try {
                callback()
            } catch (exception: Exception) {
                /*some processing error occurred*/
                exceptionDuringProcessing(exception)
            }
        }
    }

}