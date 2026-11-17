package com.fs.twitchminichat.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterises when a chat page attempts its IRC connection again.
 *
 * No clock and no thread: the scheduler is a list the test runs by hand, and
 * the page's connection entry point is a counter behind the controller's guard.
 */
class ChatReconnectControllerTest {

    private val scheduler = ManualScheduler()
    private var started = true
    private var clientPresent = false
    private var connectCalls = 0
    private val logLines = mutableListOf<String>()

    private val controller = ChatReconnectController(
        scheduler = scheduler,
        canConnect = { started },
        connect = {
            connectCalls += 1
        },
        log = ChatReconnectLog { level, message -> logLines += "$level $message" }
    )

    /** A live session that keeps ending is retried with a bounded backoff, without limit. */
    @Test
    fun sessionEndsAreRetriedWithBoundedBackoff() {
        repeat(7) {
            controller.scheduleAfterSessionEnded()
            scheduler.runNext()
        }

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 15_000L, 15_000L), scheduler.delays)
        assertEquals(7, connectCalls)
        assertEquals("WARN reconnect scheduled delayMs=1000 reason=session", logLines[0])
        assertEquals("DEBUG reconnect start reason=session", logLines[1])
    }

    /**
     * #115, defect F1, as it stands: a token step that fails ends the attempt and
     * schedules nothing, whatever the failure was.
     */
    @Test
    fun aFailedTokenStepSchedulesNothing() {
        controller.scheduleAfterSessionEnded()
        scheduler.runNext()
        controller.beginTokenAttempt()

        controller.finishTokenAttempt()

        assertFalse(controller.retryPending)
        assertTrue(scheduler.pending.isEmpty())
        assertEquals(1, connectCalls)
    }

    /** Stopping cancels the retry that was pending. */
    @Test
    fun closingCancelsThePendingRetry() {
        controller.scheduleAfterSessionEnded()

        controller.onConnectionClosed(resetBackoff = true)

        assertFalse(controller.retryPending)
        assertTrue(scheduler.pending.isEmpty())
        assertEquals(0, connectCalls)
    }

    /** Nothing is scheduled while the page is not started. */
    @Test
    fun nothingIsScheduledWhileStopped() {
        started = false

        controller.scheduleAfterSessionEnded()

        assertTrue(scheduler.delays.isEmpty())
    }

    /** A retry that comes due after the page stopped does not connect. */
    @Test
    fun aDueRetryDoesNotConnectAfterStop() {
        controller.scheduleAfterSessionEnded()
        started = false

        scheduler.runNext()

        assertEquals(0, connectCalls)
    }

    /** The guard refuses a second attempt while one runs or a client exists. */
    @Test
    fun theGuardRefusesOverlappingAttempts() {
        assertTrue(controller.mayConnect(clientPresent = false))
        assertFalse(controller.mayConnect(clientPresent = true))

        controller.beginTokenAttempt()

        assertFalse(controller.mayConnect(clientPresent = false))

        controller.finishTokenAttempt()

        assertTrue(controller.mayConnect(clientPresent = false))
    }

    /** No retry is scheduled on top of a running token attempt. */
    @Test
    fun noRetryIsScheduledDuringAnAttempt() {
        controller.beginTokenAttempt()

        controller.scheduleAfterSessionEnded()

        assertTrue(scheduler.delays.isEmpty())
    }

    /** A confirmed session starts the next drop again from the first delay. */
    @Test
    fun aReadySessionResetsTheBackoff() {
        controller.scheduleAfterSessionEnded()
        scheduler.runNext()
        controller.scheduleAfterSessionEnded()
        scheduler.runNext()

        controller.onSessionReady()
        controller.scheduleAfterSessionEnded()

        assertEquals(listOf(1_000L, 2_000L, 1_000L), scheduler.delays)
    }

    /** A client about to open cancels what was scheduled before it. */
    @Test
    fun anOpeningClientCancelsThePendingRetry() {
        controller.scheduleAfterSessionEnded()

        controller.onClientOpening()

        assertTrue(scheduler.pending.isEmpty())
        assertEquals(0, connectCalls)
    }

    /** A scheduler run by hand: tasks wait in a list until the test runs them. */
    private class ManualScheduler : ChatReconnectScheduler {

        /** Tasks posted and neither run nor cancelled, in posting order. */
        val pending = mutableListOf<Runnable>()

        /** Every delay ever posted, in order. */
        val delays = mutableListOf<Long>()

        override fun postDelayed(task: Runnable, delayMs: Long) {
            pending += task
            delays += delayMs
        }

        override fun cancel(task: Runnable) {
            pending.remove(task)
        }

        /** Runs the oldest pending task. */
        fun runNext() {
            pending.removeAt(0).run()
        }
    }
}
