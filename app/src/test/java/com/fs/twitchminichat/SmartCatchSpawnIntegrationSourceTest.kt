package com.fs.twitchminichat

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the Android entry-point ordering that keeps spawn ingestion UI-independent. */
class SmartCatchSpawnIntegrationSourceTest {

    @Test
    fun liveIrcSpawnIsIngestedBeforeFragmentUiDispatch() {
        val source = readProductionSource("ChatFragment.kt")
        val callbackStart = source.indexOf("onMessage = {")
        val callbackEnd = source.indexOf("onError =", startIndex = callbackStart)

        assertTrue("IRC onMessage callback not found", callbackStart >= 0)
        assertTrue("IRC onMessage callback end not found", callbackEnd > callbackStart)

        val callback = source.substring(callbackStart, callbackEnd)
        val ingestionIndex = callback.indexOf(
            "SmartCatchSpawnIngestion.ingestIrcMessage"
        )
        val uiDispatchIndex = callback.indexOf("runUiIfAlive")

        assertTrue("IRC spawn ingestion missing", ingestionIndex >= 0)
        assertTrue("IRC UI dispatch missing", uiDispatchIndex >= 0)
        assertTrue(
            "IRC spawn ingestion must happen before Fragment UI dispatch",
            ingestionIndex < uiDispatchIndex
        )
    }

    @Test
    fun fcmSpawnIsIngestedBeforeOptionalReminderSuppression() {
        val source = readProductionSource("MyFirebaseMessagingService.kt")
        val ingestionIndex = source.indexOf(
            "SmartCatchSpawnIngestion.ingestFcmPayload"
        )
        val displayPolicyIndex = source.indexOf(
            "PcgNotificationPayloadPolicy.shouldDisplay"
        )

        assertTrue("FCM spawn ingestion missing", ingestionIndex >= 0)
        assertTrue("FCM display policy missing", displayPolicyIndex >= 0)
        assertTrue(
            "FCM spawn ingestion must happen before reminder suppression",
            ingestionIndex < displayPolicyIndex
        )
    }

    @Test
    fun chatRowRenderingNoLongerOwnsSpawnPersistence() {
        val source = readProductionSource("ChatFragment.kt")

        assertFalse(
            "Legacy Fragment-only spawn capture must not return",
            source.contains("maybeCaptureSpawnInfoFromChat")
        )
        assertTrue(
            "Live and history paths must both feed the shared coordinator",
            Regex("SmartCatchSpawnIngestion\\.ingestIrcMessage")
                .findAll(source)
                .count() >= 2
        )
    }

    private fun readProductionSource(fileName: String): String {
        val appModule = findAppModuleDirectory()
        val path = appModule
            .resolve("src/main/java/com/fs/twitchminichat")
            .resolve(fileName)

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
