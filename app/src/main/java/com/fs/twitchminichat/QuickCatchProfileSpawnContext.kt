package com.fs.twitchminichat

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import com.fs.twitchminichat.pcg.PcgAltDexKnownForms
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import com.fs.twitchminichat.pcg.mostwanted.PcgMostWantedStore

/** Profile-owned Pokédex knowledge available for the current global spawn. */
enum class QuickCatchDexEntryStatus {
    REGISTERED,
    MISSING,
    UNKNOWN
}

/**
 * Alt-dex status of the spawn's exact form, from the profile's accepted "Update Alt Dex"
 * readings (`PcgAltDexSnapshotStore`).
 */
enum class QuickCatchAltDexFormStatus(@param:StringRes @get:StringRes val labelRes: Int) {
    /** The form is among the readings' obtained forms. */
    OBTAINED(R.string.quick_catch_form_obtained),

    /** The form is among the last reading's forms not obtained. */
    NOT_OBTAINED(R.string.quick_catch_form_not_obtained),

    /** A known form with no reading yet, or one no reading has settled. */
    UNKNOWN(R.string.quick_catch_form_unknown)
}

/**
 * Profile-scoped facts used to decorate and rank one global spawn.
 *
 * @property altDexFormStatus null when the spawn's exact name is not a known alt-dex form, so
 * the panel shows no Form line; base forms such as "Vivillon (Meadow)" are main-dex entries.
 * It never affects the Repeat Ball rule, which follows [dexEntryStatus] alone.
 */
