package com.fs.twitchminichat.chat

/**
 * What a chat page shows and holds for one channel, as [ChatChannelBinding.changeTo]
 * resets it. Implemented by the page; every member runs on the main thread.
 */
interface ChannelBoundPage {

    /**
     * Cancels the sends still awaiting their echo and forgets them. Their rows are not
     * removed one by one: [clearTimeline] removes every row.
     */
    fun forgetPendingSends()

    /** Empties the timeline, with the emote images it shows and the emote picker. */
    fun clearTimeline()

    /** Zeroes the unseen count and returns to the bottom of the timeline. */
    fun returnToBottom()

    /** Shows the mention suggestions again, after the binding has reset them. */
    fun showMentionSuggestions()

    /** Selects [channel]'s emote catalog for the composer. */
    fun selectEmoteCatalog(channel: String)

    /** Loads the stream of the channel the page is now on, when the stream is on. */
    fun reloadStream()

    /** Records [channel] among the recent channels offered by the channel field. */
    fun recordRecentChannel(channel: String)

    /** Closes the previous channel's IRC connection. Reconnecting is the caller's. */
    fun closeConnection()
}

/**
 * Which channel one chat page is bound to, and the one answer to "the channel
 * changed": [changeTo].
 *
 * A page changes channel in two ways. The user joins one from the channel field, and
 * the page calls [changeTo] directly. Or the account's channel is changed from outside
 * the page while it is stopped, and the next start finds it: the page calls
 * [onStarted], which calls [changeTo] when the channel differs. Both therefore do
 * exactly the same thing. What only one of them does - the join writes the account and
 * says so in the timeline, the start writes its lifecycle line - is the caller's, around
 * the call, and is documented there.
 *
 * Neither reconnects: the join connects right after, and a start connects as every
 * start does.
 */
class ChatChannelBinding(
    private val backfillState: HistoryBackfillState,
    private val deduplicator: ChatMessageDeduplicator,
    private val mentionUsers: ChatMentionUserTracker,
    private val authenticatedUsername: () -> String?,
    private val page: ChannelBoundPage
) {

    /* The channel the page is bound to; null until the first start. */
    private var channel: String? = null

    /**
     * Records a start on [channel]. The first start only binds it. A later one on another
     * channel - compared without case, as the account stores it - changes to it. Main
     * thread.
     */
    fun onStarted(channel: String) {
        val bound = this.channel
        if (bound == null) {
            this.channel = channel
            return
        }
        if (bound.equals(channel, ignoreCase = true)) return

        changeTo(channel)
    }

    /**
     * What a channel change means: nothing the page holds or shows may still belong to
     * the previous channel. Main thread.
     *
     * - **History backfill**: every reference is forgotten, and a result still on its
     *   way from before is discarded when it arrives.
     * - **Duplicate suppression**: forgotten. Its keys carry no channel, so a page that
     *   comes back to a channel would otherwise suppress that channel's history, whose
     *   rows the cleared timeline no longer shows.
     * - **Mention suggestions**: back to the user alone. They held the previous
     *   channel's chatters, each for ten minutes after it last spoke.
     * - **Pending sends**: cancelled and forgotten.
     * - **Timeline**: emptied, with its emote images and the emote picker.
     * - **Unseen count and scroll position**: zero, at the bottom. The count was of
     *   messages the cleared timeline no longer holds.
     * - **Emote catalog**: [channel]'s.
     * - **Stream**: reloaded when it is on. The stream session belongs to the account
     *   and survives a stop, so without this it keeps playing the previous channel.
     * - **Recent channels**: [channel] is recorded, as the page records the account's
     *   channel when it is created.
     * - **IRC connection**: closed.
     */
    fun changeTo(channel: String) {
        backfillState.onChannelChanged()
        deduplicator.clear()
        mentionUsers.reset(authenticatedUsername = authenticatedUsername())

        page.forgetPendingSends()
        page.clearTimeline()
        page.returnToBottom()
        page.showMentionSuggestions()
        page.selectEmoteCatalog(channel)
        page.reloadStream()
        page.recordRecentChannel(channel)
        page.closeConnection()

        this.channel = channel
    }
}
