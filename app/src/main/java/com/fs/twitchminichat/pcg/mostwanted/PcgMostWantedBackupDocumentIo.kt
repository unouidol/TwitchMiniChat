package com.fs.twitchminichat.pcg.mostwanted

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

/** Reasons why a selected backup document cannot be read safely. */
enum class PcgMostWantedBackupDocumentError {
    TOO_LARGE,
    INVALID_UTF8
}

/** Typed document failure mapped to a safe user-facing message by the UI. */
class PcgMostWantedBackupDocumentException(
    val reason: PcgMostWantedBackupDocumentError
) : IOException(reason.name)

/** Bounded, strict UTF-8 input/output for user-selected backup documents. */
object PcgMostWantedBackupDocumentIo {

    /** Reads one document without allowing unbounded allocation. */
    fun readUtf8(inputStream: InputStream): String {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var totalBytes = 0

        while (true) {
            val readCount = inputStream.read(buffer)
            if (readCount < 0) break

            totalBytes += readCount
            if (totalBytes > MAX_DOCUMENT_BYTES) {
                throw PcgMostWantedBackupDocumentException(
                    PcgMostWantedBackupDocumentError.TOO_LARGE
                )
            }
            bytes.write(buffer, 0, readCount)
        }

        return try {
            Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes.toByteArray()))
                .toString()
        } catch (_: CharacterCodingException) {
            throw PcgMostWantedBackupDocumentException(
                PcgMostWantedBackupDocumentError.INVALID_UTF8
            )
        }
    }

    /** Writes the complete deterministic UTF-8 document to the chosen URI. */
    fun writeUtf8(
        outputStream: OutputStream,
        text: String
    ) {
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            writer.write(text)
        }
    }

    /** Maximum accepted size, comfortably above the complete PCG catalog. */
    const val MAX_DOCUMENT_BYTES = 256 * 1024

    /** Fixed copy buffer used while enforcing the input limit. */
    private const val BUFFER_SIZE = 8 * 1024
}
