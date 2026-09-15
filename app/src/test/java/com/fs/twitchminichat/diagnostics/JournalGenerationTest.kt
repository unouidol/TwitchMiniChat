package com.fs.twitchminichat.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the decision that keeps a slow caller's line out of a journal erased
 * while its work was still running.
 */
class JournalGenerationTest {

    @Test
    fun startedBeforeAnErase_lineIsDropped() {
        val generation = JournalGeneration()
        val startedUnder = generation.current()

        generation.advance()

        assertFalse(generation.accepts(startedUnder))
    }

    @Test
    fun noEraseInBetween_lineIsKept() {
        val generation = JournalGeneration()
        val startedUnder = generation.current()

        assertTrue(generation.accepts(startedUnder))
    }

    @Test
    fun startedAfterAnErase_keepsWriting() {
        val generation = JournalGeneration()
        generation.advance()
        val startedUnder = generation.current()

        assertTrue(generation.accepts(startedUnder))
    }

    @Test
    fun everyErase_retiresEveryEarlierGeneration() {
        val generation = JournalGeneration()
        val first = generation.current()
        generation.advance()
        val second = generation.current()
        generation.advance()

        assertFalse(generation.accepts(first))
        assertFalse(generation.accepts(second))
    }

    @Test
    fun noExpectedGeneration_isKeptAcrossErases() {
        val generation = JournalGeneration()
        generation.advance()

        assertTrue(generation.accepts(null))
    }
}
