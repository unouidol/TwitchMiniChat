package com.fs.twitchminichat.pcg

import com.fs.twitchminichat.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The acceptance checks of reading 3, "Update Alt Dex", one failing condition at a time.
 *
 * Every read starts from a page that passes, the alt dex with "Obtained" off and two missing
 * forms, and changes one value. A condition only the user can change is refused at once; one
 * the page may still be rendering waits, and its reason is what the tap reports if its window
 * ends first.
 */
class PcgAltDexReadingGateTest {

    @Test
    fun twoIdenticalReadsOneSecondApartAreAccepted() {
        val decision = evaluate(read(PREVIOUS), read(LATEST))

        assertTrue(decision is PcgAltDexGateDecision.Accept)
        val forms = (decision as PcgAltDexGateDecision.Accept).forms
        assertEquals(listOf("unown b", "alo vulpix"), forms.map { it.key })
        assertEquals(listOf("Unown (B)", "Alo Vulpix"), forms.map { it.name })
        assertEquals(listOf(10510, 10103), forms.map { it.number })
    }

    /** The page marks locked forms with 🔒; the form's name is stored without it. */
    @Test
    fun aLockedFormIsStoredWithoutItsMark() {
        val locked = listOf(entry("🔒 Unown (B)", 10510), entry("Alo Vulpix", 10103))

        val decision = evaluate(read(PREVIOUS, entries = locked), read(LATEST, entries = locked))

        val forms = (decision as PcgAltDexGateDecision.Accept).forms
        assertEquals("Unown (B)", forms.first().name)
        assertEquals("unown b", forms.first().key)
    }

    @Test
    fun noFreshLatestReadWaits() {
        assertWait(PcgAltDexRejection.NO_READ, PcgAltDexReadingGate.evaluate(null, null, NOW))
        assertWait(
            PcgAltDexRejection.NO_READ,
            PcgAltDexReadingGate.evaluate(
                read(LATEST),
                read(PREVIOUS),
                LATEST_RECEIVED + PcgAltDexReadingGate.READ_MAX_AGE_MS + 1
            )
        )
    }

    @Test
    fun aSingleReadIsNeverAccepted() {
        assertWait(PcgAltDexRejection.NO_READ, PcgAltDexReadingGate.evaluate(read(LATEST), null, NOW))
    }

    @Test
    fun aStalePreviousReadWaits() {
        val stalePrevious = read(PREVIOUS).copy(
            receivedAtMs = NOW - PcgAltDexReadingGate.READ_MAX_AGE_MS - 1
        )

        assertWait(PcgAltDexRejection.NO_READ, evaluate(stalePrevious, read(LATEST)))
    }

    @Test
    fun readsCloserThanASecondWait() {
        val tooClose = read(PREVIOUS).copy(
            readAtPageMs = LATEST.pageMs - PcgAltDexReadingGate.PAIR_MIN_SPACING_MS + 1
        )

        assertWait(PcgAltDexRejection.NO_READ, evaluate(tooClose, read(LATEST)))
    }

    @Test
    fun readsTooFarApartWait() {
        val tooFar = read(PREVIOUS).copy(
            readAtPageMs = LATEST.pageMs - PcgAltDexReadingGate.PAIR_MAX_SPACING_MS - 1
        )

        assertWait(PcgAltDexRejection.NO_READ, evaluate(tooFar, read(LATEST)))
    }

    @Test
    fun twoDifferentReadsWait() {
        val oneMore = TWO_FORMS + entry("Unown (C)", 10511)

        assertWait(
            PcgAltDexRejection.UNSTABLE,
            evaluate(read(PREVIOUS), read(LATEST, entries = oneMore, counterText = "0 / 3"))
        )
    }

    @Test
    fun theMainDexIsRefused() {
        assertReject(
            PcgAltDexRejection.NOT_ALT_DEX,
            evaluateLatest { copy(dexLabel = "Main Dex", dexLabelActive = false) }
        )
        assertReject(PcgAltDexRejection.NOT_ALT_DEX, evaluateLatest { copy(dexLabelActive = false) })
        assertReject(PcgAltDexRejection.NOT_ALT_DEX, evaluateLatest { copy(dexLabelFound = false) })
    }

