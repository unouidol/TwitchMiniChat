package com.fs.twitchminichat.pcg.mostwanted

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Unit tests for bounded strict UTF-8 backup document input/output. */
class PcgMostWantedBackupDocumentIoTest {

    /** Valid UTF-8 text is preserved without platform-default encoding. */
    @Test
    fun writeAndRead_preserveUnicodeText() {
        val text = "Pal Wooper\nNidoran\u2640\nFarfetch\u2019d\n"
        val output = ByteArrayOutputStream()

        PcgMostWantedBackupDocumentIo.writeUtf8(output, text)

        val bytes = output.toByteArray()
        assertArrayEquals(text.toByteArray(Charsets.UTF_8), bytes)
        assertEquals(
            text,
            PcgMostWantedBackupDocumentIo.readUtf8(
                ByteArrayInputStream(bytes)
            )
        )
    }

    /** Oversized input is rejected before the full document is allocated. */
    @Test
    fun readUtf8_rejectsOversizedDocument() {
        val oversized = ByteArray(
            PcgMostWantedBackupDocumentIo.MAX_DOCUMENT_BYTES + 1
        ) { 'a'.code.toByte() }

        val error = assertThrows(
            PcgMostWantedBackupDocumentException::class.java
        ) {
            PcgMostWantedBackupDocumentIo.readUtf8(
                ByteArrayInputStream(oversized)
            )
        }

        assertEquals(
            PcgMostWantedBackupDocumentError.TOO_LARGE,
            error.reason
        )
    }

    /** Malformed byte sequences are not silently replaced with characters. */
    @Test
    fun readUtf8_rejectsMalformedUtf8() {
        val malformed = byteArrayOf(0xC3.toByte(), 0x28)

        val error = assertThrows(
            PcgMostWantedBackupDocumentException::class.java
        ) {
            PcgMostWantedBackupDocumentIo.readUtf8(
                ByteArrayInputStream(malformed)
            )
        }

        assertEquals(
            PcgMostWantedBackupDocumentError.INVALID_UTF8,
            error.reason
        )
    }
}
