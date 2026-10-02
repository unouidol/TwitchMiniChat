package com.fs.twitchminichat

/**
 * Tells a chat page whether it is the pager's current page, and when that may have
 * changed. Implemented by the activity that owns the pager. Main thread.
 *
 * A page cannot learn this from its own lifecycle. The pager resumes only its current
 * page, but keeps the neighbours started and never resumes or pauses one that is never
 * current, so onResume and onPause say nothing about the pages that matter most here.
 */
interface CurrentChatPageSource {

    /** Whether the page of [accountId] is the pager's current page now. */
    fun isCurrentChatPage(accountId: String): Boolean

    /** Calls [listener] whenever the current page may have changed; it asks again. */
    fun addCurrentChatPageListener(listener: () -> Unit)

    /** Stops calling [listener]. */
    fun removeCurrentChatPageListener(listener: () -> Unit)
}
