package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer

/** Pure catalog-order operations shared by selection-review dialogs. */
object PcgMostWantedSelectionReview {

    /** Builds a fixed review universe from canonical selected names. */
    fun entriesForSelection(
        catalogEntries: List<PcgPokemonCatalogEntry>,
        selectedDisplayNames: Set<String>
    ): List<PcgPokemonCatalogEntry> {
        return catalogEntries.filter { entry ->
            entry.displayName in selectedDisplayNames
        }
    }

    /** Filters only the fixed review universe without changing its order. */
    fun filterEntries(
        reviewEntries: List<PcgPokemonCatalogEntry>,
        searchText: String
    ): List<PcgPokemonCatalogEntry> {
        val searchKey = PcgPokemonNameNormalizer.normalize(searchText)
        if (searchKey.isBlank()) return reviewEntries

        return reviewEntries.filter { entry ->
            entry.normalizedName.contains(searchKey)
        }
    }

    /** Returns selected review names in stable catalog order. */
    fun selectedNames(
        reviewEntries: List<PcgPokemonCatalogEntry>,
        selectedDisplayNames: Set<String>
    ): Set<String> {
        return reviewEntries
            .filter { entry -> entry.displayName in selectedDisplayNames }
            .mapTo(linkedSetOf()) { entry -> entry.displayName }
    }
}
