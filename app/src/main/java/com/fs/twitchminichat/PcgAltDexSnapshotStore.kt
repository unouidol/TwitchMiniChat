package com.fs.twitchminichat

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import org.json.JSONArray
import org.json.JSONObject

/**
 * One form of the PCG alt dex.
 *
 * @property key the name's [PcgPokemonNameNormalizer] key, which a spawn's name is matched by.
 * @property name the name as the page shows it, without its 🔒 prefix. 22 alt-dex names are
 * unknown to the catalog, so the name is kept as read rather than as a catalog key.
 * @property number the number in the entry's sprite URL.
 */
data class PcgAltDexForm(
    val key: String,
    val name: String,
    val number: Int
)

/**
 * What one profile's accepted alt-dex readings established.
 *
 * @property obtained forms known to exist that an accepted reading did not list. With
 * "Obtained" off the alt dex lists only the forms not obtained, and PCG never resets it, so
 * this set only grows.
 * @property missing the forms the last accepted reading listed, replaced by each reading.
 * @property readAtMs wall-clock time of the last accepted reading.
 */
data class PcgAltDexSnapshot(
    val obtained: List<PcgAltDexForm>,
    val missing: List<PcgAltDexForm>,
    val readAtMs: Long
) {

    /** Keys of [obtained]. */
    fun obtainedKeys(): Set<String> = obtained.mapTo(HashSet(), PcgAltDexForm::key)

    /** Keys of [missing]. */
    fun missingKeys(): Set<String> = missing.mapTo(HashSet(), PcgAltDexForm::key)

    companion object {

        /**
         * The snapshot after one more accepted reading.
         *
         * A form is known when it is in [knownForms], the bundled list, or when an earlier
         * accepted reading listed it, so it is in [previous]. Every known form that [listed]
         * does not hold is added to the obtained forms; none is ever removed from them, not
         * even one that [listed] holds again. The missing forms become exactly [listed].
         * A wrong "obtained" can therefore be undone only by clearing the profile's data,
         * which is why the reading's acceptance checks keep half-rendered lists out.
         */
        fun afterReading(
            previous: PcgAltDexSnapshot?,
            knownForms: List<PcgAltDexForm>,
            listed: List<PcgAltDexForm>,
            readAtMs: Long
        ): PcgAltDexSnapshot {
            val listedByKey = LinkedHashMap<String, PcgAltDexForm>()
            listed.forEach { form -> listedByKey.putIfAbsent(form.key, form) }

            val obtainedByKey = LinkedHashMap<String, PcgAltDexForm>()
            previous?.obtained?.forEach { form -> obtainedByKey.putIfAbsent(form.key, form) }

            sequenceOf(
                knownForms.asSequence(),
                previous?.missing.orEmpty().asSequence()
            )
                .flatten()
                .filter { form -> form.key !in listedByKey }
                .forEach { form -> obtainedByKey.putIfAbsent(form.key, form) }

            return PcgAltDexSnapshot(
                obtained = obtainedByKey.values.toList(),
                missing = listedByKey.values.toList(),
                readAtMs = readAtMs.coerceAtLeast(0L)
            )
        }
    }
}

/**
 * Persists, per PCG profile, what the user's accepted "Update Alt Dex" readings established.
 *
 * Store 3 of `docs/design-pcg-dex-readings.md`: its own preferences file and schema, never
 * merged with [PcgPokedexSnapshotStore], which holds the main dex's Spawnable reading. Only the
 * last reading's missing names and its time leave the phone, through [PcgAltDexAlertSync], and
 * only while Missing Alt Dex form alerts are on for the profile; the obtained forms, keys and
 * sprite numbers never do. It is cleared when its profile is removed, and by every local-data
 * reset, which deletes every preferences file it does not explicitly keep.
 */
object PcgAltDexSnapshotStore {

    /** The store's preferences file. Not kept by any reset (see [LocalDataCleaner]). */
    const val PREFERENCES_NAME = "pcg_alt_dex_snapshots"

