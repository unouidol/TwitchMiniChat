package com.fs.twitchminichat.pcg.mostwanted

/** Applies explicit bulk actions to the currently shown Most Wanted names. */
object PcgMostWantedBulkSelection {

    /** Adds every shown name while preserving selections hidden by filters. */
    fun selectShown(
        selectedDisplayNames: Collection<String>,
        shownDisplayNames: Collection<String>
    ): Set<String> {
        return selectedDisplayNames
            .toCollection(linkedSetOf())
            .apply { addAll(shownDisplayNames) }
    }

    /** Removes every shown name while preserving selections hidden by filters. */
    fun deselectShown(
        selectedDisplayNames: Collection<String>,
        shownDisplayNames: Collection<String>
    ): Set<String> {
        return selectedDisplayNames
            .toCollection(linkedSetOf())
            .apply { removeAll(shownDisplayNames.toSet()) }
    }
}
