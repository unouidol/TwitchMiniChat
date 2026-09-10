package com.fs.twitchminichat

import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterises the read timeout that keeps a half-open Twitch IRC socket from
 * parking the reader thread forever.
 */
class TwitchIrcLivenessPolicyTest {

    /**
     * The timeout sits above one Twitch PING interval and below two.
     *
     * This is the property the value exists for, so it is asserted as a range
     * rather than as a number: below one interval the timeout would expire
     * during an ordinary lull on a quiet channel, and above two it would wait
     * for a second missed PING before reacting.
     */
    @Test
    fun readTimeout_sitsBetweenOneAndTwoPingIntervals() {
        assertTrue(
            "timeout must exceed one PING interval",
            TwitchIrcLivenessPolicy.READ_TIMEOUT_MS > PING_INTERVAL_MS
        )
        assertTrue(
            "timeout must not wait for two missed PINGs",
            TwitchIrcLivenessPolicy.READ_TIMEOUT_MS < 2 * PING_INTERVAL_MS
        )
    }

    /** The documented value reaches the socket unchanged. */
    @Test
    fun applyReadTimeout_setsTheTimeoutOnTheSocket() {
        Socket().use { socket ->
            assertEquals(0, socket.soTimeout)

            TwitchIrcLivenessPolicy.applyReadTimeout(socket)

            assertEquals(
                TwitchIrcLivenessPolicy.READ_TIMEOUT_MS,
                socket.soTimeout
            )
        }
    }

    /** A default socket blocks forever, which is the defect being fixed. */
    @Test
    fun applyReadTimeout_replacesTheUnboundedDefault() {
        Socket().use { socket ->
            TwitchIrcLivenessPolicy.applyReadTimeout(socket)

            assertTrue(
                "an unset timeout reads as 0 and never expires",
                socket.soTimeout > 0
            )
        }
    }

    /** A read timeout is recognised as such. */
    @Test
    fun isReadTimeout_recognisesSocketTimeout() {
        assertTrue(
            TwitchIrcLivenessPolicy.isReadTimeout(SocketTimeoutException("Read timed out"))
        )
    }

    /** Other failures are not read timeouts, including the null cause. */
    @Test
    fun isReadTimeout_rejectsEverythingElse() {
        assertFalse(TwitchIrcLivenessPolicy.isReadTimeout(IOException("broken pipe")))
        assertFalse(TwitchIrcLivenessPolicy.isReadTimeout(IllegalStateException()))
        assertFalse(TwitchIrcLivenessPolicy.isReadTimeout(null))
    }

    /**
     * A read timeout reconnects, which is the whole point of arming it.
     *
     * The timeout has to reach the same disconnect path as a stream that ended,
     * so that the existing backoff and history recovery apply to it without any
     * handling of its own.
     */
    @Test
    fun shouldReconnectAfter_reconnectsOnAReadTimeout() {
        assertTrue(
            TwitchIrcLivenessPolicy.shouldReconnectAfter(
                cause = SocketTimeoutException("Read timed out"),
                disconnectRequested = false
            )
        )
    }

    /** A read timeout is treated exactly like any other terminal failure. */
    @Test
    fun shouldReconnectAfter_treatsEveryCauseAlike() {
        for (cause in causes()) {
            assertTrue(
                "cause ${cause?.javaClass?.simpleName ?: "null"} should reconnect",
                TwitchIrcLivenessPolicy.shouldReconnectAfter(
                    cause = cause,
                    disconnectRequested = false
                )
            )
        }
    }

    /** A disconnect the user asked for is never undone by a reconnect. */
    @Test
    fun shouldReconnectAfter_staysDownWhenTheUserAskedToDisconnect() {
        for (cause in causes()) {
            assertFalse(
                "cause ${cause?.javaClass?.simpleName ?: "null"} must stay down",
                TwitchIrcLivenessPolicy.shouldReconnectAfter(
                    cause = cause,
                    disconnectRequested = true
                )
            )
        }
    }

    /** The reconnection in progress is not shown to the user as an error. */
    @Test
    fun shouldReportToUser_staysSilentForAReadTimeout() {
        assertFalse(
            TwitchIrcLivenessPolicy.shouldReportToUser(
                SocketTimeoutException("Read timed out")
            )
        )
    }

    /** Every other failure keeps reaching the status line as before. */
    @Test
    fun shouldReportToUser_stillReportsEverythingElse() {
        assertTrue(TwitchIrcLivenessPolicy.shouldReportToUser(IOException("broken pipe")))
        assertTrue(TwitchIrcLivenessPolicy.shouldReportToUser(IllegalStateException()))
        assertTrue(TwitchIrcLivenessPolicy.shouldReportToUser(null))
    }

    private fun causes(): List<Throwable?> {
        return listOf(
            SocketTimeoutException("Read timed out"),
            IOException("broken pipe"),
            IllegalStateException(),
            null
        )
    }

    private companion object {
        /** Cadence of the Twitch server PING, the only guaranteed inbound traffic. */
        const val PING_INTERVAL_MS = 300_000
    }
}
