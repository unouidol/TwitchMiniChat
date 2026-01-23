package com.fs.twitchminichat.v2

import android.content.Context
import org.json.JSONArray

class ChannelHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("channel_history", Context.MODE_PRIVATE)

    private fun key(accountId: String) = "recent_channels_$accountId"

    fun get(accountId: String): MutableList<String> {
        val raw = prefs.getString(key(accountId), "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val v = arr.optString(i, "").trim()
            if (v.isNotBlank()) out.add(v)
        }
        return out
    }

    fun add(accountId: String, channel: String, max: Int = 20) {
        val ch = channel.trim().removePrefix("#").lowercase()
        if (ch.isBlank()) return

        val list = get(accountId)
        list.removeAll { it.equals(ch, ignoreCase = true) }
        list.add(0, ch)
        while (list.size > max) list.removeAt(list.lastIndex)

        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(key(accountId), arr.toString()).apply()
    }

    fun remove(accountId: String, channel: String) {
        val ch = channel.trim().removePrefix("#").lowercase()
        val list = get(accountId)
        list.removeAll { it.equals(ch, ignoreCase = true) }

        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit().putString(key(accountId), arr.toString()).apply()
    }
}
