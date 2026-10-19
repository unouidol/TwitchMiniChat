package com.fs.twitchminichat.harness

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.fs.twitchminichat.ChatRowViewBinder
import com.fs.twitchminichat.PendingEchoView
import com.fs.twitchminichat.chat.ChatMessageRow
import com.fs.twitchminichat.chat.ChatTimelineChange
import com.fs.twitchminichat.chat.ChatTimelineRow
import com.fs.twitchminichat.chat.PendingEchoEvent
import com.fs.twitchminichat.chat.PendingEchoRow
import com.fs.twitchminichat.chat.PendingEchoStatus
import com.fs.twitchminichat.chat.SystemLineRow
import org.json.JSONArray
import org.json.JSONObject

/**
 * The 5a-2 rebind proof: a view bound to row A and then bound to row B equals a view built fresh
 * for B, in pixels and in state, for every ordered pair of catalogue rows of the same kind, A = B
 * included. Every pair, not a set chosen from what is expected to leak: a chosen set could only
 * confirm the list ChatRowViewBinder's KDoc gives, while every pair is what tests that the list is
 * complete.
 *
 * Production against production: both views come from the page's own ChatRowViewBinder, and each
 * row's data is the tag the row's producer gave its view. An echo is rebuilt the way production
 * builds one - bound as sending, then its events as rebinds - because an echo's hidden status text
 * depends on the statuses it passed through (item 5 of the binder's list).
 *
 * C-R, create against the producer, is the control that keeps the proof from being
 * self-referential: comparing the binder with itself would pass for a binder that built every row
 * wrongly the same way. C-R requires each fresh build to equal what the row's producer built, which
 * the parity pass compares with the frozen copy, so the chain reaches the frozen code.
 *
 * Production does not bind a view to a second row in 5a-2. This proof is what shows that doing so
 * is correct; 5d, which turns it on, must run it again rather than inherit its result.
 */
