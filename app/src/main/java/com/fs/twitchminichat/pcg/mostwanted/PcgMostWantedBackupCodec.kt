package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgPokemonCatalogEntry
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer

/** One validated Most Wanted backup ready to replace the editor draft. */
data class PcgMostWantedBackupImport(
    val selectedDisplayNames: Set<String>,
    val duplicateCount: Int,
    val sourceCatalogVersion: String?
)

/** Reasons why a text document cannot be accepted as a TMC backup. */
enum class PcgMostWantedBackupDecodeError {
    EMPTY_DOCUMENT,
    MISSING_HEADER,
    MISSING_FORMAT,
    UNSUPPORTED_FORMAT,
    TOO_MANY_ENTRIES,
    UNKNOWN_NAMES
}

/** Details for one rejected backup without exposing unrelated file content. */
data class PcgMostWantedBackupDecodeFailure(
    val reason: PcgMostWantedBackupDecodeError,
    val unknownNames: List<String> = emptyList()
)

/** Result of validating a user-selected Most Wanted text document. */
sealed interface PcgMostWantedBackupDecodeResult {
    /** The complete file is valid against the current bundled catalog. */
    data class Success(
        val backup: PcgMostWantedBackupImport
    ) : PcgMostWantedBackupDecodeResult

    /** Nothing should be imported because validation failed. */
    data class Failure(
        val error: PcgMostWantedBackupDecodeFailure
    ) : PcgMostWantedBackupDecodeResult
}

/** Encodes and validates the human-readable Most Wanted backup format. */
object PcgMostWantedBackupCodec {

    /** Creates one deterministic backup containing names in catalog order. */
    fun encode(
        catalogVersion: String,
        catalogEntries: List<PcgPokemonCatalogEntry>,
        selectedDisplayNames: Collection<String>
    ): String {
        val requestedNames = selectedDisplayNames.toSet()
        val orderedNames = catalogEntries
            .asSequence()
            .map(PcgPokemonCatalogEntry::displayName)
            .filter(requestedNames::contains)
            .toList()

        require(orderedNames.size == requestedNames.size) {
            "Most Wanted draft contains names outside the PCG catalog"
        }

        val safeCatalogVersion = catalogVersion
            .replace("\r", "")
            .replace("\n", "")
            .trim()

        return buildString {
            appendLine(HEADER)
            appendLine("$FORMAT_PREFIX$FORMAT_VERSION")
            if (safeCatalogVersion.isNotEmpty()) {
                appendLine("$CATALOG_PREFIX$safeCatalogVersion")
            }
            appendLine(COMMENT)
            orderedNames.forEach { name -> appendLine(name) }
        }
    }

