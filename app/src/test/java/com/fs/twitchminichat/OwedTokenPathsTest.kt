package com.fs.twitchminichat

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T16 on #117 (R2, F3 PR-3): every path to the Firebase Cloud Messaging token goes where the
 * token gate covers it, read from the sources.
 *
 * The gate is in `FcmRegistrationUploader`: a request that needs delivery obtains its token there,
 * after any owed token deletion in flight. A token read anywhere else and sent on its own would
 * pass around the gate, so these tests fail on any new reader of the cached token, and on any
 * new call asking Firebase for it.
 */
class OwedTokenPathsTest {

    /**
     * The cached token is read only by the uploader, behind the gate, and by `GeckoSessionManager`,
     * whose read only feeds `uploadToken`, which ignores it for a selection that needs delivery.
     * `MyFirebaseMessagingService` writes it and never reads it.
     */
    @Test
    fun theCachedTokenIsReadOnlyWhereTheGateCoversIt() {
        val sources = mainSources().toMap()
        val holders = sources.filterValues { "latest_fcm_token" in it }.keys

        assertEquals(
            setOf("FcmRegistrationUploader.kt", "MyFirebaseMessagingService.kt", "GeckoSessionManager.kt"),
            holders
        )

        val service = checkNotNull(sources["MyFirebaseMessagingService.kt"])
        assertFalse(service.contains("getString(KEY_LATEST_FCM_TOKEN"))

        val gecko = checkNotNull(sources["GeckoSessionManager.kt"])
        assertEquals(1, Regex("latest_fcm_token").findAll(gecko).count())
        assertTrue(
            gecko.substringAfter("prefs.getString(\"latest_fcm_token\", null)")
                .substringBefore("} else {")
                .contains("FcmRegistrationUploader.uploadToken(appContext, token, snapshot.profileId)")
        )

        val uploader = checkNotNull(sources["FcmRegistrationUploader.kt"])
        assertEquals(1, Regex("""getString\(KEY_LATEST_FCM_TOKEN""").findAll(uploader).count())
        assertTrue(
            uploader.substringAfter("private fun cachedToken(appContext: Context): String {")
                .startsWith("\n        return tokenCache(appContext).getString(KEY_LATEST_FCM_TOKEN, null)")
        )
    }

    /**
     * Firebase is asked for the token only through the uploader's seam, and by
     * `MainActivity.fetchFcmToken`, whose token only feeds `uploadToken`.
     */
    @Test
    fun firebaseIsAskedForTheTokenOnlyThroughTheSeamAndTheStartUpFetch() {
        val sources = mainSources().toMap()
        val askers = sources.filterValues { "FirebaseMessaging.getInstance().token" in it }.keys

        assertEquals(setOf("OwedWorkSeams.kt", "MainActivity.kt"), askers)

        val activity = checkNotNull(sources["MainActivity.kt"])
        assertEquals(1, Regex("""FirebaseMessaging\.getInstance\(\)\.token""").findAll(activity).count())
        val fetch = activity.substringAfter("private fun fetchFcmToken() {")
        assertTrue(fetch.startsWith("\n        FirebaseMessaging.getInstance().token.addOnCompleteListener"))
        assertTrue(fetch.contains("FcmRegistrationUploader.uploadToken(applicationContext, token, profileId)"))

        val seams = checkNotNull(sources["OwedWorkSeams.kt"])
        assertTrue(
            seams.substringAfter("internal object PlayServicesFirebaseTokens")
                .contains("FirebaseMessaging.getInstance().token.addOnCompleteListener")
        )
        // The seam's fetch is called in one place, the gate, and never by the uploader itself.
        val callers = sources.filterValues { ".fetchToken" in it }
        assertEquals(setOf("RegistrationTokenGate.kt"), callers.keys)
        assertEquals(1, Regex("""tokens\.fetchToken""").findAll(callers.values.single()).count())
    }

    /** Both requests that may need delivery obtain their token through the gate, and nothing else does. */
    @Test
    fun bothRequestsObtainTheirTokenThroughTheGate() {
        val uploader = checkNotNull(mainSources().toMap()["FcmRegistrationUploader.kt"])

        val upload = uploader
            .substringAfter("fun uploadToken(context: Context, token: String, profileId: String) {")
            .substringBefore("private fun runRegistrationPlan(")
        assertTrue(upload.contains("withDeliveryToken(\n                appContext = appContext,\n                selection = selection,\n                callerToken = token,"))

        val spawnMode = uploader
            .substringAfter("fun setProfileSpawnAlertMode(")
            .substringBefore("private fun cachedToken(")
        assertTrue(spawnMode.contains("withDeliveryToken(\n                appContext = appContext,\n                selection = selection,\n                callerToken = cachedToken(appContext),"))

        assertEquals(2, Regex("""(?<!fun )withDeliveryToken\(\n""").findAll(uploader).count())
        assertEquals(1, Regex("""RegistrationTokenGate\.obtain\(""").findAll(uploader).count())
    }

    /** R1: the application's token deletion hands a late completion to [LateTokenDeletion]. */
    @Test
    fun theApplicationsDeletionHandsALateCompletionOn() {
        val uploader = checkNotNull(mainSources().toMap()["FcmRegistrationUploader.kt"])
        val deletion = uploader
            .substringAfter("fun deleteFirebaseTokenBlocking(context: Context): Boolean {")
            .substringBefore("/** Delivers one deletion result on the Android main thread. */")

        assertTrue(deletion.contains("onLateSuccess = {\n                LateTokenDeletion.completed(\n                    coordinator = OwedWorkCoordinator.PROCESS,"))
        assertTrue(deletion.contains("requestProfileRegistration(appContext, profileId)"))
    }

    /** Every Kotlin file of the application, by name, with line ends as LF. */
    private fun mainSources(): List<Pair<String, String>> {
        val directory = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/java/com/fs/twitchminichat")
        return Files.walk(directory).use { paths ->
            paths.filter { it.toString().endsWith(".kt") }
                .map { path ->
                    path.fileName.toString() to
                        String(Files.readAllBytes(path), Charsets.UTF_8).replace("\r\n", "\n")
                }
                .iterator().asSequence().toList()
        }
    }
}
