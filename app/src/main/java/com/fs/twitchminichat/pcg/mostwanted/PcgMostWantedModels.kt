package com.fs.twitchminichat.pcg.mostwanted

/**
 * Local Most Wanted state for one application profile.
 *
 * This state is independent from the PokÃ©dex list. It only controls an
 * informative custom watchlist and never triggers gameplay commands.
 */
data class PcgMostWantedState(
    val enabled: Boolean = false,
    val selectedDisplayNames: Set<String> = emptySet()
)