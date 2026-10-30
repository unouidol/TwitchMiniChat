package com.fs.twitchminichat.harness

import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.RecyclerView
import com.fs.twitchminichat.ChatTimelineController
import com.fs.twitchminichat.ChatTimelinePosition
import com.fs.twitchminichat.PendingEchoView
import com.fs.twitchminichat.chat.ChatMessageRow
import com.fs.twitchminichat.chat.PendingEchoEvent
import com.fs.twitchminichat.chat.PendingEchoRow
import com.fs.twitchminichat.chat.PendingEchoStatus
import org.json.JSONObject

/**
 * The recycler proof (5d): a real RecyclerView, attached to the window at a fixed height, filled by
 * the production timeline and adapter, with images held until the proof delivers them, scrolled by
 * RecyclerView.scrollBy. It makes the path that 5c's defect needed run, and shows what the list does
 * on it.
 *
 * Why scrollBy: it lays out and recycles synchronously and starts no prefetch (RecyclerView 1.3.1:
 * only nestedScrollBy, touch drags and flings post to the prefetcher), so a row scrolled just out of
 * view goes to the list's cache - two holders, kept for their positions and handed back without a
 * bind - and reaches the pool, and onViewRecycled, only when it is evicted from the cache.
 *
 * - R1: an emote row in view, its images delivered and running.
 * - R2: scrolled just out of view. Its holder is detached and in the cache (read by reflection), no
 *   pool recycle of it was seen, its loads are live and its animations stopped.
 * - R3: scrolled back. The same holder, with no bind and no new load, its images, the reference's
 *   pixels, running again. This is the path 5c's defect broke: with the release on detach that 5c
 *   removed, the row would come back here without its emotes.
 * - R4: scrolled far enough to evict it. It went through onViewRecycled, and its loads were cleared then.
 * - R5: scrolled back. Bound again from the pool, with new loads, the reference's pixels, running.
 * - R6: an echo with emotes confirmed in view: rebound with the payload, its session kept.
 * - R7: that echo, confirmed, sent through the pool and back: a full bind at a status other than
 *   sending, which 5a-2's precondition would have refused, with the reference's pixels.
 * - R8: the list detached, as the page's view goes: no pool recycle at the detach and every load still
 *   live; then the page's own onDestroyView, which must clear every one (releaseAll).
 *
 * What it cannot show: the prefetcher, which binds holders ahead of time into the cache; touch and
 * fling scrolling; Glide's real timing; ViewPager2's own recycling of pages.
 */
