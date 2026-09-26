package com.fs.twitchminichat

/** What the owed-work queue must do about one profile's alerts. */
enum class OwedProfileDisableDecision {

    /** Nothing is owed for this profile. */
    NOTHING_OWED,

    /** The backend still has to be told that this profile's alerts are off. */
    DISABLE,

    /**
     * A disable was really owed - an attempt was made and failed - and the account has
     * since been signed in again here, so it must be dropped instead of sent.
     *
     * Never reported for a profile that owed nothing: see [OwedProfileDisablePolicy].
     */
    VOID_SIGNED_IN
}

/**
 * Decides what the owed-work queue does with one profile it finds still on record.
 *
 * Built on [PcgProfileAlertPushPolicy] rather than beside it: "does this profile owe a
 * disable" is the same comparison that policy already makes for a removed profile, and
 * two copies of it would be free to disagree.
 *
 * A profile still on this phone is decided first, and only the **explicit** marker can
 * make it a cancellation. That ordering matters more than it looks. The acknowledged
 * record cannot distinguish the two situations on its own: asked about a profile as if it
 * were gone, [PcgProfileAlertPushPolicy] answers `Push(DISABLED)` for *any* profile whose
 * acknowledged selection is active - which is every ordinary registered profile with
 * alerts on. Reading that answer as "a disable is owed" without checking the marker put
 * every such profile into the cancellation branch at every start, so the queue reported
 * cancelling a debt that had never existed and wrote
 * `clearOwedProfileDisable` over nothing. The log said work was being undone on a healthy
 * installation, which is worse than saying nothing: the next person reading it has to
 * rule out a defect that is not there.
 *
 * The rule that would break a working install if it were wrong is
 * [VOID_SIGNED_IN][OwedProfileDisableDecision.VOID_SIGNED_IN], and it is unchanged by
 * that ordering. A disable owed by a real failed attempt, for an account the user has
 * since signed in again, must be cancelled and not sent: registration has given that
 * profile a live selection on the backend, and sending the owed disable afterwards would
 * switch off alerts the user has just recreated, with no symptom but notifications that
 * never arrive. Signing in does not postpone such a debt, it cancels it - what it was
 * for, a profile nobody on this phone can switch off, no longer describes the situation.
 *
 * So: a cancellation is reported when there was something to cancel, and never otherwise.
 *
 * Pure, so it is tested without a device, a backend or a worker.
 */
object OwedProfileDisablePolicy {

    /**
     * @param acknowledged the last selection the backend confirmed for this profile.
     * @param explicitlyOwed set when a disable attempt has already been made and failed.
     *   It exists because the acknowledged record cannot cover every case on its own: an
     *   installation upgrading into this version has acknowledged nothing yet, so an
     *   account removed before any successful push would look as if it owed nothing. For
     *   a profile still on this phone it is also the *only* thing that can make a debt,
     *   because the acknowledged record alone cannot tell an owed disable from an ordinary
     *   registration.
     * @param local whether the account is on this phone now, re-read at the moment of
     *   acting rather than captured when the work was recorded.
     */
    fun decide(
        acknowledged: PcgProfileAlertSelection?,
        explicitlyOwed: Boolean,
        local: PcgProfileLocalSelection
    ): OwedProfileDisableDecision {
        if (local is PcgProfileLocalSelection.Present) {
            /*
             * The account is here. Only a failed attempt makes this a cancellation; an
             * active acknowledged selection is what an ordinary registered profile looks
             * like, and calling that a cancelled debt says something untrue about a
             * healthy installation.
             */
            return if (explicitlyOwed) {
                OwedProfileDisableDecision.VOID_SIGNED_IN
            } else {
                OwedProfileDisableDecision.NOTHING_OWED
            }
        }

        /*
         * The account is gone, so an active acknowledged selection does mean the backend
         * is still delivering for a profile nobody here can switch off.
         */
        val acknowledgedImpliesOwed = PcgProfileAlertPushPolicy.decide(
            local = PcgProfileLocalSelection.Removed,
            acknowledged = acknowledged
        ) == PcgProfileAlertPushDecision.Push(PcgProfileAlertSelection.DISABLED)

        return if (explicitlyOwed || acknowledgedImpliesOwed) {
            OwedProfileDisableDecision.DISABLE
        } else {
            OwedProfileDisableDecision.NOTHING_OWED
        }
    }
}