    @Test
    fun obtainedOnIsRefused() {
        assertReject(
            PcgAltDexRejection.OBTAINED_ON,
            evaluateLatest { copy(filters = listOf(obtained(checked = true))) }
        )
    }

    @Test
    fun anotherFilterOnIsRefused() {
        assertReject(
            PcgAltDexRejection.OTHER_FILTER_ON,
            evaluateLatest {
                copy(filters = listOf(obtained(), PcgDexPageRead.Filter("Shiny", true, true)))
            }
        )
    }

    @Test
    fun anOffFilterBesideObtainedPasses() {
        val withOffFilter = listOf(obtained(), PcgDexPageRead.Filter("Shiny", true, false))

        assertTrue(
            evaluate(read(PREVIOUS, filters = withOffFilter), read(LATEST, filters = withOffFilter))
                is PcgAltDexGateDecision.Accept
        )
    }

    @Test
    fun aMissingObtainedCheckboxWaits() {
        assertWait(PcgAltDexRejection.PAGE_UNREADABLE, evaluateLatest { copy(filters = emptyList()) })
        assertWait(
            PcgAltDexRejection.PAGE_UNREADABLE,
            evaluateLatest { copy(filters = listOf(PcgDexPageRead.Filter("Obtained", false, false))) }
        )
    }

    @Test
    fun aSearchIsRefused() {
        assertReject(PcgAltDexRejection.SEARCH_ACTIVE, evaluateLatest { copy(searchEmpty = false) })
    }

    /** A page without a search box is read as it is; the gate asks only that a box be empty. */
    @Test
    fun aPageWithoutASearchBoxPasses() {
        assertTrue(
            evaluate(
                read(PREVIOUS).copy(searchFound = false, searchEmpty = false),
                read(LATEST).copy(searchFound = false, searchEmpty = false)
            ) is PcgAltDexGateDecision.Accept
        )
    }

    @Test
    fun aMissingCounterWaits() {
        assertWait(PcgAltDexRejection.PAGE_UNREADABLE, evaluateLatest { copy(counterText = "") })
        assertWait(PcgAltDexRejection.PAGE_UNREADABLE, evaluateLatest { copy(counterText = "Pokedex") })
    }

    @Test
    fun aCounterWithObtainedFormsWaits() {
        assertWait(
            PcgAltDexRejection.COUNTER_OBTAINED_NOT_ZERO,
            evaluateLatest { copy(counterText = "310 / 371") }
        )
    }

    @Test
    fun noListedEntriesWait() {
        assertWait(PcgAltDexRejection.PAGE_UNREADABLE, evaluateLatest { copy(entries = null) })
    }

    @Test
    fun entriesThatDifferFromTheCounterWait() {
        assertWait(PcgAltDexRejection.COUNT_MISMATCH, evaluateLatest { copy(counterText = "0 / 3") })
        assertWait(PcgAltDexRejection.COUNT_MISMATCH, evaluateLatest { copy(entryCount = 3) })
    }

    @Test
    fun aColouredEntryWaits() {
        assertWait(
            PcgAltDexRejection.NOT_ALL_GRAYSCALE,
            evaluateLatest {
                copy(entries = listOf(TWO_FORMS[0], TWO_FORMS[1].copy(grayscale = false)))
            }
        )
    }

    @Test
    fun anUnreadableEntryWaits() {
        listOf(
            listOf(TWO_FORMS[0], TWO_FORMS[1].copy(number = -1)),
            listOf(TWO_FORMS[0], TWO_FORMS[1].copy(number = 0)),
            listOf(TWO_FORMS[0], TWO_FORMS[1].copy(name = "")),
            listOf(TWO_FORMS[0], TWO_FORMS[1].copy(name = "🔒")),
            listOf(TWO_FORMS[0], TWO_FORMS[0])
        ).forEach { entries ->
            assertWait(PcgAltDexRejection.PAGE_UNREADABLE, evaluateLatest { copy(entries = entries) })
        }
    }

