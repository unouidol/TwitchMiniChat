package com.fs.twitchminichat.chat

import com.fs.twitchminichat.BackendHistoryResult
import com.fs.twitchminichat.chat.HistoryBackfillResultOutcome.Applied
import com.fs.twitchminichat.chat.HistoryBackfillResultOutcome.Discarded
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives [ChatChannelBinding] the two ways a chat page changes channel - a join from the
 * channel field, which calls [ChatChannelBinding.changeTo], and a start that finds
 * another channel, through [ChatChannelBinding.onStarted] - and asserts the same
 * outcome for both: what the page is asked to do, which messages are still suppressed,
 * whose names are suggested, and whether a history result sent before is applied.
 */
class ChatChannelBindingTest {

    private var nowMs = T0

    private val backfillState = HistoryBackfillState()
    private val deduplicator = ChatMessageDeduplicator(currentTimeMillis = { nowMs })
    private val mentionUsers = ChatMentionUserTracker(monotonicTimeMillis = { nowMs })
    private val page = RecordingPage()

    private val binding = ChatChannelBinding(
        backfillState = backfillState,
        deduplicator = deduplicator,
        mentionUsers = mentionUsers,
        authenticatedUsername = { USER },
        page = page
    )

    // ---------------------------------------------------------------------------
    // What a change means. Each runs once per path.
    // ---------------------------------------------------------------------------

    @Test
    fun aJoin_asksThePageToLeaveNothingOfThePreviousChannel() =
        asksThePageToLeaveNothingOfThePreviousChannel { binding.changeTo("beta") }

    @Test
    fun aStartOnAnotherChannel_asksThePageToLeaveNothingOfThePreviousChannel() =
        asksThePageToLeaveNothingOfThePreviousChannel { binding.onStarted("beta") }

    @Test
    fun aJoin_forgetsWhichMessagesWereSeen() =
        forgetsWhichMessagesWereSeen { binding.changeTo("beta") }

    @Test
    fun aStartOnAnotherChannel_forgetsWhichMessagesWereSeen() =
        forgetsWhichMessagesWereSeen { binding.onStarted("beta") }

    @Test
    fun aJoin_suggestsOnlyTheUserForMentions() =
        suggestsOnlyTheUserForMentions { binding.changeTo("beta") }

    @Test
    fun aStartOnAnotherChannel_suggestsOnlyTheUserForMentions() =
        suggestsOnlyTheUserForMentions { binding.onStarted("beta") }

    @Test
    fun aJoin_discardsHistorySentForThePreviousChannel() =
        discardsHistorySentForThePreviousChannel { binding.changeTo("beta") }

    @Test
    fun aStartOnAnotherChannel_discardsHistorySentForThePreviousChannel() =
        discardsHistorySentForThePreviousChannel { binding.onStarted("beta") }

    // ---------------------------------------------------------------------------
    // When a start is a change.
    // ---------------------------------------------------------------------------

    @Test
    fun theFirstStart_onlyBindsTheChannel() {
        val held = holdAlpha()

        binding.onStarted("alpha")

        assertEquals(emptyList<String>(), page.calls)
        assertStillHeld(held)
    }

    @Test
    fun aStartOnTheSameChannel_changesNothing() {
        binding.onStarted("alpha")
        val held = holdAlpha()

        /* Case aside, the same channel. */
        binding.onStarted("Alpha")

        assertEquals(emptyList<String>(), page.calls)
        assertStillHeld(held)
    }

    @Test
    fun aStartAfterAJoin_isOnTheJoinedChannel() {
        binding.onStarted("alpha")
        binding.changeTo("beta")
        page.calls.clear()
        val held = holdAlpha()

        binding.onStarted("beta")

        assertEquals(emptyList<String>(), page.calls)
        assertStillHeld(held)
    }

