package com.fs.twitchminichat.harness

import android.view.View
import com.fs.twitchminichat.AnimatedEmoteRowListener
import com.fs.twitchminichat.ChatRowViewBinder
import com.fs.twitchminichat.EmoteImageSource
import com.fs.twitchminichat.TwitchEmoteImageLoader

/**
 * The one part of the harness that names how production owns emote sessions, so that a change of
 * owner changes the harness here and nowhere else.
 *
 * Since 5c the binding owns them: a bind ends the session of the row the view showed before, an
 * unbind ends the session of a row that leaves, and the page's end releases them all. The version of
 * this object for the ownership before 5c is in this file's history: the instruments are run through
 * it first, where they must fail at the steps 5c's device plan predicts.
 */
internal object EmoteOwnership {

    /** A loader as the page builds one, drawing from [images]. */
    fun newLoader(images: EmoteImageSource, rows: AnimatedEmoteRowListener): TwitchEmoteImageLoader =
        TwitchEmoteImageLoader(images = images, animatedRows = rows)

    /** Ends what [view] shows, as the timeline does when its row leaves and as a recycled holder will. */
    fun unbind(binder: ChatRowViewBinder, view: View) {
        binder.unbind(view)
    }

    /** Releases every session the loader holds, as the page does in onDestroyView. */
    fun releaseAll(loader: TwitchEmoteImageLoader) {
        loader.releaseAll()
    }
}
