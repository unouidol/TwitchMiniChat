package com.fs.twitchminichat

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards profile identity propagation across Smart Catch persistence entry points. */
class SmartCatchProfileIsolationSourceTest {

    @Test
    fun quickCatchLoadsUserPresetsForTheActiveProfile() {
        val factory = readProductionSource("QuickCatchMenuModelFactory.kt")
        val source = readProductionSource("UserCatchPresetSource.kt")

        assertTrue(
            Regex(
                """UserCatchPresetSource\.loadSnapshot\(\s*""" +
                    """context = context,\s*profileId = profileId"""
            ).containsMatchIn(factory)
        )
        assertTrue(source.contains("profileId = cleanProfileId"))
        assertFalse(source.contains("CatchPresetStore.loadAll(context)"))
    }

    @Test
    fun presetEditorLoadsAndSavesTheSameProfile() {
        val sheet = readProductionSource("CatchPresetSettingsBottomSheet.kt")

        assertTrue(sheet.contains("profileId = currentProfileId"))
        assertFalse(sheet.contains("CatchPresetStore.loadAll(context)"))
        assertFalse(
            sheet.contains(
                "CatchPresetStore.saveAll(requireContext(),"
            )
        )
    }

    @Test
    fun presetAndInventoryStoresUseCanonicalProfileKeys() {
        val presets = readProductionSource("CatchPresetStore.kt")
        val inventory = readProductionSource("InventoryBallStore.kt")

        assertTrue(presets.contains("ProfileScopedPreferenceKey.create("))
        assertTrue(inventory.contains("ProfileScopedPreferenceKey.create("))
        assertTrue(
            presets.contains("CatchPresetStore") &&
                presets.contains("profileId: String")
        )
    }

    @Test
    fun removingOneAccountClearsOnlyItsProfilePresets() {
        val removal = readProductionSource(
            "AccountProfileRemovalController.kt"
        )

        assertTrue(
            removal.contains(
                "CatchPresetStore.clearProfile(context, profileId)"
            )
        )
    }

    @Test
    fun manualPokedexSnapshotIsStoredAndClearedForTheSameProfile() {
        val gecko = readProductionSource("pcg/GeckoSessionManager.kt")
        val store = readProductionSource("PcgPokedexSnapshotStore.kt")
        val removal = readProductionSource("AccountProfileRemovalController.kt")

        assertTrue(gecko.contains("PcgPokedexSnapshotStore.saveMissingEntries("))
        assertTrue(store.contains("ProfileScopedPreferenceKey.create("))
        assertTrue(
            removal.contains(
                "PcgPokedexSnapshotStore.clearProfile(context, profileId)"
            )
        )
    }

    private fun readProductionSource(fileName: String): String {
        val path = findAppModuleDirectory()
            .resolve("src/main/java/com/fs/twitchminichat")
            .resolve(fileName)

        return String(Files.readAllBytes(path), Charsets.UTF_8)
    }

    private fun findAppModuleDirectory(): Path {
        val start = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath()
            .normalize()

        return generateSequence(start) { current -> current.parent }
            .take(MAX_PARENT_SEARCH_DEPTH)
            .flatMap { current -> sequenceOf(current.resolve("app"), current) }
            .firstOrNull { candidate ->
                Files.isDirectory(candidate.resolve("src/main/java"))
            }
            ?: error("Unable to locate app/src/main/java from $start")
    }

    private companion object {
        const val MAX_PARENT_SEARCH_DEPTH = 6
    }
}
