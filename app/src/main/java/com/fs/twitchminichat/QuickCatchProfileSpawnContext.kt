package com.fs.twitchminichat

import android.content.Context
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import com.fs.twitchminichat.pcg.mostwanted.PcgMostWantedStore

/** Profile-owned Pokédex knowledge available for the current global spawn. */
enum class QuickCatchDexEntryStatus {
    REGISTERED,
    MISSING,
    UNKNOWN
}

/** Profile-scoped facts used to decorate and rank one global spawn. */
data class QuickCatchProfileSpawnContext(
    val dexEntryStatus: QuickCatchDexEntryStatus,
    val isMostWanted: Boolean?
) {
    /** Repeat Ball is recommended only from an affirmative registered-entry fact. */
    val isAlreadyCaught: Boolean?
        get() = when (dexEntryStatus) {
            QuickCatchDexEntryStatus.REGISTERED -> true
            QuickCatchDexEntryStatus.MISSING -> false
            QuickCatchDexEntryStatus.UNKNOWN -> null
        }
}

/** Pure matching policy shared by the Android provider and local unit tests. */
object QuickCatchProfileSpawnContextResolver {

    fun resolve(
        spawn: SpawnSnapshot?,
        pokedexSnapshot: PcgPokedexSnapshot?,
        mostWantedNames: Set<String>?
    ): QuickCatchProfileSpawnContext {
        if (spawn == null) {
            return QuickCatchProfileSpawnContext(
                dexEntryStatus = QuickCatchDexEntryStatus.UNKNOWN,
                isMostWanted = null
            )
        }

        val spawnNameKeys = spawnNameKeys(spawn)
        val dexEntryStatus = when {
            spawn.dexKey.isNullOrBlank() || pokedexSnapshot == null ->
                QuickCatchDexEntryStatus.UNKNOWN

            spawnNameKeys.any(pokedexSnapshot.missingNameKeys::contains) ->
                QuickCatchDexEntryStatus.MISSING

            else -> QuickCatchDexEntryStatus.REGISTERED
        }

        val isMostWanted = mostWantedNames?.let { selectedNames ->
            val selectedNameKeys = selectedNames
                .asSequence()
                .map(PcgPokemonNameNormalizer::normalize)
                .filter(String::isNotEmpty)
                .toSet()
            spawnNameKeys.any(selectedNameKeys::contains)
        }

        return QuickCatchProfileSpawnContext(
            dexEntryStatus = dexEntryStatus,
            isMostWanted = isMostWanted
        )
    }

    private fun spawnNameKeys(spawn: SpawnSnapshot): Set<String> {
        return sequenceOf(
            spawn.rawName,
            spawn.displayName,
            spawn.dexKey.orEmpty()
        )
            .map(PcgPokemonNameNormalizer::normalize)
            .filter(String::isNotEmpty)
            .toSet()
    }
}

/** Loads only the active profile's Pokédex and Most Wanted state. */
object QuickCatchProfileSpawnContextProvider {

    fun load(
        context: Context,
        profileId: String?,
        spawn: SpawnSnapshot?
    ): QuickCatchProfileSpawnContext {
        val canonicalProfileId = profileId
            ?.let(AccountProfileIdResolver::normalize)
            .orEmpty()
        if (canonicalProfileId.isBlank()) {
            return QuickCatchProfileSpawnContextResolver.resolve(
                spawn = spawn,
                pokedexSnapshot = null,
                mostWantedNames = null
            )
        }

        val pokedexSnapshot = PcgPokedexSnapshotStore.load(
            context = context,
            profileId = canonicalProfileId
        )
        val mostWantedNames = runCatching {
            PcgMostWantedStore(context)
                .getSelectedDisplayNamesSnapshot(canonicalProfileId)
        }.getOrNull()

        return QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn,
            pokedexSnapshot = pokedexSnapshot,
            mostWantedNames = mostWantedNames
        )
    }
}
