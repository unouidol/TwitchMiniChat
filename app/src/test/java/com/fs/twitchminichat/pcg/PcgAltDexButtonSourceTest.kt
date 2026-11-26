package com.fs.twitchminichat.pcg

import com.fs.twitchminichat.BundledPcgCatalogFixture
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two Pokédex buttons, and the alt-dex reading's path, read from `GeckoSessionManager`.
 *
 * The session manager needs GeckoView and a device, so these checks read its source:
 *
 * - "Update Pokédex" is disabled while the alt dex is shown, and otherwise enabled exactly
 *   as before, on the confirmed Pokédex tab;
 * - "Update Alt Dex" is enabled only on the confirmed Pokédex tab showing the alt dex;
 * - the alt-dex path stores on this phone, never reaches chat, and reaches the server only
 *   through the Missing Alt Dex form alert's upload (`docs/design-alt-dex-alert.md`).
 */
class PcgAltDexButtonSourceTest {

    private val source: String by lazy {
        val path = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/java/com/fs/twitchminichat/pcg/GeckoSessionManager.kt")
        String(Files.readAllBytes(path), Charsets.UTF_8).replace("\r\n", "\n")
    }

    @Test
    fun updatePokedexIsDisabledOnlyWhileTheAltDexIsShown() {
        val state = functionBody("private fun buildManualUpdateButtonState(")

        assertTrue(state.contains("val pokedexTabConfirmed = tabState?.isPokedexConfirmedActive == true"))
        assertTrue(
            state.contains(
                "val altDexShown = pokedexTabConfirmed &&\n" +
                    "                getFreshDexPageRead(sessionKey)?.isAltDex == true"
            )
        )
        assertTrue(state.contains("pokedexEnabled = pokedexTabConfirmed && !altDexShown,"))
        assertTrue(state.contains("altDexEnabled = altDexShown,"))
    }

    /**
     * Never chat, and the server only through [com.fs.twitchminichat.PcgAltDexAlertSync]: its
     * decision (the category on and confirmed) comes first, and only its upload branch sends.
     * Reading 1's uploader and endpoint are never reached from here.
     */
    @Test
    fun theAltDexPathReachesNoChatAndTheServerOnlyThroughTheAlertSync() {
        val altDexPath = listOf(
            "fun requestManualAltDexUpdate(",
            "private fun scheduleManualAltDexTimeout(",
            "private fun completeOrRejectPendingManualAltDexUpdate(",
            "private fun saveManualAltDexReading(",
            "private fun handleDexPageRead("
        ).joinToString("\n") { functionBody(it) }

        listOf(
            "FcmRegistrationUploader",
            "uploadDexList",
            "uploadToken",
            "PcgAltDexAlertUploader",
            "sendChat",
            "ChatSender",
            "privmsg",
            "PRIVMSG",
            "HttpURLConnection",
            "PcgPokedexSnapshotStore",
            "evaluateJavascript",
            "loadUri"
        ).forEach { forbidden ->
            assertFalse("the alt-dex path must not use $forbidden", altDexPath.contains(forbidden))
        }
        assertTrue(altDexPath.contains("PcgAltDexSnapshotStore.saveReading("))

        val saving = functionBody("private fun saveManualAltDexReading(")
        val decision = saving.indexOf("when (altDexAlerts.decideAfterReading(profileId))")
        val uploadBranch = saving.indexOf("PcgAltDexAfterReadingDecision.UPLOAD ->")
        val upload = saving.indexOf("altDexAlerts.uploadAfterReading(upload)")
        assertTrue(decision >= 0 && uploadBranch > decision && upload > uploadBranch)
        assertTrue(saving.indexOf("uploadAfterReading") == saving.lastIndexOf("uploadAfterReading"))
        assertTrue(saving.indexOf("PcgAltDexSnapshotStore.saveReading(") < decision)
    }

    /**
     * The function whose declaration starts with [declaration], up to the next member's KDoc
     * or declaration, so that the next member's comment is never read as this one's code.
     */
    private fun functionBody(declaration: String): String {
        val start = source.indexOf(declaration)
        assertTrue("missing $declaration", start >= 0)
        val next = Regex("""\n    (?:/\*\*|(?:private |internal )?fun )""")
            .find(source, start + declaration.length)
            ?.range
            ?.first
            ?: source.length
        return source.substring(start, next)
    }
}
