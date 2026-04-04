package com.fs.twitchminichat.v2

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

object DexListStore {

    private const val PREFS_NAME = "dex_list_store"

    private fun key(profileId: String): String {
        return "wanted_pokemon_json_$profileId"
    }

    fun getWantedPokemon(context: Context, profileId: String): List<String> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key(profileId), null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val arr = JSONArray(raw)
            val out = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) {
                val name = arr.optString(i).trim()
                if (name.isNotEmpty()) out.add(name)
            }
            out
        }.getOrDefault(emptyList())
    }

    fun saveWantedPokemon(context: Context, profileId: String, wantedPokemon: List<String>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val normalized = wantedPokemon
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val arr = JSONArray()
        for (name in normalized) {
            arr.put(name)
        }

        prefs.edit {
            putString(key(profileId), arr.toString())
        }
    }
}