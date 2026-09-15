package com.fs.twitchminichat.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Append-only diagnostic journal for chat history backfill behavior.
 *
 * The journal deliberately records metadata only: wall-clock instants, counters,
 * lifecycle transitions, requested windows and skip reasons. Message text,
 * authentication material and message identifiers are never written, so an
 * exported journal stays safe to share without further redaction.
 *
 * Writing happens on a dedicated single background thread. The event instant is
 * captured on the calling thread so that queueing never reorders the timeline.
 */
object HistoryDiagnosticsLog {

    private const val DIRECTORY_NAME = "diagnostics"
    private const val CURRENT_FILE_NAME = "history-diagnostics.log"
    private const val PREVIOUS_FILE_NAME = "history-diagnostics-previous.log"
    private const val EXPORT_FILE_NAME = "tmc-history-diagnostics.txt"

    private const val MAX_FILE_BYTES = 512L * 1024L
    private const val MAX_VALUE_LENGTH = 64

    /**
     * Longest wait for [clear]. The writer's queue holds a handful of
     * single-line appends, each far shorter than this.
     *
     * Every caller today runs on the main thread: the reset dialog's buttons
     * directly, and the server-deletion paths from the Gecko clear callback,
     * which is delivered on the main looper. The number matters less than what
     * surrounds it. The rest of LocalDataCleaner.clearInternal does unbounded
     * I/O on that thread - deleting every shared_prefs file and emptying
     * cacheDir and codeCacheDir - and this wait is the only part of a reset
     * with a limit. That is known and not yet corrected; it is written here so
     * that a bounded wait is not read as a bounded reset.
     */
    private const val CLEAR_TIMEOUT_MS = 1_000L

