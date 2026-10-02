package com.fs.twitchminichat.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Drives [EmoteAnimationGate] through what a chat page reports - started, stopped,
 * current page or not, the viewport, rows arriving and leaving - and asserts which rows
 * it tells to run, and that it tells a row nothing when nothing changed for it.
 *
 * The viewport is 800 high. Row "a" is at the top of the timeline, "b" just below the
 * first screen, "c" two screens down.
 */
class EmoteAnimationGateTest {

    private val spans = HashMap<String, TimelineSpan?>()
    private val told = mutableListOf<String>()
    private val running = linkedSetOf<String>()

    private val gate = EmoteAnimationGate<String>(
        spanOf = { row -> spans[row] },
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
        gate.onViewport(top = 0, bottom = 800)
        gate.onViewport(top = 800, bottom = 1600)
        gate.onCurrentPage(false)

        assertEquals(emptyList<String>(), told)
    }

    @Test
    fun theCurrentStartedPage_runsOnlyTheRowsInTheViewport() {
        openCurrentPage()

        assertEquals(setOf("a"), running)
    }

    @Test
    fun aPageThatStopsBeingCurrent_stopsEverything_andRunsTheVisibleRowsWhenItIsCurrentAgain() {
        openCurrentPage()
        gate.onViewport(top = 0, bottom = 900)
        assertEquals(setOf("a", "b"), running)

        gate.onCurrentPage(false)
        assertEquals(emptySet<String>(), running)

        gate.onCurrentPage(true)
        assertEquals(setOf("a", "b"), running)
    }

    @Test
    fun theAppGoingToTheBackground_stopsEverything_andComingBackRunsTheVisibleRows() {
        openCurrentPage()

        gate.onStopped()
        assertEquals(emptySet<String>(), running)

        gate.onStarted()
        assertEquals(setOf("a"), running)
    }

    @Test
    fun aCurrentPageNotStartedYet_runsNothing() {
        gate.onCurrentPage(true)
        addRows()
        gate.onViewport(top = 0, bottom = 800)

        assertEquals(emptyList<String>(), told)
    }

    @Test
    fun aPageThatHasNotReportedItsViewport_runsNothing() {
        gate.onStarted()
        gate.onCurrentPage(true)
        addRows()

        assertEquals(emptyList<String>(), told)
    }

    // ---------------------------------------------------------------------------
    // The rows
    // ---------------------------------------------------------------------------

    @Test
    fun aRowThatScrollsOut_stops_andRunsAgainWhenItScrollsBack() {
        openCurrentPage()

        gate.onViewport(top = 800, bottom = 1600)
        assertEquals(setOf("b"), running)

        gate.onViewport(top = 0, bottom = 800)
        assertEquals(setOf("a"), running)
    }

    @Test
    fun aRowNotInTheTimelineYet_runsOnlyOnceItIsPlacedInTheViewport() {
        openCurrentPage()
        spans["d"] = null
        gate.add("d")
        assertEquals(setOf("a"), running)

        /* Inserted and laid out at the top; the next reconcile places it. */
        spans["d"] = TimelineSpan(top = 40, bottom = 80)
        gate.onViewport(top = 0, bottom = 800)

        assertEquals(setOf("a", "d"), running)
    }

    @Test
    fun aRowAddedInsideTheViewportOfTheCurrentPage_runsAtOnce() {
        openCurrentPage()
        spans["d"] = TimelineSpan(top = 100, bottom = 140)

        gate.add("d")

        assertEquals(setOf("a", "d"), running)
    }

    @Test
    fun aRemovedRow_isNeverToldAnythingAgain() {
        openCurrentPage()
        gate.remove("a")
        told.clear()

        gate.onStopped()
        gate.onStarted()

        assertEquals(emptyList<String>(), told)
    }

    @Test
    fun aRowIsToldOnlyWhenItsAnswerChanges() {
        openCurrentPage()
        told.clear()

        /* Same viewport twice, then one that changes nothing but row b, then a again. */
        gate.onViewport(top = 0, bottom = 800)
        gate.onViewport(top = 0, bottom = 800)
        gate.onViewport(top = 0, bottom = 900)
        gate.add("a")

        assertEquals(listOf("b run"), told)
    }

    // ---------------------------------------------------------------------------
    // The edges of the viewport
    // ---------------------------------------------------------------------------

    @Test
    fun aRowEndingExactlyAtTheTopOfTheViewport_isNotVisible() =
        assertVisible(row = TimelineSpan(top = 0, bottom = 500), expected = false)

    @Test
    fun aRowEndingOnePixelInsideTheTopOfTheViewport_isVisible() =
        assertVisible(row = TimelineSpan(top = 0, bottom = 501), expected = true)

    @Test
    fun aRowStartingExactlyAtTheBottomOfTheViewport_isNotVisible() =
        assertVisible(row = TimelineSpan(top = 1300, bottom = 1400), expected = false)

    @Test
    fun aRowStartingOnePixelInsideTheBottomOfTheViewport_isVisible() =
        assertVisible(row = TimelineSpan(top = 1299, bottom = 1400), expected = true)

    @Test
    fun aRowTallerThanTheViewport_isVisible() =
        assertVisible(row = TimelineSpan(top = 0, bottom = 3000), expected = true)

    @Test
    fun aRowEntirelyBelowTheViewport_isNotVisible() =
        assertVisible(row = TimelineSpan(top = 2000, bottom = 2040), expected = false)

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun addRows() {
        spans["a"] = TimelineSpan(top = 0, bottom = 40)
        spans["b"] = TimelineSpan(top = 820, bottom = 860)
        spans["c"] = TimelineSpan(top = 1700, bottom = 1740)
        gate.add("a")
        gate.add("b")
        gate.add("c")
    }

    /** Started, current, three rows, and the first screen in view. */
    private fun openCurrentPage() {
        gate.onStarted()
        gate.onCurrentPage(true)
        addRows()
        gate.onViewport(top = 0, bottom = 800)
    }

    /** One row against a viewport from 500 to 1300 on the current, started page. */
    private fun assertVisible(row: TimelineSpan, expected: Boolean) {
        gate.onStarted()
        gate.onCurrentPage(true)
        gate.onViewport(top = 500, bottom = 1300)
        spans["r"] = row

        gate.add("r")

        assertEquals("row $row against 500..1300", expected, "r" in running)
    }
}
