package com.fs.twitchminichat.harness

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isEmpty
import com.fs.twitchminichat.ChatTimelineController
import com.fs.twitchminichat.PendingEchoView
import com.fs.twitchminichat.chat.ChatMessageRow
import com.fs.twitchminichat.chat.PendingEchoEvent
import com.fs.twitchminichat.chat.PendingEchoRow
import com.fs.twitchminichat.chat.PendingEchoStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * The lifecycle proof: what happens to a row's emote session as its view is attached, detached,
 * attached again, unbound and bound again, on views attached to the window, with the main loop
 * turning between steps so that anything the loader posts runs before the next step reads.
 *
 * A RecyclerView's cache detaches a holder's view and can hand it back for the same position without
 * binding it again (RecyclerView 1.3.1: the cached holder skips onViewRecycled, and is bound only when
 * it is unbound, updated or invalid). Steps L2 and L3 do the same without a RecyclerView: detach the
 * row, let the loop turn, attach it again without a bind. Whatever the loader does on that round trip
 * shows here.
 *
 * Steps L1 to L6 run once for each emote row of the catalogue, on one view each; L7, L10, L8 and L9
 * run once, in that order.
 *
 * L10 is the path every message the user sends with an emote takes: the echo is confirmed in place
 * by Twitch's USERSTATE, an ECHO_STATUS rebind that must leave its emote session alone
 * (PendingEchoView). No catalogue echo carries emotes - the frozen echo fixes its emote tag to null
 * and the producer reads it from a catalogue a harness page does not have - so L10 builds its own,
 * as L7 does, and is compared with a reference assembled only from frozen parts.
 * Each step first asserts the state it is about to change, so that a check cannot pass because the
 * state was never there. Images come from a [FixedEmoteImageSource] that holds every load until the
 * proof delivers it, and the loader's rows are reported to an [EmoteRowRecorder] that wants every
 * row's animations, so an emote runs exactly while its row is attached.
 *
 * What it cannot show: that a RecyclerView takes the cache path in this app; that 5d calls unbind from
 * onViewRecycled; Glide's own ordering of cached and asynchronous deliveries; that a real GIF resumes
 * on its frame, since the fixed drawable has none; decoding work; ViewPager2 detaching a whole page.
 */
