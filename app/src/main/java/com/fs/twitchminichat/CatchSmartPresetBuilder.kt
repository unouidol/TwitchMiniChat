package com.fs.twitchminichat

object CatchSmartPresetBuilder {

    data class SpawnContext(
        val name: String,
        val elapsedSeconds: Int? = null
    )

    fun build(spawn: SpawnContext?): List<CatchPresetMenuItem> {
        if (spawn == null) return emptyList()

        val cleanName = spawn.name.trim()
        if (cleanName.isBlank()) return emptyList()

        return listOf(
            CatchPresetMenuItem(
                id = "smart_ultra",
                section = CatchPresetMenuSection.SMART,
                title = "Ultra Ball",
                subtitle = "80% catch rate • suggested for $cleanName",
                command = "!pokecatch ultraball",
                editable = false
            ),
            CatchPresetMenuItem(
                id = "smart_great",
                section = CatchPresetMenuSection.SMART,
                title = "Great Ball",
                subtitle = "55% catch rate • fallback for $cleanName",
                command = "!pokecatch greatball",
                editable = false
            ),
            CatchPresetMenuItem(
                id = "smart_poke",
                section = CatchPresetMenuSection.SMART,
                title = "Poké Ball",
                subtitle = "30% catch rate • basic option",
                command = "!pokecatch pokeball",
                editable = false
            )
        )
    }
}