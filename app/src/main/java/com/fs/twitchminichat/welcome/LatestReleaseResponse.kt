package com.fs.twitchminichat.welcome

import java.io.ByteArrayOutputStream
import java.io.InputStream
import org.json.JSONException
import org.json.JSONObject

/**
 * Turns GitHub's answer about the latest release into a tag, or into nothing.
 *
 * Kept apart from the connection so that every way the answer can be wrong is tested
 * without a network. Each of them gives `null`, and the caller does nothing with `null`:
 * the check fails silently.
 */
object LatestReleaseResponse {

    /**
     * The most this check reads from GitHub's answer.
     *
     * On 2026-11-12 the answer for `v5.5.1` was 11,625 bytes, most of it the release notes.
     * The limit leaves room for much longer notes. It also guarantees that a misbehaving
     * answer costs a bounded amount of memory on a background thread.
     */
    const val MAX_BODY_BYTES = 256 * 1024

    /**
     * Reads at most [maxBytes] from [input], as UTF-8.
     *
     * @return `null` if the stream holds more than [maxBytes]. An answer cut short would
     *     be JSON that does not parse, so it is refused here instead.
     */
    fun readBounded(input: InputStream, maxBytes: Int = MAX_BODY_BYTES): String? {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)

        while (true) {
            val read = input.read(chunk)
            if (read < 0) break
            if (buffer.size() + read > maxBytes) return null
            buffer.write(chunk, 0, read)
        }

        return buffer.toString(Charsets.UTF_8.name())
    }

    /**
     * The tag of the latest published release, or `null` when the answer does not name one.
     *
     * The answer counts only when:
     * - the status is 200. A 403 from the rate limit, a 404 or a server error are all
     *   failures;
     * - the body is a JSON object;
     * - it is neither a draft nor a pre-release. The endpoint excludes both, and this
     *   checks it again;
     * - its `tag_name` parses as a release version ([ReleaseVersion.parseTag]).
     */
    fun latestTag(statusCode: Int, body: String?): String? {
        if (statusCode != 200 || body == null) return null

        val json = try {
            JSONObject(body)
        } catch (_: JSONException) {
            return null
        }

        if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) {
            return null
        }

        val tag = json.opt("tag_name") as? String ?: return null
        return tag.takeIf { ReleaseVersion.parseTag(it) != null }
    }
}
