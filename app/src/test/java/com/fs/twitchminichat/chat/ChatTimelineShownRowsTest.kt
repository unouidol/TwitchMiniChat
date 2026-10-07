package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Pins [ChatTimelineShownRows] on small timelines read as names: a row's position among the
 * shown rows and back, in both overloads, and the update each change of the timeline makes
 * to the list of shown rows.
 */
class ChatTimelineShownRowsTest {

    /* a1 b1 s1 a2 e1 b2 c1: alice, bob, a system line, alice, an echo, bob, carol. */
    private val rows = listOf(
        message(0, "a1", "alice"),
        message(1, "b1", "bob"),
        SystemLineRow(position(2), "s1"),
        message(3, "a2", "alice"),
        echo(4, "e1"),
        message(5, "b2", "bob"),
        message(6, "c1", "carol")
    )

    // ---------------------------------------------------------------------------
    // Translation
    // ---------------------------------------------------------------------------

    @Test
    fun withNoUserHidden_aRowsPositionIsItsIndex_andBack() {
        rows.indices.forEach { index ->
            assertEquals(index, ChatTimelineShownRows.positionOf(rows, emptySet(), index))
            assertEquals(index, ChatTimelineShownRows.timelineIndexOf(rows, emptySet(), index))
        }
    }

    @Test
    fun aRowsPosition_countsOnlyTheShownRowsBeforeIt() {
        val hidden = setOf("bob")
        assertEquals(listOf(0, null, 1, 2, 3, null, 4), rows.indices.map { ChatTimelineShownRows.positionOf(rows, hidden, it) })
    }

    @Test
    fun aPositionTranslatesBack_toTheIndexOfThatShownRow() {
        val hidden = setOf("bob", "carol")
        assertEquals(listOf(0, 2, 3, 4), (0..3).map { ChatTimelineShownRows.timelineIndexOf(rows, hidden, it) })
    }

    @Test
    fun systemLinesAndEchoes_areShown_whoeverIsHidden() {
        val hidden = setOf("alice", "bob", "carol")
        assertEquals(0, ChatTimelineShownRows.positionOf(rows, hidden, 2))
        assertEquals(1, ChatTimelineShownRows.positionOf(rows, hidden, 4))
        assertEquals(listOf(2, 4), (0..1).map { ChatTimelineShownRows.timelineIndexOf(rows, hidden, it) })
    }

    @Test
    fun theTwoDirections_areInverse_forEveryShownRow_underEverySetOfHiddenUsers() {
        val users = listOf("alice", "bob", "carol")
        for (mask in 0 until (1 shl users.size)) {
            val hidden = users.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
            rows.indices.forEach { index ->
                val position = ChatTimelineShownRows.positionOf(rows, hidden, index) ?: return@forEach
                assertEquals("hidden $hidden index $index", index, ChatTimelineShownRows.timelineIndexOf(rows, hidden, position))
            }
        }
    }

    @Test
    fun theOverloadsOverAPredicate_countTheSameWay() {
        val shown = booleanArrayOf(true, false, false, true, true, false, true)
        assertEquals(listOf(0, 1, 1, 1, 2, 3, 3), (0..6).map { ChatTimelineShownRows.positionOf(it) { i -> shown[i] } })
        assertEquals(listOf(0, 3, 4, 6), (0..3).map { ChatTimelineShownRows.timelineIndexOf(shown.size, it) { i -> shown[i] } })
    }

    @Test
    fun aPositionPastTheShownRows_isRefused_withOrWithoutHiddenUsers() {
        assertThrows(IndexOutOfBoundsException::class.java) { ChatTimelineShownRows.timelineIndexOf(rows, emptySet(), rows.size) }
        assertThrows(IndexOutOfBoundsException::class.java) { ChatTimelineShownRows.timelineIndexOf(rows, emptySet(), -1) }
        assertThrows(IndexOutOfBoundsException::class.java) { ChatTimelineShownRows.timelineIndexOf(rows, setOf("bob"), 5) }
    }

    // ---------------------------------------------------------------------------
    // Updates of one row
    // ---------------------------------------------------------------------------

