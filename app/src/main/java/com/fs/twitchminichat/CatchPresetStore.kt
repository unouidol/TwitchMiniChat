package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object CatchPresetStore {

    private const val PREFS_NAME = "catch_preset_store"
    private const val KEY_PRESETS_JSON = "presets_json"

    const val MAX_SAVED_PRESETS = 50
    const val MAX_QUICK_PRESETS = 6

    private fun defaultPresets(): List<CatchPreset> {
        return listOf(
            CatchPreset("normal", "Catch", "!pokecatch", true),
            CatchPreset("great", "Great", "!pokecatch great ball", true),
            CatchPreset("ultra", "Ultra", "!pokecatch ultra ball", true),
            CatchPreset("timer", "Timer", "!pokecatch timer ball", true),
            CatchPreset("quick", "Quick", "!pokecatch quick ball", true),
            CatchPreset("repeat", "Repeat", "!pokecatch repeat ball", true)
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
            enabled = false
        )
    }

    private fun parsePresets(raw: String): List<CatchPreset> {
        val result = mutableListOf<CatchPreset>()

        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue

                val preset = CatchPreset(
                    id = obj.optString("id").trim().ifBlank { "custom_loaded_$i" },
                    label = obj.optString("label").trim().ifBlank { "Preset ${i + 1}" },
                    command = obj.optString("command").trim(),
                    enabled = obj.optBoolean("enabled", false)
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
            enabled = preset.enabled
        )
    }
}