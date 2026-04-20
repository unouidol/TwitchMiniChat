package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object CatchPresetStore {
    private const val PREFS_NAME = "catch_presets"
    private const val KEY_PRESETS = "presets"

    private fun defaultPresets(): List<CatchPreset> {
        return listOf(
            CatchPreset("normal", "Catch", "!pokecatch"),
            CatchPreset("great", "Great", "!pokecatch great ball"),
            CatchPreset("ultra", "Ultra", "!pokecatch ultra ball"),
            CatchPreset("timer", "Timer", "!pokecatch timer ball"),
            CatchPreset("quick", "Quick", "!pokecatch quick ball"),
            CatchPreset("repeat", "Repeat", "!pokecatch repeat ball")
        )
    }

    fun loadAll(context: Context): List<CatchPreset> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PRESETS, null)

        if (raw.isNullOrBlank()) {
            val defaults = defaultPresets()
            saveAll(context, defaults)
            return defaults
        }

        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        CatchPreset(
                            id = obj.optString("id", "preset_$i"),
                            label = obj.optString("label", "Preset ${i + 1}"),
                            command = obj.optString("command", ""),
                            enabled = obj.optBoolean("enabled", true)
                        )
                    )
                }
            }.ifEmpty {
                val defaults = defaultPresets()
                saveAll(context, defaults)
                defaults
            }
        } catch (_: Exception) {
            val defaults = defaultPresets()
            saveAll(context, defaults)
            defaults
        }
    }

    fun loadEnabled(context: Context, max: Int = 6): List<CatchPreset> {
        return loadAll(context)
            .filter { it.enabled && it.command.isNotBlank() }
            .take(max)
    }

    fun saveAll(context: Context, presets: List<CatchPreset>) {
        val arr = JSONArray()
        presets.forEach { preset ->
            arr.put(
                JSONObject().apply {
                    put("id", preset.id)
                    put("label", preset.label)
                    put("command", preset.command)
                    put("enabled", preset.enabled)
                }
            )
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_PRESETS, arr.toString())
        }
    }
}