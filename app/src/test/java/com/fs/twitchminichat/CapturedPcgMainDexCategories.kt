package com.fs.twitchminichat

import org.json.JSONObject

/**
 * The Pokémon Community Game (PCG) main Pokédex's three categories, as its page listed them
 * on 2026-11-23.
 *
 * `src/test/resources/pcg_main_dex_categories/pcg_main_dex_categories_2026-11-23.json` was
 * written from captures of the page itself, not from the catalog: its `provenance` names
 * each capture, its filters and its counter. The app reads categories from catalog flags, so
 * these captured lists are what a test compares them with.
 */
internal object CapturedPcgMainDexCategories {

    /** One page entry: the national number in its sprite URL, and its name without 🎊 or 🔒. */
    data class Entry(val number: Int, val name: String)

    /** Every captured entry by category, in national-number order. */
    val entriesByCategory: Map<MainDexCategory, List<Entry>> by lazy {
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream(RESOURCE)) {
            "Missing test resource $RESOURCE"
        }
        val root = JSONObject(stream.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() })
        require(root.getInt("schemaVersion") == 1) { "Unknown schema in $RESOURCE" }

        val categories = root.getJSONObject("categories")
        require(categories.keys().asSequence().toSet() == JSON_KEYS.keys) {
            "Unexpected categories in $RESOURCE"
        }
        JSON_KEYS.entries.associate { (jsonKey, category) ->
            val array = categories.getJSONArray(jsonKey)
            category to List(array.length()) { i ->
                val item = array.getJSONObject(i)
                Entry(number = item.getInt("number"), name = item.getString("name"))
            }
        }
    }

    private const val RESOURCE =
        "pcg_main_dex_categories/pcg_main_dex_categories_2026-11-23.json"

    private val JSON_KEYS = linkedMapOf(
        "spawnable" to MainDexCategory.SPAWNABLE,
        "starters" to MainDexCategory.STARTERS,
        "legendaries" to MainDexCategory.LEGENDARIES
    )
}
