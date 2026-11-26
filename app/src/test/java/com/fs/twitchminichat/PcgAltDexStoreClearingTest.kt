package com.fs.twitchminichat

import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Store 3 is cleared like `PcgPokedexSnapshotStore`: when its profile is removed, and by every
 * local-data reset.
 *
 * The local tests have no Android runtime, so the removal and the resets cannot run here;
 * these checks read what they would do. Clearing one profile's key is run against a real
 * editor in `PcgAltDexSnapshotStoreTest`.
 */
class PcgAltDexStoreClearingTest {

    @Test
    fun removingAProfileClearsItsAltDexSnapshot() {
        val removal = readProductionSource("AccountProfileRemovalController.kt")
        val clearing = removal.substringAfter("private fun clearKnownLocalProfileData(")

        assertTrue(clearing.contains("PcgPokedexSnapshotStore.clearProfile(context, profileId)"))
        assertTrue(clearing.contains("PcgAltDexSnapshotStore.clearProfile(context, profileId)"))
    }

    /** The Missing Alt Dex form alert flag goes with its profile, like the event flag. */
    @Test
    fun removingAProfileClearsItsAltDexFormAlertFlag() {
        val removal = readProductionSource("AccountProfileRemovalController.kt")
        val clearing = removal.substringAfter("private fun clearKnownLocalProfileData(")

        assertTrue(clearing.contains("PcgAltDexFormAlertStore.clearProfile(context, profileId)"))
    }

    /** The keep-accounts reset deletes every preferences file it does not keep by name. */
    @Test
    fun theKeepAccountsResetDeletesTheAltDexFile() {
        val kept = LocalDataCleaner.keepAccountsExclusions(
            setOf(AccountRepository.LEGACY_PREFERENCES_NAME)
        )

        assertFalse(PcgAltDexSnapshotStore.PREFERENCES_NAME in kept)
    }

    /** The full erase keeps no preferences file at all. */
    @Test
    fun theFullEraseDeletesEveryPreferencesFile() {
        val cleaner = readProductionSource("LocalDataCleaner.kt")
        val clearAll = cleaner
            .substringAfter("fun clearAllLocalData(context: Context): Result {")
            .substringBefore("fun clearNonAccountLocalData(")

        assertTrue(clearAll.contains("excludedSharedPrefs = emptySet()"))
    }

    /**
     * The resets reach shared_prefs files by name and nothing else in filesDir, so store 3
     * must keep everything in its preferences file.
     */
    @Test
    fun theStoreWritesOnlyItsPreferencesFile() {
        val store = readProductionSource("PcgAltDexSnapshotStore.kt")

        assertTrue(store.contains(".getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)"))
        listOf("filesDir", "openFileOutput", "File(", "getDatabasePath", "cacheDir").forEach { other ->
            assertFalse(other, store.contains(other))
        }
    }

    private fun readProductionSource(fileName: String): String {
        val path = BundledPcgCatalogFixture.findAppModuleDirectory()
            .resolve("src/main/java/com/fs/twitchminichat")
            .resolve(fileName)
        return String(Files.readAllBytes(path), Charsets.UTF_8)
    }
}
