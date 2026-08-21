package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/**
 * Stores the latest known PCG spawn.
 *
 * PCG spawns are global across channels, so this store must NOT be scoped to the
 * current Twitch channel. If a Pikachu is active, it is active everywhere until
 * the 90-second spawn window expires.
 */
object CurrentSpawnStore {

    private const val PREFS_NAME = "current_spawn_store"

    /**
     * Single global key.
     *
     * Older versions used per-channel keys like "spawn_unouidol".
     * From now on, the current spawn is global.
     */
    private const val GLOBAL_SPAWN_KEY = "current_global_spawn"

    /**
     * Saves the latest global spawn.
     *
     * This should be called whenever the app detects a new valid PCG spawn.
     */
    fun save(
        context: Context,
        spawn: SpawnSnapshot
    ) {
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
            .edit {
                putString(GLOBAL_SPAWN_KEY, json.toString())
            }
    }

    /**
     * Loads the current global spawn only if it is still inside the 90-second
     * valid window.
     *
     * Expired spawns are not returned as active. Their timestamp remains available
     * through [loadLastKnown] for the existing next-spawn countdown.
     */
    fun load(
        context: Context,
        nowMs: Long = System.currentTimeMillis()
    ): SpawnSnapshot? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val raw = prefs.getString(GLOBAL_SPAWN_KEY, null)
            ?: return null

        val snapshot = parse(raw) ?: return null
        val ageMs = nowMs - snapshot.seenAtMs

        if (ageMs < 0L) {
            return snapshot
        }

        if (ageMs > SmartCatchSpawnMergePolicy.ACTIVE_WINDOW_MS) {
            return null
        }

        return snapshot
    }


    private fun parse(raw: String): SpawnSnapshot? {
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
    /**
     * Loads the last known spawn even if the 90-second active catch window expired.
     *
     * This is useful for UI countdowns such as:
     * "Next spawn in: 13:30"
     *
     * It should NOT be used to decide whether Smart Presets are available.
     * For Smart Presets, use load(...), which only returns active spawns.
     */
    fun loadLastKnown(
        context: Context
    ): SpawnSnapshot? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(GLOBAL_SPAWN_KEY, null)
            ?: return null

        return parse(raw)
    }
}