    /** Owner's ruling, 2026-11-23: an empty list is refused and nothing is stored. */
    @Test
    fun anEmptyListIsNeverAccepted() {
        val empty = emptyList<PcgDexPageRead.Entry>()

        assertWait(
            PcgAltDexRejection.EMPTY_LIST,
            evaluate(
                read(PREVIOUS, entries = empty, counterText = "0 / 0"),
                read(LATEST, entries = empty, counterText = "0 / 0")
            )
        )
        assertEquals(R.string.pcg_alt_dex_empty_list, PcgAltDexRejection.EMPTY_LIST.messageRes)
    }

    /** A condition the user must change is refused on the latest read alone, without waiting. */
    @Test
    fun aUserSettledConditionIsRefusedWithoutASecondRead() {
        assertReject(
            PcgAltDexRejection.OBTAINED_ON,
            PcgAltDexReadingGate.evaluate(
                read(LATEST, filters = listOf(obtained(checked = true))),
                null,
                NOW
            )
        )
    }

    @Test
    fun onlyTheUsersOwnSettingsAreRefusedAtOnce() {
        assertEquals(
            setOf(
                PcgAltDexRejection.WRONG_TAB,
                PcgAltDexRejection.NOT_ALT_DEX,
                PcgAltDexRejection.OBTAINED_ON,
                PcgAltDexRejection.OTHER_FILTER_ON,
                PcgAltDexRejection.SEARCH_ACTIVE
            ),
            PcgAltDexRejection.entries.filter { it.settledByTheUser }.toSet()
        )
    }

    @Test
    fun eachRejectionHasItsOwnMessage() {
        val messages = PcgAltDexRejection.entries.map { it.messageRes }

        assertEquals(PcgAltDexRejection.entries.size, messages.toSet().size)
        assertTrue(R.string.pcg_alt_dex_updated !in messages)
    }

    private data class Clock(val pageMs: Long, val receivedMs: Long)

    private fun read(
        clock: Clock,
        entries: List<PcgDexPageRead.Entry> = TWO_FORMS,
        counterText: String = "0 / ${entries.size}",
        filters: List<PcgDexPageRead.Filter> = listOf(obtained())
    ) = PcgDexPageRead(
        dexLabelFound = true,
        dexLabel = "Alt Dex",
        dexLabelActive = true,
        filters = filters,
        searchFound = true,
        searchEmpty = true,
        counterText = counterText,
        entryCount = entries.size,
        entries = entries,
        readAtPageMs = clock.pageMs,
        receivedAtMs = clock.receivedMs
    )

    private fun evaluate(previous: PcgDexPageRead, latest: PcgDexPageRead): PcgAltDexGateDecision {
        return PcgAltDexReadingGate.evaluate(latest, previous, NOW)
    }

    /** Both reads changed the same way, so only the changed condition can fail. */
    private fun evaluateLatest(change: PcgDexPageRead.() -> PcgDexPageRead): PcgAltDexGateDecision {
        return evaluate(read(PREVIOUS).change(), read(LATEST).change())
    }

    private fun assertWait(reason: PcgAltDexRejection, decision: PcgAltDexGateDecision) {
        assertEquals(PcgAltDexGateDecision.Wait(reason), decision)
    }

    private fun assertReject(reason: PcgAltDexRejection, decision: PcgAltDexGateDecision) {
        assertEquals(PcgAltDexGateDecision.Reject(reason), decision)
    }

    private companion object {
        val PREVIOUS = Clock(pageMs = 1_000L, receivedMs = 10_000L)
        val LATEST = Clock(pageMs = 2_000L, receivedMs = 11_000L)
        const val LATEST_RECEIVED = 11_000L
        const val NOW = 11_100L

        fun entry(name: String, number: Int) =
            PcgDexPageRead.Entry(name = name, number = number, grayscale = true)

        fun obtained(checked: Boolean = false) =
            PcgDexPageRead.Filter(name = "Obtained", hasCheckbox = true, checked = checked)

        val TWO_FORMS = listOf(entry("Unown (B)", 10510), entry("Alo Vulpix", 10103))
    }
}
