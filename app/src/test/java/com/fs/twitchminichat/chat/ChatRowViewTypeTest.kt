package com.fs.twitchminichat.chat

import com.fs.twitchminichat.ChatTimelinePosition
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [ChatRowViewType] gives each row kind its own view type, and every view type it lists belongs
 * to exactly one kind: what keeps a RecyclerView from offering one kind's view to another.
 *
 * Exhaustive by construction: [ChatRowViewType.of] is a `when` over the sealed row type, so a new
 * kind does not compile until it has a type. This test is what makes the mapping one-to-one rather
 * than merely total, and keeps [ChatRowViewType.all] equal to what [ChatRowViewType.of] returns.
 */
class ChatRowViewTypeTest {

    private val position = ChatTimelinePosition(timestampMillis = 1_000L, sequence = 0L)

    /* One row of every kind, as the sealed interface has them. */
    private val oneOfEachKind: List<ChatTimelineRow> = listOf(
        ChatMessageRow(position, "alice", "alice", null, "hello", 1.0, null, null),
        SystemLineRow(position, "Connected"),
        PendingEchoRow(position, "local-1", "me", "hi", null, null, 1.0, PendingEchoStatus.SENDING)
    )

    @Test
    fun eachKind_hasItsOwnViewType() {
        val types = oneOfEachKind.map(ChatRowViewType::of)

        assertEquals("one type per kind, none shared", oneOfEachKind.size, types.toSet().size)
    }

    @Test
    fun theListedTypes_areExactlyTheTypesTheKindsGet() {
        assertEquals(oneOfEachKind.map(ChatRowViewType::of).toSet(), ChatRowViewType.all.toSet())
        assertEquals("no type listed twice", ChatRowViewType.all.size, ChatRowViewType.all.toSet().size)
    }

    @Test
    fun aRowsType_doesNotDependOnItsData() {
        val echo = oneOfEachKind[2] as PendingEchoRow
        for (status in PendingEchoStatus.values()) {
            assertEquals("echo at $status", ChatRowViewType.PENDING_ECHO, ChatRowViewType.of(echo.copy(status = status)))
        }
        val message = oneOfEachKind[0] as ChatMessageRow
        assertEquals(ChatRowViewType.MESSAGE, ChatRowViewType.of(message.copy(emotesRaw = "25:0-4", messageId = "id")))
    }
}
