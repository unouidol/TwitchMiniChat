package com.fs.twitchminichat

import android.content.SharedPreferences
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * The one lock, lock L, over the owed-work records and the decisions taken on them (#117, F3,
 * PR-2).
 *
 * The owed work is written by four parties that run on different threads:
 *
 * - the passes of [OwedServerOperationRunner], on the worker's thread and the start-up thread;
 * - a sign-in's local section ([NewAccountLocalCommit]), on `"tmc-oauth-finalize"`;
 * - an account removal and its answer ([AccountRemovalDisableAnswer]), on the main thread;
 * - an erase ([DeviceEraseOwedWork]), on the main thread.
 *
 * Before this, nothing ordered them, and five races were shown (cases (a)-(e) of the plan on
 * #117). The rules this class exists to keep:
 *
 * - **Every read-modify-write of the owed file and of the acknowledgement file is made under L**,
 *   with the file's [SharedPreferences] obtained inside the section. An erase deletes both files
 *   under L too, so no write can reach a file through an instance the erase has evicted, which
 *   would write the whole old map back (R3).
 * - **L never covers a network call.** A pass decides and claims under L, leaves L for its
 *   request, and takes L again to apply the answer. The sections under L are local reads and
 *   writes only, so a sign-in, a removal or an erase never waits for the network.
 * - **An answer is applied only to the entry it was sent for.** Each owed entry carries a stamp
 *   that changes whenever anything happens to it; a pass captures it when it decides and
 *   compares it before applying the answer. A record that changed in between - a sign-in, a
 *   second removal, an erase - is left alone (R4, R6).
 * - **An entry is claimed while its request is in flight.** A second attempt that finds it
 *   claimed skips it instead of sending it twice, and reports that, so the worker retries.
 *
 * The stamps and claims live in memory only. No pass outlives the process, so a stamp never has
 * to survive one, and the stored format stays the Boolean and the set 5.6.0 wrote. A counter in
 * the file would not work: the erase deletes the file, and the counter would start again at the
 * value an earlier entry had.
 *
 * One instance per process, [PROCESS]: the application is one process (no `android:process`).
 * Local tests make their own.
 */
internal class OwedWorkCoordinator {

    /**
     * Lock L. Public so a test can check that no network call is made while it is held.
     *
     * Fair: a thread waiting for L gets it before the holder can take it again. A pass takes
     * L once per profile, so without fairness a sign-in waiting behind one section could be
     * overtaken by the next, and the pass would decide on the profile before the sign-in that
     * voids its debt (T9 on #117). Fairness also keeps the wait of a sign-in, a removal or an
     * erase to the sections already queued ahead of it.
     */
    val lock = ReentrantLock(true)

    /**
     * Which version of an owed entry an attempt is acting on.
     *
     * Compared, never ordered: two stamps are equal only if nothing happened to the entry
     * between the moments they were read.
     */
    data class Stamp(val erases: Long, val changes: Long)

    private var erases = 0L
    private var nextChange = 0L
    private var tokenChange = 0L
    private val profileChanges = HashMap<String, Long>()

    private var tokenClaimed = false
    private val claimedProfiles = HashSet<String>()

    /** Runs [block] under L. */
    fun <T> locked(block: () -> T): T = lock.withLock(block)

    // --- Stamps -----------------------------------------------------------------------

    /** The current stamp of the owed token deletion. Under L. */
    fun tokenStamp(): Stamp {
        checkLocked()
        return Stamp(erases, tokenChange)
    }

    /** The current stamp of [profileId]'s owed disable. Under L. */
    fun profileStamp(profileId: String): Stamp {
        checkLocked()
        return Stamp(erases, profileChanges[key(profileId)] ?: 0L)
    }

    /**
     * Marks that [profileId]'s owed record changed: recorded by a failed removal, or cleared by a
     * sign-in, a void decision or an acknowledged disable. Every attempt in flight for it then
     * leaves its answer unapplied. Under L.
     *
     * A sign-in always clears the record, so signing a profile in again always changes its
     * stamp; that is what an attempt in flight relies on to leave the new sign-in's session
     * alone. Removing an account need not change it: a profile can be removed again only after
     * signing in again.
     */
    private fun profileChanged(profileId: String) {
        checkLocked()
        val normalized = key(profileId)
        if (normalized.isBlank()) return
        profileChanges[normalized] = ++nextChange
    }

    /** Marks that the owed token deletion was recorded or dropped. Under L. */
    private fun tokenChanged() {
        checkLocked()
        tokenChange = ++nextChange
    }

    /**
     * Marks that an erase deleted the owed records: every entry an attempt is acting on is now
     * gone, whatever its own stamp said. Under L.
     */
    fun erased() {
        checkLocked()
        erases += 1
        profileChanges.clear()
    }

    // --- Claims -----------------------------------------------------------------------

    /** Claims the owed token deletion; false when another attempt holds it. Under L. */
    fun claimToken(): Boolean {
        checkLocked()
        if (tokenClaimed) return false
        tokenClaimed = true
        return true
    }

    /** Releases the claim [claimToken] took. Under L. */
    fun releaseToken() {
        checkLocked()
        tokenClaimed = false
    }

    /** Claims [profileId]'s disable; false when another attempt holds it. Under L. */
    fun claimProfile(profileId: String): Boolean {
        checkLocked()
        return claimedProfiles.add(key(profileId))
    }

    /** Releases the claim [claimProfile] took. Under L. */
    fun releaseProfile(profileId: String) {
        checkLocked()
        claimedProfiles.remove(key(profileId))
    }

    // --- The owed records, each write with its stamp ---------------------------------------

    /** Records the owed token deletion in [owedRecords]. Under L. */
    fun recordTokenDeletion(owedRecords: SharedPreferences) {
        checkLocked()
        OwedServerOperationStore.recordFirebaseTokenDeletion(owedRecords)
        tokenChanged()
    }

    /** Drops the owed token deletion, whichever entry it is. Under L. */
    fun clearTokenDeletion(owedRecords: SharedPreferences) {
        checkLocked()
        OwedServerOperationStore.clearFirebaseTokenDeletion(owedRecords)
        tokenChanged()
    }

    /**
     * Drops the owed token deletion only if it is still the entry [stamp] was read from.
     * Returns whether it did. Under L.
     */
    fun clearTokenDeletionIfUnchanged(owedRecords: SharedPreferences, stamp: Stamp): Boolean {
        if (tokenStamp() != stamp) return false
        clearTokenDeletion(owedRecords)
        return true
    }

    /** Records [profileId]'s owed disable in [owedRecords]. Under L. */
    fun recordProfileDisable(owedRecords: SharedPreferences, profileId: String) {
        checkLocked()
        OwedServerOperationStore.recordOwedProfileDisable(owedRecords, profileId)
        profileChanged(profileId)
    }

    /** Drops [profileId]'s owed disable, whichever entry it is. Under L. */
    fun clearProfileDisable(owedRecords: SharedPreferences, profileId: String) {
        checkLocked()
        OwedServerOperationStore.clearOwedProfileDisable(owedRecords, profileId)
        profileChanged(profileId)
    }

    private fun checkLocked() {
        check(lock.isHeldByCurrentThread) { "owed-work state used outside lock L" }
    }

    private fun key(profileId: String): String = AccountProfileIdResolver.normalize(profileId)

    companion object {

        /** The application's coordinator. */
        val PROCESS = OwedWorkCoordinator()
    }
}
