package com.fs.twitchminichat.pcg

import android.content.Context
import com.fs.twitchminichat.PcgAltDexForm
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import org.json.JSONObject

/**
 * The PCG alt dex's forms as the 2026-11-23 capture rounds listed them, bundled in the APK.
 *
 * `assets/pcg_alt_dex/pcg_alt_dex_forms.json` records its provenance. These are the "known
 * forms" of reading 3: with "Obtained" off the alt dex lists only the forms not obtained, so a
 * known form that an accepted reading does not list is obtained.
 */
object PcgAltDexKnownForms {

    private const val ASSET_PATH = "pcg_alt_dex/pcg_alt_dex_forms.json"
    private const val SCHEMA_VERSION = 1
    private const val LOCK = "🔒"
    private const val CONFETTI = "🎊"

    @Volatile
    private var cached: List<PcgAltDexForm>? = null

    /** The bundled forms, read once per process. Throws when the asset cannot be read. */
    fun load(context: Context): List<PcgAltDexForm> {
        cached?.let { return it }

        synchronized(this) {
            cached?.let { return it }

            val json = context.applicationContext.assets
                .open(ASSET_PATH)
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> reader.readText() }
            return parse(json).also { forms -> cached = forms }
        }
    }

    /** Parses the bundled list; throws on any schema it does not know. */
    fun parse(json: String): List<PcgAltDexForm> {
        val root = JSONObject(json)
        require(root.getInt("schemaVersion") == SCHEMA_VERSION) {
            "Unknown alt-dex forms schema"
        }

        val array = root.getJSONArray("forms")
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            formOf(rawName = item.getString("name"), number = item.getInt("number"))
                ?: error("Alt-dex form $index has no name")
        }
    }

    /**
     * A form from a name as the page shows it: 🔒 and 🎊 are removed from the stored name,
     * never a reason to refuse it. Null when nothing is left of the name.
     */
    fun formOf(rawName: String, number: Int): PcgAltDexForm? {
        val name = rawName
            .replace(LOCK, "")
            .replace(CONFETTI, "")
            .trim()
        val key = PcgPokemonNameNormalizer.normalize(name)
        if (key.isEmpty()) return null

        return PcgAltDexForm(key = key, name = name, number = number)
    }
}
