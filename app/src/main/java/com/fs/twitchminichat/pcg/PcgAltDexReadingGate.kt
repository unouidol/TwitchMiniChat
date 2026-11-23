package com.fs.twitchminichat.pcg

import androidx.annotation.StringRes
import com.fs.twitchminichat.PcgAltDexForm
import com.fs.twitchminichat.R
import com.fs.twitchminichat.pcg.catalog.PcgPokemonNameNormalizer

/**
 * Why an "Update Alt Dex" tap took no reading, each with its own message.
 *
 * @property settledByTheUser true when only the user can change it (a filter, the search box,
 * the dex shown, the tab): the tap is refused at once. False when the page may still be
 * rendering: the tap waits for the page until its window ends, and then reports this.
 */
enum class PcgAltDexRejection(
    @param:StringRes @get:StringRes val messageRes: Int,
    val settledByTheUser: Boolean
) {
    /** The scout does not confirm the Pokédex tab. */
    WRONG_TAB(R.string.pcg_alt_dex_wrong_tab, settledByTheUser = true),

    /** The dex label does not read "Alt Dex" shown active. */
    NOT_ALT_DEX(R.string.pcg_alt_dex_not_alt_dex, settledByTheUser = true),

    /** The live "Obtained" checkbox is checked. */
    OBTAINED_ON(R.string.pcg_alt_dex_obtained_on, settledByTheUser = true),

    /** Another filter checkbox is checked. The alt dex showed none on 2026-11-23. */
    OTHER_FILTER_ON(R.string.pcg_alt_dex_other_filter_on, settledByTheUser = true),

    /** The search box holds text, so the list may show only part of the missing forms. */
    SEARCH_ACTIVE(R.string.pcg_alt_dex_search_active, settledByTheUser = true),

    /**
     * The list is empty. It cannot be told from a page not yet loaded, and store 3 cannot undo
     * a wrong "obtained", so it is refused and nothing is stored (owner's ruling, 2026-11-23).
     * Not settled by the user: a page still loading may yet list its forms, so the tap waits
     * for its window and is refused only if the list is still empty when the window ends.
     */
    EMPTY_LIST(R.string.pcg_alt_dex_empty_list, settledByTheUser = false),

    /** The counter's obtained number is not 0. */
    COUNTER_OBTAINED_NOT_ZERO(R.string.pcg_alt_dex_counter_obtained, settledByTheUser = false),

    /** The number of entries differs from the counter's shown number. */
    COUNT_MISMATCH(R.string.pcg_alt_dex_count_mismatch, settledByTheUser = false),

    /** An entry's sprite is not grayscale, so the list shows a form as obtained. */
    NOT_ALL_GRAYSCALE(R.string.pcg_alt_dex_not_all_grayscale, settledByTheUser = false),

    /** No "Obtained" checkbox, no counter, an unnamed or unnumbered entry, or a repeated one. */
    PAGE_UNREADABLE(R.string.pcg_alt_dex_page_unreadable, settledByTheUser = false),

    /** Two reads one second apart differed. */
    UNSTABLE(R.string.pcg_alt_dex_unstable, settledByTheUser = false),

    /** No fresh read of the page, or no second one to compare it with, in time. */
    NO_READ(R.string.pcg_alt_dex_read_timeout, settledByTheUser = false)
}

/** What [PcgAltDexReadingGate] decided for the latest reads. */
sealed interface PcgAltDexGateDecision {

    /** Every check held: [forms] are the forms not obtained, in page order. */
    data class Accept(val forms: List<PcgAltDexForm>) : PcgAltDexGateDecision

    /** Refused now, with [reason]'s message. */
    data class Reject(val reason: PcgAltDexRejection) : PcgAltDexGateDecision

    /** Not yet decidable; [reason] is reported if the tap's window ends first. */
    data class Wait(val reason: PcgAltDexRejection) : PcgAltDexGateDecision
}

/**
 * The acceptance checks of reading 3, "Update Alt Dex" (`docs/design-pcg-dex-readings.md`).
 *
 * Every check must hold on the latest read:
 *
 * - the dex label reads "Alt Dex" and is active;
 * - the live "Obtained" checkbox exists and is off, and no other filter is on;
 * - the search box, when the page has one, is empty;
 * - the counter reads "obtained / shown", and its obtained number is 0;
 * - the count of entries equals the counter's shown number;
 * - every entry is grayscale, and has a name and a sprite number, with no name twice;
 * - the list is not empty;
 *
 * and the read before it, at least a second earlier by the page's clock and at most three,
 * read exactly the same page. The Pokédex tab itself is confirmed by the scout, which the
 * caller checks first.
 *
 * The checks only read; nothing here sends a chat command or changes the page.
 */
internal object PcgAltDexReadingGate {

    /** How long a read stays the page's current state, by Android's clock. */
    const val READ_MAX_AGE_MS = 3_000L

