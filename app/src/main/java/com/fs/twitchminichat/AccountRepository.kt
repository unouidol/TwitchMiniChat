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
        var needsMigration = false

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)

            val sortOrder = if (o.has("sortOrder")) {
                o.optInt("sortOrder", i)
            } else {
                needsMigration = true
                i
            }

            out.add(
                AccountConfig(
                    id = o.getString("id"),
                    username = o.getString("username"),
                    channel = o.getString("channel"),
                    accessToken = o.getString("accessToken"),
                    profileId = o.optString("profileId", ""),
                    sortOrder = sortOrder
                )
            )
        }

        val normalized = out
            .sortedWith(compareBy<AccountConfig> { it.sortOrder }.thenBy { it.username.lowercase() })
            .mapIndexed { index, cfg ->
                if (cfg.sortOrder != index) {
                    needsMigration = true
                }
                cfg.copy(sortOrder = index)
            }

        if (needsMigration) {
            saveAll(normalized)
        }

        return normalized
    }

    fun addAccount(cfg: AccountConfig) {
        val list = loadAccounts().toMutableList()
        val nextOrder = (list.maxOfOrNull { it.sortOrder } ?: -1) + 1
        list.add(cfg.copy(sortOrder = nextOrder))
        saveAll(list)
    }

    fun saveAll(list: List<AccountConfig>) {
        val normalized = list.mapIndexed { index, cfg ->
            cfg.copy(sortOrder = index)
        }

        val arr = JSONArray()
        normalized.forEach {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("username", it.username)
            o.put("channel", it.channel)
            o.put("accessToken", it.accessToken)
            o.put("profileId", it.profileId)
            o.put("sortOrder", it.sortOrder)
            arr.put(o)
        }

        prefs.edit {
            putString(key, arr.toString())
        }
    }

    fun getById(id: String): AccountConfig? =
        loadAccounts().firstOrNull { it.id == id }

    fun updateChannel(accountId: String, newChannel: String) {
        val ch = newChannel.trim().removePrefix("#")
        if (ch.isBlank()) return

        val list = loadAccounts().toMutableList()
        val idx = list.indexOfFirst { it.id == accountId }
        if (idx == -1) return

        val old = list[idx]
        list[idx] = old.copy(channel = ch)

        saveAll(list)
    }

    fun reorderAccounts(orderedIds: List<String>) {
        val current = loadAccounts()
        if (current.isEmpty()) return

        val byId = current.associateBy { it.id }
        val reordered = mutableListOf<AccountConfig>()

        orderedIds.forEach { id ->
            byId[id]?.let { reordered.add(it) }
        }

        current.forEach { cfg ->
            if (reordered.none { it.id == cfg.id }) {
                reordered.add(cfg)
            }
        }

        saveAll(
            reordered.mapIndexed { index, cfg ->
                cfg.copy(sortOrder = index)
            }
        )
    }
}