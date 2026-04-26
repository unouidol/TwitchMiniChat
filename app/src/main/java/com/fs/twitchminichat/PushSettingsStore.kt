package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

object PushSettingsStore {
    private const val PREFS_NAME = "push_settings"

    const val MODE_DEX_AND_A_TIER = 0
    const val MODE_ALL_SPAWNS = 1
    const val MODE_OFF = 2

    private fun normalizeProfileId(profileId: String?): String {
        return profileId?.trim()?.lowercase().orEmpty()
    }

    private fun legacyEnabledKey(profileId: String): String {
        return "push_enabled_${normalizeProfileId(profileId)}"
    }

    private fun modeKey(profileId: String): String {
        return "push_mode_${normalizeProfileId(profileId)}"
    }

    private fun coerceMode(mode: Int): Int {
        return when (mode) {
            MODE_DEX_AND_A_TIER,
            MODE_ALL_SPAWNS,
            MODE_OFF -> mode

            else -> MODE_DEX_AND_A_TIER
        }
    }

    fun getPushMode(context: Context, profileId: String): Int {
        val normalized = normalizeProfileId(profileId)
        if (normalized.isEmpty()) return MODE_DEX_AND_A_TIER

        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val modeKey = modeKey(normalized)
        if (prefs.contains(modeKey)) {
            return coerceMode(
                prefs.getInt(modeKey, MODE_DEX_AND_A_TIER)
            )
        }

        // Compatibilità con il vecchio booleano:
        // true  -> Dex + A-tier
        // false -> Off
        val legacyKey = legacyEnabledKey(normalized)
        if (prefs.contains(legacyKey)) {
            val legacyEnabled = prefs.getBoolean(legacyKey, true)
            return if (legacyEnabled) {
                MODE_DEX_AND_A_TIER
            } else {
                MODE_OFF
            }
        }

        return MODE_DEX_AND_A_TIER
    }

    fun setPushMode(context: Context, profileId: String, mode: Int) {
        val normalized = normalizeProfileId(profileId)
        if (normalized.isEmpty()) return

        val safeMode = coerceMode(mode)

        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putInt(modeKey(normalized), safeMode)

                // Scriviamo anche il vecchio booleano per compatibilità temporanea
                // con codice non ancora migrato.
                putBoolean(
                    legacyEnabledKey(normalized),
                    safeMode != MODE_OFF
                )
            }
    }

    fun nextPushMode(currentMode: Int): Int {
        return when (coerceMode(currentMode)) {
            MODE_DEX_AND_A_TIER -> MODE_ALL_SPAWNS
            MODE_ALL_SPAWNS -> MODE_OFF
            MODE_OFF -> MODE_DEX_AND_A_TIER
            else -> MODE_DEX_AND_A_TIER
        }
    }

    fun isPushEnabled(context: Context, profileId: String): Boolean {
        return getPushMode(context, profileId) != MODE_OFF
    }

    fun isDexAndATierMode(context: Context, profileId: String): Boolean {
        return getPushMode(context, profileId) == MODE_DEX_AND_A_TIER
    }

    fun isAllSpawnsMode(context: Context, profileId: String): Boolean {
        return getPushMode(context, profileId) == MODE_ALL_SPAWNS
    }

    fun setPushEnabled(context: Context, profileId: String, enabled: Boolean) {
        setPushMode(
            context = context,
            profileId = profileId,
            mode = if (enabled) MODE_DEX_AND_A_TIER else MODE_OFF
        )
    }

    fun syncProfilesForDevice(
        context: Context,
        allProfileIds: Collection<String>,
        enabledProfileIds: Collection<String>
    ) {
        val normalizedEnabled = enabledProfileIds
            .map { normalizeProfileId(it) }
            .filter { it.isNotEmpty() }
            .toSet()

        val normalizedAll = linkedSetOf<String>()
        for (profileId in allProfileIds) {
            val normalized = normalizeProfileId(profileId)
            if (normalized.isNotEmpty()) {
                normalizedAll.add(normalized)
            }
        }

        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit {
            for (profileId in normalizedAll) {
                val mode = if (profileId in normalizedEnabled) {
                    MODE_DEX_AND_A_TIER
                } else {
                    MODE_OFF
                }

                putInt(modeKey(profileId), mode)
                putBoolean(legacyEnabledKey(profileId), mode != MODE_OFF)
            }
        }
    }

    fun syncProfileModesForDevice(
        context: Context,
        allProfileIds: Collection<String>,
        pushModeByProfile: Map<String, Int>
    ) {
        val normalizedAll = linkedSetOf<String>()
        for (profileId in allProfileIds) {
            val normalized = normalizeProfileId(profileId)
            if (normalized.isNotEmpty()) {
                normalizedAll.add(normalized)
            }
        }

        val normalizedModes = pushModeByProfile
            .mapKeys { normalizeProfileId(it.key) }
            .filterKeys { it.isNotEmpty() }
            .mapValues { coerceMode(it.value) }

        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        prefs.edit {
            for (profileId in normalizedAll) {
                val mode = normalizedModes[profileId] ?: MODE_OFF

                putInt(modeKey(profileId), mode)
                putBoolean(legacyEnabledKey(profileId), mode != MODE_OFF)
            }
        }
    }
}