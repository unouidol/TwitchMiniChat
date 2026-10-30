package com.fs.twitchminichat.harness

import android.graphics.Bitmap
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.fs.twitchminichat.ChatRowViewBinder
import com.fs.twitchminichat.ChatTimelineAdapter
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
 * The rebind proof: a view bound to row A and then bound to row B equals a view built fresh for B,
 * in pixels and in state, for every ordered pair of catalogue rows of the same kind, A = B included.
 * Every pair, not a set chosen from what is expected to leak: a chosen set could only confirm the
 * list ChatRowViewBinder's KDoc gives, while every pair is what tests that the list is complete.
 *
 * Production against production: both views come from the page's own ChatRowViewBinder, and each
 * row's data is the tag the row's producer gave its view. Since 5d every view is created and bound
 * through the chat list's adapter, as a holder back from the pool is ([AdapterBinding]). An echo is
 * rebuilt the way production builds one - bound as sending, then its events as payload binds -
 * because an echo's hidden status text depends on the statuses it passed through (item 5 of the
 * binder's list).
 *
 * The emote rows take part. The page's loader draws from [images], which delivers every image inside
 * the load, and every load is recorded with the bind that asked for it. So each pair also asks what
 * pixels cannot: once B is bound, are A's loads all cleared and B's all live - one session per view,
 * the one of the row it shows - and does the view's own text hold B's images? A session left over
 * from A draws into a buffer the view no longer shows, so no pixel and no field of the view can see
 * it; only the record of the loads can.
 *
 * C-R, create against the producer, is the control that keeps the proof from being
 * self-referential: comparing the binder with itself would pass for a binder that built every row
 * wrongly the same way. C-R requires each fresh build to equal what the row's producer built, which
 * the parity pass compares with the frozen copy, so the chain reaches the frozen code. The hidden
 * row's subject is produced with its user shown: in a list a hidden row has no view to read its data
 * from, and the row is the same either way; the parity pass compares that shown row with the frozen
 * copy too ("rowsBeforeHiding"). So C-R has no declared exception since 5d: the one it had, the
 * hidden row's visibility, was the sync's GONE on a view the list no longer has.
 *
 * The direct-bind check (5d): every echo bound at once at its last status, as the list binds an echo
 * that comes back from the pool, against the same echo built through its history. The pixels must
 * match for every echo, and the state for all but [DIRECT_BIND_EXCEPTION], whose hidden status line
 * keeps a different text (ChatRowViewBinder, item 5).
 *
 * Since 5d production binds a view to a second row whenever a holder comes back from the pool. This
 * proof is what shows that doing so is correct.
 */
