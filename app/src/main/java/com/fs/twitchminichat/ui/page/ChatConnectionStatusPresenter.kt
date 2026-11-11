package com.fs.twitchminichat.ui.page

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.fs.twitchminichat.R

/**
 * Shows the chat connection's state in the channel bar, variant B of the 5d layout proposal: an
 * indicator, then the user name and the channel on two short lines. The states that carry an
 * instruction or an error keep their words, in a row of their own below the bar.
 *
 * PROTOTYPE (branch `prototype/5d-layout-b`), not for merging.
 *
 * The indicator is never colour alone: each state has its own shape, and its own content
 * description in words. The cluster is read as one item, with today's status sentence, so a
 * screen reader hears what the status line said before.
 */
class ChatConnectionStatusPresenter(
    private val cluster: View,
    private val indicator: ImageView,
    private val userText: TextView,
    private val channelText: TextView,
    private val problemRow: TextView
) {

    /** The four states, each with its own shape and its words. */
    enum class State(@param:DrawableRes val icon: Int, @param:StringRes val description: Int) {
        /** A filled circle. */
        CONNECTED(R.drawable.ic_status_connected, R.string.status_state_connected),

        /** An open ring: loading or connecting. */
        IN_PROGRESS(R.drawable.ic_status_in_progress, R.string.status_state_in_progress),

        /** A hollow circle with a bar. */
        DISCONNECTED(R.drawable.ic_status_disconnected, R.string.status_state_disconnected),

        /** A triangle with an exclamation mark; its words are shown in the problem row. */
        PROBLEM(R.drawable.ic_status_problem, R.string.status_state_problem)
    }

    private var state = State.DISCONNECTED
    private var user: String? = null
    private var channel: String? = null

    /** Today's sentence for the state, as a resource taking the user and the channel; 0 for none. */
    @StringRes
    private var sentence: Int = 0

    /** The problem's own words, shown in the problem row. */
    private var problem: String? = null

    private var demoUser: String? = null
    private var demoChannel: String? = null

    /** Loading [user] on [channel]: the in-progress state. */
    fun showLoading(user: String, channel: String) =
        show(State.IN_PROGRESS, user, channel, R.string.status_loading, null)

    /** Connecting as [user] on [channel]: the in-progress state. */
    fun showConnecting(user: String, channel: String) =
        show(State.IN_PROGRESS, user, channel, R.string.status_connecting, null)

    /** Connected as [user] on [channel]. */
    fun showConnected(user: String, channel: String) =
        show(State.CONNECTED, user, channel, R.string.status_connected, null)

    /** Disconnected; the names are shown when known. */
    fun showDisconnected(user: String?, channel: String?) = show(
        State.DISCONNECTED,
        user,
        channel,
        if (user.isNullOrBlank()) R.string.status_disconnected else R.string.status_disconnected_account,
        null
    )

    /** A problem, with its own words in the problem row; the names are shown when known. */
    fun showProblem(message: String, user: String?, channel: String?) =
        show(State.PROBLEM, user, channel, 0, message)

    /**
     * Display-only names, for the prototype's long-name screenshot: they replace the names on
     * screen and in the description, and change nothing else. Null restores the real ones.
     */
    fun setDemoNames(user: String?, channel: String?) {
        demoUser = user?.takeIf { it.isNotBlank() }
        demoChannel = channel?.takeIf { it.isNotBlank() }
        render()
    }

    private fun show(state: State, user: String?, channel: String?, @StringRes sentence: Int, problem: String?) {
        this.state = state
        this.user = user?.trim()?.takeIf { it.isNotEmpty() }
        this.channel = channel?.trim()?.removePrefix("#")?.takeIf { it.isNotEmpty() }
        this.sentence = sentence
        this.problem = problem
        render()
    }

    private fun render() {
        val res = cluster.resources
        val shownUser = demoUser ?: user
        val shownChannel = demoChannel ?: channel
        val stateWords = res.getString(state.description)

        indicator.setImageResource(state.icon)
        indicator.contentDescription = stateWords

        userText.text = shownUser?.let { res.getString(R.string.status_user_name, it) }
        userText.isVisible = shownUser != null
        channelText.text = shownChannel?.let { res.getString(R.string.status_channel_name, it) }
        channelText.isVisible = shownChannel != null

        cluster.contentDescription = when {
            state == State.PROBLEM -> stateWords
            sentence == R.string.status_disconnected -> res.getString(sentence)
            sentence == R.string.status_disconnected_account -> res.getString(sentence, shownUser.orEmpty())
            sentence != 0 -> res.getString(sentence, shownUser.orEmpty(), shownChannel.orEmpty())
            else -> stateWords
        }

        problemRow.text = problem
        problemRow.isVisible = state == State.PROBLEM && problem != null
    }
}
