package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Drives [ChatTimelineViewSync] through hiding and un-hiding users over a list standing in
 * for the layout, and checks that the views shown are the derivation of the rows, that a
 * view is hidden and shown again in place - never removed, never rebuilt - and that a row
 * never leaves the timeline for being hidden.
 *
 * The last tests replay random operations against a transcription of ChatFragment's
 * hidden-user code at 692a7dc, and pin the one case where the two differ.
 */
class ChatTimelineHiddenUsersTest {

    private val views = LayoutViews()
    private val timeline = ChatTimeline(currentTimeMillis = { 1_000_000L })
    private val sync = ChatTimelineViewSync(views, timeline)

    // ---------------------------------------------------------------------------
    // Hiding and un-hiding
    // ---------------------------------------------------------------------------

    @Test
    fun hidingAUser_hidesTheViewsOfItsMessages_wherever_theyAre() {
        message("b2", "bob", 4.0)
        message("a1", "alice", 1.0)
        message("b1", "bob", 2.0)
        message("a2", "alice", 3.0)

        assertTrue(sync.hideUser("bob"))

        assertEquals(listOf("a1", "a2"), views.visible())
        assertEquals(setOf("b1", "b2"), views.hidden)
    }

    @Test
    fun hidingAUser_keepsItsRowsInTheTimeline_andItsViewsInTheLayout() {
        message("a1", "alice", 1.0)
        message("b1", "bob", 2.0)
        val addsBefore = views.adds

        sync.hideUser("bob")

        assertEquals(2, timeline.rows.size)
        assertEquals(listOf("a1", "b1"), views.views)
        assertEquals(addsBefore, views.adds)
        assertEquals(0, views.removes)
        assertEquals(listOf(timeline.rows[0]), sync.shownRows)
    }

    @Test
    fun unHiding_showsTheSameViewsAgain_inTheirPlaces_withoutRebuildingThem() {
        message("a1", "alice", 1.0)
        message("b1", "bob", 2.0)
        message("a2", "alice", 3.0)
        val addsBefore = views.adds
        sync.hideUser("bob")

        assertTrue(sync.setHiddenUsers(emptySet()))

        assertEquals(listOf("a1", "b1", "a2"), views.visible())
        assertEquals(addsBefore, views.adds)
        assertEquals(0, views.removes)
        assertEquals(listOf("b1" to false, "b1" to true), views.setShownCalls)
    }

    @Test
    fun hidingAUserWithNoRows_changesNoView_andSaysSo() {
        message("a1", "alice", 1.0)

        assertFalse(sync.hideUser("bob"))
        assertEquals(emptyList<Pair<String, Boolean>>(), views.setShownCalls)
    }

    @Test
    fun hidingAUserAlreadyHidden_changesNothing_andSaysSo() {
        message("b1", "bob", 1.0)
        sync.hideUser("bob")

        assertFalse(sync.hideUser("bob"))
        assertEquals(listOf("b1" to false), views.setShownCalls)
    }

    @Test
    fun hidingASecondUser_keepsTheFirstHidden() {
        message("a1", "alice", 1.0)
        message("b1", "bob", 2.0)
        message("c1", "carol", 3.0)

        sync.hideUser("alice")
        sync.hideUser("bob")

        assertEquals(listOf("c1"), views.visible())
    }

    @Test
    fun settingTheHiddenUsers_replacesThem_andShowsWhoIsNoLongerHidden() {
        message("a1", "alice", 1.0)
        message("b1", "bob", 2.0)
        sync.hideUser("alice")
        sync.hideUser("bob")

        assertTrue(sync.setHiddenUsers(setOf("bob")))

        assertEquals(listOf("a1"), views.visible())
    }

    @Test
    fun settingHiddenUsersWithNoRows_changesNoView_andSaysSo() {
        message("a1", "alice", 1.0)

        assertFalse(sync.setHiddenUsers(setOf("zed")))
        assertEquals(emptyList<Pair<String, Boolean>>(), views.setShownCalls)
    }

    // ---------------------------------------------------------------------------
    // Rows that arrive
    // ---------------------------------------------------------------------------

    @Test
    fun aUserHiddenBeforeAnyRowArrives_hasTheirRowsAddedHidden() {
        sync.setHiddenUsers(setOf("bob"))

        message("b1", "bob", 1.0)
        message("a1", "alice", 2.0)

        assertEquals(listOf("b1", "a1"), views.views)
        assertEquals(listOf("a1"), views.visible())
    }

    @Test
    fun aRowArrivingFromAUserAlreadyHidden_isAddedHidden_andNoOtherViewIsTouched() {
        message("a1", "alice", 1.0)
        message("b1", "bob", 3.0)
        sync.hideUser("bob")

        message("b2", "bob", 2.0)

        assertEquals(listOf("a1", "b2", "b1"), views.views)
        assertEquals(listOf("a1"), views.visible())
        assertEquals(listOf("b1" to false, "b2" to false), views.setShownCalls)
    }

    // ---------------------------------------------------------------------------
    // Rows that are not messages
    // ---------------------------------------------------------------------------

