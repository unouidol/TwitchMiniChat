package com.fs.twitchminichat

import kotlin.math.abs

/** Represents one locally rendered message awaiting Twitch confirmation. */
data class PendingOutgoingChatMessage(
    val localId: String,
    val channel: String,
    val username: String,
    val message: String,
    val sentAtSec: Double,
    val transportConfirmed: Boolean = false
)

/** Tracks local chat echoes until USERSTATE or canonical history confirms them. */
class OutgoingChatMessageTracker(
    private val canonicalMatchWindowSec: Double = 45.0,
    private val userStateConfirmWindowSec: Double = 10.0,
    private val maximumPendingMessages: Int = 50
) {
    private val pendingById = LinkedHashMap<String, PendingOutgoingChatMessage>()
    private var nextSequence = 0L

    init {
        require(canonicalMatchWindowSec > 0.0) {
            "canonicalMatchWindowSec must be positive"
        }
        require(userStateConfirmWindowSec > 0.0) {
            "userStateConfirmWindowSec must be positive"
        }
        require(maximumPendingMessages > 0) {
            "maximumPendingMessages must be positive"
        }
    }

    /** Registers one successful socket write without retrying the command. */
    @Synchronized
    fun register(
        channel: String,
        username: String,
        message: String,
        sentAtSec: Double
    ): PendingOutgoingChatMessage {
        nextSequence += 1L
        val item = PendingOutgoingChatMessage(
            localId = "pending:${(sentAtSec * 1000.0).toLong()}:$nextSequence",
            channel = normalizeChannel(channel),
            username = normalizeUsername(username),
            message = normalizeMessage(message),
            sentAtSec = sentAtSec
        )

        pendingById[item.localId] = item
        trimToCapacity()
        return item
    }

    /** Marks the oldest recent write as transport-confirmed by Twitch USERSTATE. */
    @Synchronized
    fun confirmOldestFromUserState(
        channel: String,
        confirmedAtSec: Double
    ): PendingOutgoingChatMessage? {
        val normalizedChannel = normalizeChannel(channel)
        val entry = pendingById.entries.firstOrNull { (_, item) ->
            !item.transportConfirmed &&
                    item.channel == normalizedChannel &&
                    confirmedAtSec >= item.sentAtSec &&
                    confirmedAtSec - item.sentAtSec <= userStateConfirmWindowSec
        } ?: return null

        val confirmed = entry.value.copy(transportConfirmed = true)
        pendingById[entry.key] = confirmed
        return confirmed
    }

    /** Removes the best local match when canonical live/history data arrives. */
    @Synchronized
    fun confirmCanonical(
        channel: String,
        username: String,
        message: String,
        messageTimestampSec: Double
    ): PendingOutgoingChatMessage? {
        val normalizedChannel = normalizeChannel(channel)
        val normalizedUsername = normalizeUsername(username)
        val normalizedMessage = normalizeMessage(message)

        val entry = pendingById.entries
            .asSequence()
            .filter { (_, item) ->
                item.channel == normalizedChannel &&
                        item.username == normalizedUsername &&
                        item.message == normalizedMessage &&
                        abs(item.sentAtSec - messageTimestampSec) <= canonicalMatchWindowSec
            }
            .minByOrNull { (_, item) ->
                abs(item.sentAtSec - messageTimestampSec)
            } ?: return null

        pendingById.remove(entry.key)
        return entry.value
    }

    /** Removes the newest unconfirmed write after a Twitch rejection NOTICE. */
    @Synchronized
    fun removeNewestAwaiting(channel: String): PendingOutgoingChatMessage? {
        val normalizedChannel = normalizeChannel(channel)
        val entry = pendingById.entries
            .toList()
            .asReversed()
            .firstOrNull { (_, item) ->
                item.channel == normalizedChannel && !item.transportConfirmed
            } ?: return null

        pendingById.remove(entry.key)
        return entry.value
    }

    /** Removes one local record by identifier. */
    @Synchronized
    fun remove(localId: String): PendingOutgoingChatMessage? {
        return pendingById.remove(localId)
    }

    /** Returns true while a local record may still be reconciled. */
    @Synchronized
    fun contains(localId: String): Boolean {
        return pendingById.containsKey(localId)
    }

    /** Clears every pending record. */
    @Synchronized
    fun clear() {
        pendingById.clear()
    }

    /** Keeps tracker memory bounded without affecting socket behavior. */
    private fun trimToCapacity() {
        while (pendingById.size > maximumPendingMessages) {
            val oldestId = pendingById.keys.firstOrNull() ?: return
            pendingById.remove(oldestId)
        }
    }

    /** Normalizes channel identifiers for deterministic matching. */
    private fun normalizeChannel(value: String): String {
        return value.trim().removePrefix("#").lowercase()
    }

    /** Normalizes Twitch usernames for deterministic matching. */
    private fun normalizeUsername(value: String): String {
        return value.trim().lowercase()
    }

    /** Normalizes normal and /me messages to the same canonical text. */
    private fun normalizeMessage(value: String): String {
        val trimmed = value.trim()

        if (
            trimmed.startsWith("\u0001ACTION ") &&
            trimmed.endsWith("\u0001") &&
            trimmed.length > 8
        ) {
            return trimmed.substring(8, trimmed.length - 1).trim()
        }

        if (trimmed.startsWith("/me ", ignoreCase = true)) {
            return trimmed.substring(4).trim()
        }

        return trimmed
    }
}
