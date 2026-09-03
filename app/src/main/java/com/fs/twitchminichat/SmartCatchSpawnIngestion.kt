package com.fs.twitchminichat

import android.content.Context
import android.util.Log
import com.fs.twitchminichat.diagnostics.HistoryDiagnosticsLog
import com.fs.twitchminichat.pcg.PcgNotificationPayloadPolicy

/**
 * Application-scoped Smart Catch spawn entry point shared by IRC and FCM.
 *
 * The coordinator retains only the application context, so it remains safe when
 * the chat Fragment has no visible view or the app receives a background push.
 */
internal object SmartCatchSpawnIngestion {

    @Volatile
    private var coordinator: SmartCatchSpawnCoordinator? = null

    fun ingestIrcMessage(
        context: Context,
        user: String,
        message: String,
        messageTimestampSec: Double?
    ): SmartCatchSpawnIngestionResult {
        return safely(SmartCatchSpawnSource.IRC) {
            coordinator(context).ingestIrcMessage(
                user = user,
                message = message,
                messageTimestampSec = messageTimestampSec
            )
        }.also { result -> recordObservation(context, result) }
    }

    fun ingestFcmPayload(
        context: Context,
        data: Map<String, String>,
        messageSentAtMs: Long
    ): SmartCatchSpawnIngestionResult {
        val source = if (PcgNotificationPayloadPolicy.isSpawnReminder(data)) {
            SmartCatchSpawnSource.FCM_REMINDER
        } else {
            SmartCatchSpawnSource.FCM_INITIAL
        }

        return safely(source) {
            coordinator(context).ingestFcmPayload(
                data = data,
                messageSentAtMs = messageSentAtMs
            )
        }.also { result -> recordObservation(context, result) }
    }

    private fun coordinator(context: Context): SmartCatchSpawnCoordinator {
        coordinator?.let { return it }

        return synchronized(this) {
            coordinator ?: run {
                val applicationContext = context.applicationContext
                SmartCatchSpawnCoordinator(
                    resolvePokemon = { rawName ->
                        PokemonTypeDex.findByPokemonName(
                            applicationContext,
                            rawName
                        )
                    },
                    loadCurrentSpawn = { nowMs ->
                        CurrentSpawnStore.load(
                            context = applicationContext,
                            nowMs = nowMs
                        )
                    },
                    saveCurrentSpawn = { spawn ->
                        CurrentSpawnStore.save(
                            context = applicationContext,
                            spawn = spawn
                        )
                    }
                ).also {
                    coordinator = it
                }
            }
        }
    }

    private inline fun safely(
        source: SmartCatchSpawnSource,
        action: () -> SmartCatchSpawnIngestionResult
    ): SmartCatchSpawnIngestionResult {
        return try {
            action()
        } catch (error: Throwable) {
            Log.w(
                LOG_TAG,
                "Spawn ingestion failed source=${source.name.lowercase()} " +
                    "errorType=${DiagnosticError.typeOf(error)}"
            )
            SmartCatchSpawnIngestionResult(
                source = source,
                outcome = SmartCatchSpawnIngestionOutcome.FAILED
            )
        }
    }

    /**
     * Logs one spawn observation and journals the ones describing a real spawn.
     *
     * The journal recorded what happened to an arriving push but never that a
     * spawn had occurred at all, so a push that never reached the process left no
     * trace and could not be told apart from a window containing no spawn. An IRC
     * observation does not depend on Firebase and supplies that missing anchor.
     */
    private fun recordObservation(
        context: Context,
        result: SmartCatchSpawnIngestionResult
    ) {
        val source = result.source ?: return

        if (result.outcome != SmartCatchSpawnIngestionOutcome.FAILED) {
            Log.d(
                LOG_TAG,
                "Spawn ingestion source=${source.name.lowercase()} " +
                    "outcome=${result.outcome.name.lowercase()}"
            )
        }

        /*
         * IGNORED_NOT_SPAWN covers every ordinary chat line, so excluding it keeps
         * the journal at roughly one entry per announced spawn instead of one per
         * message.
         */
        if (result.outcome == SmartCatchSpawnIngestionOutcome.IGNORED_NOT_SPAWN) {
            return
        }

        HistoryDiagnosticsLog.record(
            context,
            "spawn.observed",
            "source" to source.name.lowercase(),
            "outcome" to result.outcome.name.lowercase()
        )
    }

    private const val LOG_TAG = "SMART_CATCH_SPAWN"
}
