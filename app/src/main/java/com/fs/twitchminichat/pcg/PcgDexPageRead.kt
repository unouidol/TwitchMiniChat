package com.fs.twitchminichat.pcg

import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import org.json.JSONArray
import org.json.JSONObject

/**
 * One read of the PCG Pokédex page by `pcg_dex_reader.js`, as the page showed it.
 *
 * The script reads and reports; it decides nothing. Android checks every value here before
 * any reading built from it is accepted ([PcgAltDexReadingGate]).
 *
 * @property entries each entry in page order, or null when the script did not list them: it
 * lists them only while the alt dex's label is shown active.
 * @property readAtPageMs the page's clock when the read was taken, which spaces two reads.
 * @property receivedAtMs Android's elapsed-realtime clock when the read arrived, which says
 * whether it is still the page's current state.
 */
internal data class PcgDexPageRead(
    val dexLabelFound: Boolean,
    val dexLabel: String,
    val dexLabelActive: Boolean,
    val filters: List<Filter>,
    val searchFound: Boolean,
    val searchEmpty: Boolean,
    val counterText: String,
    val entryCount: Int,
    val entries: List<Entry>?,
    val readAtPageMs: Long,
    val receivedAtMs: Long
) {

    /** One filter checkbox: its label text and its live "checked" property. */
    data class Filter(val name: String, val hasCheckbox: Boolean, val checked: Boolean)

    /** One entry: its name as shown, its sprite number (-1 when absent), its grayscale flag. */
    data class Entry(val name: String, val number: Int, val grayscale: Boolean)

    /** The counter's "obtained / shown". */
    data class Counter(val obtained: Int, val shown: Int)

    /** The page shows the alt dex: its label reads "Alt Dex" and is active. */
    val isAltDex: Boolean
        get() = dexLabelFound && dexLabelActive && labelKey() == ALT_DEX_LABEL_KEY

    /** The page shows the main dex: its label reads "Main Dex" and is not active. */
    val isMainDex: Boolean
        get() = dexLabelFound && !dexLabelActive && labelKey() == MAIN_DEX_LABEL_KEY

    /** The counter, or null when it is absent or not "obtained / shown". */
    val counter: Counter?
        get() {
            val match = COUNTER_REGEX.matchEntire(counterText.trim()) ?: return null
            val obtained = match.groupValues[1].toIntOrNull() ?: return null
            val shown = match.groupValues[2].toIntOrNull() ?: return null
            return Counter(obtained = obtained, shown = shown)
        }

    /** True when [other] read exactly the same page; only the two clocks may differ. */
    fun showsTheSamePageAs(other: PcgDexPageRead): Boolean {
        return copy(readAtPageMs = 0L, receivedAtMs = 0L) ==
            other.copy(readAtPageMs = 0L, receivedAtMs = 0L)
    }

    private fun labelKey(): String = PcgPokemonNameNormalizer.normalize(dexLabel)

    companion object {
        private const val ALT_DEX_LABEL_KEY = "alt dex"
        private const val MAIN_DEX_LABEL_KEY = "main dex"
        private val COUNTER_REGEX = Regex("""(\d+)\s*/\s*(\d+)""")

        /** Parses the `pcg_dex_page_read` payload; a missing value reads as absent. */
        fun fromPayload(payload: JSONObject, receivedAtMs: Long): PcgDexPageRead {
            val entries = if (payload.optBoolean("entriesIncluded", false)) {
                payload.optJSONArray("entries")?.let(::entriesOf)
            } else {
                null
            }

            return PcgDexPageRead(
                dexLabelFound = payload.optBoolean("dexLabelFound", false),
                dexLabel = payload.optString("dexLabel", "").trim(),
                dexLabelActive = payload.optBoolean("dexLabelActive", false),
                filters = filtersOf(payload.optJSONArray("filters")),
                searchFound = payload.optBoolean("searchFound", false),
                searchEmpty = payload.optBoolean("searchEmpty", false),
                counterText = if (payload.optBoolean("counterFound", false)) {
                    payload.optString("counterText", "").trim()
                } else {
                    ""
                },
                entryCount = payload.optInt("entryCount", -1),
                entries = entries,
                readAtPageMs = payload.optLong("readAtMs", -1L),
                receivedAtMs = receivedAtMs
            )
        }

        private fun filtersOf(array: JSONArray?): List<Filter> {
            if (array == null) return emptyList()

            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        Filter(
                            name = item.optString("name", "").trim(),
                            hasCheckbox = item.optBoolean("hasCheckbox", false),
                            checked = item.optBoolean("checked", false)
                        )
                    )
                }
            }
        }

        /** Null when any entry is not an object: a list with a hole is not a list. */
        private fun entriesOf(array: JSONArray): List<Entry>? {
            return List(array.length()) { index ->
                val item = array.optJSONObject(index) ?: return null
                Entry(
                    name = item.optString("name", "").trim(),
                    number = item.optInt("number", -1),
                    grayscale = item.optBoolean("grayscale", false)
                )
            }
        }
    }
}