internal class RecyclerProof(
    private val root: FrameLayout,
    private val page: ProductionRowViews,
    private val targets: ReflectiveTargets,
    private val images: FixedEmoteImageSource,
    private val recorder: EmoteRowRecorder,
    private val rowData: Map<String, ChatMessageRow>,
    private val references: Map<String, () -> LinearLayout>,
    private val echoReference: () -> LinearLayout,
    private val render: (ViewGroup) -> Bitmap
) {

    private val steps = ProofSteps(EXPECTED_STEPS)
    private val emoteData = rowData.getValue(EMOTE_ROW)
    private val emotes = ParityCatalogue.emoteCount(emoteData.emotesRaw)
    private val echoEmotes = ParityCatalogue.emoteCount(EmoteLifecycleProof.ECHO_EMOTES)

    private lateinit var list: RecyclerView
    private lateinit var timeline: ChatTimelineController
    private var heightPx = 0

    /* Every holder the list sent to the pool, in order, as its RecyclerListener saw it. */
    private val recycled = mutableListOf<RecyclerView.ViewHolder>()

    private lateinit var emoteHolder: RecyclerView.ViewHolder
    private var scrolledAway = 0
    private var scrolledFar = 0
    private lateinit var echoHolder: RecyclerView.ViewHolder

    /** Runs R1 to R8, one phase per turn of the main loop, and gives the report to [done]. */
    fun start(done: (JSONObject) -> Unit) {
        check(images.holdDeliveries) { "the recycler proof's images must wait to be delivered" }
        addSteps()
        steps.start { report ->
            report.put("listHeightPx", heightPx)
            report.put("scrolledAwayPx", scrolledAway)
            report.put("scrolledFarPx", scrolledFar)
            report.put("poolRecycles", recycled.size)
            done(report)
        }
    }

    private fun addSteps() {
        steps.add("R1 an emote row in view, delivered and running") { r ->
            listOf({
                heightPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, LIST_HEIGHT_DP, root.resources.displayMetrics).toInt()
                list = page.newList()
                root.addView(list, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx))
                timeline = page.newTimeline(list)
                list.addRecyclerListener { holder -> recycled += holder }
                images.label = "R1"
                for (index in 0 until ROWS) {
                    val data = if (index == EMOTE_INDEX) emoteData else rowData.getValue(EmoteLifecycleProof.PLAIN_ROW)
                    timeline.insert(messageTimestampSec = BASE_SEC + index) { position -> data.copy(position = position) }
                }
                page.layOut(list, "R1 insert", heightPx)
                r.pre("attached", list.isAttachedToWindow)
                r.pre("loadsWaiting", images.loadsOf("R1").count { it.pending } == emotes)
                images.deliverPending()
                emoteHolder = checkNotNull(list.findViewHolderForAdapterPosition(EMOTE_INDEX)) { "the emote row has no view" }
                r.pre("rowsBelowTheView", list.childCount < ROWS)
                checkShowing(r, emoteHolder.itemView as TextView, "R1", emotes, references.getValue(EMOTE_ROW))
            })
        }

        steps.add("R2 scrolled just out of view: in the cache, loads live, stopped") { r ->
            listOf({
                r.pre("attached", emoteHolder.itemView.isAttachedToWindow)
                r.pre("running", animatedOf("R1").all { it.isRunning })
                val recycledBefore = recycled.size
                while (emoteHolder.itemView.parent != null && scrolledAway < MAX_SCROLL_PX) {
                    list.scrollBy(0, 1)
                    scrolledAway++
                }
                checkList("R2 scrolled $scrolledAway px")
                r.observe("scrolledPx", scrolledAway)
                r.check("detached", emoteHolder.itemView.parent == null && !emoteHolder.itemView.isAttachedToWindow)
                r.check("inTheCache", cachedHolders().any { it === emoteHolder })
                r.check("noPoolRecycle", recycled.size == recycledBefore && recycled.none { it === emoteHolder })
                r.check("loadsLive", images.loadsOf("R1").count { it.live } == emotes)
                r.check("stopped", animatedOf("R1").none { it.isRunning })
                r.check("releaseNotReported", recorder.releasesOf(emoteHolder.itemView) == 0)
            })
        }

        steps.add("R3 scrolled back: the cached holder, no bind, its images, running") { r ->
            listOf({
                r.pre("inTheCache", cachedHolders().any { it === emoteHolder })
                images.label = "R3"
                list.scrollBy(0, -scrolledAway)
                checkList("R3 scrolled back")
                val back = list.findViewHolderForAdapterPosition(EMOTE_INDEX)
                r.check("theSameHolder", back === emoteHolder)
                r.check("noNewLoad", images.loadsOf("R3").isEmpty())
                checkShowing(r, emoteHolder.itemView as TextView, "R1", emotes, references.getValue(EMOTE_ROW))
                r.check("releaseNotReported", recorder.releasesOf(emoteHolder.itemView) == 0)
            })
        }

        steps.add("R4 scrolled far: evicted to the pool, unbound there") { r ->
            listOf({
                r.pre("loadsLive", images.loadsOf("R1").count { it.live } == emotes)
                while (recycled.none { it === emoteHolder } && scrolledFar < MAX_SCROLL_PX) {
                    list.scrollBy(0, SCROLL_STEP_PX)
                    scrolledFar += SCROLL_STEP_PX
                }
                checkList("R4 scrolled $scrolledFar px")
                r.observe("scrolledPx", scrolledFar)
                r.check("recycledToThePool", recycled.any { it === emoteHolder })
                r.check("notInTheCache", cachedHolders().none { it === emoteHolder })
                r.check("everyLoadCleared", images.loadsOf("R1").all { it.cleared })
                r.check("releaseReportedOnce", recorder.releasesOf(emoteHolder.itemView) == 1)
            })
        }

        steps.add("R5 scrolled back: bound again from the pool, new loads, running") { r ->
            listOf({
                images.label = "R5"
                list.scrollBy(0, -scrolledFar)
                checkList("R5 scrolled back")
                r.pre("loadsWaiting", images.loadsOf("R5").count { it.pending } == emotes)
                images.deliverPending()
                val back = checkNotNull(list.findViewHolderForAdapterPosition(EMOTE_INDEX)) { "the emote row has no view" }
                r.observe("theHolderOfR1", back === emoteHolder)
                checkShowing(r, back.itemView as TextView, "R5", emotes, references.getValue(EMOTE_ROW))
                emoteHolder = back
            })
        }

        steps.add("R6 an echo with emotes confirmed in view: the payload, its session kept") { r ->
            listOf({
                images.label = "R6 elsewhere"
                timeline.insert(messageTimestampSec = BASE_SEC + ECHO_INDEX - 0.5) { position -> echoRow(position, PendingEchoStatus.SENDING) }
                page.layOut(list, "R6 insert", heightPx)
                /* Far from the emote row, so that what R7 binds on its way back is the echo alone. */
                images.label = "R6"
                list.scrollToPosition(ECHO_INDEX)
                page.layOut(list, "R6 scrolled to the echo", heightPx)
                images.deliverPending()
                echoHolder = checkNotNull(list.findViewHolderForAdapterPosition(ECHO_INDEX)) { "the echo has no view" }
                val echo = echoHolder.itemView as PendingEchoView
                r.pre("isTheEcho", (echo.tag as? PendingEchoRow)?.localId == ECHO_LOCAL_ID)
                r.pre("loadsLive", images.loadsOf("R6").count { it.live } == echoEmotes)
                r.pre("sending", echo.alpha < 1f)
                r.check("statusChanged", timeline.applyEchoEvent(ECHO_LOCAL_ID, PendingEchoEvent.USERSTATE))
                page.layOut(list, "R6 confirm", heightPx)
                r.check("sameHolder", list.findViewHolderForAdapterPosition(ECHO_INDEX) === echoHolder)
                r.check("confirmedLook", echo.alpha == 1f)
                r.check("noLoadCleared", images.loadsOf("R6").none { it.cleared })
                r.check("imagesStillInstalled", imagesShownBy(echo.messageView).let { shown ->
                    shown.size == echoEmotes && shown.all { drawable -> images.loadsOf("R6").any { it.delivered === drawable } }
                })
                r.check("running", animatedOf("R6").let { animated -> animated.size == echoEmotes && animated.all { it.isRunning } })
            })
        }

        steps.add("R7 the confirmed echo through the pool and back: a full bind at confirmed") { r ->
            listOf({
                r.pre("confirmed", ((echoHolder.itemView.tag as? PendingEchoRow)?.status) == PendingEchoStatus.CONFIRMED)
                /* Up, away from the echo and short of the emote row; then back down to the echo. */
                images.label = "R7 away"
                var far = 0
                while (recycled.none { it === echoHolder } && far < MAX_SCROLL_PX) {
                    list.scrollBy(0, -SCROLL_STEP_PX)
                    far += SCROLL_STEP_PX
                }
                checkList("R7 scrolled $far px")
                r.observe("scrolledPx", far)
                r.check("recycledToThePool", recycled.any { it === echoHolder })
                r.check("itsLoadsCleared", images.loadsOf("R6").all { it.cleared })
                r.check("theEmoteRowNotReached", images.loadsOf("R7 away").isEmpty())
                images.label = "R7"
                list.scrollBy(0, far)
                checkList("R7 scrolled back")
                r.check("loadsRequested", images.loadsOf("R7").count { it.pending } == echoEmotes)
                images.deliverPending()
                val back = checkNotNull(list.findViewHolderForAdapterPosition(ECHO_INDEX)) { "the echo has no view" }
                val echo = back.itemView as PendingEchoView
                r.check("boundAtConfirmed", (echo.tag as? PendingEchoRow)?.status == PendingEchoStatus.CONFIRMED)
                r.check("confirmedLook", echo.alpha == 1f)
                r.check("imagesShown", imagesShownBy(echo.messageView).size == echoEmotes)
                r.check("running", animatedOf("R7").let { animated -> animated.size == echoEmotes && animated.all { it.isRunning } })
                r.check("pixelsEqualTheReference", rowPixelsEqual(echo, echoReference()))
            })
        }

        steps.add("R8 the page's view goes: nothing recycled at the detach, releaseAll clears every load") { r ->
            listOf({
                val live = images.loads.count { it.live }
                r.pre("loadsLive", live > 0)
                val recycledBefore = recycled.size
                root.removeView(list)
                r.check("detached", !list.isAttachedToWindow)
                r.check("noPoolRecycleAtTheDetach", recycled.size == recycledBefore)
                r.check("everyLoadStillLiveAfterTheDetach", images.loads.count { it.live } == live)
                page.destroyView()
                r.check("everyLoadClearedByOnDestroyView", images.loads.none { it.live })
                r.observe("liveLoadsBeforeTheDetach", live)
            })
        }
    }

    private fun echoRow(position: ChatTimelinePosition, status: PendingEchoStatus) = PendingEchoRow(
        position = position,
        localId = ECHO_LOCAL_ID,
        user = ParityCatalogue.SIGNED_IN,
        messageText = EmoteLifecycleProof.ECHO_TEXT,
        emotesRaw = EmoteLifecycleProof.ECHO_EMOTES,
        replyParentUserLogin = null,
        sentAtSec = BASE_SEC + ECHO_INDEX - 0.5,
        status = status
    )

    /* Whether [view] shows [label]'s images: one image span per emote, each a delivered one, running, and the reference's pixels. */
    private fun checkShowing(r: ProofSteps.Record, view: TextView, label: String, emotes: Int, reference: () -> LinearLayout) {
        val delivered = images.loadsOf(label).filter { it.live }.mapNotNull { it.delivered }
        val shown = imagesShownBy(view)
        r.check("attachedAfter", view.isAttachedToWindow)
        r.check("oneImagePerEmote", shown.size == emotes && delivered.size == emotes)
        r.check("imagesAreTheDeliveredOnes", shown.all { drawable -> delivered.any { it === drawable } })
        r.check("running", animatedOf(label).let { animated -> animated.size == emotes && animated.all { it.isRunning } })
        r.check("pixelsEqualTheReference", rowPixelsEqual(view, reference()))
    }

    /* The holders in the list's cache, read by reflection: RecyclerView.mRecycler.mCachedViews. */
    private fun cachedHolders(): List<RecyclerView.ViewHolder> {
        val recycler = targets.recyclerViewRecycler.get(list)
        @Suppress("UNCHECKED_CAST")
        return (targets.recyclerCachedViews.get(recycler) as List<RecyclerView.ViewHolder>).toList()
    }

    private fun checkList(where: String) {
        page.checkList(list, where)
    }

    private fun animatedOf(label: String): List<FixedAnimatedEmoteDrawable> =
        images.loadsOf(label).mapNotNull { load -> load.delivered as? FixedAnimatedEmoteDrawable }

    private fun imagesShownBy(view: TextView): List<Any> {
        val text = view.text as? Spanned ?: return emptyList()
        return text.getSpans(0, text.length, ImageSpan::class.java).map { span -> span.drawable }
    }

    /* [view], a row in the list, against the reference's only row, each drawn alone at its own size. */
    private fun rowPixelsEqual(view: View, reference: LinearLayout): Boolean {
        render(reference).recycle()
        val left = draw(view)
        val right = draw(reference.getChildAt(0))
        return PixelDiff.of(left, right).identical.also {
            left.recycle()
            right.recycle()
        }
    }

    private fun draw(view: View): Bitmap {
        val bitmap = createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    companion object {
        /** R1 to R8. */
        const val EXPECTED_STEPS = 8

        private const val EMOTE_ROW = "msg.emotes"
        private const val ROWS = 40
        private const val EMOTE_INDEX = 2
        private const val ECHO_INDEX = 30
        private const val ECHO_LOCAL_ID = "parity-recycler-echo"
        private const val LIST_HEIGHT_DP = 160f
        private const val SCROLL_STEP_PX = 8
        private const val MAX_SCROLL_PX = 20_000
        private const val BASE_SEC = 1_791_000_300.0
    }
}
