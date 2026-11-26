package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.PcgAltDexKnownForms
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

/**
 * The table of the alt-dex page's names that the app and the backend both test against
 * (`docs/design-alt-dex-alert.md`, *c*, *Keeping the list in step*).
 *
 * `src/test/resources/pcg_alt_dex/pcg_alt_dex_forms_v1.json` is the backend's
 * `alt_dex_forms_v1.json` (tmc-backend `53fe1b4`), byte for byte, generated there from this
 * repository's asset. A change to the asset that the table has not taken fails here; the backend
 * then regenerates its table in its next change.
 */
class PcgAltDexSharedFormsTableTest {

    private val moduleDirectory = BundledPcgCatalogFixture.findAppModuleDirectory()

    private val table: JSONObject by lazy {
        JSONObject(read("src/test/resources/pcg_alt_dex/pcg_alt_dex_forms_v1.json"))
    }

    private val tableNames: List<String> by lazy {
        val names = table.getJSONArray("names")
        List(names.length()) { names.getString(it) }
    }

    private val bundledForms: List<PcgAltDexForm> by lazy {
        PcgAltDexKnownForms.parse(read("src/main/assets/pcg_alt_dex/pcg_alt_dex_forms.json"))
    }

    @Test
    fun theTableListsExactlyTheBundledNamesInOrder() {
        assertEquals(1, table.getInt("schema_version"))
        assertEquals(tableNames.size, table.getInt("name_count"))
        assertEquals(bundledForms.map(PcgAltDexForm::name), tableNames)
    }

    @Test
    fun theUploaderSendsEveryTableNameUnchanged() {
        val upload = PcgAltDexAlertUpload.of(
            profileId = "profile-a",
            profileLabel = "label-a",
            snapshot = PcgAltDexSnapshot(obtained = emptyList(), missing = bundledForms, readAtMs = 0L)
        )

        val names = PcgAltDexAlertUploader.payload(upload, "profile-a").getJSONArray("missing_forms")

        assertEquals(tableNames, List(names.length()) { names.getString(it) })
    }

    private fun read(relativePath: String): String {
        return String(Files.readAllBytes(moduleDirectory.resolve(relativePath)), Charsets.UTF_8)
    }
}
