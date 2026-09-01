package com.fs.twitchminichat

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests for the encrypted account file.
 *
 * A reversible test cipher replaces the Android Keystore so the file format, the
 * atomic replacement and the fail-closed reads can be verified locally.
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

        override fun encrypt(plainText: ByteArray): EncryptedPayload? {
            if (!encryptionWorks) return null
            return EncryptedPayload(
                initializationVector = VECTOR,
                cipherText = plainText.map { (it.toInt() xor MASK).toByte() }.toByteArray()
            )
        }

        override fun decrypt(payload: EncryptedPayload): ByteArray? {
            if (!decryptionWorks) return null
            if (!payload.initializationVector.contentEquals(VECTOR)) return null
            return payload.cipherText.map { (it.toInt() xor MASK).toByte() }.toByteArray()
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

        assertTrue(store.write(json))
        assertEquals(AccountJsonLookup.Present(json), store.read())
    }

    /** The stored bytes are not the plain-text account list. */
    @Test
    fun storedBytesAreNotPlainText() {
        val store = newStore()
        val json = """[{"accessToken":"secret-token"}]"""

        assertTrue(store.write(json))

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

        assertTrue(store.write("""["first"]"""))
        assertTrue(store.write("""["second"]"""))

        assertEquals(AccountJsonLookup.Present("""["second"]"""), store.read())
    }

    /** A truncated or foreign file is never returned as content. */
    @Test
    fun corruptedFile_isUnavailable() {
        val store = newStore()
        assertTrue(store.write("""["first"]"""))

        val file = File(File(temporaryFolder.root, "accounts_v1"), "accounts.bin")
        file.writeBytes(byteArrayOf(9, 9, 9, 9))

        assertEquals(AccountJsonLookup.Unavailable, store.read())
    }

    /** A key that can no longer decrypt fails closed instead of returning garbage. */
    @Test
    fun failedDecryption_isUnavailable() {
        val cipher = FakeCipher()
        val store = newStore(cipher)
        assertTrue(store.write("""["first"]"""))

        cipher.decryptionWorks = false

        assertEquals(AccountJsonLookup.Unavailable, store.read())
    }

    /** Nothing is written when the payload cannot be encrypted. */
    @Test
    fun failedEncryption_writesNothing() {
        val store = newStore(FakeCipher(encryptionWorks = false))

        assertFalse(store.write("""["first"]"""))
        assertEquals(AccountJsonLookup.Missing, store.read())
    }

    /** An empty payload is rejected rather than stored. */
    @Test
    fun blankJsonIsRejected() {
        val store = newStore()

        assertFalse(store.write("   "))
        assertEquals(AccountJsonLookup.Missing, store.read())
    }

    /** Clearing removes both the stored accounts and the key protecting them. */
    @Test
    fun clearRemovesAccountsAndKey() {
        val cipher = FakeCipher()
        val store = newStore(cipher)
        assertTrue(store.write("""["first"]"""))

        assertTrue(store.clear())
        assertEquals(AccountJsonLookup.Missing, store.read())
        assertTrue(cipher.keyDeleted)
    }
}
