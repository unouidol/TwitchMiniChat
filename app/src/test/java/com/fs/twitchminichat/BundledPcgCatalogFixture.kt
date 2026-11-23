package com.fs.twitchminichat

import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalog
import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogJsonParser
import java.nio.file.Files
import java.nio.file.Path

/**
 * The PCG catalog bundled in the app, read from the module's assets once per test run.
 *
 * Tests of the main-Pokédex species use the real catalog rather than a hand-written one,
 * because the forms they guard (Vivillon patterns, Unown letters, regional prefixes) are
 * properties of that data.
 */
internal object BundledPcgCatalogFixture {

    val pcgCatalog: PcgPokemonCatalog by lazy {
        val json = String(
            Files.readAllBytes(
                findAppModuleDirectory().resolve(
                    "src/main/assets/pcg_catalog/pcg_pokemon_catalog_v1.json"
                )
            ),
            Charsets.UTF_8
        )
        PcgPokemonCatalogJsonParser.parse(json)
    }

    val typeDex: PokemonTypeDexCatalog by lazy {
        PokemonTypeDexCatalog.build(
            pcgEntries = pcgCatalog.entries,
            metadataEntries = emptyList()
        )
    }

    val speciesIndex: MainDexSpeciesIndex by lazy {
        typeDex.mainDexSpeciesResult.getOrThrow()
    }

    /** A spawn snapshot built from [rawName] the way `SmartCatchSpawnCoordinator` builds it. */
    fun spawn(rawName: String, seenAtMs: Long = 1_000L): SpawnSnapshot {
        val dexEntry = typeDex.find(rawName)
        return SpawnSnapshot(
            rawName = rawName,
            dexKey = dexEntry?.key,
            displayName = dexEntry?.pcgName ?: rawName,
            tier = dexEntry?.tier,
            type1 = dexEntry?.type1,
            type2 = dexEntry?.type2,
            weightKg = dexEntry?.weightKg,
            baseSpeed = dexEntry?.baseSpeed,
            baseHp = dexEntry?.baseHp,
            evolvesTwice = dexEntry?.evolvesTwice,
            seenAtMs = seenAtMs
        )
    }

    /** The `app` module's directory, found from the test's working directory upwards. */
    internal fun findAppModuleDirectory(): Path {
        val start = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath()
            .normalize()

        return generateSequence(start) { current -> current.parent }
            .take(MAX_PARENT_SEARCH_DEPTH)
            .flatMap { current -> sequenceOf(current.resolve("app"), current) }
            .firstOrNull { candidate ->
                Files.isDirectory(candidate.resolve("src/main/assets"))
            }
            ?: error("Unable to locate app/src/main/assets from $start")
    }

    private const val MAX_PARENT_SEARCH_DEPTH = 6
}
