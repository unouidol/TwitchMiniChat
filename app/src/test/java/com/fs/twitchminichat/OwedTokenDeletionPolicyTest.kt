package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterizes the rule that decides whether an owed token deletion may still run.
 *
 * Each case names the wrong implementation it rejects, because a test that passes
 * against the broken version is worse than no test: it is a claim of cover that is not
 * there.
 */
class OwedTokenDeletionPolicyTest {

    /**
     * Rejects an implementation that never runs the owed work - the state the
     * application was in before this change, where a phone with no accounts kept its
     * token and kept receiving alerts.
     */
    @Test
    fun `owed deletion runs on a phone with no accounts`() {
        assertEquals(
            OwedTokenDeletionDecision.RUN,
            OwedTokenDeletionPolicy.decide(
                tokenDeletionOwed = true,
                signedInAccountCount = 0
            )
        )
    }

    /**
     * Rejects the implementation that breaks a working install: one that acts on the
     * owed record without re-checking whether an account has been signed in since.
     * Deleting the token there cuts the alerts of a registration the user has just
     * recreated, and nothing reports it - the alerts simply never arrive.
     */
    @Test
    fun `owed deletion is void once an account is signed in again`() {
        assertEquals(
            OwedTokenDeletionDecision.VOID_SIGNED_IN,
            OwedTokenDeletionPolicy.decide(
                tokenDeletionOwed = true,
                signedInAccountCount = 1
            )
        )
    }

    /** Several accounts void it just as one does. */
    @Test
    fun `owed deletion is void with more than one account`() {
        assertEquals(
            OwedTokenDeletionDecision.VOID_SIGNED_IN,
            OwedTokenDeletionPolicy.decide(
                tokenDeletionOwed = true,
                signedInAccountCount = 3
            )
        )
    }

    /**
     * Rejects an implementation that deletes the token whenever it finds no accounts,
     * regardless of the record - which would destroy the token of any phone opened
     * before its first sign-in.
     */
    @Test
    fun `nothing happens when no deletion is owed`() {
        assertEquals(
            OwedTokenDeletionDecision.NOTHING_OWED,
            OwedTokenDeletionPolicy.decide(
                tokenDeletionOwed = false,
                signedInAccountCount = 0
            )
        )
    }

    /** Not owed stays not owed whatever the account count says. */
    @Test
    fun `nothing happens when no deletion is owed and accounts exist`() {
        assertEquals(
            OwedTokenDeletionDecision.NOTHING_OWED,
            OwedTokenDeletionPolicy.decide(
                tokenDeletionOwed = false,
                signedInAccountCount = 2
            )
        )
    }
}
