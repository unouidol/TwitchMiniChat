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
 * - the alt-dex path stores on this phone and reaches neither chat nor the server.
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

    @Test
    fun theAltDexPathReachesNeitherChatNorTheServer() {
        val altDexPath = listOf(
            "fun requestManualAltDexUpdate(",
            "private fun scheduleManualAltDexTimeout(",
            "private fun completeOrRejectPendingManualAltDexUpdate(",
            "private fun saveManualAltDexReading(",
            "private fun handleDexPageRead("
        ).joinToString("\n") { functionBody(it) }

        listOf(
            "upload",
            "Uploader",
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
