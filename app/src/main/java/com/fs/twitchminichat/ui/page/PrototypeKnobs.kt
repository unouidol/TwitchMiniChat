package com.fs.twitchminichat.ui.page

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.fs.twitchminichat.BuildConfig
import com.fs.twitchminichat.R

/**
 * The prototype's switches: the list's floor in rows, the stream button's labels, and
 * display-only demo names. Set from the computer, in the dev flavour only, with
 *
 * `adb shell am broadcast -a com.fs.twitchminichat.PROTOTYPE_KNOBS -p com.fs.twitchminichat.dev
 * --ei floor_rows 3 --es stream_labels watch --es demo_user <name> --es demo_channel <name>`
 *
 * and `--ez demo_clear true` to drop the demo names. Every extra is optional.
 *
 * PROTOTYPE (branch `prototype/5d-layout-b`), not for merging. None of these values is a choice:
 * the defaults exist only so the prototype has a value, and Federico chooses from the pictures.
 */
class PrototypeKnobs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The stream button's two label sets. */
    enum class StreamLabels(val wire: String, @param:StringRes val whenOff: Int, @param:StringRes val whenOn: Int) {
        /** Today's labels: "Video ON" while the player is off, "Video OFF" while it is on. */
        TODAY("today", R.string.stream_off, R.string.stream_on),

        /** The action labels "Watch stream" and "Hide stream". */
        WATCH("watch", R.string.stream_watch, R.string.stream_hide);

        companion object {
            /** The set named [wire], or null for an unknown name. */
            fun fromWire(wire: String?): StreamLabels? = values().firstOrNull { it.wire == wire }
        }
    }

    /** The list's floor, in chat rows. */
    val floorRows: Int get() = clampRows(prefs.getInt(KEY_FLOOR_ROWS, DEFAULT_FLOOR_ROWS))

    /** The stream button's labels. */
    val streamLabels: StreamLabels get() = StreamLabels.fromWire(prefs.getString(KEY_STREAM_LABELS, null)) ?: StreamLabels.TODAY

    /** A display-only user name, or null. */
    val demoUser: String? get() = prefs.getString(KEY_DEMO_USER, null)

    /** A display-only channel name, or null. */
    val demoChannel: String? get() = prefs.getString(KEY_DEMO_CHANNEL, null)

    /** Stores what [intent] carries. Unknown or missing extras change nothing. */
    fun apply(intent: Intent) {
        prefs.edit {
            if (intent.hasExtra(EXTRA_FLOOR_ROWS)) {
                putInt(KEY_FLOOR_ROWS, clampRows(intent.getIntExtra(EXTRA_FLOOR_ROWS, DEFAULT_FLOOR_ROWS)))
            }
            StreamLabels.fromWire(intent.getStringExtra(EXTRA_STREAM_LABELS))?.let { putString(KEY_STREAM_LABELS, it.wire) }
            intent.getStringExtra(EXTRA_DEMO_USER)?.let { putString(KEY_DEMO_USER, it) }
            intent.getStringExtra(EXTRA_DEMO_CHANNEL)?.let { putString(KEY_DEMO_CHANNEL, it) }
            if (intent.getBooleanExtra(EXTRA_DEMO_CLEAR, false)) {
                remove(KEY_DEMO_USER)
                remove(KEY_DEMO_CHANNEL)
            }
        }
        Log.d(TAG, "knobs floorRows=$floorRows streamLabels=${streamLabels.wire} demoNames=${demoUser != null || demoChannel != null}")
    }

    /**
     * Registers a receiver for the switches on [context] while the page is started, in the dev
     * flavour only, and returns it for [unregister]; returns null in any other flavour.
     *
     * The receiver is exported because the broadcast comes from the computer's shell, which a
     * non-exported receiver does not hear. It only changes what this page displays.
     */
    fun register(context: Context, onChanged: () -> Unit): BroadcastReceiver? {
        if (!ENABLED) return null
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                apply(intent)
                onChanged()
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(ACTION), ContextCompat.RECEIVER_EXPORTED)
        return receiver
    }

    /** Unregisters what [register] returned. */
    fun unregister(context: Context, receiver: BroadcastReceiver?) {
        if (receiver != null) context.unregisterReceiver(receiver)
    }

    companion object {
        /** True in the dev flavour, the only one the prototype's switches exist in. */
        val ENABLED: Boolean = BuildConfig.FLAVOR == "dev"

        /** The broadcast that sets the switches. */
        const val ACTION = "com.fs.twitchminichat.PROTOTYPE_KNOBS"

        /** The default floor, only so the prototype has one. */
        const val DEFAULT_FLOOR_ROWS = 3

        /** The floor's range, in rows. */
        const val MIN_FLOOR_ROWS = 0
        const val MAX_FLOOR_ROWS = 8

        /** [rows], kept within [MIN_FLOOR_ROWS] and [MAX_FLOOR_ROWS]. */
        fun clampRows(rows: Int): Int = rows.coerceIn(MIN_FLOOR_ROWS, MAX_FLOOR_ROWS)

        private const val PREFS_NAME = "prototype_knobs"
        private const val KEY_FLOOR_ROWS = "floor_rows"
        private const val KEY_STREAM_LABELS = "stream_labels"
        private const val KEY_DEMO_USER = "demo_user"
        private const val KEY_DEMO_CHANNEL = "demo_channel"

        private const val EXTRA_FLOOR_ROWS = "floor_rows"
        private const val EXTRA_STREAM_LABELS = "stream_labels"
        private const val EXTRA_DEMO_USER = "demo_user"
        private const val EXTRA_DEMO_CHANNEL = "demo_channel"
        private const val EXTRA_DEMO_CLEAR = "demo_clear"

        private const val TAG = "CHAT_PAGE"
    }
}