    private val writeLock = Any()
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tmc-history-diagnostics").apply { isDaemon = true }
    }

    /** Advanced by every [clear], on the writer, before its files are deleted. */
    private val journalGeneration = JournalGeneration()

    /**
     * Returns the journal's current generation, for a caller about to start work
     * whose line will be written later.
     *
     * Pass it back as `expectedGeneration` to [record] when the work finishes.
     * If the journal was erased in between, the line is dropped: it would
     * otherwise be the first line of the fresh journal while describing, with
     * its account and channel, something that began before the erase.
     */
    fun generation(): Long = journalGeneration.current()

    /** Returns the directory holding the journal files. */
    fun directory(context: Context): File {
        val directory = File(
            context.applicationContext.filesDir,
            DIRECTORY_NAME
        )
        directory.mkdirs()
        return directory
    }

    /** Returns the file currently receiving journal lines. */
    fun currentFile(context: Context): File {
        return File(directory(context), CURRENT_FILE_NAME)
    }

    /**
     * Queues one journal entry.
     *
     * Field values are sanitized and truncated, so callers may pass channel or
     * account names without preparing them.
     *
     * [expectedGeneration] is the value [generation] returned when the work
     * behind this line started, or null for a line describing the present. It is
     * compared on the writer, after every queued erase has run, so an erase
     * requested before this call is always seen.
     */
    fun record(
        context: Context,
        event: String,
        vararg fields: Pair<String, Any?>,
        expectedGeneration: Long? = null
    ) {
        val applicationContext = context.applicationContext
        val instant = System.currentTimeMillis()

        val renderedFields = fields
            .filter { (_, value) -> value != null }
            .joinToString(separator = " ") { (key, value) ->
                "${sanitize(key)}=${sanitize(value.toString())}"
            }

        val line = buildString {
            append(formatInstant(instant))
            append(' ')
            append(sanitize(event))

            if (renderedFields.isNotEmpty()) {
                append(' ')
                append(renderedFields)
            }
        }

        writer.execute {
            if (journalGeneration.accepts(expectedGeneration)) {
                appendLine(applicationContext, line)
            }
        }
    }

    /**
     * Builds one shareable snapshot containing the retained journal history.
     *
     * Returns null when nothing has been recorded yet.
     */
    fun exportSnapshot(context: Context): File? {
        val applicationContext = context.applicationContext

        return synchronized(writeLock) {
            val directory = directory(applicationContext)
            val previous = File(directory, PREVIOUS_FILE_NAME)
            val current = File(directory, CURRENT_FILE_NAME)

            if (!previous.exists() && !current.exists()) {
                return@synchronized null
            }

            val export = File(directory, EXPORT_FILE_NAME)

            runCatching {
                export.bufferedWriter().use { output ->
                    output.appendLine(
                        "# TwitchMiniChat diagnostics"
                    )
                    output.appendLine(
                        "# exported ${formatInstant(System.currentTimeMillis())}"
                    )
                    output.appendLine(
                        "# device ${sanitize(Build.MODEL)} " +
                                "android ${Build.VERSION.SDK_INT}"
                    )
                    output.appendLine(
                        "# metadata only: no message text, no credentials"
                    )
                    output.appendLine()

                    listOf(previous, current)
                        .filter(File::exists)
                        .forEach { source ->
                            source.forEachLine { line ->
                                output.appendLine(line)
                            }
                        }
                }
            }.getOrNull() ?: return@synchronized null

            export
        }
    }

    /**
     * Removes every retained journal file, in order with the lines already queued.
     *
     * The deletion runs on the writer rather than beside it. Run on the calling
     * thread, it could land before a line that record() had already queued, and
     * that line would then recreate the file with its account and channel in it.
     *
     * The caller waits for the deletion, for at most [CLEAR_TIMEOUT_MS], so that
     * whatever it reports afterwards comes after the files are gone. On timeout
     * the deletion is left queued and still runs; it is only unconfirmed.
     *
     * Returns true when the deletion ran and removed every file.
     */
    fun clear(context: Context): Boolean {
        val applicationContext = context.applicationContext

        return runCatching {
            writer.submit<Boolean> {
                synchronized(writeLock) {
                    /* First, so that a failed deletion still retires older work. */
                    journalGeneration.advance()

                    directory(applicationContext)
                        .listFiles()
                        .orEmpty()
                        .map { file -> file.delete() }
                        .all { deleted -> deleted }
                }
            }.get(CLEAR_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }.getOrDefault(false)
    }

    private fun appendLine(context: Context, line: String) {
        synchronized(writeLock) {
            runCatching {
                val directory = directory(context)
                val current = File(directory, CURRENT_FILE_NAME)

                if (current.exists() && current.length() >= MAX_FILE_BYTES) {
                    val previous = File(directory, PREVIOUS_FILE_NAME)
                    previous.delete()
                    current.renameTo(previous)
                }

                val isNewFile = !current.exists()

                current.appendText(
                    buildString {
                        if (isNewFile) {
                            append(
                                "# journal started " +
                                        formatInstant(System.currentTimeMillis())
                            )
                            append('\n')
                        }

                        append(line)
                        append('\n')
                    }
                )
            }
        }
    }

    private fun formatInstant(instant: Long): String {
        val format = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            Locale.US
        )
        return format.format(Date(instant))
    }

    /**
     * Reduces one value to a compact, shell-safe and content-free token.
     *
     * Characters outside the allowed set are replaced rather than dropped so that
     * an unexpected value stays visible as an anomaly instead of disappearing.
     */
    private fun sanitize(value: String): String {
        val collapsed = value
            .take(MAX_VALUE_LENGTH)
            .map { character ->
                when {
                    character.isLetterOrDigit() -> character
                    character in "._:@#/+-" -> character
                    else -> '_'
                }
            }
            .joinToString(separator = "")

        return collapsed.ifBlank { "_" }
    }
}

/**
 * Erase counter behind [HistoryDiagnosticsLog.generation].
 *
 * Kept apart from the journal so the decision it makes - write this line or drop
 * it - can be tested without a Context or a file.
 */
internal class JournalGeneration {

    private val value = AtomicLong(0L)

    /** Returns the generation work starting now belongs to. */
    fun current(): Long = value.get()

    /** Retires every generation handed out so far. */
    fun advance() {
        value.incrementAndGet()
    }

    /**
     * Whether a line belongs in the journal as it is now.
     *
     * A line with no expected generation describes the present and is always
     * kept. A line from work that started under an earlier generation is dropped,
     * while work started after the erase keeps writing.
     */
    fun accepts(expectedGeneration: Long?): Boolean {
        return expectedGeneration == null || expectedGeneration == value.get()
    }
}