    /**
     * Validates the whole document before returning any imported selection.
     *
     * Unknown names reject the complete file. Exact catalog spelling is
     * resolved before normalized aliases so gender and regional forms remain
     * distinct during a backup round trip.
     */
    fun decode(
        text: String,
        catalogEntries: List<PcgPokemonCatalogEntry>
    ): PcgMostWantedBackupDecodeResult {
        val cleanText = text.removePrefix(UNICODE_BOM)
        val lines = cleanText
            .lineSequence()
            .map(String::trim)
            .toList()
        val firstContentIndex = lines.indexOfFirst(String::isNotEmpty)

        if (firstContentIndex < 0) {
            return failure(PcgMostWantedBackupDecodeError.EMPTY_DOCUMENT)
        }

        if (lines[firstContentIndex] != HEADER) {
            return failure(PcgMostWantedBackupDecodeError.MISSING_HEADER)
        }

        val formatValue = lines
            .asSequence()
            .drop(firstContentIndex + 1)
            .firstOrNull { line -> line.startsWith(FORMAT_PREFIX) }
            ?.removePrefix(FORMAT_PREFIX)
            ?.trim()

        if (formatValue == null) {
            return failure(PcgMostWantedBackupDecodeError.MISSING_FORMAT)
        }

        if (formatValue != FORMAT_VERSION.toString()) {
            return failure(
                PcgMostWantedBackupDecodeError.UNSUPPORTED_FORMAT
            )
        }

        val sourceCatalogVersion = lines
            .asSequence()
            .drop(firstContentIndex + 1)
            .firstOrNull { line -> line.startsWith(CATALOG_PREFIX) }
            ?.removePrefix(CATALOG_PREFIX)
            ?.trim()
            ?.takeIf(String::isNotEmpty)

        val requestedNames = lines
            .asSequence()
            .drop(firstContentIndex + 1)
            .filter(String::isNotEmpty)
            .filterNot { line -> line.startsWith(COMMENT_PREFIX) }
            .toList()

        if (requestedNames.size > MAX_ENTRY_LINES) {
            return failure(PcgMostWantedBackupDecodeError.TOO_MANY_ENTRIES)
        }

        val exactEntries = catalogEntries.associateBy(
            PcgPokemonCatalogEntry::displayName
        )
        val normalizedEntries = catalogEntries.groupBy(
            PcgPokemonCatalogEntry::normalizedName
        )
        val resolvedNames = linkedSetOf<String>()
        val unknownNames = linkedSetOf<String>()

        requestedNames.forEach { requestedName ->
            val exactEntry = exactEntries[requestedName]
            val normalizedKey = PcgPokemonNameNormalizer.normalize(
                requestedName
            )
            val normalizedEntry = normalizedEntries[normalizedKey]
                ?.singleOrNull()
            val resolvedEntry = exactEntry ?: normalizedEntry

            if (resolvedEntry == null) {
                unknownNames.add(requestedName)
            } else {
                resolvedNames.add(resolvedEntry.displayName)
            }
        }

        if (unknownNames.isNotEmpty()) {
            return PcgMostWantedBackupDecodeResult.Failure(
                PcgMostWantedBackupDecodeFailure(
                    reason = PcgMostWantedBackupDecodeError.UNKNOWN_NAMES,
                    unknownNames = unknownNames.toList()
                )
            )
        }

        val namesInCatalogOrder = catalogEntries
            .asSequence()
            .map(PcgPokemonCatalogEntry::displayName)
            .filter(resolvedNames::contains)
            .toCollection(linkedSetOf())

        return PcgMostWantedBackupDecodeResult.Success(
            PcgMostWantedBackupImport(
                selectedDisplayNames = namesInCatalogOrder,
                duplicateCount = requestedNames.size - resolvedNames.size,
                sourceCatalogVersion = sourceCatalogVersion
            )
        )
    }

    /** Creates a validation failure that carries no file excerpts. */
    private fun failure(
        reason: PcgMostWantedBackupDecodeError
    ): PcgMostWantedBackupDecodeResult.Failure {
        return PcgMostWantedBackupDecodeResult.Failure(
            PcgMostWantedBackupDecodeFailure(reason = reason)
        )
    }

    /** Human-readable document signature. */
    private const val HEADER = "# TwitchMiniChat Most Wanted"

    /** Metadata prefix containing the supported backup schema. */
    private const val FORMAT_PREFIX = "# format="

    /** Metadata prefix recording the source catalog for diagnostics. */
    private const val CATALOG_PREFIX = "# catalog="

    /** Comment explaining the editable portion of the document. */
    private const val COMMENT = "# One PCG catalog name per line."

    /** Every metadata or explanatory line starts with this character. */
    private const val COMMENT_PREFIX = "#"

    /** Version of the text contract understood by this app release. */
    private const val FORMAT_VERSION = 1

    /** Prevents pathological but otherwise small duplicate-heavy documents. */
    private const val MAX_ENTRY_LINES = 5_000

    /** Optional marker accepted at the beginning of UTF-8 text files. */
    private const val UNICODE_BOM = "\uFEFF"
}