internal class RebindProof(
    private val production: ProductionRowViews,
    private val snapshot: ViewStateSnapshot,
    private val newContainer: () -> LinearLayout,
    private val render: (ViewGroup) -> Bitmap
) {

    /** One difference C-R allows between the binder's build of a row and its producer's, named, with its reason. */
    data class DeclaredException(val rowId: String, val field: String, val value: Int, val reason: String)

    private val binder: ChatRowViewBinder = production.binder

    /* A catalogue row taking part, with the data its producer gave its view and that view's container. */
    private class Subject(val row: ParityRow, val data: ChatTimelineRow, val produced: ViewGroup)

    /** Runs the proof, C-R and the precondition controls, and reports all of them. */
    fun run(): JSONObject {
        val result = JSONObject()
        val excluded = JSONArray()
        ParityCatalogue.emoteRows.forEach { id -> excluded.put(JSONObject().put("id", id).put("reason", EMOTE_ROW_REASON)) }
        result.put("excluded", excluded)

        val subjects = ParityCatalogue.rows
            .filter { row -> row.id !in ParityCatalogue.emoteRows }
            .map { row -> subjectOf(row) }
        result.put("rowsTakingPart", subjects.size)

        val freshBuilds = subjects.associate { subject -> subject.row.id to fresh(subject) }
        val freshStates = freshBuilds.mapValues { (_, container) -> snapshot.of(container.getChildAt(0)) }

        val createVersusProducer = createVersusProducer(subjects)
        result.put("createVersusProducer", createVersusProducer)

        val pairs = pairs(subjects, freshBuilds, freshStates)
        result.put("pairs", pairs)

        val preconditions = preconditions(subjects)
        result.put("preconditions", preconditions)

        val passed = pairs.getBoolean("passed") && createVersusProducer.getBoolean("passed") &&
            preconditions.getBoolean("passed")
        result.put("passed", passed)
        return result
    }

    /* The row through its producer, into a container of its own: the data the binder is given. */
    private fun subjectOf(row: ParityRow): Subject {
        val container = newContainer()
        ParityCatalogue.produce(row, ParityCatalogue.rows.indexOf(row), production, container)
        check(container.childCount == 1) {
            "the producer of ${row.id} gave ${container.childCount} views, not 1: a catalogue user hidden on the device?"
        }
        val data = checkNotNull(container.getChildAt(0).tag as? ChatTimelineRow) {
            "the producer of ${row.id} left no row as its view's tag"
        }
        return Subject(row, data, container)
    }

    /* [subject] built fresh by the binder: created from its first data, then its events. */
    private fun fresh(subject: Subject): LinearLayout {
        val container = newContainer()
        val view = binder.create(firstData(subject))
        container.addView(view)
        replayEvents(view, subject)
        return container
    }

    /* What the row's view is first bound with: its data, an echo as sending, as its producer adds it. */
    private fun firstData(subject: Subject): ChatTimelineRow {
        val data = subject.data
        return if (data is PendingEchoRow) data.copy(status = PendingEchoStatus.SENDING) else data
    }

    /* An echo's events, each a rebind when it changes the status, as ChatTimelineViewSync.applyEchoEvent applies them. */
    private fun replayEvents(view: View, subject: Subject) {
        val row = subject.row as? ParityRow.Echo ?: return
        var echo = firstData(subject) as PendingEchoRow
        for (event in row.events) {
            val status = echo.status.after(PendingEchoEvent.valueOf(event))
            if (status == echo.status) continue
            echo = echo.copy(status = status)
            binder.rebind(view, echo, ChatTimelineChange.ECHO_STATUS)
        }
    }

    /* Binds [subject]'s first data to [view], which shows a row of the same kind, then replays its events. */
    private fun bindAgain(view: View, subject: Subject) {
        when (val data = firstData(subject)) {
            is ChatMessageRow -> binder.bindMessage(view as TextView, data)
            is SystemLineRow -> binder.bindSystemLine(view as TextView, data)
            is PendingEchoRow -> binder.bindEcho(view as PendingEchoView, data)
        }
        replayEvents(view, subject)
    }

    /*
     * C-R: every fresh build against its producer's build, in pixels and in state. Its builds are its
     * own, not the pairs' references, because a declared exception sets its one field on the fresh
     * build first; anything else that differs fails.
     */
    private fun createVersusProducer(subjects: List<Subject>): JSONObject {
        val out = JSONObject()
        out.put("declaredExceptions", DECLARED_EXCEPTIONS.size)
        out.put("declaredExceptionsExpected", EXPECTED_DECLARED_EXCEPTIONS)
        out.put("declared", JSONArray(DECLARED_EXCEPTIONS.map { e ->
            JSONObject().put("row", e.rowId).put("field", e.field).put("value", e.value).put("reason", e.reason)
        }))
        var applied = 0
        var identical = 0
        val failures = JSONArray()
        for (subject in subjects) {
            val fresh = fresh(subject)
            for (exception in DECLARED_EXCEPTIONS.filter { e -> e.rowId == subject.row.id }) {
                check(exception.field == "visibility") { "a declared exception names a field C-R cannot set: ${exception.field}" }
                fresh.getChildAt(0).visibility = exception.value
                applied++
            }
            val pixels = compare(fresh, subject.produced)
            val state = snapshot.diff(snapshot.of(fresh.getChildAt(0)), snapshot.of(subject.produced.getChildAt(0)))
            if (pixels.identical && state.isEmpty() && fresh.childCount == 1) {
                identical++
            } else {
                failures.put(JSONObject().put("row", subject.row.id).put("bitmap", pixels.describe())
                    .put("stateDiff", JSONArray(state.take(8))))
            }
        }
        out.put("exceptionsApplied", applied)
        out.put("rows", subjects.size)
        out.put("identical", identical)
        out.put("failures", failures)
        out.put("passed", identical == subjects.size && DECLARED_EXCEPTIONS.size == EXPECTED_DECLARED_EXCEPTIONS &&
            applied == EXPECTED_DECLARED_EXCEPTIONS)
        return out
    }

    /* Every ordered pair of the same kind: A bound, then B bound to the same view, against B fresh. */
    private fun pairs(
        subjects: List<Subject>,
        freshBuilds: Map<String, LinearLayout>,
        freshStates: Map<String, Map<String, Any?>>
    ): JSONObject {
        val out = JSONObject()
        var pairs = 0
        var oneChildEach = 0
        var sameView = 0
        var differentRows = 0
        var startedEqualToB = 0
        var endedEqualToA = 0
        var bitmapIdentical = 0
        var stateIdentical = 0
        var identical = 0
        val failures = JSONArray()
        val vacuous = JSONArray()

        for (group in subjects.groupBy { subject -> subject.data.javaClass }.values) {
            for (a in group) {
                for (b in group) {
                    pairs++
                    val container = newContainer()
                    val view = binder.create(firstData(a))
                    container.addView(view)
                    replayEvents(view, a)
                    val before = snapshot.of(view)

                    bindAgain(view, b)

                    val fresh = freshBuilds.getValue(b.row.id)
                    if (container.childCount == 1 && fresh.childCount == 1) oneChildEach++
                    if (container.childCount == 1 && container.getChildAt(0) === view) sameView++

                    val after = snapshot.of(view)
                    if (a !== b) {
                        differentRows++
                        if (snapshot.diff(before, freshStates.getValue(b.row.id)).isEmpty()) {
                            startedEqualToB++
                            vacuous.put("${a.row.id} -> ${b.row.id}: A's view already equalled B fresh before the bind")
                        }
                        if (snapshot.diff(after, freshStates.getValue(a.row.id)).isEmpty()) {
                            endedEqualToA++
                            vacuous.put("${a.row.id} -> ${b.row.id}: the rebound view still equals A fresh")
                        }
                    }

                    val pixels = compare(container, fresh)
                    val state = snapshot.diff(after, freshStates.getValue(b.row.id))
                    if (pixels.identical) bitmapIdentical++
                    if (state.isEmpty()) stateIdentical++
                    if (pixels.identical && state.isEmpty()) {
                        identical++
                    } else {
                        failures.put(JSONObject().put("a", a.row.id).put("b", b.row.id)
                            .put("bitmapIdentical", pixels.identical).put("bitmap", pixels.describe())
                            .put("stateIdentical", state.isEmpty()).put("stateDiff", JSONArray(state.take(8))))
                    }
                }
            }
        }

        val vacuity = JSONObject()
            .put("expectedPairs", EXPECTED_PAIRS)
            .put("pairs", pairs)
            .put("oneChildEach", oneChildEach)
            .put("reboundIsTheSameView", sameView)
            .put("pairsOfDifferentRows", differentRows)
            .put("startedEqualToBFresh", startedEqualToB)
            .put("endedEqualToAFresh", endedEqualToA)
            .put("vacuousPairs", vacuous)
        out.put("vacuity", vacuity)
        out.put("identical", identical)
        out.put("bitmapIdentical", bitmapIdentical)
        out.put("stateIdentical", stateIdentical)
        out.put("failures", failures)
        val notVacuous = pairs == EXPECTED_PAIRS && oneChildEach == pairs && sameView == pairs &&
            startedEqualToB == 0 && endedEqualToA == 0
        out.put("notVacuous", notVacuous)
        out.put("passed", notVacuous && identical == pairs)
        return out
    }

    /*
     * Each precondition the binder and the echo view hold, made to fire once on purpose: a
     * precondition that never fails in a test is as untested as the guard it replaced.
     */
    private fun preconditions(subjects: List<Subject>): JSONObject {
        val echo = subjects.first { subject -> subject.data is PendingEchoRow }
        val message = subjects.first { subject -> subject.data is ChatMessageRow }
        val echoRow = firstData(echo) as PendingEchoRow
        val messageView = binder.create(message.data)

        val out = JSONObject()
        out.put("echoBoundAsNotSending", fires("an echo is bound as it enters the timeline") {
            binder.bindEcho(binder.create(echoRow) as PendingEchoView, echoRow.copy(status = PendingEchoStatus.CONFIRMED))
        })
        out.put("rebindOfAViewThatCannotBe", fires("only an echo is rebound in place") {
            binder.rebind(messageView, echoRow, ChatTimelineChange.ECHO_STATUS)
        })
        out.put("echoRebindWithAnotherRow", fires("ChatTimelineViewSync.applyEchoEvent is the only source of ECHO_STATUS") {
            (binder.create(echoRow) as PendingEchoView).rebind(message.data, ChatTimelineChange.ECHO_STATUS)
        })
        out.put("passed", out.getBoolean("echoBoundAsNotSending") && out.getBoolean("rebindOfAViewThatCannotBe") &&
            out.getBoolean("echoRebindWithAnotherRow"))
        return out
    }

    /* Whether [action] fails with an IllegalStateException whose message names [invariant]. */
    private fun fires(invariant: String, action: () -> Unit): Boolean = try {
        action()
        false
    } catch (failure: IllegalStateException) {
        failure.message.orEmpty().contains(invariant)
    }

    private fun compare(a: ViewGroup, b: ViewGroup): PixelDiff {
        val left = render(a)
        val right = render(b)
        return PixelDiff.of(left, right).also {
            left.recycle()
            right.recycle()
        }
    }

    companion object {
        /**
         * Every ordered pair of the same kind among the rows taking part, A = B included: 22 messages,
         * 9 echoes and 1 system line, so 22 * 22 + 9 * 9 + 1 * 1. A change to the catalogue changes it,
         * on purpose: the run fails until this is updated to what was meant.
         */
        const val EXPECTED_PAIRS = 566

        /** Why the emote rows take no part until 5c. */
        const val EMOTE_ROW_REASON = "until 5c: whether a second bind releases the row's emote session cannot be " +
            "shown without an image source the harness can drive"

        /** C-R's declared exceptions. There is one; the run fails if there are ever more or fewer. */
        val DECLARED_EXCEPTIONS = listOf(
            DeclaredException(
                rowId = "msg.hidden",
                field = "visibility",
                value = View.GONE,
                reason = "visibility belongs to the sync, not the bind: the producer's timeline hides the row " +
                    "after its view is built, and the binder never sets visibility"
            )
        )

        const val EXPECTED_DECLARED_EXCEPTIONS = 1
    }
}
