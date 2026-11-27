package com.fs.twitchminichat

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bell button's accessibility description is written in one place, read from the sources.
 *
 * After a restart the chat used to describe the bell as "Alerts ON for this account", a
 * hard-coded string written by the start-up path, even when the active category was Most
 * Wanted or Missing Alt Dex form alerts; only the bell menu's OK wrote the category names.
 * These tests hold both paths to the one writer, so the two descriptions cannot differ again.
 */
class BellContentDescriptionSourceTest {

    private val chatFragment: String by lazy {
        val path = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/java/com/fs/twitchminichat/ChatFragment.kt")
        String(Files.readAllBytes(path), Charsets.UTF_8).replace("\r\n", "\n")
    }

    @Test
    fun theHardCodedBellDescriptionsAreGone() {
        assertFalse(chatFragment.contains("Alerts ON for this account"))
        assertFalse(chatFragment.contains("Alerts OFF for this account"))
    }

    @Test
    fun theDescriptionIsWrittenOnlyByTheSharedWriter() {
        assertEquals(
            1,
            Regex("""btnTogglePush\.contentDescription\s*=""").findAll(chatFragment).count()
        )
        val writer = body("private fun updateBellContentDescription(profileId: String) {")
        assertTrue(writer.contains("btnTogglePush.contentDescription = PcgAlertBellDescription.labelResIds(selection)"))
        assertTrue(writer.contains(".joinToString(PcgAlertBellDescription.SEPARATOR) { getString(it) }"))
    }

    @Test
    fun theWriterReadsTheSelectionTheMenusOkShows() {
        val writer = body("private fun updateBellContentDescription(profileId: String) {")
        assertTrue(writer.contains("readProfileAlertSelection(profileId)"))
        assertFalse(writer.contains("readConfirmedProfileAlertSelection"))
    }

    @Test
    fun theStartUpPathAndTheMenusOkBothUseTheWriter() {
        assertTrue(
            body("private fun refreshPushToggleUi() {")
                .contains("updateBellContentDescription(profileId)")
        )
        assertTrue(
            body("private fun updateSpawnAlertBellUi(profileId: String) {")
                .contains("updateBellContentDescription(profileId)")
        )
    }

    /** The body of the member function declared by [signature], up to its closing brace. */
    private fun body(signature: String): String {
        assertTrue("missing: $signature", chatFragment.contains(signature))
        return chatFragment.substringAfter(signature).substringBefore("\n    }\n")
    }
}
