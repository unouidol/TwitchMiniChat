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
 * Encrypts and decrypts locally stored account credentials.
 *
 * Extracted as an interface so the surrounding store can be covered by local unit
 * tests without an Android Keystore.
 */
internal interface AccountCipher {

    /** Returns the encrypted form of [plainText], or null when encryption fails. */
    fun encrypt(plainText: ByteArray): EncryptedPayload?

    /** Returns the decrypted bytes, or null when the payload cannot be trusted. */
    fun decrypt(payload: EncryptedPayload): ByteArray?

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

    override fun encrypt(plainText: ByteArray): EncryptedPayload? {
        return runCatching {
            val secretKey = getOrCreateKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            EncryptedPayload(
                initializationVector = cipher.iv,
                cipherText = cipher.doFinal(plainText)
            )
        }.getOrNull()
    }

    override fun decrypt(payload: EncryptedPayload): ByteArray? {
        if (payload.initializationVector.isEmpty() || payload.cipherText.isEmpty()) {
            return null
        }

        return runCatching {
            val secretKey = loadExistingKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(AUTHENTICATION_TAG_BITS, payload.initializationVector)
            )

            cipher.doFinal(payload.cipherText)
        }.getOrNull()
    }

    override fun deleteKey(): Boolean {
        return runCatching {
            val keyStore = openKeyStore() ?: return false
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
            true
        }.getOrDefault(false)
    }

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
