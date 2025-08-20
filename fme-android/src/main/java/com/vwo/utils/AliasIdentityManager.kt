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

    private const val KEY_CANONICAL_ID = "canonical_id"
    private const val KEY_ASSOCIATED_IDS = "associated_ids"
    private const val KEY_IDENTITY_STORE = "vwo_identity_store_final"

    init {
        LoggerService.log(
            LogLevelEnum.INFO, "NOTE: canonical (id = userId) used internally for getFlag()"
        )
    }

    private fun String?.log() = LoggerService.log(LogLevelEnum.INFO, "FINAL_NVN_CALL: $this")

    /**
     * An alias can only be associated with one tempId.
     *
     * @return [Boolean] true if already associated, else false
     */
    private fun isAlreadyAssociated(aliasId: String): Boolean {
        val locallySavedArray = getLocalJsonArray()
        if (locallySavedArray != null) {
            for (i in 0 until locallySavedArray.length()) {
                val obj = locallySavedArray.getJSONObject(i)

                if (obj.optString(KEY_ASSOCIATED_IDS, "").contains(aliasId)) {

                    // count occurrence, if > 1, return true
                    val associatedIds = obj.getJSONArray(KEY_ASSOCIATED_IDS)
                    var occurrenceCounter = 0
                    for (j in 0 until associatedIds.length()) {
                        val id = associatedIds.getString(j)
                        if (aliasId == id) occurrenceCounter++
                    }

                    if (occurrenceCounter > 1) {
                        LoggerService.log(
                            LogLevelEnum.ERROR,
                            "Alias `$aliasId` has already been mapped to another userId."
                        )
                        return true
                    }
                }
            }
        }
        return false
    }

    // the aliasId is the userId ( after logging in ) -> is the actual user id after login ; or something that we might get at later stage
    // userId is tempId -> the id that was passed during the VWOUserContext init phase
    // the {alias} will be linked with the {idPassedOnInit}
    fun setAlias(userId: String, aliasId: String) {

        // check if already aliased
        if (isAlreadyAssociated(aliasId)) {
            "cannot set the same alias twice $aliasId is already associated with a canonical id ...".log()
            return
        }

        ioThreadAsync(callback = {
            val response = NetworkUtil.AliasApiService.setAlias(userId, aliasId)
            ("got response back after invoking >> setAlias($userId, $aliasId) <<").log()

            if (response?.statusCode != 200) {
                // the request was not successful.
                ("[ ERROR IN REQUEST ]: error:${response?.error}, statusCode:${response?.statusCode}, headers:${response?.headers}, data:${response?.data}")
                return@ioThreadAsync
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
     * @param vwoUserContext - the user context object
     * @param callback       - the optional callback that is invoked if required,
     *                         true if call success, else false
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

        makeResolveIdentityCall(
            userIdFromContext,
            success = { json ->

                // example response from server -> {"aliasId":"Scenario_1_LOGIN_USER_ID_1"}
                val canonicalIdFromServer = JSONObject(json).getString("aliasId")
                ("[ SERVER_RESPONSE ] -> $json").log()
                ("[ USEFUL_INFO ] $userIdFromContext is mapped to -> { $canonicalIdFromServer }").log()

                saveCanonicalIdLocallyAfterGetAlias(userIdFromContext, canonicalIdFromServer)

                callback?.invoke(true)
            }, error = {
                callback?.invoke(false)

                it.log()
                ("error occurred $it").log()
            }
        )
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
        val localStorage = getLocalStorageController() ?: kotlin.run {
            "[ ERROR ] getLocalStorageController() returned null ...".log()
            return
        }
        val arrFromLocalStorage = getLocalJsonArray() ?: kotlin.run {
            "[ ERROR ] getLocalJsonArray() returned null ...".log()
            return
        }

        "locally saved entries count ${arrFromLocalStorage.length()} ...".log()
        "[ AVOID DUPLICATE ] making sure ($canonicalIdFromServer) is not already present in local entries ...".log()

        // check if we already have the same canonical id
        for (index in 0 until arrFromLocalStorage.length()) {
            val obj = arrFromLocalStorage.getJSONObject(index)

            if (obj.optString(KEY_CANONICAL_ID, "") == canonicalIdFromServer) {
                val ids = obj.getJSONArray(KEY_ASSOCIATED_IDS)

                "[ SKIP ] found locally saved id $canonicalIdFromServer .. aborting operation ...".log()
                // if there's already a canonical id just update the associated id
                ids.put(userIdFromContext)

                obj.put(KEY_ASSOCIATED_IDS, ids)

                ("$userIdFromContext will be added to list $obj ::: final updated array -> $arrFromLocalStorage").log()

                localStorage.saveString(KEY_IDENTITY_STORE, arrFromLocalStorage.toString())

                return
            }

        }

        // create a new entry because the old one doesn't exist
        val objCanonical = JSONObject().apply {
            put(KEY_CANONICAL_ID, canonicalIdFromServer)
            put(KEY_ASSOCIATED_IDS, JSONArray().apply { put(userIdFromContext) })
        }
        arrFromLocalStorage.put(objCanonical)
        localStorage.saveString(KEY_IDENTITY_STORE, arrFromLocalStorage.toString())

        "[ LOCAL JSON ] updated local values -> ${getLocalJsonArray()}".log()
    }

    // we get canonical id by comparing the alias / user id from context to associated ids
    private fun maybeGetCanonicalIdFromLocalStorage(userIdFromContext: String): String? {
        val localJsonArray = getLocalJsonArray() ?: return null // cannot get any entries
        if (localJsonArray.length() == 0) return null // there is no entry

        "[ SEARCH ] maybeGetCanonicalIdFromLocalStorage() -> localJsonArray size = ${localJsonArray.length()}".log()

        for (index in 0 until localJsonArray.length()) {

            val objMapping = localJsonArray.getJSONObject(index)

            val associatedIds = objMapping.getString(KEY_ASSOCIATED_IDS)

            if (associatedIds.contains(userIdFromContext)) {
                // found the id associated with the passed key
                "[ FOUND ] local storage has mapping for $userIdFromContext -> ${
                    objMapping.getString(
                        KEY_CANONICAL_ID
                    )
                }".log()
                return objMapping.getString(KEY_CANONICAL_ID)
            }
        }

        "[ OOOPS ] did not find anything for : $userIdFromContext".log()
        return null // nothing found
    }

    private fun getCanonicalIdFor(userContextId: String): String? {
        val jsonArray = getLocalJsonArray() ?: return null

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            if (!obj.has(KEY_ASSOCIATED_IDS)) continue

            val associatedIds = obj.optString(KEY_ASSOCIATED_IDS, "")
            if (associatedIds.isNotBlank() && associatedIds.contains(userContextId)) {
                return obj.getString(KEY_CANONICAL_ID)
            }
        }

        return null
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

    private fun makeResolveIdentityCall(
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