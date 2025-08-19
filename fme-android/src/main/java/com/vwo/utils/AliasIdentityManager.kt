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
import org.json.JSONArray
import org.json.JSONObject

private const val ERROR_INVALID_DATA_FROM_SERVER = "Got invalid data from server, statusCode."

object AliasIdentityManager {

    private const val KEY_CANONICAL_ID = "canonical_id"
    private const val KEY_ASSOCIATED_IDS = "associated_ids"
    private const val KEY_IDENTITY_STORE = "vwo_identity_store_final"

    init {
        LoggerService.log(
            LogLevelEnum.INFO, "NOTE: canonical (id = userId) used internally for getFlag()"
        )
    }

    private fun String?.log() = LoggerService.log(LogLevelEnum.INFO, this)

    // the alias is the userId    -> is the actual user id after login ; or something that we might get at later stage
    // idPassedOnInit  is tempId  -> the id that was passed during the VWOUserContext init phase
    // the {alias} will be linked with the {idPassedOnInit}
    fun setAlias(idPassedOnInit: String, userId: String) {

        ioThreadAsync(callback = {
            val response = NetworkUtil.AliasApiService.setAlias(idPassedOnInit, userId)
            println("ERR_: idPassedOnInit: $idPassedOnInit and userId $userId")
            if (response?.statusCode != 200) {
                // the request was not successful.
                println("ERR_IN_REQ: error:${response?.error}, statusCode:${response?.statusCode}, headers:${response?.headers}, data:${response?.data}")
                return@ioThreadAsync
            }

            val data = response.data
            println("ERR_IN_REQ: we got some data : $data")
            println("DEMO_NVN: sent the data to server for mapping ...")
        }, exceptionDuringProcessing = {
            it.printStackTrace()
            println("TEST ${it.message}")
        })

        "will associate $userId to $idPassedOnInit if found locally".log()
        println("DEMO_FILTER: mapped and saved locally ...")

        val localArray = getLocalJsonArray() ?: return

        for (index in 0 until localArray.length()) {

            val obj = localArray.getJSONObject(index)

            val associatesIds = obj.getString(KEY_ASSOCIATED_IDS)
            val idPassedOnInitExist = associatesIds.contains(idPassedOnInit)
            if (idPassedOnInitExist) {

                // [{"canonical_id": "userId1", aliases: ["userId1", "Scenario_1_LOGIN_USER_ID_1"]]}]

                // save the alias in the array of associated id list

                if (associatesIds.contains(userId)) {
                    "[DUPLICATE] the alias { $userId } already exists in this canonical id .. skipping insertion ...".log()
                    return
                }

                val associatedIdArray = obj.getJSONArray(KEY_ASSOCIATED_IDS)
                associatedIdArray.put(userId)

                obj.put(KEY_ASSOCIATED_IDS, associatedIdArray)

                "$userId has been mapped to ${obj.getString(KEY_CANONICAL_ID)} ...".log()
                "new association array for the canonical id -> $localArray".log()


                "[IMPPPP] before saving -> $localArray".log()
                val ctx = StorageProvider.contextRef.get() ?: return // Uncomment in your project
                val preferences = LocalStorageController(ctx) // Uncomment in your project
                preferences.saveString(
                    KEY_IDENTITY_STORE,
                    localArray.toString()
                ) // Uncomment in your project

                return // there's no need to do anything , just exit the method
            }
        }

        LoggerService.log(LogLevelEnum.ERROR, "")

        // the idPassedOnInit was not found in local list or user id passed on init differs
        // from the user id one passed here
        // TODO discuss what must be done ?
        "[NOT IMPLEMENTED] $idPassedOnInit was not found locally ... not sure what to do ...".log()
    }

