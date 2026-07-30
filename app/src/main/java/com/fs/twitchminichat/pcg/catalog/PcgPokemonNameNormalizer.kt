package com.fs.twitchminichat.pcg.catalog

import java.text.Normalizer
import java.util.Locale

/** Produces stable keys for accent-insensitive PCG name matching. */
object PcgPokemonNameNormalizer {

    /** Normalizes accents, case, punctuation and repeated whitespace. */
    fun normalize(value: String): String {
        return Normalizer
            .normalize(value, Normalizer.Form.NFKD)
            .replace(COMBINING_MARKS_REGEX, "")
            .lowercase(Locale.ROOT)
            .replace(NON_NAME_CHARACTERS_REGEX, " ")
            .trim()
            .replace(REPEATED_WHITESPACE_REGEX, " ")
    }

    /** Unicode combining marks removed after canonical decomposition. */
    private val COMBINING_MARKS_REGEX = Regex("\\p{M}+")

    /** Characters that do not contribute to a catalog search key. */
    private val NON_NAME_CHARACTERS_REGEX = Regex("[^a-z0-9â™€â™‚]+")

    /** Consecutive whitespace collapsed after punctuation removal. */
    private val REPEATED_WHITESPACE_REGEX = Regex("\\s+")
}
