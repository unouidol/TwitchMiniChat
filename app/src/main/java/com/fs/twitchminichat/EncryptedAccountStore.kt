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
import java.util.UUID

/**
 * Result of reading the locally stored account list.
 *
 * [Unavailable] is intentionally distinct from [Missing]. An unreadable or tampered
 * file must never be treated as "no accounts yet" in a way that could silently fall
 * back to a less protected storage path.
 */
internal sealed interface AccountJsonLookup {

    /** No account list has been stored on this device. */
    data object Missing : AccountJsonLookup

    /** A complete account list is available. */
    data class Present(val json: String) : AccountJsonLookup

    /**
     * Storage exists but cannot be read or authenticated.
     *
     * [reason] names the step that failed and [errorType] the exception class behind
     * it. Both come from vocabularies defined in code and neither can carry stored
     * content, which is what makes them safe to put in a diagnostic report.
     */
    data class Unavailable(
        val reason: String,
        val errorType: String? = null
    ) : AccountJsonLookup
}

/**
 * Result of replacing the stored account list.
 *
 * A failed write is reported rather than returned as a bare false, because the user
 * has already been shown the change as if it had been saved: a silent failure here
 * is an account that disappears at the next start-up with no trace of why.
 */
internal sealed interface AccountWriteOutcome {

    /** The account list was stored. */
    data object Success : AccountWriteOutcome

    /** Nothing was stored. See [AccountJsonLookup.Unavailable] for the field rules. */
    data class Failure(
        val reason: String,
        val errorType: String? = null
    ) : AccountWriteOutcome
}

/** Storage contract for the serialized account list. */
internal interface AccountJsonStore {

    /** Returns the complete lookup state of the stored account list. */
    fun read(): AccountJsonLookup

    /** Replaces the stored account list. */
    fun write(json: String): AccountWriteOutcome

    /** Removes every stored account and the key protecting them. */
    fun clear(): Boolean
}

/**
 * Stores the account list encrypted, in app-private, non-backed-up storage.
 *
 * The file is replaced atomically and validated completely before any content is
 * returned, mirroring [BackendSessionStore]. Because the encryption key lives in the
 * Android Keystore, a copy of this file taken off the device cannot be decrypted.
 */
