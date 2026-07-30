package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgEvolutionStage
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogFilter
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogQuery
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import com.fs.twitchminichat.pcg.catalog.PcgPokemonType
import com.fs.twitchminichat.pcg.catalog.PcgSpawnAvailability
import com.fs.twitchminichat.pcg.catalog.PcgVariantKind

/** Special catalog categories treated as alternatives within one group. */
enum class PcgMostWantedCategory {
    STARTER,
    LEGENDARY,
    MYTHICAL
}

/** Advanced filters exposed by the Most Wanted catalog screen. */
data class PcgMostWantedFilterState(
    val tiers: Set<PcgPokemonTier> = emptySet(),
    val types: Set<PcgPokemonType> = emptySet(),
    val generations: Set<Int> = emptySet(),
    val evolutionStages: Set<PcgEvolutionStage> = emptySet(),
    val variantKinds: Set<PcgVariantKind> = emptySet(),
    val spawnAvailability: PcgSpawnAvailability =
        PcgSpawnAvailability.ANY,
    val categories: Set<PcgMostWantedCategory> = emptySet(),
    val selectedOnly: Boolean = false
) {

    /** Returns how many advanced filter groups are currently active. */
    fun activeFilterCount(): Int {
        return listOf(
            tiers.isNotEmpty(),
            types.isNotEmpty(),
            generations.isNotEmpty(),
            evolutionStages.isNotEmpty(),
            variantKinds.isNotEmpty(),
            spawnAvailability != PcgSpawnAvailability.ANY,
            categories.isNotEmpty(),
            selectedOnly
        ).count { active -> active }
    }
}

/** Combines structured text, visible controls and selection state. */
object PcgMostWantedUiFilter {

    /**
     * Applies every active filter group while retaining catalog order.
     *
     * Different groups use AND. Values belonging to the same group use OR.
     */
    fun apply(
        entries: List<PcgPokemonCatalogEntry>,
        searchText: String,
        filterState: PcgMostWantedFilterState,
        selectedDisplayNames: Set<String>
    ): List<PcgPokemonCatalogEntry> {
        val textCriteria = PcgMostWantedSearchParser.parse(searchText)
        val categories = filterState.categories + textCriteria.categories

        val catalogMatches = PcgPokemonCatalogFilter.apply(
            entries = entries,
            query = PcgPokemonCatalogQuery(
                nameQuery = "",
                tiers = filterState.tiers + textCriteria.tiers,
                types = filterState.types + textCriteria.types,
                generations =
                    filterState.generations + textCriteria.generations,
                evolutionStages =
                    filterState.evolutionStages +
                        textCriteria.evolutionStages,
                variantKinds = filterState.variantKinds,
                starterOnly = false,
                legendaryOnly = false,
                mythicalOnly = false,
                spawnAvailability = filterState.spawnAvailability
            )
        )

        return catalogMatches.filter { entry ->
            textCriteria.nameTerms.all { term ->
                entry.normalizedName.contains(term)
            } &&
                matchesCategory(entry, categories) &&
                (
                    !filterState.selectedOnly ||
                        entry.displayName in selectedDisplayNames
                    )
        }
    }

    /** Matches any selected special category, or accepts an empty group. */
    private fun matchesCategory(
        entry: PcgPokemonCatalogEntry,
        categories: Set<PcgMostWantedCategory>
    ): Boolean {
        return categories.isEmpty() || categories.any { category ->
            when (category) {
                PcgMostWantedCategory.STARTER -> entry.starterFamily
                PcgMostWantedCategory.LEGENDARY -> entry.legendary
                PcgMostWantedCategory.MYTHICAL -> entry.mythical
            }
        }
    }
}