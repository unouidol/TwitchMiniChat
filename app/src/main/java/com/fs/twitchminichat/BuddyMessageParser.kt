package com.fs.twitchminichat

object BuddyMessageParser {

    private val buddyPattern = Regex(
        pattern = """^@([A-Za-z0-9_]+)\s+Buddy:\s+(.+?)\s+\(Lvl\s+(\d+)\)\s+.*?Avg IV:\s+(\d+)\s*$""",
        option = RegexOption.IGNORE_CASE
    )

    data class ParsedBuddyMessage(
        val addressedUsername: String,
        val pokemonName: String,
        val level: Int?,
        val avgIv: Int?
    )

    fun parse(message: String): ParsedBuddyMessage? {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return null

        val match = buddyPattern.find(trimmed) ?: return null

        val addressedUsername = match.groupValues.getOrNull(1).orEmpty().trim().lowercase()
        val pokemonName = match.groupValues.getOrNull(2).orEmpty().trim()
        val level = match.groupValues.getOrNull(3).orEmpty().toIntOrNull()
        val avgIv = match.groupValues.getOrNull(4).orEmpty().toIntOrNull()

        if (addressedUsername.isBlank() || pokemonName.isBlank()) return null

        return ParsedBuddyMessage(
            addressedUsername = addressedUsername,
            pokemonName = pokemonName,
            level = level,
            avgIv = avgIv
        )
    }
}