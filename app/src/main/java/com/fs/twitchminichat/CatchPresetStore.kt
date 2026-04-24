package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object CatchPresetStore {

    private const val PREFS_NAME = "catch_preset_store"
    private const val KEY_PRESETS_JSON = "presets_json"

    const val MAX_SAVED_PRESETS = 50
    const val MAX_QUICK_PRESETS = 6

    const val BALL_ID_AUTO_CATCH_BASIC = "auto_catch_basic"

    private fun defaultPresets(): List<CatchPreset> {
        return listOf(
            CatchPreset("normal", "Pokeball", "!pokecatch", true, BALL_ID_AUTO_CATCH_BASIC),
            CatchPreset("great", "Greatball", "!pokecatch great ball", true, "great_ball"),
            CatchPreset("ultra", "Ultraball", "!pokecatch ultra ball", true, "ultra_ball"),
            CatchPreset("timer", "Timerball", "!pokecatch timer ball", true, "timer_ball"),
            CatchPreset("quick", "Quickball", "!pokecatch quick ball", true, "quick_ball"),
            CatchPreset("repeat", "Repeatball", "!pokecatch repeat ball", true, "repeat_ball")
        )
    }

    fun loadAll(context: Context): List<CatchPreset> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PRESETS_JSON, null)

        if (raw.isNullOrBlank()) {
            return defaultPresets()
        }

        return parsePresets(raw).take(MAX_SAVED_PRESETS)
    }

    fun loadQuickMenuPresets(context: Context): List<CatchPreset> {
        return loadAll(context)
            .filter { it.enabled && it.command.isNotBlank() }
            .take(MAX_QUICK_PRESETS)
    }

    fun saveAll(context: Context, presets: List<CatchPreset>) {
        val cleaned = presets
            .mapIndexedNotNull { index, preset -> sanitizeForSave(preset, index) }
            .take(MAX_SAVED_PRESETS)

        val array = JSONArray()
        cleaned.forEach { preset ->
            array.put(
                JSONObject().apply {
                    put("id", preset.id)
                    put("label", preset.label)
                    put("command", preset.command)
                    put("enabled", preset.enabled)
                    if (!preset.ballId.isNullOrBlank()) {
                        put("ballId", preset.ballId)
                    }
                }
            )
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_PRESETS_JSON, array.toString())
        }
    }

    fun newEmptyPreset(positionHint: Int): CatchPreset {
        return CatchPreset(
            id = "custom_${System.currentTimeMillis()}_$positionHint",
            label = "",
            command = "",
            enabled = false,
            ballId = null
        )
    }
    fun mergeMissingInventoryPresets(
        existing: List<CatchPreset>,
        inventoryBalls: List<InventoryBallItem>
    ): List<CatchPreset> {
        if (inventoryBalls.isEmpty()) return existing

        val out = existing.toMutableList()
        val representedBallIds = existing.mapNotNull { it.ballId }.toMutableSet()

        val hasAutoCatchBasic = existing.any { it.ballId == BALL_ID_AUTO_CATCH_BASIC }

        inventoryBalls
            .sortedBy { it.name.lowercase(Locale.ROOT) }
            .forEach { ball ->
                if (ball.ballId.isBlank()) return@forEach
                if (representedBallIds.contains(ball.ballId)) return@forEach

                // Evita di creare un preset "Poké Ball" esplicito se c'è già il preset Catch automatico.
                if (ball.ballId == "poke_ball" && hasAutoCatchBasic) {
                    representedBallIds += ball.ballId
                    return@forEach
                }

                out += CatchPreset(
                    id = "auto_${ball.ballId}",
                    label = defaultLabelForBall(ball),
                    command = defaultCommandForBall(ball),
                    enabled = ball.count > 0,
                    ballId = ball.ballId
                )

                representedBallIds += ball.ballId
            }

        return out
    }

    private fun defaultLabelForBall(ball: InventoryBallItem): String {
        val name = ball.name.trim()
        return when {
            name.equals("Poké Ball", ignoreCase = true) -> "Poké"
            name.equals("Poke Ball", ignoreCase = true) -> "Poké"
            name.endsWith(" Ball", ignoreCase = true) -> name.removeSuffix(" Ball")
            else -> name
        }
    }

    private fun defaultCommandForBall(ball: InventoryBallItem): String {
        val normalizedName = ball.name
            .trim()
            .lowercase(Locale.ROOT)
            .replace("poké", "poke")

        return "!pokecatch $normalizedName"
    }
    private fun parsePresets(raw: String): List<CatchPreset> {
        val result = mutableListOf<CatchPreset>()

        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue

                val id = obj.optString("id").trim().ifBlank { "custom_loaded_$i" }
                val label = obj.optString("label").trim().ifBlank { "Preset ${i + 1}" }
                val command = obj.optString("command").trim()
                val explicitBallId = obj.optString("ballId").trim().ifBlank { null }

                val preset = CatchPreset(
                    id = id,
                    label = label,
                    command = command,
                    enabled = obj.optBoolean("enabled", false),
                    ballId = normalizeStoredBallId(
                        id = id,
                        command = command,
                        explicitBallId = explicitBallId
                    )
                )

                result += preset
            }
        }

        return result
    }

    private fun sanitizeForSave(preset: CatchPreset, index: Int): CatchPreset? {
        val id = preset.id.trim().ifBlank { "custom_${System.currentTimeMillis()}_$index" }
        val label = preset.label.trim()
        val command = preset.command.trim()

        if (label.isBlank() && command.isBlank()) {
            return null
        }

        return CatchPreset(
            id = id,
            label = label.ifBlank { "Preset ${index + 1}" },
            command = command,
            enabled = preset.enabled,
            ballId = normalizeStoredBallId(
                id = id,
                command = command,
                explicitBallId = preset.ballId
            )
        )
    }

    private fun normalizeStoredBallId(
        id: String,
        command: String,
        explicitBallId: String?
    ): String? {
        val normalizedCommand = command.trim().lowercase()
        val normalizedExplicit = explicitBallId?.trim()?.ifBlank { null }

        if (normalizedCommand == "!pokecatch") {
            return BALL_ID_AUTO_CATCH_BASIC
        }

        if (normalizedExplicit != null) {
            return normalizedExplicit
        }

        return inferBallIdFromCommand(command)
    }

    private fun inferBallIdFromCommand(command: String): String? {
        val normalized = command.trim().lowercase()

        if (normalized == "!pokecatch") {
            return BALL_ID_AUTO_CATCH_BASIC
        }

        if (!normalized.startsWith("!pokecatch ")) {
            return null
        }

        val suffix = normalized.removePrefix("!pokecatch ").trim()
        if (!suffix.endsWith(" ball")) {
            return null
        }

        return suffix
            .replace("poké", "poke")
            .replace(Regex("\\s+"), "_")
    }
}