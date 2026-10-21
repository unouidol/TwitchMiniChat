package com.fs.twitchminichat.chat

import java.util.IdentityHashMap

/**
 * Which emote session each chat row's view holds: at most one per view, found by the view's
 * identity. Main thread.
 *
 * Pure: the loader supplies [release], which ends a session, and nothing but this registry calls it.
 * A session ends in exactly one of three ways, each removing it before releasing it, so none is
 * released twice:
 * - its view is bound to another row: [release], before the next session is [put];
 * - its row leaves the timeline, or its view is recycled: [release];
 * - the page's view goes: [releaseAll].
 *
 * Detaching a view from the window ends nothing. A RecyclerView detaches a holder into its cache and
 * may hand the same view back for the same row without binding it again, so a session that ended on
 * detach would leave that view without its emotes.
 */
class EmoteSessions<V : Any, S : Any>(private val release: (S) -> Unit) {

    private val byView = IdentityHashMap<V, S>()

    /** How many sessions are held. */
    val size: Int
        get() = byView.size

    /** Records [session] as the one [view] holds. The caller has released the one it held before. */
    fun put(view: V, session: S) {
        byView[view] = session
    }

    /** Ends the session [view] holds, if it holds one. */
    fun release(view: V) {
        byView.remove(view)?.let(release)
    }

    /** Ends every session held. */
    fun releaseAll() {
        val sessions = byView.values.toList()
        byView.clear()
        sessions.forEach(release)
    }
}
