package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterizes when the backend still has to be told about a profile's alert selection.
 *
 * Each case names the wrong implementation it rejects. The two the application actually
 * had are *nothing active is never sent* and *a removed profile owes nothing*.
 */
class PcgProfileAlertPushPolicyTest {

    private val active = PcgProfileAlertSelection(
        spawnSettings = PcgSpawnAlertSettings(
            regularMode = PcgSpawnAlertMode.DEX_AND_TIER_A,
            eventSpawnsEnabled = false
        ),
        mostWantedEnabled = false
    )

    private val otherActive = PcgProfileAlertSelection(
        spawnSettings = PcgSpawnAlertSettings(
            regularMode = PcgSpawnAlertMode.ALL_SPAWNS,
            eventSpawnsEnabled = true
        ),
        mostWantedEnabled = true
    )

    /** Nothing to say when the backend has already confirmed exactly this. */
    @Test
    fun `a selection already acknowledged is not sent again`() {
        assertEquals(
            PcgProfileAlertPushDecision.UpToDate,
            PcgProfileAlertPushPolicy.decide(
                local = PcgProfileLocalSelection.Present(active),
                acknowledged = active
            )
        )
    }

    /** A changed selection is sent. */
    @Test
    fun `a changed selection is sent`() {
        assertEquals(
            PcgProfileAlertPushDecision.Push(otherActive),
            PcgProfileAlertPushPolicy.decide(
                local = PcgProfileLocalSelection.Present(otherActive),
                acknowledged = active
            )
        )
    }

    /**
     * Rejects the implementation the application had: one that treats "no category
     * active" as "nothing to say", because it derives the need to send from whether a
     * notification would be delivered. That is why a phone whose alerts were all switched
     * off never told the server, and the server kept sending.
     */
    @Test
    fun `an all-off selection is sent when the backend has not acknowledged it`() {
        assertEquals(
            PcgProfileAlertPushDecision.Push(PcgProfileAlertSelection.DISABLED),
            PcgProfileAlertPushPolicy.decide(
                local = PcgProfileLocalSelection.Present(
                    PcgProfileAlertSelection.DISABLED
                ),
                acknowledged = active
            )
        )
    }

    /**
     * Rejects an implementation that sends the all-off selection at every start once it
     * is the local state, which would be a request per launch for ever.
     */
    @Test
    fun `an all-off selection already acknowledged is not sent again`() {
        assertEquals(
            PcgProfileAlertPushDecision.UpToDate,
            PcgProfileAlertPushPolicy.decide(
                local = PcgProfileLocalSelection.Present(
                    PcgProfileAlertSelection.DISABLED
                ),
                acknowledged = PcgProfileAlertSelection.DISABLED
            )
        )
    }

    /** With nothing acknowledged, the local selection is sent whatever it is. */
    @Test
    fun `a profile with no acknowledgement is sent its local selection`() {
        assertEquals(
            PcgProfileAlertPushDecision.Push(active),
            PcgProfileAlertPushPolicy.decide(
                local = PcgProfileLocalSelection.Present(active),
                acknowledged = null
            )
        )
    }

    /**
     * Rejects the implementation the application had: an account removal whose disable
     * failed left the profile in that device's `profile_ids` for good, because nothing
     * afterwards could tell that anything was still owed.
     */
    @Test
    fun `a removed profile the backend knows as active owes a disable`() {
        assertEquals(
            PcgProfileAlertPushDecision.Push(PcgProfileAlertSelection.DISABLED),
            PcgProfileAlertPushPolicy.decide(
                local = PcgProfileLocalSelection.Removed,
                acknowledged = active
            )
        )
    }

    /** Rejects an implementation that keeps disabling a profile already disabled. */
    @Test
    fun `a removed profile the backend knows as disabled owes nothing`() {
        assertEquals(
            PcgProfileAlertPushDecision.UpToDate,
            PcgProfileAlertPushPolicy.decide(
                local = PcgProfileLocalSelection.Removed,
                acknowledged = PcgProfileAlertSelection.DISABLED
            )
        )
    }

    /**
     * Rejects an implementation that reads a missing acknowledgement as "acknowledged
     * disabled", and one that reads it as "certainly active". Nothing is known about the
     * backend's copy for a profile that has left the phone without ever confirming
     * anything, so no request is made on a guess.
     */
    @Test
    fun `a removed profile with no acknowledgement owes nothing`() {
        assertEquals(
            PcgProfileAlertPushDecision.UpToDate,
            PcgProfileAlertPushPolicy.decide(
                local = PcgProfileLocalSelection.Removed,
                acknowledged = null
            )
        )
    }
}
