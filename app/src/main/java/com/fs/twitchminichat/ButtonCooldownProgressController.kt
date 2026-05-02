package com.fs.twitchminichat.ui

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ProgressBar

/**
 * Small UI helper that temporarily disables a button while a horizontal progress
 * bar fills up.
 *
 * This is intentionally UI-only. It does not know anything about PCG, Gecko, the
 * Pokédex probe, snapshots, uploads, or backend state.
 */
class ButtonCooldownProgressController(
    private val button: Button,
    private val progressBar: ProgressBar,
    private val normalText: () -> String,
    private val waitingText: () -> String,
    private val durationMs: Long
) {
    private var animator: ValueAnimator? = null
    private var finishRunnable: Runnable? = null

    /**
     * Starts or restarts the cooldown UI.
     */
    fun start() {
        cancel(reset = false)

        button.isEnabled = false
        button.text = waitingText()

        progressBar.max = 1000
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE

        animator = ValueAnimator.ofInt(0, progressBar.max).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
            start()
        }

        val runnable = Runnable {
            finish()
        }

        finishRunnable = runnable
        button.postDelayed(runnable, durationMs)
    }

    /**
     * Cancels the cooldown. Use this from the Fragment view cleanup to avoid
     * leaving delayed UI work attached to an old view.
     */
    fun cancel(reset: Boolean = true) {
        animator?.cancel()
        animator = null

        finishRunnable?.let { runnable ->
            button.removeCallbacks(runnable)
        }
        finishRunnable = null

        if (reset) {
            button.isEnabled = true
            button.text = normalText()
            progressBar.progress = 0
            progressBar.visibility = View.INVISIBLE
        }
    }

    /**
     * Restores the button after the cooldown window ends.
     */
    private fun finish() {
        animator?.cancel()
        animator = null
        finishRunnable = null

        button.isEnabled = true
        button.text = normalText()

        progressBar.progress = 0
        progressBar.visibility = View.INVISIBLE
    }
}