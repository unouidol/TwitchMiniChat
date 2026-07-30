package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer

/** Validates Most Wanted names against the bundled PCG catalog. */
object PcgMostWantedSelectionValidator {

    /**
     * Returns canonical PCG display names in catalog order.
     *
     * Unknown names are discarded, duplicates are collapsed and normalized
     * input such as an unaccented name resolves to the catalog spelling.
     */
    fun sanitize(
        catalogEntries: List<PcgPokemonCatalogEntry>,
        requestedNames: Collection<String>
    ): Set<String> {
        val requestedKeys = requestedNames
            .asSequence()
            .map(PcgPokemonNameNormalizer::normalize)
            .filter(String::isNotEmpty)
            .toSet()

        return catalogEntries
            .asSequence()
            .filter { entry -> entry.normalizedName in requestedKeys }
            .map(PcgPokemonCatalogEntry::displayName)
            .toCollection(linkedSetOf())
    }

    /**
     * Resolves one user-facing name to the exact PCG catalog display name.
     */
    fun resolveDisplayName(
        catalogEntries: List<PcgPokemonCatalogEntry>,
        requestedName: String
    ): String? {
        val requestedKey = PcgPokemonNameNormalizer.normalize(requestedName)
        if (requestedKey.isEmpty()) {
            return null
        }

        return catalogEntries
            .firstOrNull { entry -> entry.normalizedName == requestedKey }
            ?.displayName
    }
}