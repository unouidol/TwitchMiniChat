package com.fs.twitchminichat.v3

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AccountRepository(context: Context) {

    private val prefs = context.getSharedPreferences("accounts_v3", Context.MODE_PRIVATE)
    private val KEY_ACCOUNTS_JSON = "accounts_json"

    fun getAll(): List<AccountConfig> {
        val raw = prefs.getString(KEY_ACCOUNTS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id", "")
                    val username = o.optString("username", "")
                    val channel = o.optString("channel", "")
                    val token = o.optString("accessToken", "")
                    if (id.isBlank() || username.isBlank() || channel.isBlank()) continue
                    add(AccountConfig(id, username, channel, token))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getById(id: String): AccountConfig? = getAll().firstOrNull { it.id == id }

    fun deleteById(id: String) {
        val list = getAll().filterNot { it.id == id }
        saveAll(list)
    }

    fun findByUserAndChannel(username: String, channel: String): AccountConfig? {
        val u = username.trim().lowercase()
        val c = channel.trim().removePrefix("#").lowercase()
        return getAll().firstOrNull {
            it.username.trim().lowercase() == u &&
                    it.channel.trim().removePrefix("#").lowercase() == c
        }
    }

    /**
     * Se esiste già (username+channel) aggiorna il token e RIUSA l'id
     * altrimenti crea un nuovo account.
     */
    fun upsert(username: String, channel: String, accessToken: String): String {
        val u = username.trim()
        val c = channel.trim().removePrefix("#")

        val list = getAll().toMutableList()
        val existing = findByUserAndChannel(u, c)

        return if (existing != null) {
            val idx = list.indexOfFirst { it.id == existing.id }
            if (idx >= 0) {
                list[idx] = existing.copy(username = u, channel = c, accessToken = accessToken)
                saveAll(list)
            }
            existing.id
        } else {
            val newId = UUID.randomUUID().toString()
            list.add(AccountConfig(newId, u, c, accessToken))
            saveAll(list)
            newId
        }
    }

    private fun saveAll(list: List<AccountConfig>) {
        val arr = JSONArray()
        for (a in list) {
            val o = JSONObject()
            o.put("id", a.id)
            o.put("username", a.username)
            o.put("channel", a.channel)
            o.put("accessToken", a.accessToken)
            arr.put(o)
        }
        prefs.edit().putString(KEY_ACCOUNTS_JSON, arr.toString()).apply()
    }
}