    fun requestFromGatewayIfNotFoundLocally(vwoUserContext: VWOUserContext) {

        val userIdFromContext = vwoUserContext.maybeGetQualifyingId() ?: run {
            "the passed user id is invalid ... will not fetch from GATEWAY ...".log()
            return
        }

        println("DEMO_FILTER: checking local storage for $userIdFromContext's mapping ....")
        "trying to get the canonical id from local storage ...".log()
        val locallySavedCanonicalId = maybeGetCanonicalIdFromLocalStorage(userIdFromContext)
        if (locallySavedCanonicalId != null) {

            println("DEMO_FILTER: mapping found; ${userIdFromContext} mapped to -> { $locallySavedCanonicalId }")

            "$userIdFromContext was mapped to $locallySavedCanonicalId".log()
            "Will not request anything from GATEWAY ...".log()
            return
        }

        println("DEMO_FILTER: did not find anything stored locally ...")
        println("DEMO_FILTER: requesting server the mapping for -> { $userIdFromContext }")

        "did not find canonical id on local storage ... >>>> will send GET request to GATEWAY/SERVER with | userId->${userIdFromContext}".log()
        makeResolveIdentityCall(
            userIdFromContext,
            success = { json ->
                // example response from server -> {"aliasId":"Scenario_1_LOGIN_USER_ID_1"}
                val canonicalIdFromServer = JSONObject(json).getString("aliasId")
                println("DEMO_FILTER: SERVER RESPONSE -> $userIdFromContext is mapped to -> { $canonicalIdFromServer }")
                "<<< RESPONSE from SERVER/GATEWAY -> $canonicalIdFromServer, saving this locally for lookup ...".log()
                saveCanonicalIdLocally(userIdFromContext, canonicalIdFromServer)
            }, error = {
                it.log()
                println("DEMO_FILTER: error occurred $it")
            }
        )
    }

    private fun saveCanonicalIdLocally(userIdFromContext: String, canonicalIdFromServer: String) {
        val localStorage = getLocalStorageController() ?: return
        val arrFromLocalStorage = getLocalJsonArray() ?: return

        "there are total ${arrFromLocalStorage.length()} items in locally saved array ...".log()
        "we will check the GATEWAY sent canonical id ($canonicalIdFromServer) for duplicate ...".log()

        // check if we already have the same canonical id
        for (index in 0 until arrFromLocalStorage.length()) {
            val obj = arrFromLocalStorage.getJSONObject(index)

            if (obj.optString(KEY_CANONICAL_ID, "") == canonicalIdFromServer) {
                "[SKIP] the canonical id is already found in the local storage".log()
                // found the same key just don't save it
                return
            }

        }

        "not a duplicate canonical id ... proceeding ...".log()

        // create a new entry because the old one doesn't exist
        val objCanonical = JSONObject().apply {
            put(KEY_CANONICAL_ID, canonicalIdFromServer)
            put(KEY_ASSOCIATED_IDS, JSONArray().apply { put(userIdFromContext) })
        }
        arrFromLocalStorage.put(objCanonical)

        "mapped canonical id from GATEWAY ($canonicalIdFromServer) to user id from vwo user context ($userIdFromContext)".log()
        "final saved JSON -> $arrFromLocalStorage".log()
        localStorage.saveString(KEY_IDENTITY_STORE, arrFromLocalStorage.toString())
    }

    // we get canonical id by comparing the alias / user id from context to associated ids
    private fun maybeGetCanonicalIdFromLocalStorage(userIdFromContext: String): String? {
        val localJsonArray = getLocalJsonArray() ?: return null // cannot get any entries
        if (localJsonArray.length() == 0) return null // there is no entry

        "there are ${localJsonArray.length()} item(s) locally ...".log()

        for (index in 0 until localJsonArray.length()) {

            val objMapping = localJsonArray.getJSONObject(index)

            val associatedIds = objMapping.getString(KEY_ASSOCIATED_IDS)

            if (associatedIds.contains(userIdFromContext))
            // found the id associated with the passed key
                return objMapping.getString(KEY_CANONICAL_ID)
        }

        "x did not find anything for user id : $userIdFromContext".log()
        return null // nothing found
    }

    fun maybeGetAliasAwareUserId(vwoUserContext: VWOUserContext?): String? {

        val userContextId = vwoUserContext?.maybeGetQualifyingId() ?: return run {
            "cannot find user id ".log()
            null
        }

        val jsonArray = getLocalJsonArray() ?: return null

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)

            if (!obj.has(KEY_ASSOCIATED_IDS)) continue

            val associatedIds = obj.optString(KEY_ASSOCIATED_IDS, "")
            if (associatedIds.isNotBlank() && associatedIds.contains(userContextId)) {

                "found canonical id saved locally for $userContextId".log()
                return obj.getString(KEY_CANONICAL_ID)
            }
        }

        "did not find any flag for $userContextId that was saved locally ...".log()

        return null
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
            println("SOMETHING HAPPENED: ${throwable.message}")
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