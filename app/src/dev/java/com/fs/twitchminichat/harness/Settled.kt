package com.fs.twitchminichat.harness

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone

/**
 * The one layout state in which the harness reads or draws a view: settled, meaning laid out, with
 * no layout pending in it or in any descendant that is shown.
 *
 * A view read before a layout it has asked for is not comparable with one read after it. H1 on
 * 2026-11-02 failed twice for that reason and no other. The direct-bind check snapshotted views its
 * own rendering had just laid out against references snapshotted before any layout, and a
 * TextView's first layout attaches DynamicLayout's ChangeWatcher to a Spannable text. The recycler
 * proof drew rows whose delivered images had asked for a layout that had not run. The pairs check
 * was right only by the order of its calls.
 *
 * So the snapshot, the recycler proof's draw and the activity's render all call [require], and an
 * observation of a view in any other state fails at once, naming the view, instead of producing a
 * difference that reads like production's. Nothing here lays a view out: a helper that did would
 * also hide a row that never asks for the layout its content needs, which is production behaviour
 * under test. The caller lays out what it reads, as the frame would: a list through its own layout,
 * a container through render.
 *
 * A GONE view is not held to it: no layout pass lays one out.
 */
internal object Settled {

    /** Fails unless [view] is settled; [what] names the observation in the failure. */
    fun require(view: View, what: String) {
        val unsettled = firstUnsettled(view, view.javaClass.simpleName) ?: return
        error(
            "$what of a view that is not settled: $unsettled. Invariant: the harness reads and draws a " +
                "view only once the layout it asked for has run (Settled)"
        )
    }

    private fun firstUnsettled(view: View, path: String): String? {
        if (view.isGone) return null
        if (!view.isLaidOut) return "$path was never laid out"
        if (view.isLayoutRequested) return "$path has a layout pending"
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                firstUnsettled(view.getChildAt(index), "$path/children[$index]")?.let { return it }
            }
        }
        return null
    }
}