    /**
     * The shortest spacing of the two compared reads, by the page's clock.
     *
     * The script waits a full second after each read before the next, so consecutive reads
     * are at least 1000 ms apart; the 50 ms margin only absorbs the page clock's rounding.
     */
    const val PAIR_MIN_SPACING_MS = 950L

    /** The longest spacing of the two compared reads, by the page's clock. */
    const val PAIR_MAX_SPACING_MS = 3_000L

    /** Decides on [latest] and the read before it, [previous], at [nowMs] (elapsed realtime). */
    fun evaluate(
        latest: PcgDexPageRead?,
        previous: PcgDexPageRead?,
        nowMs: Long
    ): PcgAltDexGateDecision {
        if (latest == null || !isFresh(latest, nowMs)) {
            return PcgAltDexGateDecision.Wait(PcgAltDexRejection.NO_READ)
        }

        val forms = when (val single = checkOne(latest)) {
            is SingleRead.Refused -> return if (single.reason.settledByTheUser) {
                PcgAltDexGateDecision.Reject(single.reason)
            } else {
                PcgAltDexGateDecision.Wait(single.reason)
            }

            is SingleRead.Passed -> single.forms
        }

        if (previous == null || !isFresh(previous, nowMs)) {
            return PcgAltDexGateDecision.Wait(PcgAltDexRejection.NO_READ)
        }

        val spacingMs = latest.readAtPageMs - previous.readAtPageMs
        if (spacingMs !in PAIR_MIN_SPACING_MS..PAIR_MAX_SPACING_MS) {
            return PcgAltDexGateDecision.Wait(PcgAltDexRejection.NO_READ)
        }

        if (!latest.showsTheSamePageAs(previous)) {
            return PcgAltDexGateDecision.Wait(PcgAltDexRejection.UNSTABLE)
        }

        return PcgAltDexGateDecision.Accept(forms)
    }

    private sealed interface SingleRead {
        data class Passed(val forms: List<PcgAltDexForm>) : SingleRead
        data class Refused(val reason: PcgAltDexRejection) : SingleRead
    }

    private fun checkOne(read: PcgDexPageRead): SingleRead {
        if (!read.isAltDex) return SingleRead.Refused(PcgAltDexRejection.NOT_ALT_DEX)

        val obtainedFilter = read.filters.firstOrNull { filter ->
            PcgPokemonNameNormalizer.normalize(filter.name) == OBTAINED_FILTER_KEY
        }
        if (obtainedFilter == null || !obtainedFilter.hasCheckbox) {
            return SingleRead.Refused(PcgAltDexRejection.PAGE_UNREADABLE)
        }
        if (obtainedFilter.checked) return SingleRead.Refused(PcgAltDexRejection.OBTAINED_ON)
        if (read.filters.any { filter -> filter !== obtainedFilter && filter.checked }) {
            return SingleRead.Refused(PcgAltDexRejection.OTHER_FILTER_ON)
        }

        if (read.searchFound && !read.searchEmpty) {
            return SingleRead.Refused(PcgAltDexRejection.SEARCH_ACTIVE)
        }

        val counter = read.counter ?: return SingleRead.Refused(PcgAltDexRejection.PAGE_UNREADABLE)
        if (counter.obtained != 0) {
            return SingleRead.Refused(PcgAltDexRejection.COUNTER_OBTAINED_NOT_ZERO)
        }

        val entries = read.entries ?: return SingleRead.Refused(PcgAltDexRejection.PAGE_UNREADABLE)
        if (entries.size != counter.shown || entries.size != read.entryCount) {
            return SingleRead.Refused(PcgAltDexRejection.COUNT_MISMATCH)
        }
        if (entries.any { entry -> !entry.grayscale }) {
            return SingleRead.Refused(PcgAltDexRejection.NOT_ALL_GRAYSCALE)
        }

        val forms = entries.map { entry ->
            if (entry.number <= 0) return SingleRead.Refused(PcgAltDexRejection.PAGE_UNREADABLE)
            PcgAltDexKnownForms.formOf(rawName = entry.name, number = entry.number)
                ?: return SingleRead.Refused(PcgAltDexRejection.PAGE_UNREADABLE)
        }
        if (forms.mapTo(HashSet(), PcgAltDexForm::key).size != forms.size) {
            return SingleRead.Refused(PcgAltDexRejection.PAGE_UNREADABLE)
        }

        if (forms.isEmpty()) return SingleRead.Refused(PcgAltDexRejection.EMPTY_LIST)

        return SingleRead.Passed(forms)
    }

    private fun isFresh(read: PcgDexPageRead, nowMs: Long): Boolean {
        return nowMs - read.receivedAtMs in 0L..READ_MAX_AGE_MS
    }

    private const val OBTAINED_FILTER_KEY = "obtained"
}
