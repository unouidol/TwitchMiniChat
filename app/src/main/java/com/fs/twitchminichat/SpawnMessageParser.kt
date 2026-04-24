package com.fs.twitchminichat

object SpawnMessageParser {

    private val spawnPattern = Regex(
        pattern = """(?i)\bA wild\s+(.+?)\s+appears\b"""
    )

    data class ParsedSpawnMessage(
        val rawName: String
    )

    fun parse(message: String): ParsedSpawnMessage? {
        val cleaned = sanitize(message)
        if (cleaned.isBlank()) return null

        val match = spawnPattern.find(cleaned) ?: return null
        val rawName = match.groupValues.getOrNull(1).orEmpty().trim()

        if (rawName.isBlank()) return null

        return ParsedSpawnMessage(
            rawName = rawName
        )
    }

    private fun sanitize(raw: String): String {
        return raw
            .replace("\u0001ACTION", " ")
            .replace("\u0001", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}