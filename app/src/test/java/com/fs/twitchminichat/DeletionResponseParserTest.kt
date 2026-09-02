package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for how destructive deletion responses are interpreted.
 *
 * Both deletion endpoints share this logic, so a response must never be read as a
 * completed deletion unless the backend actually confirmed it.
 */
class DeletionResponseParserTest {

    /** A confirmed deletion is the only case reported as success. */
    @Test
    fun confirmedDeletionIsSuccess() {
        assertEquals(
            DeletionOutcome.Success,
            DeletionResponseParser.parse(
                responseCode = 200,
                rawBody = """{"ok":true,"request_id":"r-1","removed_device":true}"""
            )
        )
    }

    /** A transport failure never counts as a deletion. */
    @Test
    fun missingResponse_isFailureWithoutServerMessage() {
        assertEquals(
            DeletionOutcome.Failure(serverMessage = null),
            DeletionResponseParser.parse(responseCode = null, rawBody = null)
        )
    }

    /** A successful status without the confirmation flag is not a deletion. */
    @Test
    fun successStatusWithoutOkFlag_isFailure() {
        assertEquals(
            DeletionOutcome.Failure(serverMessage = null),
            DeletionResponseParser.parse(
                responseCode = 200,
                rawBody = """{"request_id":"r-1"}"""
            )
        )
    }

    /** An ok flag on a rejected status is not a deletion either. */
    @Test
    fun errorStatusWithOkFlag_isFailure() {
        assertEquals(
            DeletionOutcome.Failure(serverMessage = null),
            DeletionResponseParser.parse(
                responseCode = 500,
                rawBody = """{"ok":true}"""
            )
        )
    }

    /** The first entry of the errors array is surfaced to the user. */
    @Test
    fun errorsArray_providesTheMessage() {
        assertEquals(
            DeletionOutcome.Failure(serverMessage = "device credential rejected"),
            DeletionResponseParser.parse(
                responseCode = 403,
                rawBody = """{"ok":false,"errors":["device credential rejected"]}"""
            )
        )
    }

    /** A single error field is used when no errors array is present. */
    @Test
    fun errorField_providesTheMessage() {
        assertEquals(
            DeletionOutcome.Failure(serverMessage = "unauthorized"),
            DeletionResponseParser.parse(
                responseCode = 401,
                rawBody = """{"ok":false,"error":"unauthorized"}"""
            )
        )
    }

    /** A blank backend message leaves the caller free to use its own text. */
    @Test
    fun blankErrorText_isReportedAsNoServerMessage() {
        assertEquals(
            DeletionOutcome.Failure(serverMessage = null),
            DeletionResponseParser.parse(
                responseCode = 400,
                rawBody = """{"ok":false,"errors":["  "],"error":""}"""
            )
        )
    }

    /** A body that is not valid JavaScript Object Notation is treated as absent. */
    @Test
    fun malformedBody_isFailureWithoutServerMessage() {
        assertEquals(
            DeletionOutcome.Failure(serverMessage = null),
            DeletionResponseParser.parse(responseCode = 200, rawBody = "not json")
        )
    }

    /** An empty body on a successful status is not a deletion. */
    @Test
    fun emptyBody_isFailure() {
        assertEquals(
            DeletionOutcome.Failure(serverMessage = null),
            DeletionResponseParser.parse(responseCode = 200, rawBody = "   ")
        )
    }

    /** Diagnostics report the removal flag and a count, never identifiers. */
    @Test
    fun scopeDescriptionReportsFlagAndCount() {
        assertEquals(
            "removedDevice=true removedDeviceProfileCount=2",
            DeletionResponseParser.describeScope(
                """{"ok":true,"removed_device":true,""" +
                    """"removed_device_profiles":["profile-a","profile-b"]}"""
            )
        )
    }

    /** A response without removal metadata produces no diagnostic text. */
    @Test
    fun scopeDescriptionIsEmptyWhenMetadataIsAbsent() {
        assertEquals(
            "",
            DeletionResponseParser.describeScope("""{"ok":true}""")
        )
        assertEquals("", DeletionResponseParser.describeScope(null))
        assertEquals("", DeletionResponseParser.describeScope("not json"))
    }
}
