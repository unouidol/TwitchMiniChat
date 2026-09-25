package com.fs.twitchminichat

/** Where one profile's alert selection stands on this phone. */
sealed interface PcgProfileLocalSelection {

    /** The account is on this phone and this is what it has chosen. */
    data class Present(val selection: PcgProfileAlertSelection) : PcgProfileLocalSelection

    /**
     * The account is no longer on this phone.
     *
     * Deliberately not "read the stores and see": an account removal clears the
     * profile-scoped stores, and reading them afterwards returns
     * [PcgSpawnAlertMode.DEFAULT], an *active* mode. A removed profile that looked
     * active would be pushed as an enable rather than a disable.
     */
    data object Removed : PcgProfileLocalSelection
}

/** Whether the backend has to be told about one profile's alert selection. */
sealed interface PcgProfileAlertPushDecision {

    /** The backend's copy already matches; send nothing. */
    data object UpToDate : PcgProfileAlertPushDecision

    /** Send this selection to the backend. */
    data class Push(val selection: PcgProfileAlertSelection) : PcgProfileAlertPushDecision
}

/**
 * Decides whether the backend still has to be told about a profile's alert selection.
 *
 * The comparison is between what this phone holds now and what the backend last
 * acknowledged, never between two local values. That is what makes a failed request
 * recoverable: the difference survives the failure, so it can be noticed later.
 *
 * Two cases are the reason this exists, and neither was handled before:
 *
 * - **Nothing active.** A selection with no category enabled still has to be sent. The
 *   registration path treated "nothing to deliver" as "nothing to say", so a phone whose
 *   alerts were all switched off never told the server, which kept sending.
 * - **The profile is gone.** An account removed from this phone owes the backend a
 *   disable, and after the removal there is no local selection to compare - only the
 *   acknowledged one, which is why it is recorded.
 *
 * A profile with no acknowledgement at all is *not* treated as acknowledged-disabled.
 * Nothing is known about what the backend holds, so for a profile still on the phone the
 * selection is sent, and for one already gone nothing is claimed.
 *
 * Pure, so every combination is testable without a device or a backend.
 */
object PcgProfileAlertPushPolicy {

    /**
     * @param local what this phone holds now, or [PcgProfileLocalSelection.Removed].
     * @param acknowledged the last selection the backend confirmed, or null if it has
     *   never confirmed one.
     */
    fun decide(
        local: PcgProfileLocalSelection,
        acknowledged: PcgProfileAlertSelection?
    ): PcgProfileAlertPushDecision {
        return when (local) {
            is PcgProfileLocalSelection.Present -> {
                if (local.selection == acknowledged) {
                    PcgProfileAlertPushDecision.UpToDate
                } else {
                    PcgProfileAlertPushDecision.Push(local.selection)
                }
            }

            PcgProfileLocalSelection.Removed -> when (acknowledged) {
                /*
                 * Nothing acknowledged means nothing is known about the backend's copy.
                 * Sending a disable for a profile this phone may never have registered
                 * would be a request made on a guess.
                 */
                null -> PcgProfileAlertPushDecision.UpToDate

                PcgProfileAlertSelection.DISABLED ->
                    PcgProfileAlertPushDecision.UpToDate

                else -> PcgProfileAlertPushDecision.Push(
                    PcgProfileAlertSelection.DISABLED
                )
            }
        }
    }
}
