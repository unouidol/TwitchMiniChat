package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for session-only backend history loading. */
class BackendHistoryClientTest {

    /** A missing session blocks the request instead of selecting legacy authentication. */
    @Test
    fun missingSessionDoesNotOpenTransport() {
        val transport = RecordingTransport(
            result = BackendHistoryTransportResult.Success("[]")
        )
        val client = BackendHistoryClient(
            sessionReader = BackendSessionReader {
                BackendSessionLookup.Missing
            },
            transport = transport
        )

        val result = client.load(
            profileId = "profile-a",
            channel = "unouidol",
            seconds = 3600
        )

        assertEquals(BackendHistoryResult.SessionRequired, result)
        assertEquals(0, transport.callCount)
    }

    /** One stored session produces one Bearer-authenticated request. */
    @Test
    fun presentSessionUsesBearerExactlyOnce() {
        val transport = RecordingTransport(
            result = BackendHistoryTransportResult.Success("[]")
        )
        val client = BackendHistoryClient(
            sessionReader = BackendSessionReader {
                BackendSessionLookup.Present("backend-session")
            },
            transport = transport
        )

        val result = client.load(
            profileId = "PROFILE-A",
            channel = "#Unouidol",
            seconds = 7200
        )

        assertTrue(result is BackendHistoryResult.Success)
        assertEquals(1, transport.callCount)
        assertEquals("unouidol", transport.channel)
        assertEquals(3600, transport.seconds)
        assertEquals("Bearer backend-session", transport.authorizationHeader)
    }

    /** A rejected Bearer session requires reauthorization without a legacy retry. */
    @Test
    fun rejectedSessionDoesNotRetryThroughLegacyAuthentication() {
        val transport = RecordingTransport(
            result = BackendHistoryTransportResult.HttpError(401)
        )
        val client = BackendHistoryClient(
            sessionReader = BackendSessionReader {
                BackendSessionLookup.Present("rejected-session")
            },
            transport = transport
        )

        val result = client.load(
            profileId = "profile-a",
            channel = "unouidol",
            seconds = 60
        )

        assertEquals(BackendHistoryResult.ReauthorizationRequired, result)
        assertEquals(1, transport.callCount)
    }

    /** Parser preserves identifiers, emotes, and timestamps while skipping blank rows. */
    @Test
    fun parserBuildsHistoryMessages() {
        val messages = BackendHistoryResponseParser.parse(
            """
            [
              {
                "user": "viewer",
                "text": "Kappa hello",
                "emotes": "25:0-4",
                "id": "message-1",
                "timestamp": 123.5
              },
              {
                "user": "ignored",
                "text": ""
              }
            ]
            """.trimIndent()
        )

        requireNotNull(messages)
        assertEquals(1, messages.size)
        assertEquals("viewer", messages.single().user)
        assertEquals("Kappa hello", messages.single().text)
        assertEquals("25:0-4", messages.single().emotesRaw)
        assertEquals("message-1", messages.single().messageId)
        assertEquals(123.5, messages.single().timestampSec, 0.0)
    }

    /** Malformed JSON is rejected as a complete response. */
    @Test
    fun malformedResponseIsRejected() {
        assertNull(BackendHistoryResponseParser.parse("{not-an-array}"))
    }

    /** Captures transport arguments and returns one configured response. */
    private class RecordingTransport(
        private val result: BackendHistoryTransportResult
    ) : BackendHistoryTransport {

        /** Number of transport calls made by the client. */
        var callCount: Int = 0

        /** Normalized channel passed to the transport. */
        var channel: String? = null

        /** Bounded history window passed to the transport. */
        var seconds: Int? = null

        /** Authorization header passed to the transport. */
        var authorizationHeader: String? = null

        /** Records one request without performing network input/output. */
        override fun load(
            channel: String,
            seconds: Int,
            authorizationHeader: String
        ): BackendHistoryTransportResult {
            callCount += 1
            this.channel = channel
            this.seconds = seconds
            this.authorizationHeader = authorizationHeader
            return result
        }
    }
}
