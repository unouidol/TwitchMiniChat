package com.fs.twitchminichat

import android.content.Context
import org.json.JSONObject

object CurrentSpawnStore {

    private const val PREFS_NAME = "current_spawn_store"

    private fun key(channel: String): String {
        return "spawn_${channel.trim().removePrefix("#").lowercase()}"
    }

    fun saveForChannel(
        context: Context,
        channel: String,
        spawn: SpawnSnapshot
    ) {
        val normalizedChannel = channel.trim().removePrefix("#").lowercase()
        if (normalizedChannel.isBlank()) return

        val json = JSONObject()
            .put("rawName", spawn.rawName)
            .put("dexKey", spawn.dexKey ?: "")
            .put("displayName", spawn.displayName)
            .put("type1", spawn.type1 ?: "")
            .put("type2", spawn.type2 ?: "")
            .put("weightKg", spawn.weightKg ?: JSONObject.NULL)
            .put("baseSpeed", spawn.baseSpeed ?: JSONObject.NULL)
            .put("baseHp", spawn.baseHp ?: JSONObject.NULL)
            .put("evolvesTwice", spawn.evolvesTwice ?: JSONObject.NULL)
            .put("seenAtMs", spawn.seenAtMs)
            .put("isAlreadyCaught", spawn.isAlreadyCaught ?: JSONObject.NULL)

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key(normalizedChannel), json.toString())
            .apply()
    }

    fun loadForChannel(
        context: Context,
        channel: String
    ): SpawnSnapshot? {
        val normalizedChannel = channel.trim().removePrefix("#").lowercase()
        if (normalizedChannel.isBlank()) return null

        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key(normalizedChannel), null)
            ?: return null

        return try {
            val json = JSONObject(raw)

            val rawName = json.optString("rawName").trim()
            val dexKey = json.optString("dexKey").trim().ifBlank { null }
            val displayName = json.optString("displayName").trim().ifBlank { rawName }
            val type1 = json.optString("type1").trim().ifBlank { null }
            val type2 = json.optString("type2").trim().ifBlank { null }
            val weightKg = if (json.isNull("weightKg")) null else json.optDouble("weightKg")
            val baseSpeed = if (json.isNull("baseSpeed")) null else json.optInt("baseSpeed")
            val baseHp = if (json.isNull("baseHp")) null else json.optInt("baseHp")
            val evolvesTwice = if (json.isNull("evolvesTwice")) null else json.optBoolean("evolvesTwice")
            val seenAtMs = json.optLong("seenAtMs", 0L)
            val isAlreadyCaught = if (json.isNull("isAlreadyCaught")) null else json.optBoolean("isAlreadyCaught")

            if (rawName.isBlank()) {
                null
            } else {
                SpawnSnapshot(
                    rawName = rawName,
                    dexKey = dexKey,
                    displayName = displayName,
                    type1 = type1,
                    type2 = type2,
                    weightKg = weightKg,
                    baseSpeed = baseSpeed,
                    baseHp = baseHp,
                    evolvesTwice = evolvesTwice,
                    seenAtMs = seenAtMs,
                    isAlreadyCaught = isAlreadyCaught
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun clearForChannel(
        context: Context,
        channel: String
    ) {
        val normalizedChannel = channel.trim().removePrefix("#").lowercase()
        if (normalizedChannel.isBlank()) return

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key(normalizedChannel))
            .apply()
    }
}