internal class EmoteLifecycleProof(
    private val root: FrameLayout,
    private val page: ProductionRowViews,
    private val images: FixedEmoteImageSource,
    private val recorder: EmoteRowRecorder,
    private val rowData: Map<String, ChatMessageRow>,
    private val references: Map<String, () -> LinearLayout>,
    private val echoReference: () -> LinearLayout,
    private val snapshot: ViewStateSnapshot,
    private val newContainer: () -> LinearLayout,
    private val render: (ViewGroup) -> Bitmap
) {

    /* One step's record: what it asserted before acting, what it checked after, and what threw. */
    private class StepRecord(val name: String, val row: String?) {
        val preconditions = JSONObject()
        val checks = JSONObject()
        var error: String? = null

        fun pre(name: String, holds: Boolean) {
            preconditions.put(name, holds)
        }

        fun check(name: String, holds: Boolean) {
            checks.put(name, holds)
        }

        fun preconditionsHold(): Boolean = preconditions.keys().asSequence().all { key -> preconditions.getBoolean(key) }

        fun passed(): Boolean = error == null && checks.length() > 0 && preconditionsHold() &&
            checks.keys().asSequence().all { key -> checks.getBoolean(key) }

        fun toJson(): JSONObject = JSONObject()
            .put("step", name)
            .put("row", row ?: JSONObject.NULL)
            .put("preconditions", preconditions)
            .put("checks", checks)
            .put("error", error ?: JSONObject.NULL)
            .put("passed", passed())
    }

    /* A step as phases, each run on its own turn of the main loop. */
    private class Step(val record: StepRecord, val phases: List<() -> Unit>)

    private val handler = Handler(Looper.getMainLooper())
    private val binder = page.binder
    private val steps = mutableListOf<Step>()
    private val records = mutableListOf<StepRecord>()

    /** Runs every step, one phase per turn of the main loop, and gives the report to [done]. */
    fun start(done: (JSONObject) -> Unit) {
        check(images.holdDeliveries) { "the lifecycle proof's images must wait to be delivered" }
        ParityCatalogue.emoteRows.forEach { id -> addRowSteps(id) }
        addRemoveEchoStep()
        addConfirmEchoStep()
        addClearStep()
        addReleaseAllStep()
        runNext(0, 0, done)
    }

    private fun runNext(stepIndex: Int, phaseIndex: Int, done: (JSONObject) -> Unit) {
        if (stepIndex == steps.size) {
            done(report())
            return
        }
        val step = steps[stepIndex]
        if (phaseIndex == 0) records += step.record
        if (phaseIndex == step.phases.size || step.record.error != null || !step.record.preconditionsHold()) {
            handler.post { runNext(stepIndex + 1, 0, done) }
            return
        }
        handler.post {
            try {
                step.phases[phaseIndex]()
            } catch (failure: RuntimeException) {
                step.record.error = "${failure.javaClass.name}: ${failure.message}"
            }
            runNext(stepIndex, phaseIndex + 1, done)
        }
    }

    private fun report(): JSONObject {
        val out = JSONObject()
        out.put("steps", JSONArray(records.map { record -> record.toJson() }))
        out.put("expectedSteps", EXPECTED_STEPS)
        out.put("stepsRun", records.size)
        out.put("passed", records.count { record -> record.passed() })
        out.put("failed", JSONArray(records.filter { r -> !r.passed() }.map { r -> "${r.name} ${r.row ?: ""}".trim() }))
        out.put("unclearedLoadsAtTheEnd", images.loads.count { load -> !load.cleared })
        out.put("allPassed", records.size == EXPECTED_STEPS && records.all { record -> record.passed() })
        return out
    }

    /* L1 to L6 for one emote row, on one view attached to the window. */
    private fun addRowSteps(id: String) {
        val data = rowData.getValue(id)
        val emotes = ParityCatalogue.emoteCount(data.emotesRaw)
        val container = newContainer()
        lateinit var view: TextView

        val l1 = StepRecord("L1 bind, attach, deliver", id)
        steps += Step(l1, listOf(
            {
                root.addView(container)
                images.label = "L1 $id"
                view = binder.create(data) as TextView
                container.addView(view)
                l1.pre("attached", view.isAttachedToWindow)
                l1.pre("loadsWaiting", images.loadsOf("L1 $id").count { it.pending } == emotes)
                images.deliverPending()
                checkShowing(l1, view, "L1 $id", emotes, id)
                l1.check("releaseNotReported", recorder.releasesOf(view) == 0)
            }
        ))

        val l2 = StepRecord("L2 detach, one loop turn", id)
        steps += Step(l2, listOf(
            {
                l2.pre("attached", view.isAttachedToWindow)
                l2.pre("running", animatedOf("L1 $id").all { it.isRunning })
                root.removeView(container)
            },
            {
                l2.check("detached", !view.isAttachedToWindow)
                l2.check("noLoadCleared", images.loadsOf("L1 $id").count { it.live } == emotes)
                l2.check("stopped", animatedOf("L1 $id").none { it.isRunning })
                l2.check("releaseNotReported", recorder.releasesOf(view) == 0)
            }
        ))

        val l3 = StepRecord("L3 attach again without a bind, one loop turn", id)
        steps += Step(l3, listOf(
            {
                l3.pre("detached", !view.isAttachedToWindow)
                root.addView(container)
            },
            {
                checkShowing(l3, view, "L1 $id", emotes, id)
                l3.check("releaseNotReported", recorder.releasesOf(view) == 0)
            }
        ))

        val l4 = StepRecord("L4 unbind", id)
        steps += Step(l4, listOf(
            {
                l4.pre("loadsLive", images.loadsOf("L1 $id").count { it.live } == emotes)
                val releasesBefore = recorder.releasesOf(view)
                EmoteOwnership.unbind(binder, view)
                l4.check("everyLoadClearedAtTheCall", images.loadsOf("L1 $id").all { it.cleared })
                l4.check("releaseReportedOnce", recorder.releasesOf(view) == releasesBefore + 1)
                l4.check("noImageInTheText", imagesShownBy(view).isEmpty())
            }
        ))

        val l5 = StepRecord("L5 bind the row again after the unbind, deliver", id)
        steps += Step(l5, listOf(
            {
                l5.pre("noLoadLeft", images.loads.none { load -> load.label.endsWith(" $id") && !load.cleared })
                images.label = "L5 $id"
                binder.bindMessage(view, data)
                l5.pre("loadsWaiting", images.loadsOf("L5 $id").count { it.pending } == emotes)
                images.deliverPending()
                checkShowing(l5, view, "L5 $id", emotes, id)
            }
        ))

        val l6 = StepRecord("L6 bind with loads waiting, bind a plain row, deliver late", id)
        steps += Step(l6, listOf(
            {
                images.label = "L6 $id"
                binder.bindMessage(view, data)
                l6.pre("loadsWaiting", images.loadsOf("L6 $id").count { it.pending } == emotes)
                images.label = "L6 plain $id"
                val plain = rowData.getValue(PLAIN_ROW)
                binder.bindMessage(view, plain)
                l6.check("firstRowsLoadsClearedAtTheSecondBind", images.loadsOf("L6 $id").all { it.cleared })
                images.deliverPending()
                l6.check("noImageInTheText", imagesShownBy(view).isEmpty())
                images.label = "L6 plain fresh $id"
                val fresh = newContainer().also { it.addView(binder.create(plain)) }
                l6.check("equalsThePlainRowFresh", sameAs(container, fresh))
            }
        ))
    }

    /* L7: an echo with emotes, inserted through a real timeline, removed as its Twitch copy arrives. */
    private fun addRemoveEchoStep() {
        val record = StepRecord("L7 removeEcho", ECHO_LOCAL_ID)
        steps += Step(record, listOf(
            {
                val container = newContainer()
                root.addView(container)
                val timeline = ChatTimelineController(container, binder)
                images.label = "L7"
                timeline.insert(messageTimestampSec = ECHO_SENT_AT_SEC) { position ->
                    PendingEchoRow(
                        position = position,
                        localId = ECHO_LOCAL_ID,
                        user = ParityCatalogue.SIGNED_IN,
                        messageText = ECHO_TEXT,
                        emotesRaw = ECHO_EMOTES,
                        replyParentUserLogin = null,
                        sentAtSec = ECHO_SENT_AT_SEC,
                        status = PendingEchoStatus.SENDING
                    )
                }
                images.deliverPending()
                val messageView = (container.getChildAt(0) as PendingEchoView).messageView
                record.pre("loadsLive", images.loadsOf("L7").count { it.live } == ParityCatalogue.emoteCount(ECHO_EMOTES))
                record.pre("attached", messageView.isAttachedToWindow)
                timeline.removeEcho(ECHO_LOCAL_ID)
                record.check("removed", container.isEmpty())
                record.check("everyLoadClearedAtTheCall", images.loadsOf("L7").all { it.cleared })
                record.check("releaseReportedOnce", recorder.releasesOf(messageView) == 1)
            }
        ))
    }

    /*
     * L10: an echo with emotes, inserted through a real timeline, confirmed in place as Twitch's
     * USERSTATE confirms a live send. Its session must survive the status rebind.
     */
    private fun addConfirmEchoStep() {
        val record = StepRecord("L10 confirm an echo in place", CONFIRM_LOCAL_ID)
        val emotes = ParityCatalogue.emoteCount(ECHO_EMOTES)
        lateinit var container: LinearLayout
        lateinit var echo: PendingEchoView
        fun checkKept(suffix: String) {
            val loads = images.loadsOf("L10")
            record.check("noLoadCleared$suffix", loads.count { it.live } == emotes && loads.none { it.cleared })
            record.check("imagesStillInstalled$suffix", imagesShownBy(echo.messageView).let { shown ->
                shown.size == emotes && shown.all { drawable -> loads.any { it.delivered === drawable } }
            })
            record.check("running$suffix", animatedOf("L10").let { animated -> animated.size == emotes && animated.all { it.isRunning } })
            record.check("releaseNotReported$suffix", recorder.releasesOf(echo.messageView) == 0)
        }
        steps += Step(record, listOf(
            {
                container = newContainer()
                root.addView(container)
                val timeline = ChatTimelineController(container, binder)
                images.label = "L10"
                timeline.insert(messageTimestampSec = CONFIRM_STEP_SEC) { position ->
                    PendingEchoRow(
                        position = position,
                        localId = CONFIRM_LOCAL_ID,
                        user = ParityCatalogue.SIGNED_IN,
                        messageText = ECHO_TEXT,
                        emotesRaw = ECHO_EMOTES,
                        replyParentUserLogin = null,
                        sentAtSec = CONFIRM_STEP_SEC,
                        status = PendingEchoStatus.SENDING
                    )
                }
                images.deliverPending()
                echo = container.getChildAt(0) as PendingEchoView
                record.pre("loadsLive", images.loadsOf("L10").count { it.live } == emotes)
                record.pre("imagesShown", imagesShownBy(echo.messageView).size == emotes)
                record.pre("sending", echo.alpha < 1f)
                record.check("statusChanged", timeline.applyEchoEvent(CONFIRM_LOCAL_ID, PendingEchoEvent.USERSTATE))
                record.check("confirmedLook", echo.alpha == 1f)
                checkKept("AtTheCall")
            },
            {
                checkKept("AfterALoopTurn")
                record.check("pixelsEqualTheReference", pixelsEqual(container, echoReference()))
            }
        ))
    }

    /* L8: a timeline holding both emote rows, cleared as a channel change clears it. */
    private fun addClearStep() {
        val record = StepRecord("L8 clear", null)
        steps += Step(record, listOf(
            {
                val container = newContainer()
                root.addView(container)
                val timeline = ChatTimelineController(container, binder)
                images.label = "L8"
                ParityCatalogue.emoteRows.forEachIndexed { index, id ->
                    timeline.insert(messageTimestampSec = CLEAR_STEP_SEC + index) { position ->
                        rowData.getValue(id).copy(position = position)
                    }
                }
                images.deliverPending()
                val emotes = ParityCatalogue.emoteRows.sumOf { id -> ParityCatalogue.emoteCount(rowData.getValue(id).emotesRaw) }
                record.pre("loadsLive", images.loadsOf("L8").count { it.live } == emotes)
                record.pre("rows", container.childCount == ParityCatalogue.emoteRows.size)
                timeline.clear()
                record.check("removed", container.isEmpty())
                record.check("everyLoadClearedAtTheCall", images.loadsOf("L8").all { it.cleared })
            }
        ))
    }

    /* L9: both emote rows bound and shown, then every session released, as onDestroyView releases them. */
    private fun addReleaseAllStep() {
        val record = StepRecord("L9 release all", null)
        steps += Step(record, listOf(
            {
                val container = newContainer()
                root.addView(container)
                images.label = "L9"
                val views = ParityCatalogue.emoteRows.map { id -> binder.create(rowData.getValue(id)).also(container::addView) }
                images.deliverPending()
                val emotes = ParityCatalogue.emoteRows.sumOf { id -> ParityCatalogue.emoteCount(rowData.getValue(id).emotesRaw) }
                record.pre("loadsLive", images.loadsOf("L9").count { it.live } == emotes)
                EmoteOwnership.releaseAll(checkNotNull(page.emoteLoader))
                record.check("everyLoadClearedAtTheCall", images.loadsOf("L9").all { it.cleared })
                record.check("releaseReportedForEachRow", views.all { view -> recorder.releasesOf(view) == 1 })
            }
        ))
    }

    /* Whether [view] shows its row's images: one image span per emote, each a drawable [label]'s loads delivered, running, and the reference's pixels. */
    private fun checkShowing(record: StepRecord, view: TextView, label: String, emotes: Int, id: String) {
        val delivered = images.loadsOf(label).filter { it.live }.mapNotNull { it.delivered }
        val shown = imagesShownBy(view)
        record.check("oneImagePerEmote", shown.size == emotes && delivered.size == emotes)
        record.check("imagesAreTheDeliveredOnes", shown.all { drawable -> delivered.any { it === drawable } })
        record.check("running", animatedOf(label).let { animated -> animated.size == emotes && animated.all { it.isRunning } })
        record.check("pixelsEqualTheReference", pixelsEqual(view.parent as ViewGroup, references.getValue(id)()))
    }

    private fun animatedOf(label: String): List<FixedAnimatedEmoteDrawable> =
        images.loadsOf(label).mapNotNull { load -> load.delivered as? FixedAnimatedEmoteDrawable }

    private fun imagesShownBy(view: TextView): List<Any> {
        val text = view.text as? Spanned ?: return emptyList()
        return text.getSpans(0, text.length, ImageSpan::class.java).map { span -> span.drawable }
    }

    private fun pixelsEqual(a: ViewGroup, b: ViewGroup): Boolean {
        val left = render(a)
        val right = render(b)
        return PixelDiff.of(left, right).identical.also {
            left.recycle()
            right.recycle()
        }
    }

    /* Pixels and state, as the rebind proof compares a rebound view with a fresh one. */
    private fun sameAs(a: LinearLayout, b: LinearLayout): Boolean {
        return pixelsEqual(a, b) && snapshot.diff(snapshot.of(a.getChildAt(0)), snapshot.of(b.getChildAt(0))).isEmpty()
    }

    companion object {
        /** L1 to L6 for each of the 2 emote rows, then L7, L10, L8 and L9. */
        const val EXPECTED_STEPS = 16

        /** The catalogue row L6 binds over an emote row. */
        const val PLAIN_ROW = "msg.plain"

        private const val ECHO_LOCAL_ID = "parity-lifecycle-echo"
        private const val CONFIRM_LOCAL_ID = "parity-lifecycle-confirmed-echo"

        /** The text and emote tag of the echoes L7 and L10 build, which the activity's reference for L10 restates. */
        const val ECHO_TEXT = "Kappa sent Kappa"
        const val ECHO_EMOTES = "25:0-4,11-15"
        private const val ECHO_SENT_AT_SEC = 1_791_000_100.0

        /** When L10's echo was sent. */
        const val CONFIRM_STEP_SEC = 1_791_000_150.0
        private const val CLEAR_STEP_SEC = 1_791_000_200.0
    }
}
