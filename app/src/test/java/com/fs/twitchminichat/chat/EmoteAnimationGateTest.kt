package com.fs.twitchminichat.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Drives [EmoteAnimationGate] through what a chat page reports - started, stopped, current
 * page or not, rows arriving and leaving - and asserts which rows it wants to run, and that
 * it tells a row nothing when nothing changed for it.
 *
 * Whether a row is on screen is no longer the gate's question: since 5d the list detaches a row
 * that scrolls away, and the row's own session stops its emotes while it is detached
 * (TwitchEmoteImageLoader). So every tracked row is wanted exactly while the page is started and
 * current. The viewport tests this file had before 5d went with the viewport.
 */
class EmoteAnimationGateTest {

    private val told = mutableListOf<String>()
    private val running = linkedSetOf<String>()

    private val gate = EmoteAnimationGate<String>(
        setAnimating = { row, animating ->
            told += "$row ${if (animating) "run" else "stop"}"
            if (animating) running += row else running -= row
        }
    )

    // ---------------------------------------------------------------------------
    // The page
    // ---------------------------------------------------------------------------

    @Test
    fun aPageThatHasNeverBeenCurrent_runsNothing() {
        gate.onStarted()
        addRows()
        gate.onCurrentPage(false)

        assertEquals(emptyList<String>(), told)
    }

    @Test
    fun theCurrentStartedPage_wantsEveryTrackedRow() {
        openCurrentPage()

        assertEquals(setOf("a", "b", "c"), running)
    }

    @Test
    fun aPageThatStopsBeingCurrent_stopsEverything_andWantsItsRowsAgainWhenItIsCurrentAgain() {
        openCurrentPage()

        gate.onCurrentPage(false)
        assertEquals(emptySet<String>(), running)

        gate.onCurrentPage(true)
        assertEquals(setOf("a", "b", "c"), running)
    }

    @Test
    fun theAppGoingToTheBackground_stopsEverything_andComingBackWantsItsRowsAgain() {
        openCurrentPage()

        gate.onStopped()
        assertEquals(emptySet<String>(), running)

        gate.onStarted()
        assertEquals(setOf("a", "b", "c"), running)
    }

    @Test
    fun aCurrentPageNotStartedYet_runsNothing() {
        gate.onCurrentPage(true)
        addRows()

        assertEquals(emptyList<String>(), told)
    }

    @Test
    fun aStartedPageNotKnownToBeCurrent_runsNothing() {
        gate.onStarted()
        addRows()

        assertEquals(emptyList<String>(), told)
    }

    // ---------------------------------------------------------------------------
    // The rows
    // ---------------------------------------------------------------------------

    @Test
    fun aRowAddedToTheCurrentStartedPage_runsAtOnce() {
        openCurrentPage()

        gate.add("d")

        assertEquals(setOf("a", "b", "c", "d"), running)
    }

    @Test
    fun aRowAddedToAPageThatIsNotRunning_isToldNothingUntilThePageRuns() {
        gate.onStarted()
        gate.add("d")
        assertEquals(emptyList<String>(), told)

        gate.onCurrentPage(true)

        assertEquals(listOf("d run"), told)
    }

    @Test
    fun aRemovedRow_isNeverToldAnythingAgain() {
        openCurrentPage()
        gate.remove("a")
        told.clear()

        gate.onStopped()
        gate.onStarted()

        assertEquals(listOf("b stop", "c stop", "b run", "c run"), told)
    }

    @Test
    fun aRowIsToldOnlyWhenItsAnswerChanges() {
        openCurrentPage()
        told.clear()

        /* The same answers again, and a row added twice. */
        gate.onStarted()
        gate.onCurrentPage(true)
        gate.add("a")

        assertEquals(emptyList<String>(), told)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun addRows() {
        gate.add("a")
        gate.add("b")
        gate.add("c")
    }

    /** Started, current, three rows. */
    private fun openCurrentPage() {
        gate.onStarted()
        gate.onCurrentPage(true)
        addRows()
    }
}
