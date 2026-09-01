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

    /** Storage exists but cannot be read or authenticated. */
    data object Unavailable : AccountJsonLookup
}

/** Storage contract for the serialized account list. */
internal interface AccountJsonStore {

    /** Returns the complete lookup state of the stored account list. */
    fun read(): AccountJsonLookup

    /** Replaces the stored account list. Returns false when nothing was written. */
    fun write(json: String): Boolean

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
            return@synchronized AccountJsonLookup.Unavailable
        }

        val payload = readCompletePayload(file)
            ?: return@synchronized AccountJsonLookup.Unavailable

        val plainText = cipher.decrypt(payload)
            ?: return@synchronized AccountJsonLookup.Unavailable

        val json = runCatching { plainText.toString(Charsets.UTF_8) }.getOrNull()
        if (json.isNullOrBlank()) {
            return@synchronized AccountJsonLookup.Unavailable
        }

        AccountJsonLookup.Present(json)
    }

    override fun write(json: String): Boolean = synchronized(STORE_LOCK) {
        if (json.isBlank()) return@synchronized false

        val payload = cipher.encrypt(json.toByteArray(Charsets.UTF_8))
            ?: return@synchronized false

        if (!storageDirectory.exists() && !storageDirectory.mkdirs()) {
            return@synchronized false
        }
        if (!storageDirectory.isDirectory) {
            return@synchronized false
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
    private fun writePayloadAtomically(targetFile: File, payload: EncryptedPayload): Boolean {
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
            true
        } catch (_: Exception) {
            false
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
        }
    }

    companion object {

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
