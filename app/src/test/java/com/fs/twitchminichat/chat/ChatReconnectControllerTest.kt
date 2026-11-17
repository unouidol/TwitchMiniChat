package com.fs.twitchminichat.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When a chat page attempts its IRC connection again.
 *
 * No clock and no thread: the scheduler is a list the test runs by hand, and the
 * page's connection entry point is [pageConnect], which passes the controller's guard
 * the way `ChatFragment.connectIfNeeded` does before it starts a token attempt.
 *
 * Changed by the fix for #115 from the characterisation in `badb458`:
 * - the backoff's cap is 16 s instead of 15 s, the sequence Chatterino2 uses;
 * - "a failed token step schedules nothing" holds only for a failure that is not
 *   transient. A transient one now stays in the reconnect cycle.
 *
 * Added after the first device trial of that fix (V1, 2026-11-17): a network that
 * becomes available starts the backoff again, in every state.
 */
class ChatReconnectControllerTest {

    private val scheduler = ManualScheduler()
    private var started = true
    private var clientPresent = false

    /* Calls of the page's entry point, whether or not the guard let them through. */
    private var connectCalls = 0

    /* Token attempts the guard let through: each one is a connection the page would open. */
    private var attemptsBegun = 0

    private val logLines = mutableListOf<String>()

    private val controller = ChatReconnectController(
        scheduler = scheduler,
        canConnect = { started },
        connect = { pageConnect() },
        log = ChatReconnectLog { level, message -> logLines += "$level $message" }
    )

    /* What ChatFragment.connectIfNeeded does before the token step: guard, then begin. */
    private fun pageConnect() {
        connectCalls += 1
        if (!controller.mayConnect(clientPresent = clientPresent)) return
        controller.beginTokenAttempt()
        attemptsBegun += 1
    }

    /* One token attempt that fails transiently, as the page reports it. */
    private fun failTransiently(minimumDelayMs: Long? = null) {
        controller.finishTokenAttempt()
        controller.scheduleAfterTransientTokenFailure(minimumDelayMs)
    }