internal class RebindProof(
    private val production: ProductionRowViews,
    private val images: FixedEmoteImageSource,
    private val snapshot: ViewStateSnapshot,
    private val newContainer: () -> LinearLayout,
    private val render: (ViewGroup) -> Bitmap
) {

    /** One difference a check allows, named, with its reason. */
    data class DeclaredException(val rowId: String, val field: String, val reason: String)

    private val binder: ChatRowViewBinder = production.binder
    private val binding = AdapterBinding(binder)

    /* A catalogue row taking part, with the data its producer gave its view and the list that view is in. */
    private class Subject(val row: ParityRow, val data: ChatTimelineRow, val produced: ViewGroup) {
        /** How many emote images the row's text holds, from the emote tag its producer gave it. */
        val emotes: Int = ParityCatalogue.emoteCount(
            when (data) {
                is ChatMessageRow -> data.emotesRaw
                is PendingEchoRow -> data.emotesRaw
                is SystemLineRow -> null
            }
        )
    }

    /** Runs the proof, C-R, the direct-bind check and the precondition controls, and reports all of them. */
    fun run(): JSONObject {
        val result = JSONObject()
        check(production.emoteLoader != null) { "the rebind proof's page has no emote loader" }
        check(!images.holdDeliveries) { "the rebind proof's images must deliver inside the load" }

        val subjects = ParityCatalogue.rows.map { row -> subjectOf(row) }
        result.put("rowsTakingPart", subjects.size)
        result.put("emoteRows", JSONArray(subjects.filter { it.emotes > 0 }.map { it.row.id }))

        val freshBuilds = subjects.associate { subject -> subject.row.id to fresh(subject, "fresh ${subject.row.id}") }
        val freshStates = freshBuilds.mapValues { (_, container) -> snapshot.of(container.getChildAt(0)) }

        val createVersusProducer = createVersusProducer(subjects)
        result.put("createVersusProducer", createVersusProducer)

        val pairs = pairs(subjects, freshBuilds, freshStates)
        result.put("pairs", pairs)

        val directBind = directBind(subjects, freshBuilds, freshStates)
        result.put("directBind", directBind)

        val preconditions = preconditions(subjects)
        result.put("preconditions", preconditions)

        val passed = pairs.getBoolean("passed") && createVersusProducer.getBoolean("passed") &&
            directBind.getBoolean("passed") && preconditions.getBoolean("passed")
        result.put("passed", passed)
        return result
    }

    /* The row through its producer, into a list of its own, its user shown: the data the binder is given. */
    private fun subjectOf(row: ParityRow): Subject {
        val list = production.newList()
        images.label = "produced ${row.id}"
        ParityCatalogue.produce(row, ParityCatalogue.rows.indexOf(row), production, list, hide = false)
        check(list.childCount == 1) {
            "the producer of ${row.id} gave ${list.childCount} views, not 1: a catalogue user hidden on the device?"
        }
        val data = checkNotNull(list.getChildAt(0).tag as? ChatTimelineRow) {
            "the producer of ${row.id} left no row as its view's tag"
        }
        return Subject(row, data, list)
    }

    /* [subject] built fresh through the adapter: created from its first data, then its events. Its loads carry [label]. */
    private fun fresh(subject: Subject, label: String): LinearLayout {
        val container = newContainer()
        images.label = label
        val holder = binding.create(firstData(subject), container)
        container.addView(holder.itemView)
        replayEvents(holder, subject)
        return container
    }

    /* What the row's view is first bound with: its data, an echo as sending, as its producer adds it. */
    private fun firstData(subject: Subject): ChatTimelineRow {
        val data = subject.data
        return if (data is PendingEchoRow) data.copy(status = PendingEchoStatus.SENDING) else data
    }

    /* An echo's events, each a payload bind when it changes the status, as the list delivers ECHO_STATUS. */
    private fun replayEvents(holder: ChatTimelineAdapter.RowHolder, subject: Subject) {
        val row = subject.row as? ParityRow.Echo ?: return
        var echo = firstData(subject) as PendingEchoRow
        for (event in row.events) {
            val status = echo.status.after(PendingEchoEvent.valueOf(event))
            if (status == echo.status) continue
            echo = echo.copy(status = status)
            binding.rebind(holder, echo, ChatTimelineChange.ECHO_STATUS)
        }
    }

    /* Binds [subject]'s first data to [holder], which shows a row of the same kind, then replays its events. */
    private fun bindAgain(holder: ChatTimelineAdapter.RowHolder, subject: Subject) {
        binding.bindAgain(holder, firstData(subject))
        replayEvents(holder, subject)
    }

    /*
     * C-R: every fresh build against its producer's build, in pixels and in state. Its builds are its
     * own, not the pairs' references; anything that differs fails.
     */
    private fun createVersusProducer(subjects: List<Subject>): JSONObject {
        val out = JSONObject()
        out.put("declaredExceptions", 0)
        out.put("declaredExceptionsExpected", 0)
        var identical = 0
        val failures = JSONArray()
        for (subject in subjects) {
            val fresh = fresh(subject, "create-versus-producer ${subject.row.id}")
            val pixels = compare(fresh, subject.produced)
            val state = snapshot.diff(snapshot.of(fresh.getChildAt(0)), snapshot.of(subject.produced.getChildAt(0)))
            if (pixels.identical && state.isEmpty() && fresh.childCount == 1) {
                identical++
            } else {
                failures.put(JSONObject().put("row", subject.row.id).put("bitmap", pixels.describe())
                    .put("stateDiff", JSONArray(state.take(8))))
            }
        }
        out.put("rows", subjects.size)
        out.put("identical", identical)
        out.put("failures", failures)
        out.put("passed", identical == subjects.size)
        return out
    }

    /*
     * Every ordered pair of the same kind: A bound, then B bound to the same view, against B fresh, in
     * pixels and state; then what the loads say of the view's sessions, and whether its text holds B's
     * images.
     */
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
        var emoteA = 0
        var emoteB = 0
        var aLoadsRequested = 0
        var bLoadsRequested = 0
        var sessionsHeld = 0
        var imagesHeld = 0
        val failures = JSONArray()
        val sessionFailures = JSONArray()
        val imageFailures = JSONArray()
        val vacuous = JSONArray()

        for (group in subjects.groupBy { subject -> subject.data.javaClass }.values) {
            for (a in group) {
                for (b in group) {
                    pairs++
                    val aLabel = "pair $pairs A ${a.row.id}"
                    val bLabel = "pair $pairs B ${b.row.id}"
                    val container = newContainer()
                    images.label = aLabel
                    val holder = binding.create(firstData(a), container)
                    val view = holder.itemView
                    container.addView(view)
                    replayEvents(holder, a)
                    val before = snapshot.of(view)

                    images.label = bLabel
                    bindAgain(holder, b)

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

                    /* One session per view, B's: A's loads all cleared, B's all live, one per emote. */
                    val aLoads = images.loadsOf(aLabel)
                    val bLoads = images.loadsOf(bLabel)
                    if (a.emotes > 0) emoteA++
                    if (b.emotes > 0) emoteB++
                    if (aLoads.size == a.emotes) aLoadsRequested++
                    if (bLoads.size == b.emotes) bLoadsRequested++
                    val aUncleared = aLoads.count { load -> !load.cleared }
                    val bLive = bLoads.count { load -> load.live }
                    if (aUncleared == 0 && bLive == b.emotes && bLoads.size == b.emotes) {
                        sessionsHeld++
                    } else {
                        sessionFailures.put(JSONObject().put("a", a.row.id).put("b", b.row.id)
                            .put("aLoads", aLoads.size).put("aUncleared", aUncleared)
                            .put("bLoads", bLoads.size).put("bLive", bLive).put("bEmotes", b.emotes))
                    }

                    /* B's images are in the text the view shows, not in a buffer it no longer shows. */
                    val shown = imageDrawablesOf(view)
                    val delivered = bLoads.mapNotNull { load -> load.delivered }
                    if (shown.size == b.emotes && shown.all { drawable -> delivered.any { it === drawable } }) {
                        imagesHeld++
                    } else {
                        imageFailures.put(JSONObject().put("a", a.row.id).put("b", b.row.id)
                            .put("imagesShown", shown.size).put("bEmotes", b.emotes))
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
            .put("expectedPairsWithAnEmoteA", EXPECTED_EMOTE_PAIRS)
            .put("pairsWithAnEmoteA", emoteA)
            .put("expectedPairsWithAnEmoteB", EXPECTED_EMOTE_PAIRS)
            .put("pairsWithAnEmoteB", emoteB)
            .put("aRequestedOneLoadPerEmote", aLoadsRequested)
            .put("bRequestedOneLoadPerEmote", bLoadsRequested)
        out.put("vacuity", vacuity)
        out.put("identical", identical)
        out.put("bitmapIdentical", bitmapIdentical)
        out.put("stateIdentical", stateIdentical)
        out.put("failures", failures)
        out.put("sessionsHeld", sessionsHeld)
        out.put("sessionFailures", sessionFailures)
        out.put("imagesHeld", imagesHeld)
        out.put("imageFailures", imageFailures)
        val notVacuous = pairs == EXPECTED_PAIRS && oneChildEach == pairs && sameView == pairs &&
            startedEqualToB == 0 && endedEqualToA == 0 && emoteA == EXPECTED_EMOTE_PAIRS &&
            emoteB == EXPECTED_EMOTE_PAIRS && aLoadsRequested == pairs && bLoadsRequested == pairs
        out.put("notVacuous", notVacuous)
        out.put("passed", notVacuous && identical == pairs && sessionsHeld == pairs && imagesHeld == pairs)
        return out
    }

    /*
     * Every echo bound at once at its last status, through the adapter, against the same echo built
     * through its history. Pixels for every echo; state for every echo but the declared one, whose one
     * difference must be exactly the one declared: the text of the hidden status line.
     */
    private fun directBind(
        subjects: List<Subject>,
        freshBuilds: Map<String, LinearLayout>,
        freshStates: Map<String, Map<String, Any?>>
    ): JSONObject {
        val out = JSONObject()
        val echoes = subjects.filter { subject -> subject.data is PendingEchoRow }
        var pixelsIdentical = 0
        var stateIdentical = 0
        var exceptionsMet = 0
        val rows = JSONArray()
        for (echo in echoes) {
            val last = echo.data as PendingEchoRow
            val container = newContainer()
            images.label = "direct ${echo.row.id}"
            container.addView(binding.create(last, container).itemView)
            val pixels = compare(container, freshBuilds.getValue(echo.row.id))
            val state = snapshot.diff(snapshot.of(container.getChildAt(0)), freshStates.getValue(echo.row.id))
            if (pixels.identical) pixelsIdentical++
            val declared = echo.row.id == DIRECT_BIND_EXCEPTION.rowId
            if (state.isEmpty()) {
                stateIdentical++
            } else if (declared && state.size == 1 && state[0].startsWith(DIRECT_BIND_EXCEPTION.field + ":")) {
                exceptionsMet++
            }
            rows.put(JSONObject().put("row", echo.row.id).put("status", last.status.name)
                .put("bitmapIdentical", pixels.identical).put("bitmap", pixels.describe())
                .put("stateDiff", JSONArray(state.take(8))))
        }
        out.put("echoes", echoes.size)
        out.put("expectedEchoes", EXPECTED_ECHOES)
        out.put("pixelsIdentical", pixelsIdentical)
        out.put("stateIdentical", stateIdentical)
        out.put("declaredException", JSONObject().put("row", DIRECT_BIND_EXCEPTION.rowId)
            .put("field", DIRECT_BIND_EXCEPTION.field).put("reason", DIRECT_BIND_EXCEPTION.reason))
        out.put("declaredExceptionsMet", exceptionsMet)
        out.put("rows", rows)
        out.put("passed", echoes.size == EXPECTED_ECHOES && pixelsIdentical == echoes.size &&
            stateIdentical == echoes.size - 1 && exceptionsMet == 1)
        return out
    }

    /* The drawables of the image spans in the text [view] shows. */
    private fun imageDrawablesOf(view: View): List<Any> {
        val text = (view as? TextView)?.text as? Spanned ?: return emptyList()
        return text.getSpans(0, text.length, ImageSpan::class.java).map { span -> span.drawable }
    }

    /*
     * Each precondition the binder, the echo view and the adapter hold, made to fire once on purpose:
     * a precondition that never fails in a test is as untested as the guard it replaced.
     */
    private fun preconditions(subjects: List<Subject>): JSONObject {
        val echo = subjects.first { subject -> subject.data is PendingEchoRow }
        val message = subjects.first { subject -> subject.data is ChatMessageRow }
        val echoRow = firstData(echo) as PendingEchoRow
        images.label = "preconditions"
        val messageView = binder.create(message.data)

        val out = JSONObject()
        var made = 0
        var fired = 0
        fun record(name: String, invariant: String, action: () -> Unit) {
            val outcome = fires(invariant, action)
            made++
            if (outcome) fired++
            out.put(name, outcome)
        }
        record("rebindOfAViewThatCannotBe", "only an echo is rebound in place") {
            binder.rebind(messageView, echoRow, ChatTimelineChange.ECHO_STATUS)
        }
        record("echoRebindWithAnotherRow", "ChatTimelineListSync.applyEchoEvent is the only source of ECHO_STATUS") {
            (binder.create(echoRow) as PendingEchoView).rebind(message.data, ChatTimelineChange.ECHO_STATUS)
        }
        record("unbindOfAViewNewViewNeverBuilds", "newView builds three kinds of view") {
            EmoteOwnership.unbind(binder, LinearLayout(messageView.context))
        }
        record("viewOfATypeThereIsNot", "ChatRowViewType is the only source of view types") {
            binder.newView(UNKNOWN_VIEW_TYPE)
        }
        record("bindWithAPayloadThatIsNotAChange", "the only payload is a ChatTimelineChange") {
            val container = newContainer()
            binding.bindWithPayload(binding.create(echoRow, container), NOT_A_CHANGE)
        }
        out.put("made", made)
        out.put("fired", fired)
        out.put("passed", made == EXPECTED_PRECONDITIONS && fired == made)
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
         * Every ordered pair of the same kind among the rows taking part, A = B included: 24 messages,
         * 9 echoes and 1 system line, so 24 * 24 + 9 * 9 + 1 * 1. A change to the catalogue changes it,
         * on purpose: the run fails until this is updated to what was meant.
         */
        const val EXPECTED_PAIRS = 658

        /** The pairs whose A, or whose B, is one of the 2 emote rows: 2 * 24 each. */
        const val EXPECTED_EMOTE_PAIRS = 48

        /** The catalogue's echoes, which the direct-bind check binds at their last status. */
        const val EXPECTED_ECHOES = 9

        /**
         * The direct-bind check's one declared exception: an echo that passed through unconfirmed before
         * it was confirmed hides "unconfirmed" in its status line when rebound through its history, and
         * "sending" when bound at once. The line is hidden in both, so no pixel differs.
         */
        val DIRECT_BIND_EXCEPTION = DeclaredException(
            rowId = "echo.unconfirmed-confirmed",
            field = "/children[1]/text",
            reason = "a hidden status line keeps the text it last showed (ChatRowViewBinder, item 5); a full bind " +
                "shows sending before the row's status, a history passes through unconfirmed"
        )

        /**
         * The preconditions the proof makes fire: two of the binder's and the echo view's from 5a-2, unbind's
         * from 5c, and two from 5d, the adapter's payload and the binder's view type. 5a-2's third, an echo
         * bound at a status other than sending, is gone: 5d binds an echo from the pool at any status.
         */
        const val EXPECTED_PRECONDITIONS = 5

        private const val UNKNOWN_VIEW_TYPE = 99
        private const val NOT_A_CHANGE = "not a change"
    }
}