    @Test
    fun systemLinesAndTheEcho_areNeverHidden_byAnyUserFilter() {
        systemLine("switched", "bob", 1.0)
        echo("echo", 2.0)
        message("b1", "bob", 3.0)

        sync.setHiddenUsers(setOf("bob", "me", "switched", "echo"))
        sync.hideUser("bob")

        assertEquals(listOf("switched", "echo"), views.visible())
        assertEquals(listOf("b1" to false), views.setShownCalls)
    }

    // ---------------------------------------------------------------------------
    // The lookup from a row to its view
    // ---------------------------------------------------------------------------

    @Test
    fun theRightViewIsHidden_afterRowsBeforeItAreRemoved() {
        message("a1", "alice", 1.0)
        message("b1", "bob", 2.0)
        message("a2", "alice", 3.0)
        message("b2", "bob", 4.0)
        sync.remove("a1")

        sync.hideUser("bob")
        assertEquals(listOf("a2"), views.visible())

        sync.setHiddenUsers(emptySet())
        assertEquals(listOf("b1", "a2", "b2"), views.visible())
    }

    @Test
    fun theRightViewIsHidden_whenHistoryArrivedAfterLiveRows() {
        message("live-alice", "alice", 10.0)
        message("live-bob", "bob", 11.0)
        message("history-bob", "bob", 5.0)
        message("history-alice", "alice", 6.0)

        sync.hideUser("alice")

        assertEquals(listOf("history-bob", "live-bob"), views.visible())
    }

    @Test
    fun clear_keepsTheHiddenUsers_soTheirNextRowsArriveHidden() {
        message("b1", "bob", 1.0)
        sync.hideUser("bob")

        sync.clear()
        message("b2", "bob", 2.0)
        message("a1", "alice", 3.0)

        assertEquals(listOf("a1"), views.visible())
    }

    @Test
    fun shownRows_isTheDerivationOverEveryRow() {
        message("a1", "alice", 1.0)
        message("b1", "bob", 2.0)
        systemLine("s", "bob", 3.0)
        sync.hideUser("bob")

        assertEquals(3, timeline.rows.size)
        assertEquals(ChatTimelineVisibility.shownRows(timeline.rows, setOf("bob")), sync.shownRows)
    }

    // ---------------------------------------------------------------------------
    // Against ChatFragment at 692a7dc
    // ---------------------------------------------------------------------------

    @Test
    fun randomHidingAndUnHiding_showsTheSameViews_asChatFragmentAt692a7dc() {
        val users = listOf("alice", "Bob", "bob", " carol ", "dave")

        for (seed in 1..200) {
            val random = Random(seed)
            val store = mutableSetOf<String>()
            val old = Fragment692a7dc()
            val views = LayoutViews()
            val sync = ChatTimelineViewSync(views, ChatTimeline(currentTimeMillis = { 1_000_000L }))
            var nextName = 0

            repeat(300) { step ->
                val where = "seed $seed, step $step"
                val user = users[random.nextInt(users.size)]
                val normalized = user.trim().lowercase()

                when (random.nextInt(12)) {
                    in 0..4 -> {
                        /* appendChatLine drops a message from a user the store hides before any view exists. */
                        if (normalized !in store) {
                            val name = "v${nextName++}"
                            val timestampSec = random.nextInt(1, 20).toDouble()
                            old.insert(name, timestampSec) { position -> messageRow(position, user) }
                            sync.insert(name, timestampSec, null) { position -> messageRow(position, user) }
                        }
                    }
                    5 -> {
                        val name = "v${nextName++}"
                        old.insert(name, 7.0) { position -> SystemLineRow(position, user) }
                        sync.insert(name, 7.0, null) { position -> SystemLineRow(position, user) }
                    }
                    6 -> {
                        val name = "v${nextName++}"
                        old.insert(name, 8.0) { position -> echoRow(position) }
                        sync.insert(name, 8.0, null) { position -> echoRow(position) }
                    }
                    7 -> if (old.views.isNotEmpty()) {
                        val name = old.views[random.nextInt(old.views.size)]
                        old.remove(name)
                        sync.remove(name)
                    }
                    in 8..9 -> {
                        /* onHideUserRequested: HiddenUsersStore.add, then removeMessagesOfHiddenUser. */
                        if (normalized.isNotBlank()) {
                            store += normalized
                            assertEquals(where, old.removeMessagesOfHiddenUser(normalized), sync.hideUser(normalized))
                        }
                    }
                    10 -> {
                        /*
                         * Un-hiding happens in BlockedUsersActivity, which stops this page;
                         * onResume refreshes before any row can arrive.
                         */
                        store -= normalized
                        assertEquals(where, old.refreshHiddenUserVisibilityInChat(store), sync.setHiddenUsers(store))
                    }
                    else -> if (random.nextInt(6) == 0) {
                        old.clear()
                        sync.clear()
                    }
                }

                assertEquals(where, old.views, views.views)
                assertEquals(where, old.visible(), views.visible())
            }
        }
    }

