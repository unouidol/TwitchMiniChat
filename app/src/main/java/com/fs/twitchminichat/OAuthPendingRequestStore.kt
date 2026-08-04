package com.fs.twitchminichat

import android.annotation.SuppressLint
import android.content.Context

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
    val expectedProfileId: String,
    val codeVerifier: String,
    val createdAtEpochMs: Long
) {
    /** Returns true when the callback must update an existing account. */
    val isReauthorization: Boolean
        get() = accountId.isNotBlank()

    /** Prevents the short-lived verifier and account metadata from entering logs. */
    override fun toString(): String {
        return "OAuthPendingRequest(slot=$slot, isReauthorization=$isReauthorization)"
    }
}

/**
 * Stores short-lived OAuth browser request metadata by callback slot.
 *
 * The store keeps the local account identifier outside the OAuth URL so the callback
 * can update exactly one existing row without exposing the identifier to Twitch.
 */
internal class OAuthPendingRequestStore(
    context: Context,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
    private val slotAllocator: OAuthCallbackSlotAllocator = OAuthCallbackSlotAllocator(),
    private val codeVerifierGenerator: OAuthCodeVerifierGenerator = OAuthCodeVerifierGenerator()
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Atomically reserves and saves a request that will add a new account. */
    fun createNewAccount(channel: String): OAuthPendingRequest? {
        return reserve(
            OAuthPendingRequest(
                slot = 0,
                channel = normalizeChannel(channel),
                accountId = "",
                expectedUsername = "",
                expectedProfileId = "",
                codeVerifier = "",
                createdAtEpochMs = 0L
            )
        )
    }

    /** Atomically reserves and saves an in-place account reauthorization request. */
    fun createReauthorization(
        account: AccountConfig,
        profileId: String
    ): OAuthPendingRequest? {
        val accountId = account.id.trim()
        val expectedUsername = account.username.trim()
        val expectedProfileId = profileId.trim()

        if (
            accountId.isBlank() ||
            (expectedUsername.isBlank() && expectedProfileId.isBlank())
        ) {
            return null
        }

        return reserve(
            OAuthPendingRequest(
                slot = 0,
                channel = normalizeChannel(account.channel),
                accountId = accountId,
                expectedUsername = expectedUsername,
                expectedProfileId = expectedProfileId,
                codeVerifier = "",
                createdAtEpochMs = 0L
            )
        )
    }

    /**
     * Atomically removes and returns one fresh request.
     *
     * Clearing before the network exchange makes every local callback one-shot. A
     * failed exchange requires the user to start a new browser login.
     */
    fun consume(slot: Int): OAuthPendingRequest? {
        if (!OAuthFlowSecurityPolicy.isValidAndroidSlot(slot)) return null

        return synchronized(STORE_LOCK) {
            val request = load(slot)
            val consumedDurably = clearInternal(slot)
            if (!consumedDurably) return@synchronized null

            request?.takeIf { pending ->
                OAuthFlowSecurityPolicy.isPendingRequestFresh(
                    createdAtEpochMs = pending.createdAtEpochMs,
                    nowEpochMs = nowEpochMs()
                )
            }
        }
    }

    /** Loads pending metadata without consuming it. Call only while holding [STORE_LOCK]. */
    private fun load(slot: Int): OAuthPendingRequest? {
        val channel = readString(channelKey(slot))
            .let(::normalizeChannel)
        val codeVerifier = readString(codeVerifierKey(slot))

        if (
            channel.isBlank() ||
            !OAuthProofKeyPolicy.isValidCodeVerifier(codeVerifier)
        ) {
            return null
        }

        return OAuthPendingRequest(
            slot = slot,
            channel = channel,
            accountId = readString(accountIdKey(slot)).trim(),
            expectedUsername = readString(expectedUsernameKey(slot)).trim(),
            expectedProfileId = readString(expectedProfileIdKey(slot)).trim(),
            codeVerifier = codeVerifier,
            createdAtEpochMs = readLong(createdAtKey(slot))
        )
    }

    /** Removes all local metadata associated with one completed or rejected request. */
    fun clear(slot: Int) {
        if (!OAuthFlowSecurityPolicy.isValidAndroidSlot(slot)) return

        synchronized(STORE_LOCK) {
            clearInternal(slot)
        }
    }

    /** Reserves a random slot and persists the complete request before opening a browser. */
    private fun reserve(template: OAuthPendingRequest): OAuthPendingRequest? {
        if (template.channel.isBlank()) return null

        return synchronized(STORE_LOCK) {
            pruneExpiredRequests()

            val createdAtEpochMs = nowEpochMs()
            if (createdAtEpochMs <= 0L) return@synchronized null

            val codeVerifier = codeVerifierGenerator.generate()
                ?: return@synchronized null

            val slot = slotAllocator.allocate { candidate ->
                prefs.contains(channelKey(candidate))
            } ?: return@synchronized null

            val request = template.copy(
                slot = slot,
                codeVerifier = codeVerifier,
                createdAtEpochMs = createdAtEpochMs
            )
            if (save(request)) {
                request
            } else {
                clearInternal(slot)
                null
            }
        }
    }

    /** Removes expired and legacy timestamp-free requests before reserving a new slot. */
    private fun pruneExpiredRequests() {
        val now = nowEpochMs()
        val slots = prefs.all.keys
            .asSequence()
            .filter { key -> key.startsWith(CHANNEL_KEY_PREFIX) }
            .mapNotNull { key -> key.removePrefix(CHANNEL_KEY_PREFIX).toIntOrNull() }
            .toList()

        slots.forEach { slot ->
            val request = load(slot)
            if (
                request == null ||
                !OAuthFlowSecurityPolicy.isPendingRequestFresh(
                    createdAtEpochMs = request.createdAtEpochMs,
                    nowEpochMs = now
                )
            ) {
                clearInternal(slot)
            }
        }
    }

    /** Removes one request synchronously while holding [STORE_LOCK]. */
    @SuppressLint("ApplySharedPref")
    private fun clearInternal(slot: Int): Boolean {
        return prefs.edit()
            .remove(channelKey(slot))
            .remove(accountIdKey(slot))
            .remove(expectedUsernameKey(slot))
            .remove(expectedProfileIdKey(slot))
            .remove(codeVerifierKey(slot))
            .remove(createdAtKey(slot))
            .commit()
    }

    /** Persists all fields durably before the browser receives the request slot. */
    @SuppressLint("ApplySharedPref")
    private fun save(request: OAuthPendingRequest): Boolean {
        if (!OAuthProofKeyPolicy.isValidCodeVerifier(request.codeVerifier)) return false

        return prefs.edit()
            .putString(channelKey(request.slot), request.channel)
            .putString(accountIdKey(request.slot), request.accountId)
            .putString(expectedUsernameKey(request.slot), request.expectedUsername)
            .putString(expectedProfileIdKey(request.slot), request.expectedProfileId)
            .putString(codeVerifierKey(request.slot), request.codeVerifier)
            .putLong(createdAtKey(request.slot), request.createdAtEpochMs)
            .commit()
    }

    /** Reads a string preference and treats a corrupt value as missing. */
    private fun readString(key: String): String {
        return try {
            prefs.getString(key, "").orEmpty()
        } catch (_: ClassCastException) {
            ""
        }
    }

    /** Reads a timestamp preference and treats a corrupt value as expired. */
    private fun readLong(key: String): Long {
        return try {
            prefs.getLong(key, 0L)
        } catch (_: ClassCastException) {
            0L
        }
    }

    /** Normalizes a Twitch channel for local storage and callback restoration. */
    private fun normalizeChannel(raw: String): String {
        return raw.trim()
            .removePrefix("#")
            .lowercase()
    }

    /** Builds the legacy-compatible channel key for one slot. */
    private fun channelKey(slot: Int): String = "$CHANNEL_KEY_PREFIX$slot"

    /** Builds the local account identifier key for one slot. */
    private fun accountIdKey(slot: Int): String = "pending_account_id_slot_$slot"

    /** Builds the expected Twitch username key for one slot. */
    private fun expectedUsernameKey(slot: Int): String = "pending_username_slot_$slot"

    /** Builds the expected backend profile identifier key for one slot. */
    private fun expectedProfileIdKey(slot: Int): String = "pending_profile_id_slot_$slot"

    /** Builds the private Proof Key for Code Exchange verifier key for one slot. */
    private fun codeVerifierKey(slot: Int): String = "pending_code_verifier_slot_$slot"

    /** Builds the creation-time key used to enforce the short callback lifetime. */
    private fun createdAtKey(slot: Int): String = "pending_created_at_slot_$slot"

    private companion object {
        /** SharedPreferences file used by the existing OAuth callback flow. */
        private const val PREFS_NAME = "oauth_pending"

        /** Prefix retained for compatibility with the existing preference file. */
        private const val CHANNEL_KEY_PREFIX = "pending_channel_slot_"

        /** Serializes reservation and one-shot consumption across store instances. */
        private val STORE_LOCK = Any()
    }
}
