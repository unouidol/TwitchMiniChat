package com.fs.twitchminichat

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the fix for #116, where Android 12 and later cut toasts after two lines.
 *
 * Read from the sources, because the local tests have no Android runtime: no test here
 * can run a fragment and see which window it opens. These checks fail if the dialog is
 * turned back into a toast, or if the erase's messages are queued after its restart.
 */
class TruncatedToastSourceTest {

    /** The owed-alerts message is a dialog with one OK button, not a toast. */
    @Test
    fun owedAlertsAfterAnAccountRemovalOpenADialog() {
        val source = readAppFile("src/main/java/com/fs/twitchminichat/LoginFragment.kt")

        val callbackStart = source.indexOf("onAlertDisableOwed = {")
        val callbackEnd = source.indexOf(") { result ->", startIndex = callbackStart)
        assertTrue(callbackStart >= 0 && callbackEnd > callbackStart)
        val callback = source.substring(callbackStart, callbackEnd)

        assertTrue(callback.contains("showAlertsOwedDialog()"))
        assertFalse(callback.contains("Toast"))

        val dialogStart = source.indexOf("private fun showAlertsOwedDialog()")
        assertTrue(dialogStart >= 0)
        val dialog = source.substring(dialogStart, source.indexOf(".show()", dialogStart))

        assertTrue(dialog.contains("AlertDialog.Builder("))
        assertTrue(dialog.contains(".setTitle(R.string.account_remove_alerts_owed_title)"))
        assertTrue(dialog.contains(".setMessage(R.string.account_remove_alerts_owed)"))
        assertTrue(dialog.contains(".setPositiveButton(android.R.string.ok, null)"))
        assertFalse(dialog.contains("setNegativeButton"))

        assertFalse(source.contains("getString(R.string.account_remove_alerts_owed)"))
    }

    /**
     * The erase's toasts are queued before the restart. Queued after it, they would be
     * raised by a fragment that is already going away.
     */
    @Test
    fun eraseMessagesAreQueuedBeforeTheRestart() {
        val source = readAppFile(
            "src/main/java/com/fs/twitchminichat/SafetyPrivacyFragment.kt"
        )

        val notice = source.indexOf("DeviceEraseNotice.show(")
        val restart = source.indexOf("restartAppAfterLocalClear()", startIndex = notice)

        assertTrue(notice >= 0)
        assertTrue(restart > notice)
        assertFalse(source.contains("erase_device_web_data_failed"))
    }

    /** The browser failure carries no error text, so nothing technical lengthens it. */
    @Test
    fun browserFailureMessageTakesNoArgument() {
        val strings = readAppFile("src/main/res/values/strings.xml")
        val line = strings.lineSequence()
            .single { it.contains("name=\"erase_device_web_data_failed\"") }

        assertTrue(line.contains(">Browser data could not be cleared.</string>"))
        assertFalse(line.contains("%"))
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
