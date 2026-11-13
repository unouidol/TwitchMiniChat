package com.fs.twitchminichat.welcome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The table of situations in `docs/proposal-welcome-page.md`, one test per row, plus the
 * daily check and the start gate.
 *
 * Installed here: versionCode 9, versionName 5.6.0. That is a fictional next release,
 * used only so that the numbers read clearly.
 */
class WelcomePolicyTest {

    private val code = 9
    private val name = "5.6.0"

    private fun decide(state: WelcomeState, enabled: Boolean = true) =
        WelcomePolicy.decideAtStart(state, code, name, enabled)

    // Row: first launch, a fresh install or the first start after either reset.
    @Test
    fun firstLaunch_showsTheFirstLaunchPage() {
        assertEquals(
            WelcomePage(WelcomeSituation.FIRST_LAUNCH, name),
            decide(WelcomeState())
        )
    }

    /** A check in flight during a reset can leave a tag in the fresh file. */
    @Test
    fun firstLaunch_winsOverAKnownUpdate() {
        val page = decide(WelcomeState(latestReleaseTag = "v5.6.1"))
        assertEquals(WelcomeSituation.FIRST_LAUNCH, page?.situation)
        assertNull(page?.availableVersion)
    }

    // Row: an existing user's first start of the version that adds the page. Nothing is
    // recorded yet, so it reads as a first launch: Federico's "show it once".
    @Test
    fun existingUserOnTheFirstVersionWithThePage_seesItOnce() {
        assertEquals(WelcomeSituation.FIRST_LAUNCH, decide(WelcomeState())?.situation)
        val record = WelcomePolicy.recordAfterStart(WelcomeState(), decide(WelcomeState()), code)
        assertNull(decide(WelcomeState(lastSeenVersionCode = record.lastSeenVersionCode)))
    }

    // Row: a signed-in user, no update known.
    @Test
    fun sameVersionAndNoUpdateKnown_showsNothing() {
        assertNull(decide(WelcomeState(lastSeenVersionCode = code)))
        assertNull(decide(WelcomeState(lastSeenVersionCode = code, latestReleaseTag = "v5.6.0")))
        assertNull(decide(WelcomeState(lastSeenVersionCode = code, latestReleaseTag = "v5.5.1")))
    }

    // Row added by Federico on 2026-11-12: once after every update.
    @Test
    fun updatedSinceTheLastStart_showsTheUpdatedPageOnce() {
        val before = WelcomeState(lastSeenVersionCode = 8)
        val page = decide(before)
        assertEquals(WelcomePage(WelcomeSituation.UPDATED, name), page)

        val record = WelcomePolicy.recordAfterStart(before, page, code)
        assertEquals(code, record.lastSeenVersionCode)
        assertNull(decide(before.copy(lastSeenVersionCode = record.lastSeenVersionCode)))
    }

    /** Only installable with a debug build, but the decision must still be quiet. */
    @Test
    fun aLowerVersionThanLastSeen_showsNothing() {
        assertNull(decide(WelcomeState(lastSeenVersionCode = 10)))
    }

    // Row: an update known and not shown for that tag.
    @Test
    fun newerReleaseNotYetShown_showsTheNotice() {
        val state = WelcomeState(lastSeenVersionCode = code, latestReleaseTag = "v5.6.1")
        assertEquals(
            WelcomePage(WelcomeSituation.UPDATE_AVAILABLE, name, availableVersion = "5.6.1"),
            decide(state)
        )
    }

    // Row: an update known, already shown for that tag.
    @Test
    fun newerReleaseAlreadyShown_showsNothing() {
        val state = WelcomeState(
            lastSeenVersionCode = code,
            latestReleaseTag = "v5.6.1",
            dismissedReleaseTag = "v5.6.1"
        )
        assertNull(decide(state))
    }

    /** Showing the notice is what dismisses it, for that tag. */
    @Test
    fun showingTheNotice_recordsItsTag() {
        val state = WelcomeState(lastSeenVersionCode = code, latestReleaseTag = "v5.6.1")
        val record = WelcomePolicy.recordAfterStart(state, decide(state), code)
        assertEquals("v5.6.1", record.dismissedReleaseTag)
    }

    @Test
    fun aStartWithoutTheNotice_keepsTheDismissedTag() {
        val state = WelcomeState(lastSeenVersionCode = 8, dismissedReleaseTag = "v5.5.9")
        val record = WelcomePolicy.recordAfterStart(state, decide(state), code)
        assertEquals("v5.5.9", record.dismissedReleaseTag)
        assertEquals(code, record.lastSeenVersionCode)
    }

    // Row: a tag later than the dismissed one.
    @Test
    fun aLaterReleaseThanTheDismissedOne_showsTheNoticeAgain() {
        val state = WelcomeState(
            lastSeenVersionCode = code,
            latestReleaseTag = "v5.6.2",
            dismissedReleaseTag = "v5.6.1"
        )
        assertEquals("5.6.2", decide(state)?.availableVersion)
    }

    /** Installing one release while a later one is known shows one page, not two. */
    @Test
    fun updatedWhileALaterReleaseIsKnown_showsOnlyTheNotice() {
        val before = WelcomeState(lastSeenVersionCode = 8, latestReleaseTag = "v5.6.1")
        val page = decide(before)
        assertEquals(WelcomeSituation.UPDATE_AVAILABLE, page?.situation)

        val record = WelcomePolicy.recordAfterStart(before, page, code)
        val after = before.copy(
            lastSeenVersionCode = record.lastSeenVersionCode,
            dismissedReleaseTag = record.dismissedReleaseTag
        )
        assertNull(decide(after))
    }

