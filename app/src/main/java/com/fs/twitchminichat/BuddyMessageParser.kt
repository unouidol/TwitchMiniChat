package com.fs.twitchminichat

object BuddyMessageParser {

    private val headerPattern = Regex(
        pattern = """^@([A-Za-z0-9_]+)\s+Buddy:\s+(.+?)\s*$""",
        option = RegexOption.IGNORE_CASE
    )

    private val levelPattern = Regex(
        pattern = """\s*\(Lvl\s+(\d+)\)""",
        option = RegexOption.IGNORE_CASE
    )

    private val avgIvPattern = Regex(
        pattern = """(?:👀\s*)?Avg IV:\s*(\d+)""",
        option = RegexOption.IGNORE_CASE
    )

    data class ParsedBuddyMessage(
        val addressedUsername: String,
        val rawName: String,
        val level: Int?,
        val avgIv: Int?
    )

    fun parse(message: String): ParsedBuddyMessage? {
        val trimmed = message.trim()
        if (trimmed.isBlank()) return null

        val match = headerPattern.find(trimmed) ?: return null

        val addressedUsername = match.groupValues.getOrNull(1).orEmpty().trim().lowercase()
        var tail = match.groupValues.getOrNull(2).orEmpty().trim()

        if (addressedUsername.isBlank() || tail.isBlank()) return null

        val level = levelPattern.find(tail)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        tail = tail.replace(levelPattern, "").trim()

        val avgIv = avgIvPattern.find(tail)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        tail = tail.replace(avgIvPattern, "").trim()

        // pulizia finale
        tail = tail
            .replace(Regex("""\s+"""), " ")
            .trim('-', '•', '·', ' ')

        if (tail.isBlank()) return null

        return ParsedBuddyMessage(
            addressedUsername = addressedUsername,
            rawName = tail,
            level = level,
            avgIv = avgIv
        )
    }
}