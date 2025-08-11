package com.vwo.utils

import android.os.Environment
import android.util.Log
import com.vwo.models.user.VWOUserContext
import com.vwo.packages.storage.LocalStorageController
import com.vwo.providers.StorageProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AliasIdentityManager {

    private const val KEY_CANONICAL_ID = "canonical_id"
    private const val KEY_ASSOCIATED_IDS = "associated_ids"
    private const val KEY_IDENTITY_STORE = "vwo_identity_store_final"

    init {
        Log.e("FTR__", "NOTE: canonical (id = userId) used internally for getFlag()")
    }

    private fun String?.log() = println("LOG_ROF_: $this")

    // the alias        -> is the actual user id after login ; or something that we might get at later stage
    // idPassedOnInit   -> the id that was passed during the VWOUserContext init phase
    // the {alias} will be linked with the {idPassedOnInit}
    fun setAlias(idPassedOnInit: String, alias: String) {

        "will associate $alias to $idPassedOnInit if found locally".log()

        val localArray = getLocalJsonArray() ?: return

        for (index in 0 until localArray.length()) {

            val obj = localArray.getJSONObject(index)

            val associatesIds = obj.getString(KEY_ASSOCIATED_IDS)
            val idPassedOnInitExist = associatesIds.contains(idPassedOnInit)
            if (idPassedOnInitExist) {

                // [{"canonical_id": "userId1", aliases: ["userId1", "Scenario_1_LOGIN_USER_ID_1"]]}]

                // save the alias in the array of associated id list

                if (associatesIds.contains(alias)) {
                    "[DUPLICATE] the alias { $alias } already exists in this canonical id .. skipping insertion ...".log()
                    return
                }

                val associatedIdArray = obj.getJSONArray(KEY_ASSOCIATED_IDS)
                associatedIdArray.put(alias)

                obj.put(KEY_ASSOCIATED_IDS, associatedIdArray)

                "$alias has been mapped to ${obj.getString(KEY_CANONICAL_ID)} ...".log()
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



        // the idPassedOnInit was not found in local list or user id passed on init differs
        // from the user id one passed here
        // TODO discuss what must be done ?
        "[NOT IMPLEMENTED] $idPassedOnInit was not found locally ... not sure what to do ...".log()
    }

    // this will be called on init and internally only
    fun requestFromGatewayIfNotFoundLocally(vwoUserContext: VWOUserContext) {

        val userIdFromContext = vwoUserContext.id ?: run {

            "the passed user id is invalid ... will not fetch from GATEWAY ...".log()
            return
        }

        "trying to get the canonical id from local storage ...".log()
        val locallySavedCanonicalId = maybeGetCanonicalIdFromLocalStorage(userIdFromContext)
        if (locallySavedCanonicalId != null) {

            "$userIdFromContext was mapped to $locallySavedCanonicalId".log()
            "Will not request anything from GATEWAY ...".log()
            return
        }

        "did not find canonical id on local storage ... will send a request to GATEWAY next ...".log()

        ">>> sending request for id from GATEWAY, initial user id -> '${userIdFromContext}' ...".log()
        val canonicalIdFromServer = makeResolveIdentityCall(userIdFromContext)
        "<<< got back response from the GATEWAY $canonicalIdFromServer, will save this id locally ...".log()
        saveCanonicalIdLocally(userIdFromContext, canonicalIdFromServer)
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

    fun getUserIdForFlag(vwoUserContext: VWOUserContext?): String? {

        val userContextId = vwoUserContext?.id ?: return run {

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

        "did not find any flag for $userContextId that is saved locally ...".log()

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

    private val mockServerDatabase = HashMap<String, String>()
    private fun makeResolveIdentityCall(userId: String): String {

        val mockServerStateFile =
            File(Environment.getExternalStorageDirectory().absolutePath, "GATEWAY_STATE.txt")

        if (mockServerDatabase.isEmpty() && mockServerStateFile.exists()) {
            "trying to restore state before uninstall ...".log()
            val jA = JSONArray(mockServerStateFile.readText())
            for (index in 0 until jA.length()) {
                val str = jA.getString(index)
                val aStr = str.split(":")
                mockServerDatabase[aStr[0]] = aStr[1]
            }
            // restoring previous state
        }

        "[IGNORE Fake] GATEWAY request for a canonical id associated with -> $userId".log()

        val id = mockServerDatabase[userId] ?: run {
            "[IGNORE Fake] GATEWAY: User '$userId' is new. Sending back the canonical id for the first time.".log()
            mockServerDatabase[userId] = userId
            userId
        }

        // SAVE GATEWAY STATE TO A FILE

        val sA = JSONArray()
        mockServerDatabase.forEach { eat ->
            sA.apply {
                put("${eat.key}:${eat.value}")
            }
        }
        mockServerStateFile.writeText(sA.toString(4))

        return id
    }

}