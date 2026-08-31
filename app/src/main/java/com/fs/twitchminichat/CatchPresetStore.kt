package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Persists user catch presets per canonical PCG profile.
 *
 * Runtime Smart Presets are intentionally not stored here. A one-time migration
 * copies the legacy global preset snapshot to every account that already exists,
 * then removes the global key so future accounts start from defaults.
 */
object CatchPresetStore {

    private const val PREFS_NAME = "catch_preset_store"
    private const val LEGACY_KEY_PRESETS_JSON = "presets_json"
    private const val PROFILE_PRESETS_KEY_PREFIX = "presets_json"
    private const val KEY_PROFILE_SCOPE_MIGRATION_COMPLETE =
        "profile_scope_migration_v1"

    const val MAX_SAVED_PRESETS = 50

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

    /**
     * Loads the saved preset list for exactly one canonical profile.
     *
     * A missing profile fails closed instead of reading shared/global state.
     */
    fun loadAll(
        context: Context,
        profileId: String
    ): List<CatchPreset> {
        val storageKey = profilePresetsKey(profileId) ?: return emptyList()
        migrateLegacyPresetsIfNeeded(context)

        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(storageKey, null)

        if (raw.isNullOrBlank()) {
            return defaultPresets()
        }

        return parsePresets(raw).take(MAX_SAVED_PRESETS)
    }

    /** Saves presets for exactly one profile and ignores blank identities. */
    fun saveAll(
        context: Context,
        profileId: String,
        presets: List<CatchPreset>
    ) {
        val storageKey = profilePresetsKey(profileId) ?: return
        migrateLegacyPresetsIfNeeded(context)

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
            putString(storageKey, array.toString())
        }
    }

    /** Deletes only the presets owned by [profileId]. */
    fun clearProfile(context: Context, profileId: String) {
        val storageKey = profilePresetsKey(profileId) ?: return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(storageKey)
        }
    }

    private fun profilePresetsKey(profileId: String): String? {
        return ProfileScopedPreferenceKey.create(
            prefix = PROFILE_PRESETS_KEY_PREFIX,
            profileId = profileId
        )
    }

    /**
     * Copies the former global preset snapshot to every account present at upgrade.
     *
     * Existing profile-specific snapshots win. When a legacy snapshot exists but
     * no account can be resolved yet, migration is postponed to avoid data loss.
     */
    @Synchronized
    private fun migrateLegacyPresetsIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_PROFILE_SCOPE_MIGRATION_COMPLETE, false)) {
            return
        }

        val legacyRaw = prefs.getString(LEGACY_KEY_PRESETS_JSON, null)
        if (legacyRaw.isNullOrBlank()) {
            prefs.edit {
                remove(LEGACY_KEY_PRESETS_JSON)
                putBoolean(KEY_PROFILE_SCOPE_MIGRATION_COMPLETE, true)
            }
            return
        }

        val profileIds = AccountRepository(context)
            .loadAccounts()
            .map(AccountProfileIdResolver::resolve)
            .filter { it.isNotBlank() }

        if (profileIds.isEmpty()) {
            return
        }

        val targetKeys = CatchPresetProfileMigrationPlanner.missingTargetKeys(
            profileIds = profileIds,
            existingKeys = prefs.all.keys,
            keyPrefix = PROFILE_PRESETS_KEY_PREFIX
        )

        prefs.edit {
            targetKeys.forEach { targetKey ->
                putString(targetKey, legacyRaw)
            }
            remove(LEGACY_KEY_PRESETS_JSON)
            putBoolean(KEY_PROFILE_SCOPE_MIGRATION_COMPLETE, true)
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
                command = command,
                explicitBallId = preset.ballId
            )
        )
    }

    private fun normalizeStoredBallId(
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