    private const val SNAPSHOT_KEY_PREFIX = "alt_dex_forms"
    private const val SCHEMA_VERSION = 1

    /**
     * Records one accepted reading for [profileId] and returns the stored snapshot, or null
     * when the profile identifier is blank and nothing was stored.
     */
    fun saveReading(
        context: Context,
        profileId: String,
        knownForms: List<PcgAltDexForm>,
        listed: List<PcgAltDexForm>,
        readAtMs: Long = System.currentTimeMillis()
    ): PcgAltDexSnapshot? {
        return saveReading(preferences(context), profileId, knownForms, listed, readAtMs)
    }

    /** Returns null when this profile has no accepted reading, or a stored one is unreadable. */
    fun load(
        context: Context,
        profileId: String
    ): PcgAltDexSnapshot? {
        return load(preferences(context), profileId)
    }

    /** Deletes only the alt-dex snapshot owned by one profile. */
    fun clearProfile(
        context: Context,
        profileId: String
    ) {
        clearProfile(preferences(context), profileId)
    }

    internal fun saveReading(
        preferences: SharedPreferences,
        profileId: String,
        knownForms: List<PcgAltDexForm>,
        listed: List<PcgAltDexForm>,
        readAtMs: Long
    ): PcgAltDexSnapshot? {
        val storageKey = storageKey(profileId) ?: return null
        val snapshot = PcgAltDexSnapshot.afterReading(
            previous = load(preferences, profileId),
            knownForms = knownForms,
            listed = listed,
            readAtMs = readAtMs
        )

        val payload = JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("readAtMs", snapshot.readAtMs)
            .put("obtained", formsToJson(snapshot.obtained))
            .put("missing", formsToJson(snapshot.missing))

        preferences.edit {
            putString(storageKey, payload.toString())
        }
        return snapshot
    }

    internal fun load(
        preferences: SharedPreferences,
        profileId: String
    ): PcgAltDexSnapshot? {
        val storageKey = storageKey(profileId) ?: return null
        val raw = preferences.getString(storageKey, null) ?: return null

        return runCatching {
            val payload = JSONObject(raw)
            if (payload.optInt("schemaVersion", -1) != SCHEMA_VERSION) {
                return@runCatching null
            }

            PcgAltDexSnapshot(
                obtained = formsFromJson(payload.optJSONArray("obtained") ?: return@runCatching null),
                missing = formsFromJson(payload.optJSONArray("missing") ?: return@runCatching null),
                readAtMs = payload.optLong("readAtMs", 0L).coerceAtLeast(0L)
            )
        }.getOrNull()
    }

    internal fun clearProfile(
        preferences: SharedPreferences,
        profileId: String
    ) {
        val storageKey = storageKey(profileId) ?: return
        preferences.edit {
            remove(storageKey)
        }
    }

    private fun preferences(context: Context): SharedPreferences {
        return context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private fun storageKey(profileId: String): String? {
        return ProfileScopedPreferenceKey.create(
            prefix = SNAPSHOT_KEY_PREFIX,
            profileId = profileId
        )
    }

    private fun formsToJson(forms: List<PcgAltDexForm>): JSONArray {
        val array = JSONArray()
        forms.forEach { form ->
            array.put(
                JSONObject()
                    .put("key", form.key)
                    .put("name", form.name)
                    .put("number", form.number)
            )
        }
        return array
    }

    /** Skips an unreadable form rather than inventing one: a skipped form reads as "?". */
    private fun formsFromJson(array: JSONArray): List<PcgAltDexForm> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val key = PcgPokemonNameNormalizer.normalize(item.optString("key"))
                if (key.isEmpty()) continue
                add(
                    PcgAltDexForm(
                        key = key,
                        name = item.optString("name"),
                        number = item.optInt("number", -1)
                    )
                )
            }
        }
    }
}
