package com.fs.twitchminichat

import com.fs.twitchminichat.chat.ChatMessageDeduplicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the contracts shared by local echoes, live Internet Relay Chat (IRC),
 * and backend history without Android views or network access.
 */
class ChatMessageReconciliationScenarioTest {

    /** History may replace the local echo before the matching live delivery arrives. */
    @Test
    fun historyThenLiveReconcilesLocalEchoOnlyOnce() {
        val outgoing = OutgoingChatMessageTracker()
        val deduplicator = fixedTimeDeduplicator()
        val pending = outgoing.register(
            channel = "channel",
            username = "User",
            message = "hello",
            sentAtSec = 100.0
        )

        val history = acceptCanonical(
            outgoing = outgoing,
            deduplicator = deduplicator,
            deliveryKey = "id:message-1",
            channel = "#CHANNEL",
            username = "user",
            message = "hello",
            timestampSec = 101.0
        )
        val interveningLive = acceptCanonical(
            outgoing = outgoing,
            deduplicator = deduplicator,
            deliveryKey = "id:intervening-message",
            channel = "channel",
            username = "Other",
            message = "another row",
            timestampSec = 102.0
        )
        val duplicateLive = acceptCanonical(
            outgoing = outgoing,
            deduplicator = deduplicator,
            deliveryKey = "id:message-1",
            channel = "channel",
            username = "User",
            message = "hello",
            timestampSec = 101.0
        )

        assertTrue(history.accepted)
        assertEquals(pending.localId, history.reconciledLocalId)
        assertTrue(interveningLive.accepted)
        assertNull(interveningLive.reconciledLocalId)
        assertFalse(duplicateLive.accepted)
        assertNull(duplicateLive.reconciledLocalId)
        assertFalse(outgoing.contains(pending.localId))
    }

    /** A duplicate canonical row must not consume the next identical local echo. */
    @Test
    fun duplicateCanonicalDeliveryCannotConsumeSecondIdenticalLocalEcho() {
        val outgoing = OutgoingChatMessageTracker()
        val deduplicator = fixedTimeDeduplicator()
        val firstPending = outgoing.register(
            channel = "channel",
            username = "User",
            message = "same text",
            sentAtSec = 100.0
        )
        val secondPending = outgoing.register(
            channel = "channel",
            username = "User",
            message = "same text",
            sentAtSec = 102.0
        )

        val firstLive = acceptCanonical(
            outgoing = outgoing,
            deduplicator = deduplicator,
            deliveryKey = "id:message-1",
            channel = "channel",
            username = "User",
            message = "same text",
            timestampSec = 100.1
        )
        val duplicateHistory = acceptCanonical(
            outgoing = outgoing,
            deduplicator = deduplicator,
            deliveryKey = "id:message-1",
            channel = "channel",
            username = "User",
            message = "same text",
            timestampSec = 102.0
        )

        assertTrue(firstLive.accepted)
        assertEquals(firstPending.localId, firstLive.reconciledLocalId)
        assertFalse(duplicateHistory.accepted)
        assertTrue(outgoing.contains(secondPending.localId))

        val secondLive = acceptCanonical(
            outgoing = outgoing,
            deduplicator = deduplicator,
            deliveryKey = "id:message-2",
            channel = "channel",
            username = "User",
            message = "same text",
            timestampSec = 102.1
        )

        assertTrue(secondLive.accepted)
        assertEquals(secondPending.localId, secondLive.reconciledLocalId)
        assertFalse(outgoing.contains(secondPending.localId))
    }

    /** USERSTATE confirms transport only; canonical history still replaces the echo. */
    @Test
    fun userStateConfirmationRemainsEligibleForCanonicalHistory() {
        val outgoing = OutgoingChatMessageTracker()
        val deduplicator = fixedTimeDeduplicator()
        val pending = outgoing.register(
            channel = "channel",
            username = "User",
            message = "/me waves",
            sentAtSec = 100.0
        )

        val transportConfirmation = outgoing.confirmOldestFromUserState(
            channel = "#channel",
            confirmedAtSec = 101.0
        )

        assertEquals(pending.localId, transportConfirmation?.localId)
        assertTrue(transportConfirmation?.transportConfirmed == true)
        assertTrue(outgoing.contains(pending.localId))

        val history = acceptCanonical(
            outgoing = outgoing,
            deduplicator = deduplicator,
            deliveryKey = "id:action-message",
            channel = "channel",
            username = "user",
            message = "\u0001ACTION waves\u0001",
            timestampSec = 101.0
        )

        assertTrue(history.accepted)
        assertEquals(pending.localId, history.reconciledLocalId)
        assertFalse(outgoing.contains(pending.localId))
    }

