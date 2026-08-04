package com.fs.twitchminichat.chat

/**
 * Tracks channel-scoped usernames offered by the chat mention picker.
 *
 * Usernames are matched case-insensitively, retain their latest display casing,
 * and expire after inactivity. The authenticated user remains available until
 * [reset] clears the candidates for a channel change.
 */
class ChatMentionUserTracker(
    private val inactivityTimeoutMs: Long = DEFAULT_INACTIVITY_TIMEOUT_MS,
    private val monotonicTimeMillis: () -> Long = {
        System.nanoTime() / NANOSECONDS_PER_MILLISECOND
    }
) {
    private data class Entry(
        var displayName: String,
        var lastSeenAtMs: Long
    )

    private val entries = LinkedHashMap<String, Entry>()

    init {
        require(inactivityTimeoutMs > 0L) {
            "inactivityTimeoutMs must be positive"
        }
    }

    /** Records [username] as a mention candidate and returns whether it was non-blank. */
    fun record(username: String, authenticatedUsername: String?): Boolean {
        val displayName = username.trim()
        if (displayName.isBlank()) return false

        val nowMs = monotonicTimeMillis()
        val key = normalize(displayName)
        val existing = entries[key]

        if (existing == null) {
            entries[key] = Entry(
                displayName = displayName,
                lastSeenAtMs = nowMs
            )
        } else {
            existing.displayName = displayName
            existing.lastSeenAtMs = nowMs
        }

        prune(authenticatedUsername = authenticatedUsername, nowMs = nowMs)
        return true
    }

    /** Clears previous-channel candidates and seeds the authenticated user. */
    fun reset(authenticatedUsername: String?) {
        entries.clear()

        val displayName = authenticatedUsername?.trim().orEmpty()
        if (displayName.isBlank()) return

        entries[normalize(displayName)] = Entry(
            displayName = displayName,
            lastSeenAtMs = monotonicTimeMillis()
        )
    }

    /** Prunes inactive entries and returns display names in case-insensitive order. */
    fun activeDisplayNames(authenticatedUsername: String?): List<String> {
        prune(
            authenticatedUsername = authenticatedUsername,
            nowMs = monotonicTimeMillis()
        )

        return entries.values
            .map { it.displayName }
            .sortedBy { it.lowercase() }
    }

    /** Removes inactive users while preserving the authenticated user. */
    private fun prune(authenticatedUsername: String?, nowMs: Long) {
        val authenticatedUserKey = normalize(authenticatedUsername)
        val iterator = entries.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val isAuthenticatedUser = entry.key == authenticatedUserKey
            val expired = nowMs - entry.value.lastSeenAtMs > inactivityTimeoutMs

            if (!isAuthenticatedUser && expired) {
                iterator.remove()
            }
        }
    }

    /** Normalizes a username for case-insensitive identity comparisons. */
    private fun normalize(user: String?): String {
        return user?.trim()?.lowercase().orEmpty()
    }

    private companion object {
        /** Existing inactivity window for mention suggestions. */
        const val DEFAULT_INACTIVITY_TIMEOUT_MS = 10 * 60 * 1_000L

        /** Converts Java monotonic nanoseconds to milliseconds. */
        const val NANOSECONDS_PER_MILLISECOND = 1_000_000L
    }
}