    @Test
    fun comingBackToAChannelByStarts_showsTheRowsItsHistoryBringsBack() {
        /*
         * Alpha, then beta, then alpha again, each found by a start. The row alpha's
         * history brings back the second time was seen the first time; the timeline that
         * showed it is gone, so it has to be accepted again.
         */
        binding.onStarted("alpha")
        assertFalse(deduplicator.shouldSuppress(ALPHA_ROW_KEY))
        binding.onStarted("beta")
        nowMs += 60_000L

        binding.onStarted("alpha")

        assertFalse(deduplicator.shouldSuppress(ALPHA_ROW_KEY))
    }

    // ---------------------------------------------------------------------------
    // Shared bodies
    // ---------------------------------------------------------------------------

    private fun asksThePageToLeaveNothingOfThePreviousChannel(change: () -> Unit) {
        binding.onStarted("alpha")
        holdAlpha()

        change()

        assertEquals(
            listOf(
                "forgetPendingSends",
                "clearTimeline",
                "returnToBottom",
                "showMentionSuggestions",
                "selectEmoteCatalog beta",
                "reloadStream",
                "recordRecentChannel beta",
                "closeConnection"
            ),
            page.calls
        )
    }

    private fun forgetsWhichMessagesWereSeen(change: () -> Unit) {
        binding.onStarted("alpha")
        holdAlpha()

        change()

        assertFalse(
            "a row seen on alpha is still suppressed",
            deduplicator.shouldSuppress(ALPHA_ROW_KEY)
        )
    }

    private fun suggestsOnlyTheUserForMentions(change: () -> Unit) {
        binding.onStarted("alpha")
        holdAlpha()

        change()

        assertEquals(listOf(USER), mentionUsers.activeDisplayNames(USER))
    }

    private fun discardsHistorySentForThePreviousChannel(change: () -> Unit) {
        binding.onStarted("alpha")
        val held = holdAlpha()

        change()

        val outcome = backfillState.onResult(BackendHistoryResult.Success(emptyList()), held.opening, nowMs)
        assertTrue("alpha's history was applied: $outcome", outcome is Discarded)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private companion object {
        /** 2026-09-21T20:26:40Z. */
        const val T0 = 1_790_000_000_000L

        const val USER = "Me"

        const val ALPHA_ROW_KEY = "id:alpha-row-1"
    }

    /** What the page holds on alpha: a row seen, a chatter, and a history request in flight. */
    private class Held(val opening: HistoryBackfillSend)

    private fun holdAlpha(): Held {
        assertFalse(deduplicator.shouldSuppress(ALPHA_ROW_KEY))
        mentionUsers.record("viewer", authenticatedUsername = USER)
        val opening = checkNotNull(
            backfillState.decide(nowMs, ircClientPresent = false, policy = HistoryBackfillPolicy::onConnect).send
        )
        /* Past the deduplicator's 1.5 s window: only its memory of the key can suppress it now. */
        nowMs += 2_000L
        return Held(opening)
    }

    private fun assertStillHeld(held: Held) {
        assertTrue("the row seen is no longer suppressed", deduplicator.shouldSuppress(ALPHA_ROW_KEY))
        assertTrue("the chatter is no longer suggested", "viewer" in mentionUsers.activeDisplayNames(USER))
        val outcome = backfillState.onResult(BackendHistoryResult.Success(emptyList()), held.opening, nowMs)
        assertEquals(Applied(failure = null), outcome)
    }

    /** Records what the binding asks of the page, in order. */
    private class RecordingPage : ChannelBoundPage {
        val calls = mutableListOf<String>()

        override fun forgetPendingSends() { calls += "forgetPendingSends" }
        override fun clearTimeline() { calls += "clearTimeline" }
        override fun returnToBottom() { calls += "returnToBottom" }
        override fun showMentionSuggestions() { calls += "showMentionSuggestions" }
        override fun selectEmoteCatalog(channel: String) { calls += "selectEmoteCatalog $channel" }
        override fun reloadStream() { calls += "reloadStream" }
        override fun recordRecentChannel(channel: String) { calls += "recordRecentChannel $channel" }
        override fun closeConnection() { calls += "closeConnection" }
    }
}
