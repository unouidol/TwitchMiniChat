package com.fs.twitchminichat

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the fixed catch-rate text and the special basic-catch subtitle composition. */
class QuickCatchRatePresentationSourceTest {

    @Test
    fun fixedCatchRatesContainOnePercentSymbol() {
        val strings = readAppFile("src/main/res/values/strings.xml")

        assertTrue(strings.contains(">30% catch rate</string>"))
        assertTrue(strings.contains(">55% catch rate</string>"))
        assertTrue(strings.contains(">80% catch rate</string>"))
        assertFalse(strings.contains("%% catch rate"))
    }

    @Test
    fun autoCatchKeepsBallStatusAndCatchRate() {
        val helper = readProductionSource("BasicCatchPresetDisplayHelper.kt")
        val builder = readProductionSource("QuickCatchMenuBuilder.kt")

        assertTrue(
            helper.contains("quick_catch_basic_using_ball_with_rate")
        )
        assertTrue(
            builder.contains("catchRateSubtitle = fallbackSubtitle")
        )
    }

    private fun readProductionSource(fileName: String): String {
        return readAppFile("src/main/java/com/fs/twitchminichat/$fileName")
    }

    private fun readAppFile(relativePath: String): String {
        val path = findAppModuleDirectory().resolve(relativePath)
        return String(Files.readAllBytes(path), Charsets.UTF_8)
    }

    private fun findAppModuleDirectory(): Path {
        val start = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath()
            .normalize()

        return generateSequence(start) { current -> current.parent }
            .take(MAX_PARENT_SEARCH_DEPTH)
            .flatMap { current -> sequenceOf(current.resolve("app"), current) }
            .firstOrNull { candidate ->
                Files.isDirectory(candidate.resolve("src/main/java"))
            }
            ?: error("Unable to locate app/src/main/java from $start")
    }

    private companion object {
        const val MAX_PARENT_SEARCH_DEPTH = 6
    }
}
