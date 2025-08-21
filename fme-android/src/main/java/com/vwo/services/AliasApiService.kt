package com.vwo.services

import com.vwo.enums.UrlEnum
import com.vwo.packages.network_layer.manager.NetworkManager
import com.vwo.packages.network_layer.models.RequestModel
import com.vwo.packages.network_layer.models.ResponseModel
import com.vwo.utils.NetworkUtil.Companion.createHeaders

class AliasApiService {

    val KEY_USER_ID = "userId"
    val KEY_ALIAS_ID = "aliasId"

    val KEY_ACCOUNT_ID = "accountId"
    val KEY_SDK_KEY = "sdkKey"

    fun getAlias(userId: String): ResponseModel? {

        val headers = createHeaders(null, null)
        val queryParams = getQueryParams(mutableMapOf(KEY_USER_ID to userId))
        val request = RequestModel(
            url = SettingsManager.instance?.hostname,
            method = "GET",
            path = UrlEnum.GET_ALIAS.url,
            query = queryParams,
            body = null,
            headers = headers,
            scheme = SettingsManager.instance?.protocol,
            port = SettingsManager.instance?.port ?: 0
        )
        return NetworkManager.get(request)
    }

    fun setAlias(userId: String, aliasId: String): ResponseModel? {
        NetworkManager.attachClient()
        val headers = createHeaders(null, null)
        val requestBody = mapOf(KEY_USER_ID to userId, KEY_ALIAS_ID to aliasId)
        val request = RequestModel(
            url = SettingsManager.instance?.hostname,
            method = "POST",
            path = UrlEnum.SET_ALIAS.url,
            query = getQueryParams(),
            body = requestBody,
            headers = headers,
            scheme = SettingsManager.instance?.protocol,
            port = SettingsManager.instance?.port ?: 0
        )
        return NetworkManager.post(request)
    }

    private fun getQueryParams(map: Map<String, String> = mapOf()): MutableMap<String, String> {
        val accountId = SettingsManager.instance?.accountId.toString()
        val sdkKey = SettingsManager.instance?.sdkKey.toString()

        val result = mutableMapOf(KEY_ACCOUNT_ID to accountId, KEY_SDK_KEY to sdkKey)
        map.forEach { result[it.key] = it.value }
        return map.toMutableMap()
    }

}