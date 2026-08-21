package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.PcgNotificationPayloadPolicy

/** Identifies the independent source that observed one PCG spawn. */
internal enum class SmartCatchSpawnSource {
    IRC,
    FCM_INITIAL,
    FCM_REMINDER
}

/** Describes how one observation affected the persisted Smart Catch spawn. */
internal enum class SmartCatchSpawnIngestionOutcome {
    STORED,
    UNCHANGED,
    IGNORED_NOT_SPAWN,
    IGNORED_EXPIRED,
    IGNORED_FUTURE,
    IGNORED_OLDER,
    FAILED
}

/** Result returned to UI and notification entry points after spawn ingestion. */
internal data class SmartCatchSpawnIngestionResult(
    val source: SmartCatchSpawnSource?,
    val outcome: SmartCatchSpawnIngestionOutcome
) {
    val snapshotChanged: Boolean
        get() = outcome == SmartCatchSpawnIngestionOutcome.STORED
}

/** Pure merge decision used before writing the shared current-spawn store. */
internal data class SmartCatchSpawnMergeDecision(
    val outcome: SmartCatchSpawnIngestionOutcome,
    val seenAtMs: Long? = null
)

/**
 * Protects the 90-second Smart Catch window from stale history and FCM reminders.
 *
 * Observations of the same spawn preserve the earliest valid timestamp. This keeps
 * a delayed Firebase Cloud Messaging (FCM) reminder from restarting the timer.
 */
internal object SmartCatchSpawnMergePolicy {

    const val ACTIVE_WINDOW_MS = 90_000L
    const val FCM_REMINDER_DELAY_MS = 45_000L

    private const val MAX_FUTURE_CLOCK_SKEW_MS = 5_000L

    fun decide(
        nowMs: Long,
        incomingIdentity: String,
        incomingSeenAtMs: Long,
        existingIdentity: String?,
        existingSeenAtMs: Long?
    ): SmartCatchSpawnMergeDecision {
        if (incomingIdentity.isBlank()) {
            return SmartCatchSpawnMergeDecision(
                SmartCatchSpawnIngestionOutcome.IGNORED_NOT_SPAWN
            )
        }

        if (incomingSeenAtMs > nowMs + MAX_FUTURE_CLOCK_SKEW_MS) {
            return SmartCatchSpawnMergeDecision(
                SmartCatchSpawnIngestionOutcome.IGNORED_FUTURE
            )
        }

        val normalizedIncomingSeenAtMs = incomingSeenAtMs
            .takeIf { it > 0L }
            ?.coerceAtMost(nowMs)
            ?: nowMs

        if (nowMs - normalizedIncomingSeenAtMs > ACTIVE_WINDOW_MS) {
            return SmartCatchSpawnMergeDecision(
                SmartCatchSpawnIngestionOutcome.IGNORED_EXPIRED
            )
        }

        val normalizedExistingSeenAtMs = existingSeenAtMs
            ?.takeIf { it > 0L }
            ?.takeIf { it <= nowMs + MAX_FUTURE_CLOCK_SKEW_MS }
            ?.coerceAtMost(nowMs)
            ?.takeIf { nowMs - it <= ACTIVE_WINDOW_MS }

        if (
            normalizedExistingSeenAtMs == null ||
            existingIdentity.isNullOrBlank()
        ) {
            return SmartCatchSpawnMergeDecision(
                outcome = SmartCatchSpawnIngestionOutcome.STORED,
                seenAtMs = normalizedIncomingSeenAtMs
            )
        }

        if (existingIdentity == incomingIdentity) {
            return if (normalizedIncomingSeenAtMs < normalizedExistingSeenAtMs) {
                SmartCatchSpawnMergeDecision(
                    outcome = SmartCatchSpawnIngestionOutcome.STORED,
                    seenAtMs = normalizedIncomingSeenAtMs
                )
            } else {
                SmartCatchSpawnMergeDecision(
                    SmartCatchSpawnIngestionOutcome.UNCHANGED
                )
            }
        }

        if (normalizedExistingSeenAtMs > normalizedIncomingSeenAtMs) {
            return SmartCatchSpawnMergeDecision(
                SmartCatchSpawnIngestionOutcome.IGNORED_OLDER
            )
        }

        return SmartCatchSpawnMergeDecision(
            outcome = SmartCatchSpawnIngestionOutcome.STORED,
            seenAtMs = normalizedIncomingSeenAtMs
        )
    }
}

/**
 * Ingests PCG spawns from live/history IRC messages and FCM data messages.
 *
 * Persistence and Pokémon metadata lookup are injected so lifecycle-independent
 * merge behavior can be covered by local unit tests without Android UI objects.
 */
