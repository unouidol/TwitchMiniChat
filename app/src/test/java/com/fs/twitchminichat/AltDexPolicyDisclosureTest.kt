package com.fs.twitchminichat

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bundled disclosures of Missing Alt Dex form alerts (section *g* of
 * `docs/design-alt-dex-alert.md`): the privacy policy, the data deletion page and the
 * acceptance gate say what is sent, when, and when it is deleted.
 *
 * These are the copies users read in the gate and in Safety & Privacy. The web copies are
 * derived from them at release time, in another repository, and are not checked here.
 */
class AltDexPolicyDisclosureTest {

    @Test
    fun privacySaysWhatIsSentAndThatNothingIsWhileOff() {
        val privacy = policy("privacy.html")

        assertTrue(
            privacy.contains(
                "When you turn on Missing Alt Dex form alerts for an account: the list of forms " +
                    "missing from that account’s Alt Dex"
            )
        )
        assertTrue(
            privacy.contains(
                "Nothing about your Alt Dex is sent while these alerts are off on this phone."
            )
        )
        assertTrue(privacy.contains("whether event spawns and Missing Alt Dex form alerts are"))
    }

    /** Section 4 no longer says that Update Pokédex is the one exception. */
    @Test
    fun privacyCountsTwoExceptionsToWhatStaysOnThePhone() {
        val privacy = policy("privacy.html")

        assertTrue(privacy.contains("What TMC reads stays on your phone, with two exceptions."))
        assertFalse(privacy.contains("with one exception"))
        assertTrue(
            privacy.contains(
                "the list of forms missing from its Alt Dex is sent the same way, after each " +
                    "Update Alt Dex."
            )
        )
    }

    @Test
    fun privacySaysWhenTheListIsDeletedAndThatTheAlertsAreOffByDefault() {
        val privacy = policy("privacy.html")

        assertTrue(
            privacy.contains(
                "It is deleted when these alerts are off for that account on every device, " +
                    "and by \"Delete app account and all data\"."
            )
        )
        assertTrue(
            privacy.contains(
                "Missing Alt Dex form alerts are off unless you turn them on, for each account, " +
                    "in the PCG notifications menu."
            )
        )
    }

    /** Each of the three server-side options says what becomes of the Alt Dex list. */
    @Test
    fun theDeletionPageCoversTheListInEveryServerOption() {
        val deletion = policy("data_deletion.html")

        assertTrue(
            deletion.contains(
                "the Pokédex and Alt Dex lists and the Most Wanted watchlists uploaded for " +
                    "those accounts"
            )
        )
        assertEquals(
            2,
            Regex("""The Alt Dex list uploaded for (?:one of your accounts|that account) is deleted once no device has (?:that account’s|its) Missing Alt Dex form alerts on\.""")
                .findAll(deletion)
                .count()
        )
    }

    /** The two pages changed together carry the same "Last updated" date. */
    @Test
    fun theTwoChangedPagesCarryOneDate() {
        val date = Regex("""Last updated: (\d{4}-\d{2}-\d{2})""")
        val privacy = date.find(policy("privacy.html"))?.groupValues?.get(1)
        val deletion = date.find(policy("data_deletion.html"))?.groupValues?.get(1)

        assertEquals(privacy, deletion)
    }

    /** The gate shows the version-4 line. */
    @Test
    fun theGateShowsTheAltDexLine() {
        val module = BundledPcgCatalogFixture.findAppModuleDirectory()
        val gate = read(module.resolve("src/main/res/layout/dialog_terms_gate.xml"))
        val strings = read(module.resolve("src/main/res/values/strings.xml"))

        assertTrue(gate.contains("android:text=\"@string/terms_gate_alt_dex\""))
        assertTrue(
            strings.contains(
                "<string name=\"terms_gate_alt_dex\">If you turn on Missing Alt Dex form " +
                    "alerts, TMC sends the TwitchMiniChat server the list of forms missing from " +
                    "your Alt Dex"
            )
        )
    }

    private fun policy(name: String): String {
        return read(
            BundledPcgCatalogFixture.findAppModuleDirectory()
                .resolve("src/main/assets/policies/$name")
        )
    }

    private fun read(path: java.nio.file.Path): String {
        return String(Files.readAllBytes(path), Charsets.UTF_8)
            .replace("\r\n", "\n")
            .replace(Regex("""\s*\n\s*"""), " ")
    }
}
