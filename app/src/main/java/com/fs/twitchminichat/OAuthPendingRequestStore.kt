package com.fs.twitchminichat

import android.content.Context
import androidx.core.content.edit

/**
 * Describes one OAuth request that is waiting for the browser callback.
 *
 * A blank [accountId] identifies a new-account login. A non-blank value identifies
 * an in-place reauthorization of an existing local account.
 */
internal data class OAuthPendingRequest(
    val slot: Int,
    val channel: String,
    val accountId: String,
    val expectedUsername: String,
    val expectedProfileId: String
) {
    /** Returns true when the callback must update an existing account. */
    val isReauthorization: Boolean
        get() = accountId.isNotBlank()
}

/**
 * Stores short-lived OAuth browser request metadata by callback slot.
 *
 * The store keeps the local account identifier outside the OAuth URL so the callback
 * can update exactly one existing row without exposing the identifier to Twitch.
 */
internal class OAuthPendingRequestStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Allocates the first callback slot that is not currently in use. */
    fun allocateSlot(): Int {
        for (slot in MIN_SLOT..MAX_SLOT) {
            if (!prefs.contains(channelKey(slot))) {
                return slot
            }
        }

        return ((System.currentTimeMillis() / 1000L) % FALLBACK_SLOT_MODULO).toInt()
    }

    /** Saves a pending request that will add a new account. */
    fun saveNewAccount(slot: Int, channel: String) {
        save(
            OAuthPendingRequest(
                slot = slot,
                channel = normalizeChannel(channel),
                accountId = "",
                expectedUsername = "",
                expectedProfileId = ""
            )
        )
    }

    /** Saves a pending request that will refresh one existing account in place. */
    fun saveReauthorization(slot: Int, account: AccountConfig, profileId: String) {
        save(
            OAuthPendingRequest(
                slot = slot,
                channel = normalizeChannel(account.channel),
                accountId = account.id.trim(),
                expectedUsername = account.username.trim(),
                expectedProfileId = profileId.trim()
            )
        )
    }

    /** Loads pending metadata while remaining compatible with older channel-only entries. */
    fun load(slot: Int): OAuthPendingRequest? {
        val channel = prefs.getString(channelKey(slot), "")
            .orEmpty()
            .let(::normalizeChannel)

        if (channel.isBlank()) return null

        return OAuthPendingRequest(
            slot = slot,
            channel = channel,
            accountId = prefs.getString(accountIdKey(slot), "").orEmpty().trim(),
            expectedUsername = prefs.getString(expectedUsernameKey(slot), "").orEmpty().trim(),
            expectedProfileId = prefs.getString(expectedProfileIdKey(slot), "").orEmpty().trim()
        )
    }

    /** Removes all local metadata associated with one completed or rejected request. */
    fun clear(slot: Int) {
        prefs.edit {
            remove(channelKey(slot))
            remove(accountIdKey(slot))
            remove(expectedUsernameKey(slot))
            remove(expectedProfileIdKey(slot))
        }
    }

    /** Persists all fields for one pending request atomically. */
    private fun save(request: OAuthPendingRequest) {
        prefs.edit {
            putString(channelKey(request.slot), request.channel)
            putString(accountIdKey(request.slot), request.accountId)
            putString(expectedUsernameKey(request.slot), request.expectedUsername)
            putString(expectedProfileIdKey(request.slot), request.expectedProfileId)
        }
    }

    /** Normalizes a Twitch channel for local storage and callback restoration. */
    private fun normalizeChannel(raw: String): String {
        return raw.trim()
            .removePrefix("#")
            .lowercase()
    }

    /** Builds the legacy-compatible channel key for one slot. */
    private fun channelKey(slot: Int): String = "pending_channel_slot_$slot"

    /** Builds the local account identifier key for one slot. */
    private fun accountIdKey(slot: Int): String = "pending_account_id_slot_$slot"

    /** Builds the expected Twitch username key for one slot. */
    private fun expectedUsernameKey(slot: Int): String = "pending_username_slot_$slot"

    /** Builds the expected backend profile identifier key for one slot. */
    private fun expectedProfileIdKey(slot: Int): String = "pending_profile_id_slot_$slot"

    private companion object {
        /** SharedPreferences file used by the existing OAuth callback flow. */
        const val PREFS_NAME = "oauth_pending"

        /** First normal callback slot. */
        const val MIN_SLOT = 0

        /** Last normal callback slot. */
        const val MAX_SLOT = 99

        /** Modulo used only when every normal slot is occupied. */
        const val FALLBACK_SLOT_MODULO = 100000L
    }
}