internal class SmartCatchSpawnCoordinator(
    private val resolvePokemon: (String) -> PokemonTypeEntry?,
    private val loadCurrentSpawn: (Long) -> SpawnSnapshot?,
    private val saveCurrentSpawn: (SpawnSnapshot) -> Unit,
    private val nowMs: () -> Long = System::currentTimeMillis
) {

    fun ingestIrcMessage(
        user: String,
        message: String,
        messageTimestampSec: Double?
    ): SmartCatchSpawnIngestionResult {
        if (!user.trim().equals(PCG_BOT_USERNAME, ignoreCase = true)) {
            return ignoredNotSpawn()
        }

        val parsed = SpawnMessageParser.parse(message)
            ?: return ignoredNotSpawn()

        val currentTimeMs = nowMs()
        val seenAtMs = messageTimestampSec
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?.times(1_000.0)
            ?.toLong()
            ?: currentTimeMs

        return ingestPokemon(
            rawName = parsed.rawName,
            source = SmartCatchSpawnSource.IRC,
            seenAtMs = seenAtMs,
            currentTimeMs = currentTimeMs
        )
    }

    fun ingestFcmPayload(
        data: Map<String, String>,
        messageSentAtMs: Long
    ): SmartCatchSpawnIngestionResult {
        val rawName = data[POKEMON_KEY]
            ?.trim()
            .orEmpty()

        if (rawName.isBlank()) {
            return ignoredNotSpawn()
        }

        val currentTimeMs = nowMs()
        val isReminder = PcgNotificationPayloadPolicy.isSpawnReminder(data)
        val source = if (isReminder) {
            SmartCatchSpawnSource.FCM_REMINDER
        } else {
            SmartCatchSpawnSource.FCM_INITIAL
        }

        val explicitStartedAtMs = data[SPAWN_STARTED_AT_MS_KEY]
            ?.trim()
            ?.toLongOrNull()
            ?.takeIf { it > 0L }

        val deliveryAnchorMs = messageSentAtMs
            .takeIf { it > 0L }
            ?: currentTimeMs

        val seenAtMs = explicitStartedAtMs ?: if (isReminder) {
            (deliveryAnchorMs - SmartCatchSpawnMergePolicy.FCM_REMINDER_DELAY_MS)
                .coerceAtLeast(0L)
        } else {
            deliveryAnchorMs
        }

        return ingestPokemon(
            rawName = rawName,
            source = source,
            seenAtMs = seenAtMs,
            currentTimeMs = currentTimeMs
        )
    }

    @Synchronized
    private fun ingestPokemon(
        rawName: String,
        source: SmartCatchSpawnSource,
        seenAtMs: Long,
        currentTimeMs: Long
    ): SmartCatchSpawnIngestionResult {
        val dexEntry = resolvePokemon(rawName)
        val incomingIdentity = canonicalIdentity(
            rawName = rawName,
            dexKey = dexEntry?.key
        )
        val existing = loadCurrentSpawn(currentTimeMs)
        val existingIdentity = existing?.let {
            canonicalIdentity(
                rawName = it.rawName,
                dexKey = it.dexKey
            )
        }

        val decision = SmartCatchSpawnMergePolicy.decide(
            nowMs = currentTimeMs,
            incomingIdentity = incomingIdentity,
            incomingSeenAtMs = seenAtMs,
            existingIdentity = existingIdentity,
            existingSeenAtMs = existing?.seenAtMs
        )

        val acceptedSeenAtMs = decision.seenAtMs
            ?: return SmartCatchSpawnIngestionResult(
                source = source,
                outcome = decision.outcome
            )

        val sameExistingSpawn = existing
            ?.takeIf { existingIdentity == incomingIdentity }

        saveCurrentSpawn(
            SpawnSnapshot(
                rawName = rawName,
                dexKey = dexEntry?.key,
                displayName = dexEntry?.pcgName ?: rawName,
                type1 = dexEntry?.type1,
                type2 = dexEntry?.type2,
                weightKg = dexEntry?.weightKg,
                baseSpeed = dexEntry?.baseSpeed,
                baseHp = dexEntry?.baseHp,
                evolvesTwice = dexEntry?.evolvesTwice,
                seenAtMs = acceptedSeenAtMs,
                isAlreadyCaught = sameExistingSpawn?.isAlreadyCaught
            )
        )

        return SmartCatchSpawnIngestionResult(
            source = source,
            outcome = SmartCatchSpawnIngestionOutcome.STORED
        )
    }

    private fun canonicalIdentity(
        rawName: String,
        dexKey: String?
    ): String {
        return dexKey
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: PokemonNameNormalizer.normalize(rawName)
    }

    private fun ignoredNotSpawn(): SmartCatchSpawnIngestionResult {
        return SmartCatchSpawnIngestionResult(
            source = null,
            outcome = SmartCatchSpawnIngestionOutcome.IGNORED_NOT_SPAWN
        )
    }

    private companion object {
        const val PCG_BOT_USERNAME = "pokemoncommunitygame"
        const val POKEMON_KEY = "pokemon"
        const val SPAWN_STARTED_AT_MS_KEY = "spawn_started_at_ms"
    }
}
