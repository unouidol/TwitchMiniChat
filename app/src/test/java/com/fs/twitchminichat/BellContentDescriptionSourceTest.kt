package com.fs.twitchminichat

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bell button's look and accessibility description are written in one place, read from
 * the sources.
 *
 * After a restart the chat used to describe the bell as "Alerts ON for this account", a
 * hard-coded string written by the start-up path (#150), and drew its icon and tint from the
 * legacy push setting, so the bell looked on with every category off. Only the bell menu's OK
 * wrote the category names and the alpha. These tests hold both paths to the one writer, so
 * the bell cannot look or read differently after a restart than after OK.
 */
class BellContentDescriptionSourceTest {

    private val chatFragment: String by lazy {
        val path = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/java/com/fs/twitchminichat/ChatFragment.kt")
        String(Files.readAllBytes(path), Charsets.UTF_8).replace("\r\n", "\n")
    }

    private val writerSignature = "private fun updateBellUi(profileId: String) {"

    @Test
    fun theHardCodedBellDescriptionsAreGone() {
        assertFalse(chatFragment.contains("Alerts ON for this account"))
        assertFalse(chatFragment.contains("Alerts OFF for this account"))
    }

    @Test
    fun theLegacyPushSettingNoLongerDrawsTheBell() {
        assertFalse(chatFragment.contains("PushSettingsStore"))
    }

    @Test
    fun theDescriptionIsWrittenOnlyByTheSharedWriter() {
        assertEquals(
            1,
            Regex("""btnTogglePush\.contentDescription\s*=""").findAll(chatFragment).count()
        )
        val writer = body(writerSignature)
        assertTrue(writer.contains("btnTogglePush.contentDescription = PcgAlertBellDescription.labelResIds(selection)"))
        assertTrue(writer.contains(".joinToString(PcgAlertBellDescription.SEPARATOR) { getString(it) }"))
    }

    @Test
    fun theIconTintAndAlphaAreWrittenOnlyByTheSharedWriterFromTheSameSelection() {
        assertEquals(1, Regex("""btnTogglePush\.setImageResource\(""").findAll(chatFragment).count())
        assertEquals(1, Regex("""btnTogglePush\.setColorFilter\(""").findAll(chatFragment).count())
        assertEquals(1, Regex("""btnTogglePush\.alpha\s*=""").findAll(chatFragment).count())

        val writer = body(writerSignature)
        assertTrue(writer.contains("val appearance = PcgAlertBellDescription.appearance(selection)"))
        assertTrue(writer.contains("btnTogglePush.setImageResource(appearance.iconRes)"))
        assertTrue(writer.contains("btnTogglePush.setColorFilter(appearance.tint)"))
        assertTrue(writer.contains("btnTogglePush.alpha = appearance.alpha"))
    }

    @Test
    fun theWriterReadsTheSelectionTheMenusOkShows() {
        val writer = body(writerSignature)
        assertEquals(1, Regex("""readProfileAlertSelection\(profileId\)""").findAll(writer).count())
        assertFalse(writer.contains("readConfirmedProfileAlertSelection"))
    }

    @Test
    fun theStartUpPathAndTheMenusOkBothUseTheWriter() {
        assertTrue(
            body("private fun refreshPushToggleUi() {")
                .contains("updateBellUi(currentProfileId())")
        )
        assertTrue(
            body("private fun updateSpawnAlertBellUi(profileId: String) {")
                .contains("updateBellUi(profileId)")
        )
    }

    /** The body of the member function declared by [signature], up to its closing brace. */
    private fun body(signature: String): String {
        assertTrue("missing: $signature", chatFragment.contains(signature))
        return chatFragment.substringAfter(signature).substringBefore("\n    }\n")
    }
}
