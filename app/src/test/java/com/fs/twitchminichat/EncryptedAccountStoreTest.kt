package com.fs.twitchminichat

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Failure type produced by the test cipher, standing in for a Keystore error. */
private const val CIPHER_FAILURE_TYPE = "TestCipherFailure"

/**
 * Unit tests for the encrypted account file.
 *
 * A reversible test cipher replaces the Android Keystore so the file format, the
 * atomic replacement and the fail-closed reads can be verified locally. The failure
 * outcomes are asserted by name too: they are what a diagnostic report will say when
 * a real device stops being able to read its own accounts.
 */
class EncryptedAccountStoreTest {

    /** Temporary app-private directory used as the no-backup store root. */
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** Reversible stand-in for the Keystore-backed cipher. */
    private class FakeCipher(
        var encryptionWorks: Boolean = true,
        var decryptionWorks: Boolean = true
    ) : AccountCipher {

        var keyDeleted: Boolean = false
            private set

        override fun encrypt(plainText: ByteArray): CipherOutcome<EncryptedPayload> {
            if (!encryptionWorks) return CipherOutcome.Failure(CIPHER_FAILURE_TYPE)

            return CipherOutcome.Success(
                EncryptedPayload(
                    initializationVector = VECTOR,
                    cipherText = plainText.map { (it.toInt() xor MASK).toByte() }.toByteArray()
                )
            )
        }

        override fun decrypt(payload: EncryptedPayload): CipherOutcome<ByteArray> {
            if (!decryptionWorks) return CipherOutcome.Failure(CIPHER_FAILURE_TYPE)
            if (!payload.initializationVector.contentEquals(VECTOR)) {
                return CipherOutcome.Failure(CIPHER_FAILURE_TYPE)
            }

            return CipherOutcome.Success(
                payload.cipherText.map { (it.toInt() xor MASK).toByte() }.toByteArray()
            )
        }

        override fun deleteKey(): Boolean {
            keyDeleted = true
            return true
        }

        private companion object {
            val VECTOR = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
            const val MASK = 0x5A
        }
    }

    private fun newStore(cipher: AccountCipher = FakeCipher()): EncryptedAccountStore {
        return EncryptedAccountStore(
            storageDirectory = File(temporaryFolder.root, "accounts_v1"),
            cipher = cipher
        )
    }

    /** A device with no stored accounts is reported as missing, not as a failure. */
    @Test
    fun absentFile_isMissing() {
        assertEquals(AccountJsonLookup.Missing, newStore().read())
    }

    /** A written list is returned unchanged. */
    @Test
    fun writtenListIsReadBack() {
        val store = newStore()
        val json = """[{"id":"a"}]"""

        assertEquals(AccountWriteOutcome.Success, store.write(json))
        assertEquals(AccountJsonLookup.Present(json), store.read())
    }

    /** The stored bytes are not the plain-text account list. */
    @Test
    fun storedBytesAreNotPlainText() {
        val store = newStore()
        val json = """[{"accessToken":"secret-token"}]"""

        assertEquals(AccountWriteOutcome.Success, store.write(json))

        val stored = File(temporaryFolder.root, "accounts_v1")
            .walkTopDown()
            .filter { it.isFile }
            .map { it.readBytes().toString(Charsets.ISO_8859_1) }
            .joinToString(separator = "")

        assertFalse(stored.contains("secret-token"))
    }

    /** A second write replaces the previous list. */
    @Test
    fun writeReplacesPreviousList() {
        val store = newStore()

        assertEquals(AccountWriteOutcome.Success, store.write("""["first"]"""))
        assertEquals(AccountWriteOutcome.Success, store.write("""["second"]"""))

        assertEquals(AccountJsonLookup.Present("""["second"]"""), store.read())
    }

    /** A truncated or foreign file is never returned as content. */
    @Test
    fun corruptedFile_isUnavailable() {
        val store = newStore()
        assertEquals(AccountWriteOutcome.Success, store.write("""["first"]"""))

        val file = File(File(temporaryFolder.root, "accounts_v1"), "accounts.bin")
        file.writeBytes(byteArrayOf(9, 9, 9, 9))

        assertEquals(
            AccountJsonLookup.Unavailable(EncryptedAccountStore.REASON_PAYLOAD_INVALID),
            store.read()
        )
    }

    /** A key that can no longer decrypt fails closed, and says which step failed. */
    @Test
    fun failedDecryption_isUnavailableWithItsCause() {
        val cipher = FakeCipher()
        val store = newStore(cipher)
        assertEquals(AccountWriteOutcome.Success, store.write("""["first"]"""))

        cipher.decryptionWorks = false

        assertEquals(
            AccountJsonLookup.Unavailable(
                reason = EncryptedAccountStore.REASON_DECRYPT_FAILED,
                errorType = CIPHER_FAILURE_TYPE
            ),
            store.read()
        )
    }

    /** Nothing is written when the payload cannot be encrypted. */
    @Test
    fun failedEncryption_writesNothing() {
        val store = newStore(FakeCipher(encryptionWorks = false))

        assertEquals(
            AccountWriteOutcome.Failure(
                reason = EncryptedAccountStore.REASON_ENCRYPT_FAILED,
                errorType = CIPHER_FAILURE_TYPE
            ),
            store.write("""["first"]""")
        )
        assertEquals(AccountJsonLookup.Missing, store.read())
    }

    /** An empty payload is rejected rather than stored. */
    @Test
    fun blankJsonIsRejected() {
        val store = newStore()

        assertEquals(
            AccountWriteOutcome.Failure(EncryptedAccountStore.REASON_BLANK_CONTENT),
            store.write("   ")
        )
        assertEquals(AccountJsonLookup.Missing, store.read())
    }

    /** Clearing removes both the stored accounts and the key protecting them. */
    @Test
    fun clearRemovesAccountsAndKey() {
        val cipher = FakeCipher()
        val store = newStore(cipher)
        assertEquals(AccountWriteOutcome.Success, store.write("""["first"]"""))

        assertTrue(store.clear())
        assertEquals(AccountJsonLookup.Missing, store.read())
        assertTrue(cipher.keyDeleted)
    }
}
