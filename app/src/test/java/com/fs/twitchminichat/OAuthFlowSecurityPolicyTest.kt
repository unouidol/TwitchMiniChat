package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for Android OAuth callback correlation and fail-closed validation. */
class OAuthFlowSecurityPolicyTest {

    /** A callback from the exact app URI with one token and one random slot is accepted. */
    @Test
    fun exactCallbackIsAccepted() {
        val result = OAuthFlowSecurityPolicy.validateCallback(
            input = validCallbackInput(),
            expectedScheme = "ircminichatdev"
        )

        assertEquals("opaque-token", result?.loginToken)
        assertEquals(123456789, result?.slot)
    }

    /** Custom-scheme callbacks with a different origin or URI structure are rejected. */
    @Test
    fun unexpectedCallbackOriginIsRejected() {
        val invalidInputs = listOf(
            validCallbackInput().copy(scheme = "other-app"),
            validCallbackInput().copy(host = "not-auth"),
            validCallbackInput().copy(port = 443),
            validCallbackInput().copy(path = "/other"),
            validCallbackInput().copy(userInfo = "attacker"),
            validCallbackInput().copy(fragment = "fragment"),
            validCallbackInput().copy(isHierarchical = false)
        )

        invalidInputs.forEach { input ->
            assertNull(
                OAuthFlowSecurityPolicy.validateCallback(
                    input = input,
                    expectedScheme = "ircminichatdev"
                )
            )
        }
    }

    /** Duplicate or malformed correlation parameters fail before pending state is consumed. */
    @Test
    fun malformedCriticalParametersAreRejected() {
        val invalidInputs = listOf(
            validCallbackInput().copy(loginTokens = emptyList()),
            validCallbackInput().copy(loginTokens = listOf("first", "second")),
            validCallbackInput().copy(loginTokens = listOf(" token ")),
            validCallbackInput().copy(loginTokens = listOf("line\nbreak")),
            validCallbackInput().copy(loginTokens = listOf("a".repeat(257))),
            validCallbackInput().copy(slots = emptyList()),
            validCallbackInput().copy(slots = listOf("1", "2")),
            validCallbackInput().copy(slots = listOf("0")),
            validCallbackInput().copy(slots = listOf(" 1")),
            validCallbackInput().copy(slots = listOf("not-a-number"))
        )

        invalidInputs.forEach { input ->
            assertNull(
                OAuthFlowSecurityPolicy.validateCallback(
                    input = input,
                    expectedScheme = "ircminichatdev"
                )
            )
        }
    }

    /** Requests are valid only inside the bounded wall-clock window. */
    @Test
    fun pendingRequestLifetimeIsBounded() {
        val createdAt = 1_000_000L

        assertTrue(OAuthFlowSecurityPolicy.isPendingRequestFresh(createdAt, createdAt))
        assertTrue(
            OAuthFlowSecurityPolicy.isPendingRequestFresh(
                createdAt,
                createdAt + OAuthFlowSecurityPolicy.PENDING_REQUEST_TTL_MS
            )
        )
        assertFalse(
            OAuthFlowSecurityPolicy.isPendingRequestFresh(
                createdAt,
                createdAt + OAuthFlowSecurityPolicy.PENDING_REQUEST_TTL_MS + 1L
            )
        )
        assertFalse(OAuthFlowSecurityPolicy.isPendingRequestFresh(0L, createdAt))
        assertFalse(OAuthFlowSecurityPolicy.isPendingRequestFresh(createdAt, 0L))
        assertFalse(OAuthFlowSecurityPolicy.isPendingRequestFresh(createdAt, createdAt - 60_001L))
    }

    /** Complete backend identity and the exact consumed slot are required. */
    @Test
    fun finalizeResultMustMatchConsumedRequest() {
        val pending = pendingRequest(slot = 987654321)
        val validResult = finalizeResult(slot = pending.slot)

        assertTrue(
            OAuthFlowSecurityPolicy.isValidFinalizeResult(
                pendingRequest = pending,
                result = validResult
            )
        )
        assertFalse(
            OAuthFlowSecurityPolicy.isValidFinalizeResult(
                pendingRequest = pending,
                result = validResult.copy(slot = pending.slot - 1)
            )
        )
    }

    /** Missing backend identity or session data can never create or update an account. */
    @Test
    fun incompleteFinalizeResultIsRejected() {
        val pending = pendingRequest(slot = 123)
        val validResult = finalizeResult(slot = pending.slot)
        val invalidResults = listOf(
            validResult.copy(profileId = ""),
            validResult.copy(username = ""),
            validResult.copy(userId = ""),
            validResult.copy(accessToken = ""),
            validResult.copy(desktopSessionToken = "")
        )

        invalidResults.forEach { result ->
            assertFalse(
                OAuthFlowSecurityPolicy.isValidFinalizeResult(
                    pendingRequest = pending,
                    result = result
                )
            )
        }
    }

    /** Pending-request diagnostics never reveal the verifier or account metadata. */
    @Test
    fun pendingRequestStringIsRedacted() {
        val pending = pendingRequest(slot = 123).copy(
            channel = "private-channel",
            expectedProfileId = "private-profile"
        )
        val rendered = pending.toString()

        assertFalse(rendered.contains(pending.codeVerifier))
        assertFalse(rendered.contains(pending.channel))
        assertFalse(rendered.contains(pending.expectedProfileId))
    }

    /** The random allocator skips invalid and occupied values without falling back to zero. */
    @Test
    fun slotAllocatorRetriesInvalidAndOccupiedCandidates() {
        val candidates = ArrayDeque(listOf(0, 77, 88))
        val allocator = OAuthCallbackSlotAllocator {
            candidates.removeFirst()
        }

        val result = allocator.allocate { candidate -> candidate == 77 }

        assertEquals(88, result)
    }

    /** A faulty candidate source fails closed after bounded retries. */
    @Test
    fun slotAllocatorHasNoPredictableFallback() {
        val allocator = OAuthCallbackSlotAllocator { 0 }

        assertNull(allocator.allocate { false })
    }

    /** Builds one callback matching the dev flavor contract. */
    private fun validCallbackInput(): OAuthCallbackInput {
        return OAuthCallbackInput(
            isHierarchical = true,
            scheme = "ircminichatdev",
            host = "auth",
            port = -1,
            path = "",
            userInfo = null,
            fragment = null,
            loginTokens = listOf("opaque-token"),
            slots = listOf("123456789")
        )
    }

    /** Builds fresh local metadata for finalization policy tests. */
    private fun pendingRequest(slot: Int): OAuthPendingRequest {
        return OAuthPendingRequest(
            slot = slot,
            channel = "channel",
            accountId = "",
            expectedUsername = "",
            expectedProfileId = "",
            codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
            createdAtEpochMs = 1_000_000L
        )
    }

    /** Builds a complete backend result for one callback slot. */
    private fun finalizeResult(slot: Int): OAuthFinalizeResult {
        return OAuthFinalizeResult(
            profileId = "profile-a",
            slot = slot,
            username = "example-user",
            userId = "12345",
            accessToken = "twitch-token",
            desktopSessionToken = "backend-session"
        )
    }
}
