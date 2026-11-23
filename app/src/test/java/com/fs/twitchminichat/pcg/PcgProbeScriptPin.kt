package com.fs.twitchminichat.pcg

import java.security.MessageDigest

/**
 * Decides whether a probe file's bytes are still the git blob they are pinned to.
 *
 * The bytes a build packages are the working tree's, and a checkout made with
 * `core.autocrlf=true`, as this repository is checked out on Windows, writes every LF of the
 * blob as CRLF. Those bytes are the same blob to git, so off continuous integration (CI) that
 * one form is accepted too: every line ending CRLF and no other carriage return. On CI, a
 * Linux checkout that converts nothing, only the blob's own bytes are accepted, so a commit
 * that changes nothing but line endings still fails there.
 */
internal object PcgProbeScriptPin {

    /** Why [check] accepted or refused one file. */
    sealed interface Verdict {
        /** The bytes are the pinned blob, or its CRLF checkout off CI. */
        data object Pinned : Verdict

        /** Anything else; [message] says what was found and what the rule is. */
        data class Changed(val message: String) : Verdict
    }

    /**
     * Compares [bytes], read from the file [fileName], with the blob [pinnedBlobId].
     *
     * @param onContinuousIntegration true on CI, where only the blob's own bytes are accepted.
     */
    fun check(
        fileName: String,
        bytes: ByteArray,
        pinnedBlobId: String,
        onContinuousIntegration: Boolean
    ): Verdict {
        val rawBlobId = gitBlobId(bytes)
        if (rawBlobId == pinnedBlobId) {
            return Verdict.Pinned
        }

        val lfBytes = lfFromCrlfCheckout(bytes)
        val lfBlobId = lfBytes?.let(::gitBlobId)
        if (lfBlobId == pinnedBlobId && !onContinuousIntegration) {
            return Verdict.Pinned
        }

        val found = when {
            lfBlobId == pinnedBlobId ->
                "its bytes are the pinned blob with every line ending written as CRLF, " +
                    "which is accepted only off continuous integration"

            lfBlobId != null ->
                "its bytes give git blob $rawBlobId, and $lfBlobId with CRLF read as LF"

            else -> "its bytes give git blob $rawBlobId"
        }
        return Verdict.Changed(
            "$fileName changed: $found (${bytes.size} bytes), but it is pinned to git blob " +
                "$pinnedBlobId. The PCG probes content.js, pcg_tab_state_scout.js and " +
                "pcg_probe_inventory.js stay byte for byte as they are (AGENTS.md, owner's " +
                "ruling of 2026-11-18; docs/design-pcg-dex-readings.md, \"What stays exactly " +
                "as it is\"). Changing one needs the owner's go, and its new blob is pinned " +
                "here in the same pull request."
        )
    }

    /** The object name git gives [bytes] as a blob: SHA-1 of `blob <size>\0` and the bytes. */
    fun gitBlobId(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update("blob ${bytes.size}\u0000".toByteArray(Charsets.US_ASCII))
        digest.update(bytes)
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    /**
     * The LF bytes that a `core.autocrlf=true` checkout of them would write as [bytes], or null
     * when [bytes] is not such a checkout: a carriage return not followed by LF, an LF not
     * preceded by one, or no CRLF at all.
     */
    fun lfFromCrlfCheckout(bytes: ByteArray): ByteArray? {
        val out = java.io.ByteArrayOutputStream(bytes.size)
        var sawCrlf = false
        var index = 0
        while (index < bytes.size) {
            val byte = bytes[index]
            when {
                byte == CR && index + 1 < bytes.size && bytes[index + 1] == LF -> {
                    out.write(LF.toInt())
                    sawCrlf = true
                    index += 2
                }

                byte == CR || byte == LF -> return null

                else -> {
                    out.write(byte.toInt())
                    index++
                }
            }
        }
        return if (sawCrlf) out.toByteArray() else null
    }

    private const val CR: Byte = 0x0D
    private const val LF: Byte = 0x0A
}
