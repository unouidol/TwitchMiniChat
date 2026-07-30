package com.fs.twitchminichat.pcg.catalog

/** PCG rarity tiers supplied by the game catalog. */
enum class PcgPokemonTier {
    S,
    A,
    B,
    C
}

/** PokÃ©mon elemental types used by catalog filters. */
enum class PcgPokemonType {
    NORMAL,
    FIRE,
    WATER,
    ELECTRIC,
    GRASS,
    ICE,
    FIGHTING,
    POISON,
    GROUND,
    FLYING,
    PSYCHIC,
    BUG,
    ROCK,
    GHOST,
    DRAGON,
    DARK,
    STEEL,
    FAIRY
}

/** Position occupied by one entry in its evolution family. */
enum class PcgEvolutionStage {
    BASE,
    MIDDLE,
    FINAL,
    SINGLE
}

/** Kind of form represented by a distinct PCG catalog entry. */
enum class PcgVariantKind {
    NONE,
    GENDER,
    OFFICIAL_FORM,
    REGIONAL,
    PCG
}

/** Availability constraint applied by the catalog filter. */
enum class PcgSpawnAvailability {
    ANY,
    NORMALLY_SPAWNABLE,
    SPECIAL_AVAILABILITY
}

/**
 * One selectable PokÃ©mon or form exactly as named by PCG.
 *
 * The display name is preserved for synchronization with the backend. The
 * normalized name is used only for local matching and search.
 */
data class PcgPokemonCatalogEntry(
    val displayName: String,
    val normalizedName: String,
    val tier: PcgPokemonTier,
    val normallySpawnable: Boolean,
    val starterFamily: Boolean,
    val variantKind: PcgVariantKind,
    val sourceSpecies: String,
    val generation: Int,
    val types: Set<PcgPokemonType>,
    val evolutionStage: PcgEvolutionStage,
    val legendary: Boolean,
    val mythical: Boolean
)

/** Versioned PCG catalog loaded from the application assets. */
data class PcgPokemonCatalog(
    val schemaVersion: Int,
    val catalogVersion: String,
    val entries: List<PcgPokemonCatalogEntry>
)

/** Independent constraints used to search and filter the PCG catalog. */
data class PcgPokemonCatalogQuery(
    val nameQuery: String = "",
    val tiers: Set<PcgPokemonTier> = emptySet(),
    val types: Set<PcgPokemonType> = emptySet(),
    val generations: Set<Int> = emptySet(),
    val evolutionStages: Set<PcgEvolutionStage> = emptySet(),
    val variantKinds: Set<PcgVariantKind> = emptySet(),
    val starterOnly: Boolean = false,
    val legendaryOnly: Boolean = false,
    val mythicalOnly: Boolean = false,
    val spawnAvailability: PcgSpawnAvailability = PcgSpawnAvailability.ANY
)
