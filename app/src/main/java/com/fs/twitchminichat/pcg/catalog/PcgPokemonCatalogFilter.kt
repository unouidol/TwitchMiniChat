package com.fs.twitchminichat.pcg.catalog

/** Applies pure, side-effect-free filters to PCG catalog entries. */
object PcgPokemonCatalogFilter {

    /**
     * Returns entries matching every active constraint while preserving catalog
     * order.
     */
    fun apply(
        entries: List<PcgPokemonCatalogEntry>,
        query: PcgPokemonCatalogQuery
    ): List<PcgPokemonCatalogEntry> {
        val normalizedQuery = PcgPokemonNameNormalizer.normalize(query.nameQuery)

        return entries.filter { entry ->
            matchesName(entry, normalizedQuery) &&
                matchesSet(entry.tier, query.tiers) &&
                matchesTypes(entry, query.types) &&
                matchesSet(entry.generation, query.generations) &&
                matchesSet(entry.evolutionStage, query.evolutionStages) &&
                matchesSet(entry.variantKind, query.variantKinds) &&
                (!query.starterOnly || entry.starterFamily) &&
                (!query.legendaryOnly || entry.legendary) &&
                (!query.mythicalOnly || entry.mythical) &&
                matchesAvailability(entry, query.spawnAvailability)
        }
    }

    /** Matches a normalized substring against the precomputed catalog key. */
    private fun matchesName(
        entry: PcgPokemonCatalogEntry,
        normalizedQuery: String
    ): Boolean {
        return normalizedQuery.isBlank() ||
            entry.normalizedName.contains(normalizedQuery)
    }

    /** Matches at least one requested type, or accepts an empty type filter. */
    private fun matchesTypes(
        entry: PcgPokemonCatalogEntry,
        selectedTypes: Set<PcgPokemonType>
    ): Boolean {
        return selectedTypes.isEmpty() ||
            entry.types.any(selectedTypes::contains)
    }

    /** Matches one value against an optional set constraint. */
    private fun <T> matchesSet(value: T, acceptedValues: Set<T>): Boolean {
        return acceptedValues.isEmpty() || value in acceptedValues
    }

    /** Matches normal-spawn and special-availability catalog filters. */
    private fun matchesAvailability(
        entry: PcgPokemonCatalogEntry,
        availability: PcgSpawnAvailability
    ): Boolean {
        return when (availability) {
            PcgSpawnAvailability.ANY -> true
            PcgSpawnAvailability.NORMALLY_SPAWNABLE ->
                entry.normallySpawnable
            PcgSpawnAvailability.SPECIAL_AVAILABILITY ->
                !entry.normallySpawnable
        }
    }
}
