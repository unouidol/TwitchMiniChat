package com.fs.twitchminichat

import android.content.Context
import java.util.Locale

/** Complete immutable presentation model for one Quick Catch panel refresh. */
data class QuickCatchPanelModel(
    val menuEntries: List<QuickCatchPresetMenuEntry>,
    val spawnHeader: QuickCatchSpawnHeaderModel
)

/** Text and visibility state for the profile-aware spawn header. */
data class QuickCatchSpawnHeaderModel(
    val title: String,
    val metadata: String?,
    val subtitle: String
)

/** Formats global spawn metadata together with active-profile collection facts. */
object QuickCatchSpawnHeaderFormatter {

    fun build(
        context: Context,
        spawn: SpawnSnapshot?,
        lastKnownSpawn: SpawnSnapshot?,
        profileSpawnContext: QuickCatchProfileSpawnContext,
        nowMs: Long = System.currentTimeMillis()
    ): QuickCatchSpawnHeaderModel {
        if (spawn == null) {
            val nextSpawnRemainingSec = nextSpawnRemainingSec(
                lastKnownSpawn = lastKnownSpawn,
                nowMs = nowMs
            )
            return if (nextSpawnRemainingSec != null) {
                QuickCatchSpawnHeaderModel(
                    title = context.getString(R.string.quick_catch_next_spawn_title),
                    metadata = null,
                    subtitle = context.getString(
                        R.string.quick_catch_next_spawn_subtitle,
                        formatCountdownMmSs(nextSpawnRemainingSec)
                    )
                )
            } else {
                QuickCatchSpawnHeaderModel(
                    title = context.getString(R.string.quick_catch_no_spawn_title),
                    metadata = null,
                    subtitle = context.getString(R.string.quick_catch_no_spawn_subtitle)
                )
            }
        }

        val typesText = when {
            !spawn.type1.isNullOrBlank() && !spawn.type2.isNullOrBlank() ->
                context.getString(
                    R.string.quick_catch_spawn_types,
                    spawn.type1,
                    spawn.type2
                )

            !spawn.type1.isNullOrBlank() -> spawn.type1
            else -> null
        }
        val tierText = spawn.tier?.let { tier ->
            context.getString(R.string.quick_catch_spawn_tier, tier.name)
        } ?: context.getString(R.string.quick_catch_spawn_tier_unknown)
        val metadata = if (typesText.isNullOrBlank()) {
            tierText
        } else {
            context.getString(
                R.string.quick_catch_spawn_metadata,
                typesText,
                tierText
            )
        }

        val dexText = when (profileSpawnContext.dexEntryStatus) {
            QuickCatchDexEntryStatus.REGISTERED ->
                context.getString(R.string.quick_catch_dex_registered)

            QuickCatchDexEntryStatus.MISSING ->
                context.getString(R.string.quick_catch_dex_missing)

            QuickCatchDexEntryStatus.UNKNOWN ->
                context.getString(R.string.quick_catch_dex_unknown)
        }
        val mostWantedText = when (profileSpawnContext.isMostWanted) {
            true -> context.getString(R.string.quick_catch_most_wanted_yes)
            false -> context.getString(R.string.quick_catch_most_wanted_no)
            null -> context.getString(R.string.quick_catch_most_wanted_unknown)
        }
        val remainingText = context.getString(
            R.string.quick_catch_spawn_remaining,
            currentSpawnRemainingSec(spawn, nowMs)
        )

        return QuickCatchSpawnHeaderModel(
            title = spawn.displayName,
            metadata = metadata,
            subtitle = context.getString(
                R.string.quick_catch_spawn_profile_status,
                dexText,
                mostWantedText,
                remainingText
            )
        )
    }

    private fun currentSpawnRemainingSec(
        spawn: SpawnSnapshot,
        nowMs: Long
    ): Int {
        val ageMs = (nowMs - spawn.seenAtMs).coerceAtLeast(0L)
        val ageSec = (ageMs / 1_000L).toInt()
        return (SPAWN_DURATION_SEC - ageSec).coerceAtLeast(0)
    }

    private fun nextSpawnRemainingSec(
        lastKnownSpawn: SpawnSnapshot?,
        nowMs: Long
    ): Int? {
        val lastSpawn = lastKnownSpawn ?: return null
        val ageMs = nowMs - lastSpawn.seenAtMs
        if (ageMs < 0L) return null

        val completedCycles = ageMs / SPAWN_INTERVAL_MS
        val nextSpawnAtMs = lastSpawn.seenAtMs +
            ((completedCycles + 1) * SPAWN_INTERVAL_MS)
        val remainingMs = nextSpawnAtMs - nowMs
        if (remainingMs <= 0L) return 0
        return (remainingMs / 1_000L).toInt()
    }

    private fun formatCountdownMmSs(totalSeconds: Int): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val seconds = safeSeconds % 60
        return "%d:%02d".format(Locale.ROOT, minutes, seconds)
    }

    private const val SPAWN_DURATION_SEC = 90
    private const val SPAWN_INTERVAL_MS = 15 * 60 * 1_000L
}
