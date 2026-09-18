package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins [BuildIdentity.displaySha], the only path from the build's commit to the
 * version label and the exported journal.
 *
 * Every assertion names the wrong implementation it kills. An input a
 * passthrough would return unchanged proves nothing on its own, so each such
 * case is paired with one a passthrough gets wrong.
 */
class BuildIdentityTest {

    @Test
    fun blank_isUnknown() {
        // Kills a passthrough, which would show an empty "(build 8, )".
        assertEquals("unknown", BuildIdentity.displaySha(""))
        // Kills an implementation that checks isEmpty() instead of trimming or
        // matching, which would put whitespace on the label.
        assertEquals("unknown", BuildIdentity.displaySha("   \n"))
    }

    @Test
    fun unknown_staysTheOneSentinel() {
        // A passthrough satisfies this line alone. It is kept because it pins
        // the sentinel's spelling, which the label and the export both show.
        assertEquals("unknown", BuildIdentity.displaySha("unknown"))
        // Kills the passthrough: a differently cased sentinel is not a sha and
        // must come out as the one spelling, not as "UNKNOWN".
        assertEquals("unknown", BuildIdentity.displaySha("UNKNOWN"))
    }

    @Test
    fun fullSha_isTruncatedToSeven() {
        // Kills a passthrough and an implementation that validates the shape
        // but never truncates, which would print all forty characters.
        assertEquals(
            "0123456",
            BuildIdentity.displaySha("0123456789abcdef0123456789abcdef01234567")
        )
    }

    @Test
    fun alreadyShortSha_isKeptAndTrimmed() {
        // Kills a passthrough and an implementation that matches without
        // trimming: git's output ends in a newline, and a stray one reaching
        // the label would split it over two lines.
        assertEquals("abc1234", BuildIdentity.displaySha("abc1234\n"))
        // Kills an implementation that only accepts exactly seven characters:
        // git lengthens a short sha when seven are ambiguous.
        assertEquals("abc1234", BuildIdentity.displaySha("abc12345"))
    }

    @Test
    fun dirtyMarker_survivesNormalisation() {
        // A passthrough satisfies this line alone. It kills an implementation
        // that rejects the marker and returns "unknown", or strips it and
        // reports a dirty build as clean.
        assertEquals("abc1234+", BuildIdentity.displaySha("abc1234+"))
        // Kills the passthrough, and an implementation that truncates the whole
        // string to seven characters and loses the marker with the tail.
        assertEquals(
            "0123456+",
            BuildIdentity.displaySha("0123456789abcdef0123456789abcdef01234567+")
        )
    }

    @Test
    fun uppercase_isLowercased() {
        // Kills a passthrough and an implementation that validates and
        // truncates but never lowercases: the same commit would then be
        // written two ways across builds, and would not match git's own output.
        assertEquals("abc1234", BuildIdentity.displaySha("ABC1234"))
        assertEquals("abc1234+", BuildIdentity.displaySha("ABC1234+"))
    }

    @Test
    fun garbage_isUnknown() {
        // Kills a passthrough, which would put free text on the label.
        assertEquals("unknown", BuildIdentity.displaySha("not a sha"))
        // Kills a character class such as [a-z0-9], which accepts letters
        // that are not hex.
        assertEquals("unknown", BuildIdentity.displaySha("abcdefg"))
        // Kills a pattern with no lower bound on length.
        assertEquals("unknown", BuildIdentity.displaySha("abc12"))
        // Kills a search that is not anchored at both ends, which would find
        // a sha inside a longer string - an error message from git included.
        assertEquals("unknown", BuildIdentity.displaySha("fatal: abc1234"))
        assertEquals("unknown", BuildIdentity.displaySha("abc1234++"))
    }
}
