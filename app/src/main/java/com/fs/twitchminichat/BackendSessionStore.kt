package com.fs.twitchminichat

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Result of looking up one revocable backend session.
 *
 * [Unavailable] is intentionally distinct from [Missing]. A malformed or unreadable
 * session file must not silently enable the legacy authentication path.
 */
sealed interface BackendSessionLookup {

    /** No backend session has been stored for the requested profile. */
    data object Missing : BackendSessionLookup

    /** A complete backend session is available for the requested profile. */
    data class Present(val token: String) : BackendSessionLookup

    /** Session storage exists but cannot be trusted or read safely. */
    data object Unavailable : BackendSessionLookup
}

/** Read-only contract used by backend authentication policy classes. */
fun interface BackendSessionReader {

    /** Returns the complete lookup state for a normalized profile identifier. */
    fun lookup(profileId: String): BackendSessionLookup
}

/**
 * Stores revocable backend sessions in app-private, non-backed-up storage.
 *
 * Each normalized profile uses a separate atomically replaced file. File contents are
 * parsed completely before a token is returned; malformed data is never used partially.
 */
class BackendSessionStore internal constructor(
    private val storageDirectory: File
) : BackendSessionReader {

    /** Creates the production store below [Context.getNoBackupFilesDir]. */
    constructor(context: Context) : this(
        File(context.applicationContext.noBackupFilesDir, DIRECTORY_NAME)
    )

    /** Returns the session associated with [profileId] without logging sensitive data. */
    override fun lookup(profileId: String): BackendSessionLookup = synchronized(STORE_LOCK) {
        val normalizedProfileId = normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            return@synchronized BackendSessionLookup.Missing
        }

        val sessionFile = sessionFile(normalizedProfileId)
        if (!sessionFile.exists()) {
            return@synchronized BackendSessionLookup.Missing
        }
        if (!sessionFile.isFile || sessionFile.length() !in 1..MAX_FILE_SIZE_BYTES) {
            return@synchronized BackendSessionLookup.Unavailable
        }

        val token = readCompleteToken(sessionFile)
            ?: return@synchronized BackendSessionLookup.Unavailable

        BackendSessionLookup.Present(token)
    }

    /**
     * Atomically stores [sessionToken] for [profileId].
     *
     * Returns false when either value is blank or the durable replacement fails.
     */
    fun putSession(profileId: String, sessionToken: String): Boolean = synchronized(STORE_LOCK) {
        val normalizedProfileId = normalizeProfileId(profileId)
        val normalizedToken = sessionToken.trim()
        if (normalizedProfileId.isBlank() || normalizedToken.isBlank()) {
            return@synchronized false
        }

        if (!storageDirectory.exists() && !storageDirectory.mkdirs()) {
            return@synchronized false
        }
        if (!storageDirectory.isDirectory) {
            return@synchronized false
        }

        writeTokenAtomically(
            targetFile = sessionFile(normalizedProfileId),
            token = normalizedToken
        )
    }

    /**
     * Removes only the session associated with [profileId].
     *
     * Other profile files are never opened or changed by this operation.
     */
    fun removeProfile(profileId: String): Boolean = synchronized(STORE_LOCK) {
        val normalizedProfileId = normalizeProfileId(profileId)
        if (normalizedProfileId.isBlank()) {
            return@synchronized true
        }

        val targetFile = sessionFile(normalizedProfileId)
        !targetFile.exists() || targetFile.delete()
    }

    /** Deletes every locally stored backend session. */
    fun clearAll(): Boolean = synchronized(STORE_LOCK) {
        !storageDirectory.exists() || storageDirectory.deleteRecursively()
    }

    /** Reads and validates an entire session file before returning its token. */
    private fun readCompleteToken(sessionFile: File): String? {
        return runCatching {
            DataInputStream(BufferedInputStream(sessionFile.inputStream())).use { input ->
                if (input.readUTF() != FILE_MAGIC) return@use null
                if (input.readInt() != FILE_VERSION) return@use null

                val token = input.readUTF().trim()
                if (token.isBlank() || input.read() != -1) return@use null

                token
            }
        }.getOrNull()
    }

    /** Writes a synchronized temporary file and replaces the target in one atomic move. */
    private fun writeTokenAtomically(targetFile: File, token: String): Boolean {
        val temporaryFile = File(
            storageDirectory,
            "${targetFile.name}.${UUID.randomUUID()}.tmp"
        )

        return try {
            FileOutputStream(temporaryFile).use { fileOutput ->
                val output = DataOutputStream(BufferedOutputStream(fileOutput))
                output.writeUTF(FILE_MAGIC)
                output.writeInt(FILE_VERSION)
                output.writeUTF(token)
                output.flush()
                fileOutput.fd.sync()
            }

            Files.move(
                temporaryFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
            true
        } catch (_: Exception) {
            false
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
        }
    }

    /** Returns the non-reversible file name for one normalized profile identifier. */
    private fun sessionFile(normalizedProfileId: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(normalizedProfileId.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte ->
                val value = byte.toInt() and 0xff
                "${HEX_DIGITS[value ushr 4]}${HEX_DIGITS[value and 0x0f]}"
            }

        return File(storageDirectory, "$digest.session")
    }

    companion object {

        /** Normalizes backend profile identifiers consistently across all session callers. */
        fun normalizeProfileId(profileId: String): String {
            return profileId.trim().lowercase(Locale.ROOT)
        }

        /** Directory kept outside Android backup and device-transfer payloads. */
        private const val DIRECTORY_NAME = "backend_sessions_v1"

        /** Binary file marker used to reject unrelated or partially written content. */
        private const val FILE_MAGIC = "TMC_BACKEND_SESSION"

        /** Current binary session-file format. */
        private const val FILE_VERSION = 1

        /** Defensive upper bound for a single serialized session. */
        private const val MAX_FILE_SIZE_BYTES = 128L * 1024L

        /** Lowercase alphabet used for Secure Hash Algorithm 256-bit (SHA-256) file names. */
        private const val HEX_DIGITS = "0123456789abcdef"

        /** Process-wide lock preventing concurrent replacement of session files. */
        private val STORE_LOCK = Any()
    }
}
