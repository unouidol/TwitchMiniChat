package com.fs.twitchminichat.chat

/**
 * The chat page's mention suggestions: each sender recorded as their message arrives, and
 * the list the composer filters filled only when something is about to read it.
 *
 * Filling the list sorts every active user, so filling it once per message cost about as
 * much as building every message's view, while nobody was reading it. The list is read only
 * while an "@" mention is being typed - through the composer's own filtering and the page's -
 * and when the dropdown is sized for it. Each of those reads calls [fillIfStale] first, so
 * what it reads is what a fill at that moment would give.
 *
 * Main thread.
 */
class ChatMentionSuggestions(
    private val users: ChatMentionUserTracker,
    private val authenticatedUsername: () -> String?,
    private val canFill: () -> Boolean,
    private val fill: (List<String>) -> Unit
) {

    /* Whether a sender was recorded since the list was last filled. */
    private var stale = false

    /** Records the sender of a message; the list is filled when it is next read. */
    fun onMessage(sender: String) {
        if (users.record(sender, authenticatedUsername())) stale = true
    }

    /** Fills the list now, from every active user, each after an "@". */
    fun fillNow() {
        if (!canFill()) return
        stale = false
        fill(users.activeDisplayNames(authenticatedUsername()).map { name -> "@$name" })
    }

    /** Fills the list if a sender was recorded since it was last filled. Called before every read. */
    fun fillIfStale() {
        if (stale) fillNow()
    }
}
