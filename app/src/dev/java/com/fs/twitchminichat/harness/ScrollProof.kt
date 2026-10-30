package com.fs.twitchminichat.harness

import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

/**
 * The scroll proof (5d): the page's own following of new rows, on a list attached to the window at a
 * fixed height, with the rows coming through appendChatLine as live chat and history deliver them, the
 * page's own scroll listener registered on the list, and its own scrollToBottom posting to it.
 *
 * - S1: stuck at the bottom, a row arrives while the list still has a newer row it has not laid out:
 *   the layout between them reports a "scroll" with no movement, and the page must keep following.
 *   Then the newest row ends at the list's bottom edge.
 * - S2: scrolled up, a row arrives: the first visible row stays where it is, and one unseen row is
 *   counted.
 * - S3: scrolled up, a row arrives that sorts above the first visible row, as history can: the first
 *   visible row keeps its place on screen. This is a behaviour 5d changes on purpose, reasoned from
 *   LinearLayoutManager's anchoring - the ScrollView moved every row below an insert down - and this is
 *   where it is seen.
 */
internal class ScrollProof(
    private val root: FrameLayout,
    private val page: ProductionRowViews
) {

    private val steps = ProofSteps(EXPECTED_STEPS)
    private lateinit var list: RecyclerView
    private var heightPx = 0
    private var rows = 0

    /** Runs S1 to S3, one phase per turn of the main loop, and gives the report to [done]. */
    fun start(done: (JSONObject) -> Unit) {
        addSteps()
        steps.start { report -> done(report.put("listHeightPx", heightPx).put("rows", rows)) }
    }

    private fun addSteps() {
        steps.add("S1 stuck at the bottom: a layout with a newer row pending keeps following") { r ->
            /* The list's reports with no movement during the one layout this step is about, and whether the newest row had a view at each. */
            var reporting = false
            var stillReports = 0
            var stillReportsWithoutTheNewestRow = 0
            listOf(
                {
                    heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, LIST_HEIGHT_DP, root.resources.displayMetrics).toInt()
                    list = page.newList()
                    root.addView(list, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx))
                    page.newTimeline(list)
                    page.followScrolls(list)
                    list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            if (!reporting || dx != 0 || dy != 0) return
                            stillReports++
                            if (newestBottomGap() == Int.MAX_VALUE) stillReportsWithoutTheNewestRow++
                        }
                    })
                    repeat(FIRST_ROWS) { append() }
                },
                {
                    /* Each append's scrollToBottom has run; the list scrolls at this layout. */
                    page.layOut(list, "S1 filled", heightPx)
                    r.pre("filledPastItsHeight", list.childCount < rows)
                    r.pre("stuck", page.stickToBottom)
                    r.pre("newestAtTheBottomEdge", newestBottomGap() == 0)
                },
                {
                    /*
                     * Within one turn, so that no frame can lay the list out in between: a row arrives, the
                     * list is asked to show it - what that row's posted scrollToBottom does when it runs -
                     * and a second row arrives before the list lays out. That layout brings the first to the
                     * bottom edge with the second still below it, viewless, and reports it as a scroll with
                     * no movement: the moment history's rows arrive faster than the list lays them out.
                     */
                    append()
                    list.scrollToPosition((list.adapter?.itemCount ?: 0) - 1)
                    append()
                    reporting = true
                    page.layOut(list, "S1 layout with a newer row pending", heightPx)
                    reporting = false
                    r.check("theLayoutReportedAStillScrollWhileTheNewestRowHadNoView", stillReportsWithoutTheNewestRow > 0)
                    r.observe("stillReports", stillReports)
                    r.check("stillStuck", page.stickToBottom)
                },
                {
                    page.layOut(list, "S1 settled", heightPx)
                    r.check("newestAtTheBottomEdge", newestBottomGap() == 0)
                    r.check("stuck", page.stickToBottom)
                    r.check("noUnseenRow", page.unseenMessages == 0)
                }
            )
        }

        steps.add("S2 scrolled up: an arriving row moves nothing and counts as unseen") { r ->
            var firstPosition = -1
            var firstTop = 0
            listOf(
                {
                    list.scrollBy(0, -SCROLL_UP_PX)
                    page.checkList(list, "S2 scrolled up")
                    r.pre("notStuck", !page.stickToBottom)
                    r.pre("noUnseenRow", page.unseenMessages == 0)
                    val first = list.getChildAt(0)
                    firstPosition = list.getChildViewHolder(first).bindingAdapterPosition
                    firstTop = first.top
                    append()
                },
                {
                    page.layOut(list, "S2 a row arrived", heightPx)
                    val first = list.getChildAt(0)
                    r.check("firstVisibleRowUnchanged", list.getChildViewHolder(first).bindingAdapterPosition == firstPosition)
                    r.check("firstVisibleRowNotMoved", first.top == firstTop)
                    r.check("oneUnseenRow", page.unseenMessages == 1)
                    r.check("stillNotStuck", !page.stickToBottom)
                }
            )
        }

        steps.add("S3 scrolled up: a row sorting above the first visible one leaves it in place") { r ->
            var firstRow: Any? = null
            var firstTop = 0
            listOf(
                {
                    r.pre("notStuck", !page.stickToBottom)
                    val first = list.getChildAt(0)
                    val position = list.getChildViewHolder(first).bindingAdapterPosition
                    firstRow = first.tag
                    firstTop = first.top
                    /* Just before the first visible row: history arriving after it, as it can. */
                    append(timestampSec = BASE_SEC + position - 0.5)
                },
                {
                    page.layOut(list, "S3 a row arrived above", heightPx)
                    val kept = (0 until list.childCount).map(list::getChildAt).firstOrNull { child -> child.tag === firstRow }
                    r.check("firstVisibleRowStillAttached", kept != null)
                    r.check("firstVisibleRowKeptItsPlace", kept?.top == firstTop)
                    r.observe("firstVisibleRowTopBeforeAndAfter", "$firstTop/${kept?.top}")
                    r.check("twoUnseenRows", page.unseenMessages == 2)
                }
            )
        }
    }

    /* One row through appendChatLine, newer than every row so far unless [timestampSec] says otherwise. */
    private fun append(timestampSec: Double = BASE_SEC + rows) {
        val n = rows++
        page.chatMessage(
            user = "parity_scroll",
            message = "scroll row $n",
            emotesRaw = null,
            dedupKey = "id:parity-scroll-$n",
            replyParentUserLogin = null,
            messageTimestampSec = timestampSec
        )
    }

    /* How far the newest row's bottom is from the list's bottom edge; Int.MAX_VALUE when it has no view. */
    private fun newestBottomGap(): Int {
        val count = list.adapter?.itemCount ?: 0
        val newest = list.findViewHolderForAdapterPosition(count - 1)?.itemView ?: return Int.MAX_VALUE
        return newest.bottom - (list.height - list.paddingBottom)
    }

    companion object {
        /** S1 to S3. */
        const val EXPECTED_STEPS = 3

        private const val FIRST_ROWS = 30
        private const val LIST_HEIGHT_DP = 160f
        private const val SCROLL_UP_PX = 600
        private const val BASE_SEC = 1_791_000_500.0
    }
}
