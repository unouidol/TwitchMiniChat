package com.fs.twitchminichat.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

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

    private val writeLock = Any()
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tmc-history-diagnostics").apply { isDaemon = true }
    }

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
     */
    fun record(
        context: Context,
        event: String,
        vararg fields: Pair<String, Any?>
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
            appendLine(applicationContext, line)
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
                        "# TwitchMiniChat history diagnostics"
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

    /** Removes every retained journal file. */
    fun clear(context: Context) {
        val applicationContext = context.applicationContext

        synchronized(writeLock) {
            directory(applicationContext)
                .listFiles()
                ?.forEach { file -> file.delete() }
        }
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
