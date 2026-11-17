package com.fs.twitchminichat.welcome

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins how GitHub's answer becomes a tag, and every way it fails silently instead.
 */
class LatestReleaseResponseTest {

    /**
     * The fields the check reads, from the real answer of 2026-11-12 for this repository,
     * with the release notes, the author and the asset details trimmed out.
     */
    private val realAnswer = """
        {
          "html_url": "https://github.com/unouidol/TwitchMiniChat/releases/tag/v5.5.1",
          "tag_name": "v5.5.1",
          "name": "Twitch Mini Chat 5.5.1",
          "draft": false,
          "prerelease": false,
          "published_at": "2026-09-25T06:10:44Z",
          "assets": []
        }
    """.trimIndent()

    @Test
    fun realAnswer_givesItsTag() {
        assertEquals("v5.5.1", LatestReleaseResponse.latestTag(200, realAnswer))
    }

    /** 403 is what the unauthenticated rate limit answers. */
    @Test
    fun anyStatusButOk_givesNothing() {
        for (status in listOf(0, 204, 301, 304, 403, 404, 429, 500, 503)) {
            assertNull("status $status", LatestReleaseResponse.latestTag(status, realAnswer))
        }
    }

    @Test
    fun missingOrUnreadableBody_givesNothing() {
        for (body in listOf(null, "", "not json", "[1, 2]", "{", "<html></html>")) {
            assertNull("body <$body>", LatestReleaseResponse.latestTag(200, body))
        }
    }

    @Test
    fun missingOrNonStringTag_givesNothing() {
        assertNull(LatestReleaseResponse.latestTag(200, """{"name": "x"}"""))
        assertNull(LatestReleaseResponse.latestTag(200, """{"tag_name": null}"""))
        assertNull(LatestReleaseResponse.latestTag(200, """{"tag_name": 561}"""))
    }

    @Test
    fun unparsableTag_givesNothing() {
        assertNull(LatestReleaseResponse.latestTag(200, """{"tag_name": "v5.6.1-rc1"}"""))
        assertNull(LatestReleaseResponse.latestTag(200, """{"tag_name": "latest"}"""))
    }

    /** The endpoint excludes both; the parser checks again. */
    @Test
    fun draftOrPreRelease_givesNothing() {
        assertNull(
            LatestReleaseResponse.latestTag(200, """{"tag_name": "v5.6.1", "draft": true}""")
        )
        assertNull(
            LatestReleaseResponse.latestTag(200, """{"tag_name": "v5.6.1", "prerelease": true}""")
        )
    }

    @Test
    fun readBounded_readsAnAnswerWithinTheLimit() {
        val bytes = realAnswer.toByteArray(Charsets.UTF_8)
        assertEquals(
            realAnswer,
            LatestReleaseResponse.readBounded(ByteArrayInputStream(bytes), maxBytes = bytes.size)
        )
    }

    @Test
    fun readBounded_refusesAnAnswerOverTheLimit() {
        val bytes = ByteArray(20_000) { 'a'.code.toByte() }
        assertNull(
            LatestReleaseResponse.readBounded(ByteArrayInputStream(bytes), maxBytes = 19_999)
        )
    }

    /** The default limit leaves room for release notes many times today's length. */
    @Test
    fun defaultLimit_isFarAboveTodaysAnswer() {
        val todaysAnswerBytes = 11_625
        assertTrue(LatestReleaseResponse.MAX_BODY_BYTES > 20 * todaysAnswerBytes)
    }
}
