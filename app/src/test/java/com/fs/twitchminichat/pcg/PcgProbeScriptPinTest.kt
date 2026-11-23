package com.fs.twitchminichat.pcg

import com.fs.twitchminichat.BundledPcgCatalogFixture
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Pins the three PCG probes to the git blobs they had on `main-v5` at `30610e8`.
 *
 * `content.js` takes "Update Pokédex"'s reading, the scout reports the tab both manual update
 * buttons depend on, and `pcg_probe_inventory.js` takes "Update Inventory"'s. Under the owner's
 * ruling of 2026-11-18 they stay as they are in every build installed for a test, and the
 * readings designed in `docs/design-pcg-dex-readings.md` are added beside them, never in
 * their place. Until this test nothing failed when one of them changed.
 */
class PcgProbeScriptPinTest {

    @Test
    fun contentScriptIsPinned() {
        assertProbePinned("content.js", CONTENT_JS_BLOB)
    }

    @Test
    fun tabStateScoutIsPinned() {
        assertProbePinned("pcg_tab_state_scout.js", TAB_STATE_SCOUT_BLOB)
    }

    @Test
    fun inventoryProbeIsPinned() {
        assertProbePinned("pcg_probe_inventory.js", INVENTORY_PROBE_BLOB)
    }

    /** Git's own value for a known blob, so the hash is git's and not merely self-consistent. */
    @Test
    fun gitBlobIdMatchesGit() {
        // `printf 'hello\n' | git hash-object --stdin`
        assertEquals(
            "ce013625030ba8dba906f756967f9e9ca394464a",
            PcgProbeScriptPin.gitBlobId("hello\n".toByteArray(Charsets.US_ASCII))
        )
    }

    @Test
    fun theExactBlobIsPinnedEverywhere() {
        val bytes = SAMPLE_LF.toByteArray(Charsets.UTF_8)
        val blob = PcgProbeScriptPin.gitBlobId(bytes)

        assertPinned(PcgProbeScriptPin.check("x.js", bytes, blob, onContinuousIntegration = false))
        assertPinned(PcgProbeScriptPin.check("x.js", bytes, blob, onContinuousIntegration = true))
    }

    @Test
    fun oneChangedByteFails() {
        val bytes = SAMPLE_LF.toByteArray(Charsets.UTF_8)
        val blob = PcgProbeScriptPin.gitBlobId(bytes)

        for (index in bytes.indices) {
            val mutant = bytes.copyOf()
            mutant[index] = (mutant[index] + 1).toByte()
            assertChanged(PcgProbeScriptPin.check("x.js", mutant, blob, onContinuousIntegration = false))
        }
    }

    @Test
    fun anAddedOrRemovedByteFails() {
        val bytes = SAMPLE_LF.toByteArray(Charsets.UTF_8)
        val blob = PcgProbeScriptPin.gitBlobId(bytes)

        assertChanged(PcgProbeScriptPin.check("x.js", bytes + 0x20, blob, onContinuousIntegration = false))
        assertChanged(
            PcgProbeScriptPin.check("x.js", bytes.copyOf(bytes.size - 1), blob, onContinuousIntegration = false)
        )
    }

    @Test
    fun theAutocrlfCheckoutIsPinnedOnlyOffContinuousIntegration() {
        val blob = PcgProbeScriptPin.gitBlobId(SAMPLE_LF.toByteArray(Charsets.UTF_8))
        val crlf = SAMPLE_LF.replace("\n", "\r\n").toByteArray(Charsets.UTF_8)

        assertPinned(PcgProbeScriptPin.check("x.js", crlf, blob, onContinuousIntegration = false))
        val onCi = PcgProbeScriptPin.check("x.js", crlf, blob, onContinuousIntegration = true)
        assertChanged(onCi)
        assertTrue(
            (onCi as PcgProbeScriptPin.Verdict.Changed).message.contains("only off continuous integration")
        )
    }

    @Test
    fun mixedOrLoneCarriageReturnsAreNotACheckout() {
        val blob = PcgProbeScriptPin.gitBlobId(SAMPLE_LF.toByteArray(Charsets.UTF_8))
        val mixed = SAMPLE_LF.replaceFirst("\n", "\r\n").toByteArray(Charsets.UTF_8)
        val loneCr = SAMPLE_LF.replace("\n", "\r\n").replaceFirst("\r\n", "\r\r\n")
            .toByteArray(Charsets.UTF_8)

        assertChanged(PcgProbeScriptPin.check("x.js", mixed, blob, onContinuousIntegration = false))
        assertChanged(PcgProbeScriptPin.check("x.js", loneCr, blob, onContinuousIntegration = false))
    }

    @Test
    fun theFailureNamesTheFileTheBlobsAndTheRule() {
        val bytes = SAMPLE_LF.toByteArray(Charsets.UTF_8)
        val blob = PcgProbeScriptPin.gitBlobId(bytes)
        val verdict = PcgProbeScriptPin.check("content.js", bytes + 0x20, blob, onContinuousIntegration = false)

        val message = (verdict as PcgProbeScriptPin.Verdict.Changed).message
        assertTrue(message, message.startsWith("content.js changed:"))
        assertTrue(message, message.contains(blob))
        assertTrue(message, message.contains(PcgProbeScriptPin.gitBlobId(bytes + 0x20)))
        assertTrue(message, message.contains("byte for byte"))
        assertTrue(message, message.contains("2026-11-18"))
    }

    private fun assertProbePinned(fileName: String, pinnedBlobId: String) {
        val path = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/assets/pcg_probe")
            .resolve(fileName)
        val verdict = PcgProbeScriptPin.check(
            fileName = fileName,
            bytes = Files.readAllBytes(path),
            pinnedBlobId = pinnedBlobId,
            onContinuousIntegration = System.getenv("CI") == "true"
        )
        if (verdict is PcgProbeScriptPin.Verdict.Changed) {
            fail(verdict.message)
        }
    }

    private fun assertPinned(verdict: PcgProbeScriptPin.Verdict) {
        assertEquals(PcgProbeScriptPin.Verdict.Pinned, verdict)
    }

    private fun assertChanged(verdict: PcgProbeScriptPin.Verdict) {
        assertTrue("expected a change, got $verdict", verdict is PcgProbeScriptPin.Verdict.Changed)
    }

    private companion object {
        /** `git rev-parse 30610e8:app/src/main/assets/pcg_probe/content.js` */
        const val CONTENT_JS_BLOB = "4d07adb470f6d91c1bd817e7777969ce75fe7a2c"

        /** `git rev-parse 30610e8:app/src/main/assets/pcg_probe/pcg_tab_state_scout.js` */
        const val TAB_STATE_SCOUT_BLOB = "23f1fd0f5f8ef0dc96ff4f277e2f061d40fbc674"

        /** `git rev-parse 30610e8:app/src/main/assets/pcg_probe/pcg_probe_inventory.js` */
        const val INVENTORY_PROBE_BLOB = "4bdebb11cf3fdb1744bfedd6975a1128e04c28a3"

        const val SAMPLE_LF = "(function () {\n  const a = \"é\";\n})();\n"
    }
}
