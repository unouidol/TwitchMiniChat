package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Persists non-secret emote names and IDs per Twitch account and channel. */
class TwitchEmoteCatalogStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    /** Loads the last successful catalog snapshot for one account and channel. */
    fun load(accountId: String, channel: String): TwitchEmoteCatalog? {
        val key = catalogKey(accountId, channel) ?: return null
        val raw = preferences.getString(key, null) ?: return null

        return try {
            val json = JSONObject(raw)
            val entriesJson = json.optJSONArray("entries") ?: JSONArray()
            val entries = mutableListOf<TwitchEmoteCatalogEntry>()

            for (index in 0 until entriesJson.length()) {
                val item = entriesJson.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                if (id.isBlank() || name.isBlank()) continue

                val formats = LinkedHashSet<String>()
                val formatsJson = item.optJSONArray("formats")
                if (formatsJson != null) {
                    for (formatIndex in 0 until formatsJson.length()) {
                        formatsJson.optString(formatIndex)
                            .trim()
                            .takeIf { it.isNotBlank() }
                            ?.let(formats::add)
                    }
                }

                entries += TwitchEmoteCatalogEntry(
                    id = id,
                    name = name,
                    ownerId = item.optString("ownerId").trim(),
                    emoteType = item.optString("emoteType").trim(),
                    formats = formats
                )
            }

            TwitchEmoteCatalog(
                broadcasterId = json.optString("broadcasterId")
                    .trim()
                    .takeIf { it.isNotBlank() },
                fetchedAtMs = json.optLong("fetchedAtMs", 0L),
                entries = entries
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Saves one complete snapshot atomically through AndroidX preferences KTX. */
    fun save(
        accountId: String,
        channel: String,
        catalog: TwitchEmoteCatalog
    ) {
        val key = catalogKey(accountId, channel) ?: return
        val entriesJson = JSONArray()

        catalog.entries.forEach { entry ->
            entriesJson.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("name", entry.name)
                    put("ownerId", entry.ownerId)
                    put("emoteType", entry.emoteType)
                    put("formats", JSONArray(entry.formats.toList()))
                }
            )
        }

        val json = JSONObject().apply {
            put("broadcasterId", catalog.broadcasterId.orEmpty())
            put("fetchedAtMs", catalog.fetchedAtMs)
            put("entries", entriesJson)
        }

        preferences.edit {
            putString(key, json.toString())
        }
    }

    /** Removes every channel catalog owned by one locally deleted account. */
    fun clearAccount(accountId: String) {
        val normalizedAccountId = accountId.trim()
        if (normalizedAccountId.isBlank()) return

        val prefix = "$CATALOG_KEY_PREFIX$normalizedAccountId:"
        val keys = preferences.all.keys.filter { key -> key.startsWith(prefix) }

        preferences.edit {
            keys.forEach { key -> remove(key) }
        }
    }

    /** Creates a stable key without accepting blank account or channel identifiers. */
    private fun catalogKey(accountId: String, channel: String): String? {
        val normalizedAccountId = accountId.trim()
        val normalizedChannel = channel.trim().removePrefix("#").lowercase()
        if (normalizedAccountId.isBlank() || normalizedChannel.isBlank()) return null
        return "$CATALOG_KEY_PREFIX$normalizedAccountId:$normalizedChannel"
    }

    companion object {
        private const val PREFERENCES_NAME = "twitch_emote_catalogs"
        private const val CATALOG_KEY_PREFIX = "catalog:"
    }
}
