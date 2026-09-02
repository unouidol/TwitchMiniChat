package com.fs.twitchminichat

import org.json.JSONObject

/** Outcome of one destructive backend deletion request. */
internal sealed interface DeletionOutcome {

    /** The backend confirmed the deletion. */
    data object Success : DeletionOutcome

    /**
     * The deletion did not happen.
     *
     * [serverMessage] carries the backend explanation when it sent one; a null value
     * means the caller must show its own generic failure text.
     */
    data class Failure(val serverMessage: String?) : DeletionOutcome
}

/**
 * Interprets the responses of the destructive deletion endpoints.
 *
 * Both `/delete_server_data` and `/delete_device_data` answer with the same shape, so
 * the interpretation lives in one place: a transport failure, a non-success status and
 * a body without `ok` must never be mistaken for a completed deletion.
 */
internal object DeletionResponseParser {

    /** Returns the outcome of one deletion request. */
    fun parse(responseCode: Int?, rawBody: String?): DeletionOutcome {
        val body = parseBody(rawBody)

        val accepted = responseCode != null &&
            responseCode in SUCCESS_STATUS_RANGE &&
            body?.optBoolean("ok", false) == true

        if (accepted) return DeletionOutcome.Success

        return DeletionOutcome.Failure(serverMessage = firstErrorMessage(body))
    }

    /**
     * Summarizes what the backend reported removing, for diagnostics only.
     *
     * Deliberately limited to a flag and a count: identifiers of devices or profiles
     * must never reach the log.
     */
    fun describeScope(rawBody: String?): String {
        val body = parseBody(rawBody) ?: return ""
        val parts = mutableListOf<String>()

        if (body.has("removed_device")) {
            parts += "removedDevice=${body.optBoolean("removed_device", false)}"
        }

        body.optJSONArray("removed_device_profiles")?.let { removed ->
            parts += "removedDeviceProfileCount=${removed.length()}"
        }

        return parts.joinToString(separator = " ")
    }

    /** Returns the first usable backend error text, or null when there is none. */
    private fun firstErrorMessage(body: JSONObject?): String? {
        if (body == null) return null

        val errors = body.optJSONArray("errors")
        if (errors != null && errors.length() > 0) {
            errors.optString(0).takeIf(String::isNotBlank)?.let { return it }
        }

        return body.optString("error").takeIf(String::isNotBlank)
    }

    /** Parses a response body, treating malformed content as absent. */
    private fun parseBody(rawBody: String?): JSONObject? {
        val normalized = rawBody?.trim().orEmpty()
        if (normalized.isEmpty()) return null

        return runCatching { JSONObject(normalized) }.getOrNull()
    }

    /** Hypertext Transfer Protocol statuses treated as a completed request. */
    private val SUCCESS_STATUS_RANGE = 200..299
}
