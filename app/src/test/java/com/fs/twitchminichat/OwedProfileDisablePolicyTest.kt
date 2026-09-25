package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterizes what the owed-work queue does with a profile it finds on record.
 *
 * The test that matters is *void once the account is signed in again*: the wrong
 * implementation there switches off alerts the user has just recreated, and the only
 * symptom is notifications that never arrive.
 */
class OwedProfileDisablePolicyTest {

    private val active = PcgProfileAlertSelection(
        spawnSettings = PcgSpawnAlertSettings(
            regularMode = PcgSpawnAlertMode.DEX_AND_TIER_A,
            eventSpawnsEnabled = false
        ),
        mostWantedEnabled = false
    )

    /**
     * Rejects an implementation that acts on the owed record without re-checking whether
     * the account is back. Retrying the disable then silences a registration the user has
     * just recreated, and nothing in the application reports it.
     */
    @Test
    fun `an owed disable is void once the account is signed in again`() {
        assertEquals(
            OwedProfileDisableDecision.VOID_SIGNED_IN,
            OwedProfileDisablePolicy.decide(
                acknowledged = active,
                explicitlyOwed = true,
                local = PcgProfileLocalSelection.Present(active)
            )
        )
    }

    /**
     * Void applies to the acknowledgement route into "owed" as well, not only to the
     * explicit marker - otherwise half the ways of owing a disable would skip the guard.
     */
    @Test
    fun `an acknowledged-active profile signed in again is void without the marker`() {
        assertEquals(
            OwedProfileDisableDecision.VOID_SIGNED_IN,
            OwedProfileDisablePolicy.decide(
                acknowledged = active,
                explicitlyOwed = false,
                local = PcgProfileLocalSelection.Present(
                    PcgProfileAlertSelection.DISABLED
                )
            )
        )
    }

    /** The case the queue exists for: gone from the phone, active on the backend. */
    @Test
    fun `a removed profile the backend knows as active is disabled`() {
        assertEquals(
            OwedProfileDisableDecision.DISABLE,
            OwedProfileDisablePolicy.decide(
                acknowledged = active,
                explicitlyOwed = false,
                local = PcgProfileLocalSelection.Removed
            )
        )
    }

    /**
     * Rejects an implementation that relies on the acknowledgement alone. An installation
     * upgrading into this version has acknowledged nothing, so a removal whose disable
     * failed there would look as if it owed nothing and would never be retried.
     */
    @Test
    fun `a failed attempt is owed even with nothing acknowledged`() {
        assertEquals(
            OwedProfileDisableDecision.DISABLE,
            OwedProfileDisablePolicy.decide(
                acknowledged = null,
                explicitlyOwed = true,
                local = PcgProfileLocalSelection.Removed
            )
        )
    }

    /**
     * Rejects an implementation that disables every profile it happens to find on record.
     * A removed profile the backend never confirmed anything for is left alone: nothing is
     * known about what it holds.
     */
    @Test
    fun `a removed profile with nothing acknowledged and no failed attempt owes nothing`() {
        assertEquals(
            OwedProfileDisableDecision.NOTHING_OWED,
            OwedProfileDisablePolicy.decide(
                acknowledged = null,
                explicitlyOwed = false,
                local = PcgProfileLocalSelection.Removed
            )
        )
    }

    /** Rejects an implementation that keeps disabling a profile already disabled. */
    @Test
    fun `a removed profile the backend knows as disabled owes nothing`() {
        assertEquals(
            OwedProfileDisableDecision.NOTHING_OWED,
            OwedProfileDisablePolicy.decide(
                acknowledged = PcgProfileAlertSelection.DISABLED,
                explicitlyOwed = false,
                local = PcgProfileLocalSelection.Removed
            )
        )
    }

    /**
     * A profile still on the phone with nothing owed is not the queue's business at all -
     * pushing its selection is registration's job, and a queue that also pushed it could
     * race with registration over the same profile.
     */
    @Test
    fun `a present profile with nothing owed is left to registration`() {
        assertEquals(
            OwedProfileDisableDecision.NOTHING_OWED,
            OwedProfileDisablePolicy.decide(
                acknowledged = PcgProfileAlertSelection.DISABLED,
                explicitlyOwed = false,
                local = PcgProfileLocalSelection.Present(active)
            )
        )
    }
}