    /** A live session that keeps ending is retried with a bounded backoff, without limit. */
    @Test
    fun sessionEndsAreRetriedWithBoundedBackoff() {
        repeat(7) {
            controller.scheduleAfterSessionEnded()
            scheduler.runNext()
            controller.finishTokenAttempt()
        }

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 16_000L, 16_000L), scheduler.delays)
        assertEquals(7, attemptsBegun)
        assertEquals("WARN reconnect scheduled delayMs=1000 reason=session", logLines[0])
        assertEquals("DEBUG reconnect start reason=session", logLines[1])
    }

    /**
     * #115, defect F1: the token step fails while the network is down. The cycle goes
     * on, 1, 2, 4, 8, 16 s and then every 16 s, with no attempt limit.
     */
    @Test
    fun transientTokenFailuresStayInTheCycleWithoutLimit() {
        pageConnect()

        repeat(40) {
            failTransiently()
            assertTrue(controller.awaitingTransientRetry)
            scheduler.runNext()
        }

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L), scheduler.delays.take(4))
        assertEquals(List(36) { 16_000L }, scheduler.delays.drop(4))
        assertEquals(41, attemptsBegun)
        assertEquals("WARN reconnect scheduled delayMs=1000 reason=token_transient", logLines[0])
        assertEquals("DEBUG reconnect start reason=token_transient", logLines[1])
    }

    /** A failure that is not transient schedules nothing, as before #115. */
    @Test
    fun aNonTransientTokenFailureSchedulesNothing() {
        pageConnect()
        controller.finishTokenAttempt()

        controller.recordTokenFailureWithoutRetry()

        assertFalse(controller.retryPending)
        assertFalse(controller.awaitingTransientRetry)
        assertTrue(scheduler.pending.isEmpty())
        assertEquals("WARN reconnect not scheduled reason=token_not_transient", logLines.last())
    }

    /** 429 with Retry-After: the next attempt waits at least that long, then the backoff goes on. */
    @Test
    fun retryAfterIsHonouredThenTheBackoffContinues() {
        pageConnect()

        failTransiently(minimumDelayMs = 60_000L)
        scheduler.runNext()
        failTransiently()
        scheduler.runNext()
        failTransiently()

        assertEquals(listOf(60_000L, 2_000L, 4_000L), scheduler.delays)
        assertEquals(
            "WARN reconnect scheduled delayMs=60000 reason=token_transient retryAfterMs=60000",
            logLines[0]
        )
    }

    /** A Retry-After shorter than the backoff step does not shorten it. */
    @Test
    fun aShortRetryAfterDoesNotShortenTheBackoff() {
        pageConnect()
        repeat(4) {
            failTransiently()
            scheduler.runNext()
        }

        failTransiently(minimumDelayMs = 5_000L)

        assertEquals(16_000L, scheduler.delays.last())
    }

    /** Without a valid Retry-After the normal backoff applies. */
    @Test
    fun noRetryAfterMeansTheNormalBackoff() {
        pageConnect()

        failTransiently(minimumDelayMs = null)

        assertEquals(listOf(1_000L), scheduler.delays)
    }

    /** Stopping cancels the pending retry, and a stopped page retries nothing. */
    @Test
    fun stoppingCancelsTheTransientRetry() {
        pageConnect()
        failTransiently()

        started = false
        controller.onConnectionClosed(resetBackoff = true)
        controller.onNetworkAvailable()

        assertFalse(controller.retryPending)
        assertFalse(controller.awaitingTransientRetry)
        assertTrue(scheduler.pending.isEmpty())
        assertEquals(1, attemptsBegun)
    }

    /** Nothing is scheduled while the page is not started. */
    @Test
    fun nothingIsScheduledWhileStopped() {
        started = false

        controller.scheduleAfterSessionEnded()
        controller.scheduleAfterTransientTokenFailure(minimumDelayMs = null)

        assertTrue(scheduler.delays.isEmpty())
    }

    /** A retry that comes due after the page stopped does not connect. */
    @Test
    fun aDueRetryDoesNotConnectAfterStop() {
        pageConnect()
        failTransiently()
        started = false

        scheduler.runNext()

        assertEquals(1, connectCalls)
    }

    /** After a transient failure, a network that becomes available triggers one attempt at once. */
    @Test
    fun aNetworkThatComesBackTriggersOneAttempt() {
        pageConnect()
        failTransiently()

        controller.onNetworkAvailable()

        assertEquals(2, attemptsBegun)
        assertFalse("the attempt replaces the pending retry", controller.retryPending)
        assertTrue(scheduler.pending.isEmpty())
        assertTrue(logLines.contains("DEBUG reconnect start reason=network"))
    }

    /** Outside the transient-failure state, the network callback starts nothing. */
    @Test
    fun theNetworkCallbackIsIgnoredOutsideTheTransientState() {
        controller.onNetworkAvailable()

        pageConnect()
        controller.onNetworkAvailable()

        controller.finishTokenAttempt()
        controller.recordTokenFailureWithoutRetry()
        controller.onNetworkAvailable()

        controller.scheduleAfterSessionEnded()
        controller.onNetworkAvailable()

        assertEquals(1, attemptsBegun)
        assertTrue("the session retry is still pending", controller.retryPending)
    }

    /** A Retry-After hold is not brought forward by the network callback. */
    @Test
    fun theNetworkCallbackDoesNotBypassRetryAfter() {
        pageConnect()
        failTransiently(minimumDelayMs = 60_000L)

        controller.onNetworkAvailable()

        assertEquals(1, attemptsBegun)
        assertTrue(controller.retryPending)
        assertTrue(logLines.contains("DEBUG network available, retry held by Retry-After"))
    }

    /**
     * V1 on the device, 2026-11-17: the outage grew the backoff to 16 s; the attempt the
     * callback started opened its socket on the first network; the default moved to a
     * second network and that socket ended. Its retry waits 1 s, not 16.
     */
    @Test
    fun aNetworkThatBecomesTheDefaultStartsTheBackoffAgain() {
        pageConnect()
        repeat(5) {
            failTransiently()
            scheduler.runNext()
        }
        failTransiently()

        controller.onNetworkAvailable()
        controller.finishTokenAttempt()
        controller.onClientOpening()
        controller.onNetworkAvailable()
        controller.onConnectionClosed(resetBackoff = false)
        controller.scheduleAfterSessionEnded()

        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 16_000L, 1_000L),
            scheduler.delays
        )
        assertEquals(7, attemptsBegun)
    }

    /** Outside the transient state too, a network change times the next retry from the first delay. */
    @Test
    fun aNetworkChangeResetsTheBackoffOfTheSessionPath() {
        repeat(5) {
            controller.scheduleAfterSessionEnded()
            scheduler.runNext()
            controller.finishTokenAttempt()
        }

        controller.onNetworkAvailable()
        controller.scheduleAfterSessionEnded()

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 1_000L), scheduler.delays)
        assertEquals("the callback itself started nothing", 5, attemptsBegun)
    }

    /** A retry already pending keeps its delay; only the next one starts again. */
    @Test
    fun aNetworkChangeLeavesThePendingRetryAsScheduled() {
        repeat(4) {
            controller.scheduleAfterSessionEnded()
            scheduler.runNext()
            controller.finishTokenAttempt()
        }
        controller.scheduleAfterSessionEnded()

        controller.onNetworkAvailable()

        assertEquals(1, scheduler.pending.size)
        assertEquals(16_000L, scheduler.delays.last())
        scheduler.runNext()
        controller.finishTokenAttempt()
        controller.scheduleAfterSessionEnded()
        assertEquals(1_000L, scheduler.delays.last())
    }

    /** The reset does not shorten a Retry-After: a second 429 after a network change waits it again. */
    @Test
    fun aNetworkChangeDoesNotShortenRetryAfter() {
        pageConnect()
        failTransiently(minimumDelayMs = 60_000L)

        controller.onNetworkAvailable()
        scheduler.runNext()
        failTransiently(minimumDelayMs = 60_000L)

        assertEquals(listOf(60_000L, 60_000L), scheduler.delays)
        assertEquals(2, attemptsBegun)
    }

    /**
     * The callback and the scheduled retry never open two connections: whichever comes
     * first begins the attempt, and the other finds it running or already cancelled.
     */
    @Test
    fun theCallbackAndTheRetryNeverOpenTwoConnections() {
        pageConnect()
        failTransiently()

        controller.onNetworkAvailable()
        controller.onNetworkAvailable()
        scheduler.runAll()

        assertEquals(2, attemptsBegun)
        assertTrue(controller.attemptInProgress)
    }

    /** The retry that runs first makes a later callback find the attempt running. */
    @Test
    fun aRetryThatRunsFirstLeavesTheCallbackNothingToStart() {
        pageConnect()
        failTransiently()

        scheduler.runNext()
        controller.onNetworkAvailable()

        assertEquals(2, attemptsBegun)
        assertEquals(2, connectCalls)
    }

    /** With a client present, neither path begins an attempt. */
    @Test
    fun aPresentClientBlocksBothPaths() {
        pageConnect()
        failTransiently()
        clientPresent = true

        controller.onNetworkAvailable()
        scheduler.runAll()

        assertEquals(1, attemptsBegun)
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
        controller.scheduleAfterTransientTokenFailure(minimumDelayMs = null)

        assertTrue(scheduler.delays.isEmpty())
    }

    /** A confirmed session starts the next drop again from the first delay. */
    @Test
    fun aReadySessionResetsTheBackoff() {
        pageConnect()
        failTransiently()
        scheduler.runNext()
        failTransiently()
        scheduler.runNext()
        controller.finishTokenAttempt()

        controller.onSessionReady()
        controller.scheduleAfterSessionEnded()

        assertEquals(listOf(1_000L, 2_000L, 1_000L), scheduler.delays)
        assertFalse(controller.awaitingTransientRetry)
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

        /** Runs every pending task, including those posted while running. */
        fun runAll() {
            while (pending.isNotEmpty()) {
                runNext()
            }
        }
    }
}