    // Rows: offline, and check failed. A failed check stores nothing, so the start decides
    // from what is stored, exactly as if no check had been made.
    @Test
    fun aFailedCheck_storesNothing_soTheStoredStateDecides() {
        assertNull(WelcomePolicy.storableTag(null))
        assertNull(WelcomePolicy.storableTag("v5.6.1-rc1"))
        assertNull(WelcomePolicy.storableTag("garbage"))
        assertEquals("v5.6.1", WelcomePolicy.storableTag("v5.6.1"))
    }

    // Decision 6: dev builds never show the notice.
    @Test
    fun noticeDisabled_neverShowsTheNotice() {
        val state = WelcomeState(lastSeenVersionCode = code, latestReleaseTag = "v9.0.0")
        assertNull(decide(state, enabled = false))
        assertEquals(
            WelcomeSituation.UPDATED,
            decide(state.copy(lastSeenVersionCode = 8), enabled = false)?.situation
        )
        assertEquals(
            WelcomeSituation.FIRST_LAUNCH,
            decide(WelcomeState(), enabled = false)?.situation
        )
    }

    // Row: started from a notification tap, or by a sign-in that just finished.
    @Test
    fun aStartOpenedToAnAccount_doesNotDecide() {
        assertFalse(WelcomePolicy.decidesAtThisStart(savedPending = null, openedToAnAccount = true))
        assertTrue(WelcomePolicy.decidesAtThisStart(savedPending = null, openedToAnAccount = false))
    }

    // Row: recreated, for example by a rotation. A decision still owed stays owed, and one
    // already made is not made again. A page on screen comes back by itself, as a
    // DialogFragment, which only the phone shows.
    @Test
    fun aRecreation_keepsWhatTheSavedFlagSays() {
        assertTrue(WelcomePolicy.decidesAtThisStart(savedPending = true, openedToAnAccount = false))
        assertFalse(
            WelcomePolicy.decidesAtThisStart(savedPending = false, openedToAnAccount = false)
        )
        assertTrue(WelcomePolicy.decidesAtThisStart(savedPending = true, openedToAnAccount = true))
    }

    @Test
    fun theStartGate_letsOneResumeDecide() {
        val gate = WelcomeStartGate(pending = true)
        assertTrue(gate.claim())
        assertFalse(gate.pending)
        assertFalse(gate.claim())
        assertFalse(WelcomeStartGate(pending = false).claim())
    }

    // Row: opened from Safety & Privacy.
    @Test
    fun menu_namesANewerReleaseWheneverOneIsKnown_evenIfShown() {
        val state = WelcomeState(
            lastSeenVersionCode = code,
            latestReleaseTag = "v5.6.1",
            dismissedReleaseTag = "v5.6.1"
        )
        assertEquals(
            WelcomePage(WelcomeSituation.MENU, name, availableVersion = "5.6.1"),
            WelcomePolicy.pageForMenu(state, name, updateNoticeEnabled = true)
        )
    }

    @Test
    fun menu_withoutANewerRelease_saysWhatTheLastCheckFound() {
        assertEquals(
            MenuVersionLine.NO_NEWER_AT_LAST_CHECK,
            WelcomePolicy.pageForMenu(
                WelcomeState(latestReleaseTag = "v5.6.0"),
                name,
                updateNoticeEnabled = true
            ).menuVersionLine
        )
        assertEquals(
            MenuVersionLine.NOT_CHECKED,
            WelcomePolicy.pageForMenu(WelcomeState(), name, updateNoticeEnabled = true)
                .menuVersionLine
        )
    }

    @Test
    fun menu_inDev_neverMentionsAnUpdate() {
        val page = WelcomePolicy.pageForMenu(
            WelcomeState(latestReleaseTag = "v9.0.0"),
            "5.6.0-dev",
            updateNoticeEnabled = false
        )
        assertNull(page.availableVersion)
        assertEquals(MenuVersionLine.NOT_CHECKED, page.menuVersionLine)
    }

    @Test
    fun check_isDueWhenNeverMade_andAtMostOnceADay() {
        val now = 1_800_000_000_000L
        val day = WelcomePolicy.CHECK_INTERVAL_MS
        assertTrue(WelcomePolicy.isCheckDue(true, null, now))
        assertFalse(WelcomePolicy.isCheckDue(true, now, now))
        assertFalse(WelcomePolicy.isCheckDue(true, now - day + 1, now))
        assertTrue(WelcomePolicy.isCheckDue(true, now - day, now))
        assertEquals(24L * 60 * 60 * 1000, day)
    }

    /** A clock set back must not silence the check until the date comes round again. */
    @Test
    fun check_isDueWhenTheLastOneIsInTheFuture() {
        val now = 1_800_000_000_000L
        assertTrue(WelcomePolicy.isCheckDue(true, now + 1, now))
    }

    @Test
    fun check_isNeverDueInDev() {
        assertFalse(WelcomePolicy.isCheckDue(false, null, 1_800_000_000_000L))
        assertFalse(WelcomePolicy.isCheckDue(false, 0L, 1_800_000_000_000L))
    }
}
