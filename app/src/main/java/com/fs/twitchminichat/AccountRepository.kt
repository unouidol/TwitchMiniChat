package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
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
                    accessToken = o.getString("accessToken"),
                    profileId = o.optString("profileId", "")
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
            o.put("profileId", it.profileId)
            arr.put(o)
        }
        prefs.edit {
            putString(key, arr.toString())
        }
    }

    fun getById(id: String): AccountConfig? = loadAccounts().firstOrNull { it.id == id }

    /**
     * Replaces OAuth credentials for one existing account without changing its local identity.
     *
     * The account id, channel, list position, and all profile-scoped stores remain unchanged.
     * Returns false when the requested local account no longer exists.
     */
    fun updateCredentialsInPlace(
        accountId: String,
        username: String,
        accessToken: String,
        profileId: String
    ): Boolean {
        val list = loadAccounts().toMutableList()
        val index = list.indexOfFirst { it.id == accountId }
        if (index == -1) return false

        val current = list[index]
        list[index] = current.copy(
            username = username.trim(),
            accessToken = accessToken.trim(),
            profileId = profileId.trim().ifBlank { current.profileId }
        )
        saveAll(list)
        return true
    }

    /**
     * Removes one saved account by id and returns the removed config.
     *
     * Returning the removed account keeps deletion flows safer because callers can
     * still resolve profile-scoped cleanup data from the exact account that was
     * deleted.
     */
    fun removeById(id: String): AccountConfig? {
        val list = loadAccounts().toMutableList()
        val index = list.indexOfFirst { it.id == id }

        if (index == -1) return null

        val removed = list.removeAt(index)
        saveAll(list)
        return removed
    }

    fun updateChannel(accountId: String, newChannel: String) {
        val ch = newChannel.trim().removePrefix("#")
        if (ch.isBlank()) return

        val list = loadAccounts().toMutableList()
        val idx = list.indexOfFirst { it.id == accountId }
        if (idx == -1) return

        val old = list[idx]
        val updated = old.copy(channel = ch)
        list[idx] = updated

        saveAll(list)
    }

    fun reorderAccounts(orderedIds: List<String>) {
        val current = loadAccounts()
        if (current.isEmpty()) return

        val byId = current.associateBy { it.id }.toMutableMap()
        val reordered = mutableListOf<AccountConfig>()

        for (id in orderedIds) {
            val cfg = byId.remove(id)
            if (cfg != null) reordered += cfg
        }

        reordered += byId.values
        saveAll(reordered)
    }
}
