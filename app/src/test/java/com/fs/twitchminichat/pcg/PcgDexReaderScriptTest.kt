package com.fs.twitchminichat.pcg

import com.fs.twitchminichat.BundledPcgCatalogFixture
import java.nio.file.Files
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `pcg_dex_reader.js`, the alt dex's reader: loaded last, after the three pinned probes, and
 * passive.
 *
 * The script cannot run here, so these checks read its source. They are a guard, not a proof:
 * a determined change could evade them, and the review of the script is what proves it reads
 * only. They catch the ordinary ways a reader turns into an actor.
 */
class PcgDexReaderScriptTest {

    @Test
    fun theReaderLoadsLastAfterTheThreeProbesInTheirOrder() {
        val scripts = manifest()
            .getJSONArray("content_scripts")
            .getJSONObject(0)
            .getJSONArray("js")

        assertEquals(
            listOf(
                "pcg_tab_state_scout.js",
                "pcg_probe_inventory.js",
                "content.js",
                "pcg_dex_reader.js"
            ),
            List(scripts.length(), scripts::getString)
        )
    }

    /** GeckoView reloads the bundled extension only when its version changes. */
    @Test
    fun theManifestVersionIsRaised() {
        val version = manifest().getString("version")

        assertNotEquals("1.2.1", version)
        assertEquals("1.3.0", version)
    }

    @Test
    fun theReaderNeverActsOnThePage() {
        val code = readerCodeWithoutComments()

        listOf(
            ".click(",
            "scroll",
            ".focus(",
            ".blur(",
            "dispatchEvent",
            "submit(",
            ".checked =",
            ".value =",
            "innerHTML",
            "textContent =",
            "setAttribute(",
            "removeAttribute(",
            "classList.add",
            "classList.remove",
            "classList.toggle",
            "appendChild",
            "remove()",
            "fetch(",
            "XMLHttpRequest",
            "WebSocket",
            "postMessage",
            "localStorage",
            "sessionStorage",
            "runtime.sendMessage"
        ).forEach { action ->
            assertFalse("pcg_dex_reader.js must not use $action", code.contains(action))
        }
    }

    @Test
    fun theReaderSendsOnlyItsReadToAndroid() {
        val code = readerCodeWithoutComments()

        assertEquals(1, Regex("""sendNativeMessage\(""").findAll(code).count())
        assertEquals(
            listOf("pcg_dex_page_read"),
            Regex("""send\("([a-z_]+)"""").findAll(code).map { it.groupValues[1] }.toList()
        )
    }

    /** The search box's text is never sent: only whether it is empty. */
    @Test
    fun theSearchTextStaysOnThePage() {
        val code = readerCodeWithoutComments()

        assertEquals(1, Regex("""\.value\b""").findAll(code).count())
        assertTrue(code.contains("""empty: String(input.value || "").length === 0"""))
    }

    @Test
    fun aFullPayloadIsReadAsThePageShowedIt() {
        val read = PcgDexPageRead.fromPayload(
            JSONObject(
                """
                {"dexLabelFound":true,"dexLabel":" Alt Dex ","dexLabelActive":true,
                 "filters":[{"name":"Obtained","hasCheckbox":true,"checked":false}],
                 "searchFound":true,"searchEmpty":true,
                 "counterFound":true,"counterText":"0 / 2","entryCount":2,
                 "entriesIncluded":true,
                 "entries":[{"name":"Unown (B)","number":10510,"grayscale":true},
                            {"name":"Alo Vulpix","number":10103,"grayscale":true}],
                 "readAtMs":1700000000000}
                """.trimIndent()
            ),
            receivedAtMs = 42L
        )

        assertTrue(read.isAltDex)
        assertFalse(read.isMainDex)
        assertEquals("Alt Dex", read.dexLabel)
        assertEquals(listOf(PcgDexPageRead.Filter("Obtained", true, false)), read.filters)
        assertEquals(PcgDexPageRead.Counter(obtained = 0, shown = 2), read.counter)
        assertEquals(
            listOf(
                PcgDexPageRead.Entry("Unown (B)", 10510, true),
                PcgDexPageRead.Entry("Alo Vulpix", 10103, true)
            ),
            read.entries
        )
        assertEquals(1700000000000L, read.readAtPageMs)
        assertEquals(42L, read.receivedAtMs)
    }

    @Test
    fun theMainDexIsRecognisedWithoutItsEntries() {
        val read = PcgDexPageRead.fromPayload(
            JSONObject(
                """{"dexLabelFound":true,"dexLabel":"Main Dex","dexLabelActive":false,
                    "counterFound":true,"counterText":"310 / 1025","entryCount":1025,
                    "entriesIncluded":false,"entries":[],"readAtMs":1}"""
            ),
            receivedAtMs = 0L
        )

        assertTrue(read.isMainDex)
        assertFalse(read.isAltDex)
        assertEquals(null, read.entries)
        assertEquals(PcgDexPageRead.Counter(obtained = 310, shown = 1025), read.counter)
    }

    @Test
    fun missingValuesReadAsAbsent() {
        val read = PcgDexPageRead.fromPayload(JSONObject("{}"), receivedAtMs = 0L)

        assertFalse(read.dexLabelFound)
        assertFalse(read.isAltDex)
        assertFalse(read.isMainDex)
        assertEquals(emptyList<PcgDexPageRead.Filter>(), read.filters)
        assertEquals(null, read.counter)
        assertEquals(null, read.entries)
        assertEquals(-1, read.entryCount)
    }

    /** A counter the script did not find is never read from a leftover text. */
    @Test
    fun aCounterNotFoundIsNotRead() {
        val read = PcgDexPageRead.fromPayload(
            JSONObject("""{"counterFound":false,"counterText":"0 / 2"}"""),
            receivedAtMs = 0L
        )

        assertEquals(null, read.counter)
    }

    @Test
    fun aListWithAHoleIsNoList() {
        val read = PcgDexPageRead.fromPayload(
            JSONObject(
                """{"entriesIncluded":true,
                    "entries":[{"name":"Unown (B)","number":10510,"grayscale":true}, 7]}"""
            ),
            receivedAtMs = 0L
        )

        assertEquals(null, read.entries)
    }

    @Test
    fun twoReadsOfTheSamePageDifferOnlyByTheirClocks() {
        val payload = """{"dexLabelFound":true,"dexLabel":"Alt Dex","dexLabelActive":true,
            "counterFound":true,"counterText":"0 / 0","entryCount":0,"entriesIncluded":true,
            "entries":[],"readAtMs":%d}"""
        val first = PcgDexPageRead.fromPayload(JSONObject(payload.format(1000)), receivedAtMs = 1L)
        val second = PcgDexPageRead.fromPayload(JSONObject(payload.format(2000)), receivedAtMs = 2L)
        val other = second.copy(counterText = "0 / 1")

        assertTrue(first.showsTheSamePageAs(second))
        assertFalse(first.showsTheSamePageAs(other))
    }

    private fun manifest(): JSONObject = JSONObject(readProbeAsset("manifest.json"))

    /** The reader's code with its comments removed, so their prose is not mistaken for code. */
    private fun readerCodeWithoutComments(): String {
        return readProbeAsset("pcg_dex_reader.js")
            .replace(Regex("""/\*[\s\S]*?\*/"""), "")
            .replace(Regex("""(?m)^\s*//.*$"""), "")
    }

    private fun readProbeAsset(fileName: String): String {
        val path = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/assets/pcg_probe")
            .resolve(fileName)
        return String(Files.readAllBytes(path), Charsets.UTF_8)
    }
}
