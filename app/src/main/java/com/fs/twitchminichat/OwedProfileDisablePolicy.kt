package com.fs.twitchminichat

/** What the owed-work queue must do about one profile's alerts. */
enum class OwedProfileDisableDecision {

    /** Nothing is owed for this profile. */
    NOTHING_OWED,

    /** The backend still has to be told that this profile's alerts are off. */
    DISABLE,

    /**
     * A disable was owed, and the account has since been signed in again here.
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
 * The rule that would break a working install if it were wrong is
 * [VOID_SIGNED_IN][OwedProfileDisableDecision.VOID_SIGNED_IN]. An owed disable is
 * recorded for an account that has left this phone. If the user signs that account in
 * again, registration gives it a live selection on the backend - and sending the owed
 * disable afterwards would switch off alerts the user has just recreated, with no symptom
 * but notifications that never arrive. Signing in does not postpone the owed disable, it
 * cancels it: what it was for, a profile nobody on this phone can switch off, no longer
 * describes the situation.
 *
 * Pure, so it is tested without a device, a backend or a worker.
 */
object OwedProfileDisablePolicy {

    /**
     * @param acknowledged the last selection the backend confirmed for this profile.
     * @param explicitlyOwed set when a disable attempt has already been made and failed.
     *   It exists because the acknowledged record cannot cover every case on its own: an
     *   installation upgrading into this version has acknowledged nothing yet, so an
     *   account removed before any successful push would look as if it owed nothing.
     * @param local whether the account is on this phone now, re-read at the moment of
     *   acting rather than captured when the work was recorded.
     */
    fun decide(
        acknowledged: PcgProfileAlertSelection?,
        explicitlyOwed: Boolean,
        local: PcgProfileLocalSelection
    ): OwedProfileDisableDecision {
        val acknowledgedImpliesOwed = PcgProfileAlertPushPolicy.decide(
            local = PcgProfileLocalSelection.Removed,
            acknowledged = acknowledged
        ) == PcgProfileAlertPushDecision.Push(PcgProfileAlertSelection.DISABLED)

        if (!explicitlyOwed && !acknowledgedImpliesOwed) {
            return OwedProfileDisableDecision.NOTHING_OWED
        }

        return if (local is PcgProfileLocalSelection.Present) {
            OwedProfileDisableDecision.VOID_SIGNED_IN
        } else {
            OwedProfileDisableDecision.DISABLE
        }
    }
}
