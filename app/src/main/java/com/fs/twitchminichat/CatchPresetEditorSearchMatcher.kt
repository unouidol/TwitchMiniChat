package com.fs.twitchminichat

import java.util.Locale

/**
 * Matches preset editor rows against the search query.
 *
 * This helper exists so the adapter does not need to know search semantics.
 * The editor can search by:
 *
 * - visible label, for example "Luxury Ball"
 * - chat command, for example "!catch luxury"
 * - internal ball id, for example "luxury_ball"
 *
 * Searching by ball id is useful while developing/debugging because our code
 * often refers to ids like "poke_ball", "repeat_ball", or "clone_ball".
 */
object CatchPresetEditorSearchMatcher {

    fun matches(
        preset: CatchPreset,
        rawQuery: String
    ): Boolean {
        val query = normalize(rawQuery)
        if (query.isBlank()) return true

        val searchableText = buildString {
            appendNormalized(preset.label)
            append(' ')
            appendNormalized(preset.command)
            append(' ')
            appendNormalized(preset.ballId.orEmpty())
            append(' ')
            appendNormalized(preset.id)
        }

        return searchableText.contains(query)
    }

    private fun StringBuilder.appendNormalized(value: String) {
        append(normalize(value))
    }

    private fun normalize(value: String): String {
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace("é", "e")
            .replace("è", "e")
    }
}