    /** Channel replacement clears both unresolved echoes and accepted delivery IDs. */
    @Test
    fun channelResetStartsReconciliationFromEmptyState() {
        val outgoing = OutgoingChatMessageTracker()
        val deduplicator = fixedTimeDeduplicator()
        val previousPending = outgoing.register(
            channel = "channel-a",
            username = "User",
            message = "still pending",
            sentAtSec = 100.0
        )

        val unrelatedLive = acceptCanonical(
            outgoing = outgoing,
            deduplicator = deduplicator,
            deliveryKey = "id:message-1",
            channel = "channel-a",
            username = "Other",
            message = "unrelated row",
            timestampSec = 101.0
        )
        assertTrue(unrelatedLive.accepted)
        assertNull(unrelatedLive.reconciledLocalId)
        assertTrue(outgoing.contains(previousPending.localId))

        outgoing.clear()
        deduplicator.clear()

        val currentPending = outgoing.register(
            channel = "channel-b",
            username = "User",
            message = "hello",
            sentAtSec = 200.0
        )
        val currentHistory = acceptCanonical(
            outgoing = outgoing,
            deduplicator = deduplicator,
            deliveryKey = "id:message-1",
            channel = "channel-b",
            username = "User",
            message = "hello",
            timestampSec = 201.0
        )

        assertFalse(outgoing.contains(previousPending.localId))
        assertTrue(currentHistory.accepted)
        assertEquals(currentPending.localId, currentHistory.reconciledLocalId)
    }

    /** A late history batch is inserted chronologically around existing live rows. */
    @Test
    fun lateHistoryBatchStaysOrderedAroundLiveRows() {
        val positions = mutableListOf(
            ChatTimelinePosition(timestampMillis = 3_000L, sequence = 3L)
        )

        insertPosition(
            positions,
            ChatTimelinePosition(timestampMillis = 1_000L, sequence = 1L)
        )
        insertPosition(
            positions,
            ChatTimelinePosition(timestampMillis = 2_000L, sequence = 2L)
        )
        insertPosition(
            positions,
            ChatTimelinePosition(timestampMillis = 4_000L, sequence = 4L)
        )

        assertEquals(
            listOf(1_000L, 2_000L, 3_000L, 4_000L),
            positions.map { position -> position.timestampMillis }
        )
    }

    /** Applies the production deduplication-first reconciliation order. */
    private fun acceptCanonical(
        outgoing: OutgoingChatMessageTracker,
        deduplicator: ChatMessageDeduplicator,
        deliveryKey: String,
        channel: String,
        username: String,
        message: String,
        timestampSec: Double
    ): CanonicalAcceptance {
        if (deduplicator.shouldSuppress(deliveryKey)) {
            return CanonicalAcceptance(
                accepted = false,
                reconciledLocalId = null
            )
        }

        val reconciled = outgoing.confirmCanonical(
            channel = channel,
            username = username,
            message = message,
            messageTimestampSec = timestampSec
        )

        return CanonicalAcceptance(
            accepted = true,
            reconciledLocalId = reconciled?.localId
        )
    }

    /** Builds deterministic duplicate timing for scenario tests. */
    private fun fixedTimeDeduplicator(): ChatMessageDeduplicator {
        return ChatMessageDeduplicator(currentTimeMillis = { 0L })
    }

    /** Inserts one position using the production ordering policy. */
    private fun insertPosition(
        positions: MutableList<ChatTimelinePosition>,
        candidate: ChatTimelinePosition
    ) {
        val index = ChatTimelineOrderer.insertionIndex(
            existingPositions = positions,
            candidate = candidate
        )
        positions.add(index, candidate)
    }

    /** Outcome of accepting or suppressing one canonical delivery. */
    private data class CanonicalAcceptance(
        val accepted: Boolean,
        val reconciledLocalId: String?
    )
}
