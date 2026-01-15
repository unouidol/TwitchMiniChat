package com.fs.twitchminichat.v2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AccountRepository(ctx: Context) {

    private val prefs = ctx.getSharedPreferences("v2_accounts", Context.MODE_PRIVATE)
    private val key = "accounts_json"

    fun loadAccounts(): List<AccountConfig> {
        val json = prefs.getString(key, "[]") ?: "[]"
        val arr = JSONArray(json)
        val out = mutableListOf<AccountConfig>()

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                AccountConfig(
                    id = o.getString("id"),
                    username = o.getString("username"),
                    channel = o.getString("channel"),
                    accessToken = o.getString("accessToken")
                )
            )
        }
        return out
    }

    fun addAccount(cfg: AccountConfig) {
        val list = loadAccounts().toMutableList()
        list.add(cfg)
        saveAll(list)
    }

    fun saveAll(list: List<AccountConfig>) {
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("username", it.username)
            o.put("channel", it.channel)
            o.put("accessToken", it.accessToken)
            arr.put(o)
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    fun getById(id: String): AccountConfig? = loadAccounts().firstOrNull { it.id == id }
}
