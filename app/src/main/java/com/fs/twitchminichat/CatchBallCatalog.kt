package com.fs.twitchminichat

object CatchBallCatalog {

    data class Entry(
        val ballId: String,
        val label: String,
        val command: String,
        val keepAlways: Boolean = false
    )

    val entries: List<Entry> = listOf(
        Entry(
            ballId = CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC,
            label = "Poke",
            command = "!pokecatch",
            keepAlways = true
        ),
        Entry(
            ballId = "great_ball",
            label = "Great",
            command = "!pokecatch great ball",
            keepAlways = true
        ),
        Entry(
            ballId = "ultra_ball",
            label = "Ultra",
            command = "!pokecatch ultra ball",
            keepAlways = true
        ),

        Entry("friend_ball", "Friend", "!pokecatch friend ball"),
        Entry("quick_ball", "Quick", "!pokecatch quick ball"),
        Entry("timer_ball", "Timer", "!pokecatch timer ball"),
        Entry("repeat_ball", "Repeat", "!pokecatch repeat ball"),
        Entry("nest_ball", "Nest", "!pokecatch nest ball"),
        Entry("fast_ball", "Fast", "!pokecatch fast ball"),
        Entry("feather_ball", "Feather", "!pokecatch feather ball"),
        Entry("heavy_ball", "Heavy", "!pokecatch heavy ball"),
        Entry("heal_ball", "Heal", "!pokecatch heal ball"),

        Entry("frozen_ball", "Frozen", "!pokecatch frozen ball"),
        Entry("night_ball", "Night", "!pokecatch night ball"),
        Entry("phantom_ball", "Phantom", "!pokecatch phantom ball"),
        Entry("cipher_ball", "Cipher", "!pokecatch cipher ball"),
        Entry("magnet_ball", "Magnet", "!pokecatch magnet ball"),
        Entry("net_ball", "Net", "!pokecatch net ball"),
        Entry("sun_ball", "Sun", "!pokecatch sun ball"),
        Entry("fantasy_ball", "Fantasy", "!pokecatch fantasy ball"),
        Entry("geo_ball", "Geo", "!pokecatch geo ball"),
        Entry("basic_ball", "Basic", "!pokecatch basic ball"),
        Entry("mach_ball", "Mach", "!pokecatch mach ball"),

        Entry("luxury_ball", "Luxury", "!pokecatch luxury ball"),
        Entry("level_ball", "Level", "!pokecatch level ball"),
        Entry("clone_ball", "Clone", "!pokecatch clone ball"),
        Entry("stone_ball", "Stone", "!pokecatch stone ball"),
        Entry("sport_ball", "Sport", "!pokecatch sport ball"),

        Entry("premier_ball", "Premier", "!pokecatch premier ball"),

        // restano nel catalogo ma li filtri fuori dal quick menu
        Entry("master_ball", "Master", "!pokecatch master ball"),
        Entry("cherish_ball", "Cherish", "!pokecatch cherish ball"),
        Entry("great_cherish_ball", "Great Cherish", "!pokecatch great cherish ball"),
        Entry("ultra_cherish_ball", "Ultra Cherish", "!pokecatch ultra cherish ball")
    )

    fun createDefaultPreset(entry: Entry, index: Int): CatchPreset {
        return CatchPreset(
            id = "catalog_${entry.ballId}_$index",
            label = entry.label,
            command = entry.command,
            enabled = true,
            ballId = entry.ballId
        )
    }

    /**
     * Builds the internal preset candidates used by the Smart Presets system.
     *
     * These presets are NOT user presets and must NOT be saved into
     * CatchPresetStore.
     *
     * They only exist at runtime so CatchBallRecommender can score the full ball
     * catalog even if the user deleted all default/user presets.
     */
    fun createSmartCandidatePresets(): List<CatchPreset> {
        return entries.map { entry ->
            createSmartCandidatePreset(entry)
        }
    }

    /**
     * Creates one runtime-only smart preset candidate from a catalog entry.
     *
     * The ID intentionally starts with "smart_" so RecyclerView DiffUtil and any
     * future menu logic can distinguish these rows from saved user presets.
     */
    private fun createSmartCandidatePreset(entry: Entry): CatchPreset {
        return CatchPreset(
            id = "smart_${entry.ballId}",
            label = entry.label,
            command = entry.command,
            enabled = true,
            ballId = entry.ballId
        )
    }
}