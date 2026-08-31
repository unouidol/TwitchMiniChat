package com.fs.twitchminichat.pcg.mostwanted

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for explicit bulk selection of filtered Most Wanted results. */
class PcgMostWantedBulkSelectionTest {

    /** Selecting shown results keeps names hidden by the active filters. */
    @Test
    fun selectShown_addsVisibleNamesAndPreservesHiddenSelections() {
        val result = PcgMostWantedBulkSelection.selectShown(
            selectedDisplayNames = linkedSetOf("Bulbasaur", "Pal Wooper"),
            shownDisplayNames = listOf("Pal Wooper", "Gal Moltres")
        )

        assertEquals(
            linkedSetOf("Bulbasaur", "Pal Wooper", "Gal Moltres"),
            result
        )
    }

    /** Deselecting shown results keeps names hidden by the active filters. */
    @Test
    fun deselectShown_removesOnlyVisibleNames() {
        val result = PcgMostWantedBulkSelection.deselectShown(
            selectedDisplayNames = linkedSetOf(
                "Bulbasaur",
                "Pal Wooper",
                "Gal Moltres"
            ),
            shownDisplayNames = listOf("Pal Wooper", "Gal Moltres")
        )

        assertEquals(linkedSetOf("Bulbasaur"), result)
    }

    /** Empty filtered results leave the draft unchanged. */
    @Test
    fun emptyShownSelection_isNoOp() {
        val selected = linkedSetOf("Bulbasaur")

        assertEquals(
            selected,
            PcgMostWantedBulkSelection.selectShown(selected, emptyList())
        )
        assertEquals(
            selected,
            PcgMostWantedBulkSelection.deselectShown(selected, emptyList())
        )
    }
}
