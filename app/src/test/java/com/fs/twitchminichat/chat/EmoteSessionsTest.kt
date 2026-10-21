package com.fs.twitchminichat.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Drives [EmoteSessions] as the loader drives it - a bind releases the view's session and may start
 * another, an unbind releases, the page's end releases everything - and asserts the one thing it
 * owes: every session is released exactly once, by the operation that ends it, and a session still
 * held has never been released.
 */
class EmoteSessionsTest {

    /* A session, by its number; each is made once and never reused. */
    private data class Session(val number: Int)

    /* Who released each session, as the index of the operation that did. */
    private val releasedBy = HashMap<Session, MutableList<Int>>()
    private var operation = 0

    private val sessions = EmoteSessions<String, Session>(release = { session ->
        releasedBy.getOrPut(session) { mutableListOf() } += operation
    })

    @Test
    fun aBindReleasesTheSessionOfTheRowTheViewShowedBefore() {
        val first = Session(1)
        bind("view", first)
        val second = Session(2)
        bind("view", second)

        assertEquals(mapOf(first to listOf(2)), releasedBy)
        assertEquals(1, sessions.size)
    }

    @Test
    fun aBindWithNoEmotesReleasesAndHoldsNothing() {
        val first = Session(1)
        bind("view", first)
        bind("view", null)

        assertEquals(mapOf(first to listOf(2)), releasedBy)
        assertEquals(0, sessions.size)
    }

    @Test
    fun releasingAViewThatHoldsNoSessionReleasesNothing() {
        sessions.release("never bound")
        bind("other", null)
        sessions.release("other")

        assertEquals(emptyMap<Session, List<Int>>(), releasedBy)
    }

    @Test
    fun releaseAllEndsEverySessionOnce_andLeavesNothingToReleaseAgain() {
        bind("a", Session(1))
        bind("b", Session(2))
        operation = 7
        sessions.releaseAll()
        operation = 8
        sessions.releaseAll()
        sessions.release("a")

        assertEquals(mapOf(Session(1) to listOf(7), Session(2) to listOf(7)), releasedBy)
        assertEquals(0, sessions.size)
    }

    @Test
    fun sessionsOfOtherViewsAreLeftAlone() {
        val kept = Session(1)
        bind("kept", kept)
        bind("other", Session(2))
        bind("other", Session(3))
        sessions.release("other")

        assertTrue(kept !in releasedBy)
        assertEquals(1, sessions.size)
    }

    @Test
    fun randomSequences_releaseEverySessionExactlyOnce_byTheOperationThatEndsIt() {
        repeat(200) { seed ->
            val random = Random(seed)
            val views = listOf("v0", "v1", "v2", "v3")
            val held = HashMap<String, Session>()
            val expected = HashMap<Session, Int>()
            val made = mutableListOf<Session>()
            releasedBy.clear()
            sessions.releaseAll()
            releasedBy.clear()

            repeat(300) { step ->
                operation = step
                val view = views[random.nextInt(views.size)]
                when (random.nextInt(10)) {
                    in 0..5 -> {
                        held.remove(view)?.let { previous -> expected[previous] = step }
                        val next = if (random.nextBoolean()) Session(made.size).also { made += it } else null
                        bind(view, next, at = step)
                        if (next != null) held[view] = next
                    }
                    in 6..8 -> {
                        held.remove(view)?.let { previous -> expected[previous] = step }
                        sessions.release(view)
                    }
                    else -> {
                        held.values.forEach { session -> expected[session] = step }
                        held.clear()
                        sessions.releaseAll()
                    }
                }
            }

            assertEquals("seed $seed: who released what", expected.mapValues { (_, at) -> listOf(at) }, releasedBy)
            assertEquals("seed $seed: sessions still held", held.size, sessions.size)
            held.values.forEach { session -> assertTrue("seed $seed: $session is held and was released", session !in releasedBy) }
            assertEquals("seed $seed: every session made was either released or is held", made.size, expected.size + held.size)
        }
    }

    /* What the loader's bind does: release the view's session, then hold [next] if the row has emotes. */
    private fun bind(view: String, next: Session?, at: Int = operation + 1) {
        operation = at
        sessions.release(view)
        if (next != null) sessions.put(view, next)
    }
}