    @Test
    fun aRowFromAUserUnHiddenSinceTheLastRefresh_isAddedHidden_likeThatUsersOtherRows_untilTheNextRefresh() {
        message("b1", "bob", 1.0)
        sync.hideUser("bob")

        /*
         * The store no longer hides bob, but this page has not refreshed since. At 692a7dc
         * the view walk would have added b2 shown while b1 stayed hidden; here both stay
         * hidden, as the page's hidden users say, until the refresh shows them together.
         */
        message("b2", "bob", 2.0)
        assertEquals(emptyList<String>(), views.visible())

        sync.setHiddenUsers(emptySet())
        assertEquals(listOf("b1", "b2"), views.visible())
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun message(view: String, user: String, timestampSec: Double) {
        sync.insert(view, timestampSec, null) { position -> messageRow(position, user) }
    }

    private fun systemLine(view: String, text: String, timestampSec: Double) {
        sync.insert(view, timestampSec, null) { position -> SystemLineRow(position, text) }
    }

    private fun echo(view: String, timestampSec: Double) {
        sync.insert(view, timestampSec, null) { position -> echoRow(position) }
    }

    private fun echoRow(position: ChatTimelinePosition) = PendingEchoRow(
        position = position,
        localId = "local-${position.sequence}",
        user = "me",
        messageText = "text",
        emotesRaw = null,
        replyParentUserLogin = null,
        sentAtSec = position.timestampMillis / 1000.0,
        status = PendingEchoStatus.SENDING
    )

    private fun messageRow(position: ChatTimelinePosition, user: String): ChatMessageRow {
        return ChatMessageRow(
            position = position,
            user = user,
            usernameLower = user.trim().lowercase(),
            messageId = null,
            messageText = "text",
            messageTimestampSec = position.timestampMillis / 1000.0,
            emotesRaw = null,
            replyParentUserLogin = null
        )
    }

    /** Stands in for the layout: the views in order, which are hidden, and every operation on them. */
    private class LayoutViews : ChatTimelineViews<String> {
        val views = mutableListOf<String>()
        val hidden = mutableSetOf<String>()
        val setShownCalls = mutableListOf<Pair<String, Boolean>>()
        var adds = 0
        var removes = 0

        fun visible(): List<String> = views.filter { view -> view !in hidden }

        override fun indexOf(view: String): Int = views.indexOf(view)

        override fun add(view: String, row: ChatTimelineRow, index: Int) {
            views.add(index, view)
            adds++
        }

        override fun removeAt(index: Int) {
            hidden -= views.removeAt(index)
            removes++
        }

        override fun removeAll() {
            views.clear()
            hidden.clear()
            removes++
        }

        override fun setShown(index: Int, shown: Boolean) {
            val view = views[index]
            setShownCalls += view to shown
            if (shown) hidden -= view else hidden += view
        }

        /* No echo changes status in these tests; ChatTimelineEchoTest covers this. */
        override fun rebind(index: Int, row: ChatTimelineRow, change: ChatTimelineChange) {
            error("no view should be rebound here: index $index, change $change")
        }
    }

    /**
     * ChatFragment's hidden-user code at 692a7dc, with chatContainer replaced by a list.
     * The order of the views comes from a [ChatTimeline], as it did through
     * ChatTimelineController; the visibility of each is what the fragment set on it.
     */
    private class Fragment692a7dc {
        private val timeline = ChatTimeline(currentTimeMillis = { 1_000_000L })
        val views = mutableListOf<String>()
        private val gone = mutableSetOf<String>()

        fun visible(): List<String> = views.filter { view -> view !in gone }

        fun insert(view: String, timestampSec: Double, create: (ChatTimelinePosition) -> ChatTimelineRow) {
            views.add(timeline.insert(timestampSec, null, create), view)
        }

        fun remove(view: String) {
            val index = views.indexOf(view)
            timeline.removeAt(index)
            views.removeAt(index)
            gone -= view
        }

        fun clear() {
            timeline.clear()
            views.clear()
            gone.clear()
        }

        /* The loop of removeMessagesOfHiddenUser; the caller has already returned on a blank name. */
        fun removeMessagesOfHiddenUser(normalized: String): Boolean {
            var changedAnyView = false
            for (i in views.size - 1 downTo 0) {
                val meta = timeline.rows[i] as? ChatMessageRow ?: continue
                if (meta.usernameLower == normalized && views[i] !in gone) {
                    gone += views[i]
                    changedAnyView = true
                }
            }
            return changedAnyView
        }

        /* The loop of refreshHiddenUserVisibilityInChat, with HiddenUsersStore.isHidden over [store]. */
        fun refreshHiddenUserVisibilityInChat(store: Set<String>): Boolean {
            var changedAnyView = false
            for (i in 0 until views.size) {
                val meta = timeline.rows[i] as? ChatMessageRow ?: continue
                val normalized = meta.usernameLower.trim().lowercase()
                val shouldBeHidden = normalized.isNotBlank() && normalized in store
                val isGone = views[i] in gone
                if (shouldBeHidden != isGone) {
                    if (shouldBeHidden) gone += views[i] else gone -= views[i]
                    changedAnyView = true
                }
            }
            return changedAnyView
        }
    }
}
