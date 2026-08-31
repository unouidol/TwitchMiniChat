package com.fs.twitchminichat.pcg.mostwanted

import android.content.Context

/** Stable failure categories for a user-confirmed Most Wanted toggle. */
enum class PcgMostWantedToggleError {
    LOCAL_STATE_UNAVAILABLE,
    SERVER_SYNC_FAILED,
    LOCAL_PERSISTENCE_FAILED
}

/** Result of synchronizing and applying one Most Wanted enabled state. */
data class PcgMostWantedToggleResult(
    val ok: Boolean,
    val effectiveEnabled: Boolean?,
    val error: PcgMostWantedToggleError? = null
)

/**
 * Coordinates one user-confirmed Most Wanted enabled-state change.
 *
 * The backend is updated before local preferences so the bell never claims a
 * state that the notification service rejected. The operation performs one
 * request only and never sends chat or gameplay commands.
 */
class PcgMostWantedToggleController internal constructor(
    private val stateReader:
        (String) -> Result<PcgMostWantedState>,
    private val enabledPersister:
        (String, Boolean) -> Unit,
    private val stateSynchronizer:
        (String, PcgMostWantedState) -> PcgMostWantedSyncResult
) {

    /** Creates the production controller for one application context. */
    constructor(context: Context) : this(
        Dependencies(context.applicationContext)
    )

    /** Connects production dependencies while keeping the policy testable. */
    private constructor(dependencies: Dependencies) : this(
        stateReader = dependencies.store::getState,
        enabledPersister = dependencies.store::updateEnabled,
        stateSynchronizer = dependencies.syncClient::sync
    )

    /**
     * Synchronizes and persists one enabled state.
     *
     * This method performs blocking input/output and must run on a worker
     * thread. A server failure leaves the previous local value untouched.
     */
    fun setEnabled(
        profileId: String,
        enabled: Boolean
    ): PcgMostWantedToggleResult {
        val currentState = stateReader(profileId).getOrElse {
            return PcgMostWantedToggleResult(
                ok = false,
                effectiveEnabled = null,
                error =
                    PcgMostWantedToggleError.LOCAL_STATE_UNAVAILABLE
            )
        }

        val requestedState = currentState.copy(enabled = enabled)
        val syncResult = stateSynchronizer(
            profileId,
            requestedState
        )

        if (!syncResult.ok) {
            return PcgMostWantedToggleResult(
                ok = false,
                effectiveEnabled = currentState.enabled,
                error = PcgMostWantedToggleError.SERVER_SYNC_FAILED
            )
        }

        return runCatching {
            enabledPersister(profileId, enabled)
        }.fold(
            onSuccess = {
                PcgMostWantedToggleResult(
                    ok = true,
                    effectiveEnabled = enabled
                )
            },
            onFailure = {
                PcgMostWantedToggleResult(
                    ok = false,
                    effectiveEnabled = currentState.enabled,
                    error =
                        PcgMostWantedToggleError.LOCAL_PERSISTENCE_FAILED
                )
            }
        )
    }

    /** Production objects shared by the injected controller callbacks. */
    private class Dependencies(context: Context) {
        /** Profile-scoped local watchlist store. */
        val store = PcgMostWantedStore(context)

        /** One-shot authenticated backend client. */
        val syncClient = PcgMostWantedSyncClient(context)
    }
}