internal class EncryptedAccountStore internal constructor(
    private val storageDirectory: File,
    private val cipher: AccountCipher = AccountSecretCipher
) : AccountJsonStore {

    /** Creates the production store below [Context.getNoBackupFilesDir]. */
    constructor(context: Context) : this(
        File(context.applicationContext.noBackupFilesDir, DIRECTORY_NAME)
    )

    private val accountFile: File
        get() = File(storageDirectory, FILE_NAME)

    override fun read(): AccountJsonLookup = synchronized(STORE_LOCK) {
        val file = accountFile
        if (!file.exists()) {
            return@synchronized AccountJsonLookup.Missing
        }
        if (!file.isFile || file.length() !in 1..MAX_FILE_SIZE_BYTES) {
            return@synchronized AccountJsonLookup.Unavailable(REASON_FILE_SHAPE)
        }

        val payload = readCompletePayload(file)
            ?: return@synchronized AccountJsonLookup.Unavailable(REASON_PAYLOAD_INVALID)

        val plainText = when (val decrypted = cipher.decrypt(payload)) {
            is CipherOutcome.Success -> decrypted.value
            is CipherOutcome.Failure -> return@synchronized AccountJsonLookup.Unavailable(
                reason = REASON_DECRYPT_FAILED,
                errorType = decrypted.errorType
            )
        }

        val json = runCatching { plainText.toString(Charsets.UTF_8) }.getOrNull()
        if (json.isNullOrBlank()) {
            return@synchronized AccountJsonLookup.Unavailable(REASON_CONTENT_INVALID)
        }

        AccountJsonLookup.Present(json)
    }

    override fun write(json: String): AccountWriteOutcome = synchronized(STORE_LOCK) {
        if (json.isBlank()) {
            return@synchronized AccountWriteOutcome.Failure(REASON_BLANK_CONTENT)
        }

        val payload = when (
            val encrypted = cipher.encrypt(json.toByteArray(Charsets.UTF_8))
        ) {
            is CipherOutcome.Success -> encrypted.value
            is CipherOutcome.Failure -> return@synchronized AccountWriteOutcome.Failure(
                reason = REASON_ENCRYPT_FAILED,
                errorType = encrypted.errorType
            )
        }

        if (!storageDirectory.exists() && !storageDirectory.mkdirs()) {
            return@synchronized AccountWriteOutcome.Failure(REASON_DIRECTORY_UNAVAILABLE)
        }
        if (!storageDirectory.isDirectory) {
            return@synchronized AccountWriteOutcome.Failure(REASON_DIRECTORY_UNAVAILABLE)
        }

        writePayloadAtomically(accountFile, payload)
    }

    override fun clear(): Boolean = synchronized(STORE_LOCK) {
        val filesRemoved = !storageDirectory.exists() || storageDirectory.deleteRecursively()
        val keyRemoved = cipher.deleteKey()
        filesRemoved && keyRemoved
    }

    /** Reads and validates the whole file before returning its payload. */
    private fun readCompletePayload(file: File): EncryptedPayload? {
        return runCatching {
            DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
                if (input.readUTF() != FILE_MAGIC) return@use null
                if (input.readInt() != FILE_VERSION) return@use null

                val vectorLength = input.readInt()
                if (vectorLength !in 1..MAX_VECTOR_BYTES) return@use null
                val initializationVector = ByteArray(vectorLength)
                input.readFully(initializationVector)

                val cipherLength = input.readInt()
                if (cipherLength !in 1..MAX_CIPHER_BYTES) return@use null
                val cipherText = ByteArray(cipherLength)
                input.readFully(cipherText)

                if (input.read() != -1) return@use null

                EncryptedPayload(
                    initializationVector = initializationVector,
                    cipherText = cipherText
                )
            }
        }.getOrNull()
    }

    /** Writes a synchronized temporary file and replaces the target in one move. */
    private fun writePayloadAtomically(
        targetFile: File,
        payload: EncryptedPayload
    ): AccountWriteOutcome {
        val temporaryFile = File(
            storageDirectory,
            "${targetFile.name}.${UUID.randomUUID()}.tmp"
        )

        return try {
            FileOutputStream(temporaryFile).use { fileOutput ->
                val output = DataOutputStream(BufferedOutputStream(fileOutput))
                output.writeUTF(FILE_MAGIC)
                output.writeInt(FILE_VERSION)
                output.writeInt(payload.initializationVector.size)
                output.write(payload.initializationVector)
                output.writeInt(payload.cipherText.size)
                output.write(payload.cipherText)
                output.flush()
                fileOutput.fd.sync()
            }

            Files.move(
                temporaryFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
            AccountWriteOutcome.Success
        } catch (error: Exception) {
            AccountWriteOutcome.Failure(
                reason = REASON_ATOMIC_WRITE_FAILED,
                errorType = DiagnosticError.typeOf(error)
            )
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
        }
    }

    companion object {

        /** The stored file is not a regular file of a plausible size. */
        internal const val REASON_FILE_SHAPE = "file_shape"

        /** The file header or its framing did not survive validation. */
        internal const val REASON_PAYLOAD_INVALID = "payload_invalid"

        /** The Keystore could not authenticate the stored bytes. */
        internal const val REASON_DECRYPT_FAILED = "decrypt_failed"

        /** The decrypted bytes are not usable text. */
        internal const val REASON_CONTENT_INVALID = "content_invalid"

        /** Nothing was offered to store. */
        internal const val REASON_BLANK_CONTENT = "blank_content"

        /** The account list could not be encrypted. */
        internal const val REASON_ENCRYPT_FAILED = "encrypt_failed"

        /** The private storage directory could not be created or used. */
        internal const val REASON_DIRECTORY_UNAVAILABLE = "directory_unavailable"

        /** The temporary file could not be written or moved into place. */
        internal const val REASON_ATOMIC_WRITE_FAILED = "atomic_write_failed"

        /** Directory kept outside Android backup and device-transfer payloads. */
        private const val DIRECTORY_NAME = "accounts_v1"

        /** Single file holding the encrypted account list. */
        private const val FILE_NAME = "accounts.bin"

        /** Binary marker used to reject unrelated or partially written content. */
        private const val FILE_MAGIC = "TMC_ACCOUNT_STORE"

        /** Current binary account-file format. */
        private const val FILE_VERSION = 1

        /** Defensive upper bound for the whole serialized account list. */
        private const val MAX_FILE_SIZE_BYTES = 1024L * 1024L

        /** Defensive upper bound for a Galois/Counter Mode initialization vector. */
        private const val MAX_VECTOR_BYTES = 64

        /** Defensive upper bound for the encrypted account list. */
        private const val MAX_CIPHER_BYTES = 1024 * 1024

        /** Process-wide lock preventing concurrent replacement of the account file. */
        private val STORE_LOCK = Any()
    }
}
