package com.fs.twitchminichat.chat

/**
 * Suppresses duplicate chat deliveries without depending on Android UI state.
 *
 * Consecutive fallback keys are suppressed only inside a short time window, while
 * stable Twitch message identifiers remain suppressed until [clear] is called or
 * their bounded cache entry is evicted.
 */
class ChatMessageDeduplicator(
    private val recentDuplicateWindowMs: Long = DEFAULT_RECENT_DUPLICATE_WINDOW_MS,
    private val maximumStableKeys: Int = DEFAULT_MAXIMUM_STABLE_KEYS,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private val stableKeys = LinkedHashMap<String, Unit>(1024, 0.75f, true)
    private var lastKey: String? = null
    private var lastSeenAtMs = 0L

    init {
        require(recentDuplicateWindowMs > 0L) {
            "recentDuplicateWindowMs must be positive"
        }
        require(maximumStableKeys > 0) {
            "maximumStableKeys must be positive"
        }
    }

    /** Returns true when [key] represents a delivery that was already accepted. */
    fun shouldSuppress(key: String): Boolean {
        val nowMs = currentTimeMillis()

        if (
            key == lastKey &&
            nowMs - lastSeenAtMs < recentDuplicateWindowMs
        ) {
            return true
        }

        lastKey = key
        lastSeenAtMs = nowMs

        if (!key.startsWith(STABLE_KEY_PREFIX)) {
            return false
        }

        if (stableKeys.containsKey(key)) {
            return true
        }

        stableKeys[key] = Unit
        trimStableKeysToCapacity()
        return false
    }

    /** Forgets recent and stable keys when the active chat channel changes. */
    fun clear() {
        lastKey = null
        lastSeenAtMs = 0L
        stableKeys.clear()
    }

    /** Keeps the stable-key cache bounded with the same oldest-entry policy as before. */
    private fun trimStableKeysToCapacity() {
        while (stableKeys.size > maximumStableKeys) {
            val iterator = stableKeys.entries.iterator()
            if (!iterator.hasNext()) return
            iterator.next()
            iterator.remove()
        }
    }

    private companion object {
        /** Prefix used by chat rows backed by a stable Twitch message identifier. */
        const val STABLE_KEY_PREFIX = "id:"

        /** Existing short window for suppressing consecutive fallback deliveries. */
        const val DEFAULT_RECENT_DUPLICATE_WINDOW_MS = 1_500L

        /** Existing upper bound for remembered stable Twitch identifiers. */
        const val DEFAULT_MAXIMUM_STABLE_KEYS = 800
    }
}
