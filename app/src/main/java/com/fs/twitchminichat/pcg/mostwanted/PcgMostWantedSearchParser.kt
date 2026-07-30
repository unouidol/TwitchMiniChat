package com.fs.twitchminichat.pcg.mostwanted

import com.fs.twitchminichat.pcg.catalog.PcgEvolutionStage
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer
import com.fs.twitchminichat.pcg.catalog.PcgPokemonTier
import com.fs.twitchminichat.pcg.catalog.PcgPokemonType
import java.util.Locale

/** Structured constraints recognized inside the Most Wanted search field. */
data class PcgMostWantedSearchCriteria(
    val nameTerms: List<String> = emptyList(),
    val tiers: Set<PcgPokemonTier> = emptySet(),
    val types: Set<PcgPokemonType> = emptySet(),
    val generations: Set<Int> = emptySet(),
    val evolutionStages: Set<PcgEvolutionStage> = emptySet(),
    val categories: Set<PcgMostWantedCategory> = emptySet()
)

/**
 * Parses free text into composable PCG catalog constraints.
 *
 * Plus, comma and semicolon separate independent terms. Recognized metadata is
 * removed from name search, while every unrecognized term remains a Pokemon
 * name or variant constraint.
 */
object PcgMostWantedSearchParser {

    /** Parses all search segments and merges values from the same group. */
    fun parse(rawText: String): PcgMostWantedSearchCriteria {
        val nameTerms = mutableListOf<String>()
        val tiers = linkedSetOf<PcgPokemonTier>()
        val types = linkedSetOf<PcgPokemonType>()
        val generations = linkedSetOf<Int>()
        val evolutionStages = linkedSetOf<PcgEvolutionStage>()
        val categories = linkedSetOf<PcgMostWantedCategory>()

        rawText
            .split(TERM_SEPARATOR_REGEX)
            .map(PcgPokemonNameNormalizer::normalize)
            .filter(String::isNotBlank)
            .forEach { segment ->
                var remaining = segment

                TIER_REGEX.findAll(remaining).forEach { match ->
                    val symbol = match.groupValues
                        .drop(1)
                        .first(String::isNotBlank)
                    tiers.add(
                        PcgPokemonTier.valueOf(
                            symbol.uppercase(Locale.ROOT)
                        )
                    )
                }
                remaining = TIER_REGEX.replace(remaining, " ")

                GENERATION_REGEX.findAll(remaining).forEach { match ->
                    generations.add(match.groupValues[1].toInt())
                }
                remaining = GENERATION_REGEX.replace(remaining, " ")

                remaining = collectStage(
                    remaining = remaining,
                    pattern = BASE_STAGE_REGEX,
                    stage = PcgEvolutionStage.BASE,
                    destination = evolutionStages
                )
                remaining = collectStage(
                    remaining = remaining,
                    pattern = MIDDLE_STAGE_REGEX,
                    stage = PcgEvolutionStage.MIDDLE,
                    destination = evolutionStages
                )
                remaining = collectStage(
                    remaining = remaining,
                    pattern = FINAL_STAGE_REGEX,
                    stage = PcgEvolutionStage.FINAL,
                    destination = evolutionStages
                )
                remaining = collectStage(
                    remaining = remaining,
                    pattern = SINGLE_STAGE_REGEX,
                    stage = PcgEvolutionStage.SINGLE,
                    destination = evolutionStages
                )

                CATEGORY_PATTERNS.forEach { (category, pattern) ->
                    if (pattern.containsMatchIn(remaining)) {
                        categories.add(category)
                        remaining = pattern.replace(remaining, " ")
                    }
                }

                PcgPokemonType.entries.forEach { type ->
                    val pattern = Regex(
                        "\\b" +
                            Regex.escape(
                                type.name.lowercase(Locale.ROOT)
                            ) +
                            "\\b"
                    )
                    if (pattern.containsMatchIn(remaining)) {
                        types.add(type)
                        remaining = pattern.replace(remaining, " ")
                    }
                }

                val nameTerm = PcgPokemonNameNormalizer.normalize(remaining)
                if (nameTerm.isNotBlank()) {
                    nameTerms.add(nameTerm)
                }
            }

        return PcgMostWantedSearchCriteria(
            nameTerms = nameTerms,
            tiers = tiers,
            types = types,
            generations = generations,
            evolutionStages = evolutionStages,
            categories = categories
        )
    }

    /** Collects one evolution-stage alias and removes it from name text. */
    private fun collectStage(
        remaining: String,
        pattern: Regex,
        stage: PcgEvolutionStage,
        destination: MutableSet<PcgEvolutionStage>
    ): String {
        if (!pattern.containsMatchIn(remaining)) {
            return remaining
        }

        destination.add(stage)
        return pattern.replace(remaining, " ")
    }

    /** Separators supported between independent search constraints. */
    private val TERM_SEPARATOR_REGEX = Regex("[+,;]+")

    /** Tier aliases such as "tier a" and "a tier". */
    private val TIER_REGEX =
        Regex("\\b(?:tier\\s*([sabc])|([sabc])\\s*tier)\\b")

    /** Generation aliases such as "gen 1", "gen1" and "generation 1". */
    private val GENERATION_REGEX =
        Regex("\\b(?:gen|generation)\\s*([1-9])\\b")

    /** Base-form aliases recognized as the first unevolved form. */
    private val BASE_STAGE_REGEX =
        Regex("\\bbase(?:\\s+(?:form|stage))?\\b")

    /**
     * Middle-stage aliases.
     *
     * "First stage" intentionally maps to MIDDLE for the PCG terminology used
     * by this screen.
     */
    private val MIDDLE_STAGE_REGEX =
        Regex("\\b(?:middle|first)(?:\\s+(?:form|stage))?\\b")

    /** Final-form aliases recognized by structured search. */
    private val FINAL_STAGE_REGEX =
        Regex("\\bfinal(?:\\s+(?:form|stage))?\\b")

    /** Single-stage aliases recognized by structured search. */
    private val SINGLE_STAGE_REGEX =
        Regex("\\bsingle(?:\\s+(?:form|stage))?\\b")

    /** Special category labels recognized by structured search. */
    private val CATEGORY_PATTERNS = mapOf(
        PcgMostWantedCategory.STARTER to Regex("\\bstarter\\b"),
        PcgMostWantedCategory.LEGENDARY to Regex("\\blegendary\\b"),
        PcgMostWantedCategory.MYTHICAL to Regex("\\bmythical\\b")
    )
}