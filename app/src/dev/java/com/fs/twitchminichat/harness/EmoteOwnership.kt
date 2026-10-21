package com.fs.twitchminichat.harness

import android.view.View
import com.fs.twitchminichat.AnimatedEmoteRowListener
import com.fs.twitchminichat.ChatRowViewBinder
import com.fs.twitchminichat.EmoteImageSource
import com.fs.twitchminichat.TwitchEmoteImageLoader

/** A lifecycle step this tree cannot take: the proof reports it as not run, never as passed. */
internal class StepNotRunnable(message: String) : Exception(message)

/**
 * The one part of the harness that names how production owns emote sessions, so that the change of
 * owner changes the harness here and nowhere else.
 *
 * This is the ownership before 5c: the loader keeps each row's session until the row has been
 * detached for a loop turn while its page stays attached, and there is no unbind. The checks that
 * use it are the ones 5c is built against, run first on this ownership, where the proof must fail at
 * the steps it predicts.
 */
internal object EmoteOwnership {

    /** A loader as the page builds one, drawing from [images]; [pageView] is what it calls the page. */
    fun newLoader(images: EmoteImageSource, pageView: View, rows: AnimatedEmoteRowListener): TwitchEmoteImageLoader =
        TwitchEmoteImageLoader(images = images, chatPageView = pageView, animatedRows = rows)

    /** Ends what [view] shows, as a recycled holder will. There is no such call before 5c. */
    @Suppress("UNUSED_PARAMETER")
    fun unbind(binder: ChatRowViewBinder, view: View) {
        throw StepNotRunnable("ChatRowViewBinder.unbind does not exist on this tree")
    }

    /** Releases every session the loader holds, as the page does in onDestroyView. */
    fun releaseAll(loader: TwitchEmoteImageLoader) {
        loader.clearAll()
    }
}