    @Test
    fun aShownRow_isInsertedRemovedAndChanged_atItsPosition() {
        val hidden = setOf("bob")
        assertEquals(ShownRowsUpdate.Insert(3, rows[4]), ChatTimelineShownRows.inserted(rows, hidden, 4))
        assertEquals(ShownRowsUpdate.Remove(2), ChatTimelineShownRows.removed(rows, hidden, 3))
        assertEquals(
            ShownRowsUpdate.Change(3, rows[4], ChatTimelineChange.ECHO_STATUS),
            ChatTimelineShownRows.replaced(rows, hidden, 4, ChatTimelineChange.ECHO_STATUS)
        )
    }

    @Test
    fun aRowThatIsNotShown_changesNothingInTheList() {
        val hidden = setOf("bob")
        assertNull(ChatTimelineShownRows.inserted(rows, hidden, 1))
        assertNull(ChatTimelineShownRows.removed(rows, hidden, 5))
        assertNull(ChatTimelineShownRows.replaced(rows, hidden, 1, ChatTimelineChange.ECHO_STATUS))
    }

    @Test
    fun clearing_resetsTheListToNoRows() {
        assertEquals(ShownRowsUpdate.Reset(emptyList()), ChatTimelineShownRows.cleared())
    }

    // ---------------------------------------------------------------------------
    // A change of hidden users
    // ---------------------------------------------------------------------------

    @Test
    fun hidingAUser_removesOneRangePerRunOfItsRows() {
        assertEquals(
            listOf(ShownRowsUpdate.RemoveRange(1, 1), ShownRowsUpdate.RemoveRange(4, 1)),
            ChatTimelineShownRows.hiddenUsersChanged(rows, emptySet(), setOf("bob"))
        )
    }

    @Test
    fun unHidingAUser_insertsItsRows_whereTheyStandInTheTimeline() {
        assertEquals(
            listOf(ShownRowsUpdate.InsertRange(1, listOf(rows[1])), ShownRowsUpdate.InsertRange(5, listOf(rows[5]))),
            ChatTimelineShownRows.hiddenUsersChanged(rows, setOf("bob"), emptySet())
        )
    }

    @Test
    fun adjacentRowsOfHiddenUsers_areOneRange() {
        assertEquals(
            listOf(ShownRowsUpdate.RemoveRange(4, 2)),
            ChatTimelineShownRows.hiddenUsersChanged(rows, emptySet(), setOf("bob", "carol")).drop(1)
        )
    }

    @Test
    fun aRowHiddenBeforeAndAfter_doesNotSplitARun() {
        /* b1 stays hidden between a1 and s1; hiding alice removes a1, and a2 later. */
        val updates = ChatTimelineShownRows.hiddenUsersChanged(
            listOf(rows[0], rows[1], rows[3], rows[2]),
            setOf("bob"),
            setOf("bob", "alice")
        )
        assertEquals(listOf(ShownRowsUpdate.RemoveRange(0, 2)), updates)
    }

    @Test
    fun aRunLeavingAndARunArriving_atTheSamePlace_areARemovalThenAnInsertion() {
        /* bob hidden -> alice hidden: a1 leaves at position 0, and b1 arrives in its place. */
        val updates = ChatTimelineShownRows.hiddenUsersChanged(rows.take(2), setOf("bob"), setOf("alice"))
        assertEquals(listOf(ShownRowsUpdate.RemoveRange(0, 1), ShownRowsUpdate.InsertRange(0, listOf(rows[1]))), updates)
    }

    @Test
    fun noChangeOfWhatIsShown_isNoUpdate() {
        assertEquals(emptyList<ShownRowsUpdate>(), ChatTimelineShownRows.hiddenUsersChanged(rows, setOf("dave"), emptySet()))
        assertEquals(emptyList<ShownRowsUpdate>(), ChatTimelineShownRows.hiddenUsersChanged(rows, setOf("bob"), setOf("bob")))
        assertEquals(emptyList<ShownRowsUpdate>(), ChatTimelineShownRows.hiddenUsersChanged(emptyList(), emptySet(), setOf("bob")))
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun position(i: Int) = ChatTimelinePosition(timestampMillis = i * 1_000L, sequence = i.toLong())

    private fun message(i: Int, text: String, user: String) =
        ChatMessageRow(position(i), user, user, null, text, i.toDouble(), null, null)

    private fun echo(i: Int, localId: String) =
        PendingEchoRow(position(i), localId, "me", "text", null, null, i.toDouble(), PendingEchoStatus.SENDING)
}
