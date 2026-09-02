package com.fs.twitchminichat

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * One encrypted blob together with the initialization vector needed to read it.
 *
 * The vector is not secret and is stored next to the ciphertext. A fresh vector is
 * produced for every encryption, which Galois/Counter Mode (GCM) requires.
 */
internal class EncryptedPayload(
    val initializationVector: ByteArray,
    val cipherText: ByteArray
)

/**
 * Outcome of one cipher operation.
 *
 * A failure carries the type of the error and nothing else. Keystore and cipher
 * exceptions can embed key aliases and payload fragments in their messages, so only
 * the class name survives, which is what makes an outcome safe to put in a
 * diagnostic report.
 */
internal sealed interface CipherOutcome<out T> {

    /** The operation completed and produced [value]. */
    data class Success<T>(val value: T) : CipherOutcome<T>

    /** The operation failed. [errorType] is an exception class name, never a message. */
    data class Failure(val errorType: String) : CipherOutcome<Nothing>
}

/**
 * Encrypts and decrypts locally stored account credentials.
 *
 * Extracted as an interface so the surrounding store can be covered by local unit
 * tests without an Android Keystore.
 */
internal interface AccountCipher {

    /** Returns the encrypted form of [plainText], or the type of the failure. */
    fun encrypt(plainText: ByteArray): CipherOutcome<EncryptedPayload>

    /** Returns the decrypted bytes, or the type of the failure. */
    fun decrypt(payload: EncryptedPayload): CipherOutcome<ByteArray>

    /** Removes the key so previously written data can never be read again. */
    fun deleteKey(): Boolean
}

/**
 * Advanced Encryption Standard (AES) encryption backed by the Android Keystore.
 *
 * The key never leaves the Keystore and is not exported by any backup or device
 * transfer, so an encrypted account file copied off the device is unreadable. The key
 * is deliberately not bound to user authentication: the application must be able to
 * reconnect chat sessions without an unlock prompt.
 */
internal object AccountSecretCipher : AccountCipher {

    override fun encrypt(plainText: ByteArray): CipherOutcome<EncryptedPayload> = runCatching {
        val secretKey = getOrCreateKey() ?: throw KeyUnavailableException()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        EncryptedPayload(
            initializationVector = cipher.iv,
            cipherText = cipher.doFinal(plainText)
        )
    }.toCipherOutcome()

    override fun decrypt(payload: EncryptedPayload): CipherOutcome<ByteArray> = runCatching {
        if (payload.initializationVector.isEmpty() || payload.cipherText.isEmpty()) {
            throw EmptyPayloadException()
        }

        val secretKey = loadExistingKey() ?: throw KeyUnavailableException()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(AUTHENTICATION_TAG_BITS, payload.initializationVector)
        )

        cipher.doFinal(payload.cipherText)
    }.toCipherOutcome()

    override fun deleteKey(): Boolean {
        return runCatching {
            val keyStore = openKeyStore() ?: return false
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
            true
        }.getOrDefault(false)
    }

    /** Keeps only the type of a caught failure, discarding its message. */
    private fun <T> Result<T>.toCipherOutcome(): CipherOutcome<T> = fold(
        onSuccess = { value -> CipherOutcome.Success(value) },
        onFailure = { error -> CipherOutcome.Failure(DiagnosticError.typeOf(error)) }
    )

    /** Returns the stored key, or null when none exists or it cannot be recovered. */
    private fun loadExistingKey(): SecretKey? {
        return runCatching {
            val keyStore = openKeyStore() ?: return null
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        }.getOrNull()
    }

    /** Returns the stored key, creating a new one on first use. */
    private fun getOrCreateKey(): SecretKey? {
        loadExistingKey()?.let { return it }

        return runCatching {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .build()
            )
            generator.generateKey()
        }.getOrNull()
    }

    /** Opens the Android Keystore, or null when the platform refuses access. */
    private fun openKeyStore(): KeyStore? {
        return runCatching {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        }.getOrNull()
    }

    /** The Keystore holds no key usable for the account store. */
    private class KeyUnavailableException : Exception()

    /** The stored payload is structurally empty and cannot be authenticated. */
    private class EmptyPayloadException : Exception()

    /** Hardware-backed key container provided by the platform. */
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    /** Alias of the key protecting locally stored account credentials. */
    private const val KEY_ALIAS = "tmc_account_store_v1"

    /** Authenticated encryption without padding, required for GCM. */
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    /** Standard GCM authentication tag length. */
    private const val AUTHENTICATION_TAG_BITS = 128

    /** Key length used for new keys. */
    private const val KEY_SIZE_BITS = 256
}