data class QuickCatchProfileSpawnContext(
    val dexEntryStatus: QuickCatchDexEntryStatus,
    val isMostWanted: Boolean?,
    val altDexFormStatus: QuickCatchAltDexFormStatus? = null
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

    /**
     * Resolves the active profile's facts for one spawn.
     *
     * @param speciesIndex main-Pokédex species of the bundled catalog; null when the catalog
     * could not provide them, which leaves the Pokédex fact unknown.
     * @param onUnresolvedMissingKey receives each stored missing key that has no species.
     * Such a key matches nothing.
     */
    internal fun resolve(
        spawn: SpawnSnapshot?,
        pokedexSnapshot: PcgPokedexSnapshot?,
        mostWantedNames: Set<String>?,
        speciesIndex: MainDexSpeciesIndex?,
        onUnresolvedMissingKey: (String) -> Unit = {},
        altDexSnapshot: PcgAltDexSnapshot? = null,
        knownAltDexFormKeys: Set<String>? = null
    ): QuickCatchProfileSpawnContext {
        if (spawn == null) {
            return QuickCatchProfileSpawnContext(
                dexEntryStatus = QuickCatchDexEntryStatus.UNKNOWN,
                isMostWanted = null
            )
        }

        val dexEntryStatus = mainDexEntryStatus(
            spawn = spawn,
            pokedexSnapshot = pokedexSnapshot,
            speciesIndex = speciesIndex,
            onUnresolvedMissingKey = onUnresolvedMissingKey
        )

        // Most Wanted names one exact form, so it keeps matching the full form name.
        val spawnNameKeys = spawnNameKeys(spawn)
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
            isMostWanted = isMostWanted,
            altDexFormStatus = altDexFormStatus(
                spawn = spawn,
                altDexSnapshot = altDexSnapshot,
                knownAltDexFormKeys = knownAltDexFormKeys
            )
        )
    }

    /**
     * Alt-dex status of the spawn's exact form, or null when it is not a known form.
     *
     * A form is known when it is in [knownAltDexFormKeys], the bundled list, or in
     * [altDexSnapshot]. The spawn is matched by its exact name, the catalog's name first and
     * then the name chat gave, never by species: the alt dex holds one entry per form.
     *
     * - OBTAINED when the form is among the snapshot's obtained forms;
     * - NOT_OBTAINED when it is among the last reading's forms not obtained;
     * - UNKNOWN for a known form in neither, including every form before the first reading.
     */
    private fun altDexFormStatus(
        spawn: SpawnSnapshot,
        altDexSnapshot: PcgAltDexSnapshot?,
        knownAltDexFormKeys: Set<String>?
    ): QuickCatchAltDexFormStatus? {
        val obtainedKeys = altDexSnapshot?.obtainedKeys().orEmpty()
        val missingKeys = altDexSnapshot?.missingKeys().orEmpty()
        val knownKeys = knownAltDexFormKeys.orEmpty() + obtainedKeys + missingKeys
        if (knownKeys.isEmpty()) return null

        val formKey = sequenceOf(spawn.displayName, spawn.rawName)
            .map(PcgPokemonNameNormalizer::normalize)
            .filter(String::isNotEmpty)
            .firstOrNull(knownKeys::contains)
            ?: return null

        return when (formKey) {
            in obtainedKeys -> QuickCatchAltDexFormStatus.OBTAINED
            in missingKeys -> QuickCatchAltDexFormStatus.NOT_OBTAINED
            else -> QuickCatchAltDexFormStatus.UNKNOWN
        }
    }

    /**
     * Main-Pokédex status of the spawn's species.
     *
     * The main Pokédex holds one entry per species and catching any form registers it, so
     * the spawn is compared by species with the stored missing entries:
     *
     * - MISSING when the spawn's species is among the species of the missing entries;
     * - REGISTERED only when, in addition to not being MISSING, the spawn is a catalog name
     *   and its species is one that a Spawnable-only capture lists while it is missing: one
     *   of PCG's Spawnable category, never a starter or a legendary;
     * - UNKNOWN in every other case: no capture, no species index, a spawn the catalog does
     *   not know, or a species the capture does not cover.
     *
     * A spawn the catalog does not know can still be MISSING, through the species its name
     * starts with, but never REGISTERED.
     */
    private fun mainDexEntryStatus(
        spawn: SpawnSnapshot,
        pokedexSnapshot: PcgPokedexSnapshot?,
        speciesIndex: MainDexSpeciesIndex?,
        onUnresolvedMissingKey: (String) -> Unit
    ): QuickCatchDexEntryStatus {
        if (pokedexSnapshot == null || speciesIndex == null) {
            return QuickCatchDexEntryStatus.UNKNOWN
        }

        // dexKey is the catalog name the spawn resolved to; without it, its own name decides.
        val catalogSpecies = spawn.dexKey?.let(speciesIndex::speciesOfCatalogName)
        val spawnSpecies = catalogSpecies
            ?: sequenceOf(spawn.rawName, spawn.displayName)
                .firstNotNullOfOrNull(speciesIndex::speciesOf)

        val missingSpeciesKeys = pokedexSnapshot.missingNameKeys.mapNotNullTo(HashSet()) { key ->
            val species = speciesIndex.speciesOf(key)
            if (species == null) {
                onUnresolvedMissingKey(key)
            }
            species?.key
        }

        val isMissing = spawnSpecies != null && spawnSpecies.key in missingSpeciesKeys
        val isRegistered = !isMissing &&
            catalogSpecies != null &&
            catalogSpecies.coveredBySpawnableSnapshot

        return when {
            isMissing -> QuickCatchDexEntryStatus.MISSING
            isRegistered -> QuickCatchDexEntryStatus.REGISTERED
            else -> QuickCatchDexEntryStatus.UNKNOWN
        }
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

/**
 * Reports a stored Pokédex key that has no catalog species, once per key.
 *
 * The report never carries the key: a profile's Pokédex entries stay out of the logs, like
 * every other profile datum. The keys already reported are held in memory only, and at most
 * [maxReportedKeys] of them, which also bounds the number of reports.
 */
internal class UnresolvedMainDexKeyReporter(
    private val maxReportedKeys: Int,
    private val sink: (String) -> Unit
) {

    private val reportedKeys = HashSet<String>()

    /** Sends one report the first time [key] is seen, until [maxReportedKeys] is reached. */
    fun report(key: String) {
        val reportedCount = synchronized(reportedKeys) {
            if (reportedKeys.size < maxReportedKeys && reportedKeys.add(key)) {
                reportedKeys.size
            } else {
                null
            }
        } ?: return

        sink(
            "[MAIN_DEX] stored Pokedex key has no catalog species and matches nothing " +
                "distinctKeys=$reportedCount"
        )
    }
}

/** Loads only the active profile's Pokédex and Most Wanted state. */
object QuickCatchProfileSpawnContextProvider {

    private const val LOG_TAG = "SMART_CATCH_DEX"
    private const val MAX_REPORTED_UNRESOLVED_KEYS = 64

    private val unresolvedKeyReporter = UnresolvedMainDexKeyReporter(
        maxReportedKeys = MAX_REPORTED_UNRESOLVED_KEYS,
        sink = { report -> Log.w(LOG_TAG, report) }
    )

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
                mostWantedNames = null,
                speciesIndex = null
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
        val speciesIndex = if (pokedexSnapshot == null) {
            null
        } else {
            loadSpeciesIndex { PokemonTypeDex.mainDexSpeciesIndex(context) }
        }
        val altDexSnapshot = if (spawn == null) {
            null
        } else {
            PcgAltDexSnapshotStore.load(context = context, profileId = canonicalProfileId)
        }
        val knownAltDexFormKeys = if (spawn == null) {
            null
        } else {
            loadKnownAltDexFormKeys { PcgAltDexKnownForms.load(context) }
        }

        return QuickCatchProfileSpawnContextResolver.resolve(
            spawn = spawn,
            pokedexSnapshot = pokedexSnapshot,
            mostWantedNames = mostWantedNames,
            speciesIndex = speciesIndex,
            onUnresolvedMissingKey = unresolvedKeyReporter::report,
            altDexSnapshot = altDexSnapshot,
            knownAltDexFormKeys = knownAltDexFormKeys
        )
    }

    /** A bundled list that cannot be read leaves only the stored forms known. */
    internal fun loadKnownAltDexFormKeys(load: () -> List<PcgAltDexForm>): Set<String>? {
        return runCatching(load).getOrNull()?.mapTo(HashSet(), PcgAltDexForm::key)
    }

    /** A catalog that cannot be read leaves the Pokédex fact unknown, never registered. */
    internal fun loadSpeciesIndex(load: () -> MainDexSpeciesIndex?): MainDexSpeciesIndex? {
        return runCatching(load).getOrNull()
    }
}
