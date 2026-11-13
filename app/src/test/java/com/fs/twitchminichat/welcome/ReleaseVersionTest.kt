package com.fs.twitchminichat.welcome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the version comparison behind the update notice.
 *
 * Every shape it cannot read must give "not newer". The notice fails toward silence, never
 * toward a prompt for a release that is not one.
 */
class ReleaseVersionTest {

    @Test
    fun newerTag_isNewer() {
        assertTrue(ReleaseVersion.isNewer("v5.6.1", "5.6.0"))
        assertTrue(ReleaseVersion.isNewer("v5.7.0", "5.6.9"))
        assertTrue(ReleaseVersion.isNewer("v6.0.0", "5.99.99"))
    }

    /** A string comparison would put 5.10.0 before 5.9.0. */
    @Test
    fun componentsCompareAsNumbers() {
        assertTrue(ReleaseVersion.isNewer("v5.10.0", "5.9.0"))
        assertFalse(ReleaseVersion.isNewer("v5.9.0", "5.10.0"))
    }

    @Test
    fun equalTag_isNotNewer_withOrWithoutItsV() {
        assertFalse(ReleaseVersion.isNewer("v5.6.0", "5.6.0"))
        assertFalse(ReleaseVersion.isNewer("5.6.0", "5.6.0"))
    }

    @Test
    fun olderTag_isNotNewer() {
        assertFalse(ReleaseVersion.isNewer("v5.5.1", "5.6.0"))
        assertFalse(ReleaseVersion.isNewer("v4.99.99", "5.0.0"))
    }

    /** One published release, 5.2.0, was tagged without its v. */
    @Test
    fun tagWithoutV_parses() {
        assertEquals(ReleaseVersion(5, 2, 0), ReleaseVersion.parseTag("5.2.0"))
    }

    @Test
    fun malformedTags_doNotParse_andAreNeverNewer() {
        val malformed = listOf(
            null, "", "v", "latest", "v5.6", "v5", "v5.6.1.2", "V5.6.1", " v5.6.1",
            "v5.6.1 ", "v5.6.1\n", "v5.x.1", "v-5.6.1", "vv5.6.1", "v5..1",
            "v99999999999.0.0"
        )
        for (tag in malformed) {
            assertNull("tag <$tag>", ReleaseVersion.parseTag(tag))
            assertFalse("tag <$tag>", ReleaseVersion.isNewer(tag, "1.0.0"))
        }
    }

    @Test
    fun preReleaseTags_doNotParse_andAreNeverNewer() {
        for (tag in listOf("v5.6.1-rc1", "v5.6.1-beta", "v5.6.1-dev", "v6.0.0-alpha.2")) {
            assertNull("tag <$tag>", ReleaseVersion.parseTag(tag))
            assertFalse("tag <$tag>", ReleaseVersion.isNewer(tag, "5.6.0"))
        }
    }

    /** Dev's versionNameSuffix is ignored, so a dev build compares by its numbers. */
    @Test
    fun devSuffixedInstalledVersion_comparesByItsNumbers() {
        assertEquals(ReleaseVersion(5, 6, 0), ReleaseVersion.parseInstalled("5.6.0-dev"))
        assertTrue(ReleaseVersion.isNewer("v5.6.1", "5.6.0-dev"))
        assertFalse(ReleaseVersion.isNewer("v5.6.0", "5.6.0-dev"))
        assertFalse(ReleaseVersion.isNewer("v5.5.1", "5.6.0-dev"))
    }

    @Test
    fun malformedInstalledVersion_isNeverOlder() {
        for (installed in listOf(null, "", "5.6", "dev", "5.6.0-", "5.6.0 dev", "v5.6.0")) {
            assertNull("installed <$installed>", ReleaseVersion.parseInstalled(installed))
            assertFalse("installed <$installed>", ReleaseVersion.isNewer("v99.0.0", installed))
        }
    }

    @Test
    fun displaysWithoutItsV() {
        assertEquals("5.6.1", ReleaseVersion.parseTag("v5.6.1").toString())
    }
}
