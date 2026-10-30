package com.fs.twitchminichat.harness

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject

/**
 * The steps of a proof that runs across turns of the main loop, as [EmoteLifecycleProof] runs: each
 * step a list of phases, each phase on its own turn, so that whatever the code under test posts runs
 * before the next phase reads. A step whose preconditions do not hold, or that threw, stops there and
 * fails; the next step still runs.
 */
internal class ProofSteps(private val expectedSteps: Int) {

    /** One step's record: what it asserted before acting, what it checked after, and what threw. */
    class Record(val name: String) {
        val preconditions = JSONObject()
        val checks = JSONObject()
        val observations = JSONObject()
        var error: String? = null

        /** Asserts [holds] before the step acts; a false one stops the step and fails it. */
        fun pre(name: String, holds: Boolean) {
            preconditions.put(name, holds)
        }

        /** Checks [holds] after the step acted. */
        fun check(name: String, holds: Boolean) {
            checks.put(name, holds)
        }

        /** Records [value] for the reader; decides nothing. */
        fun observe(name: String, value: Any?) {
            observations.put(name, value ?: JSONObject.NULL)
        }

        fun preconditionsHold(): Boolean = preconditions.keys().asSequence().all { key -> preconditions.getBoolean(key) }

        fun passed(): Boolean = error == null && checks.length() > 0 && preconditionsHold() &&
            checks.keys().asSequence().all { key -> checks.getBoolean(key) }

        fun toJson(): JSONObject = JSONObject()
            .put("step", name)
            .put("preconditions", preconditions)
            .put("checks", checks)
            .put("observations", observations)
            .put("error", error ?: JSONObject.NULL)
            .put("passed", passed())
    }

    private class Step(val record: Record, val phases: List<() -> Unit>)

    private val handler = Handler(Looper.getMainLooper())
    private val steps = mutableListOf<Step>()
    private val records = mutableListOf<Record>()

    /** Adds a step named [name], whose phases [phases] write into the record they are given. */
    fun add(name: String, phases: (Record) -> List<() -> Unit>) {
        val record = Record(name)
        steps += Step(record, phases(record))
    }

    /** Runs every step, one phase per turn of the main loop, and gives the report to [done]. */
    fun start(done: (JSONObject) -> Unit) {
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

    private fun report(): JSONObject = JSONObject()
        .put("steps", JSONArray(records.map { record -> record.toJson() }))
        .put("expectedSteps", expectedSteps)
        .put("stepsRun", records.size)
        .put("passed", records.count { record -> record.passed() })
        .put("failed", JSONArray(records.filter { record -> !record.passed() }.map { record -> record.name }))
        .put("allPassed", records.size == expectedSteps && records.all { record -> record.passed() })
}
