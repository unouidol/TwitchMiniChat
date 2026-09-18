package com.fs.twitchminichat

/**
 * The commit this build was made from, in the one form the app shows anywhere.
 *
 * Version numbers cannot tell builds apart: several builds exist per release,
 * and three of them once reported the same 5.5.1 and build 8 with no way to say
 * which was installed. The commit does identify a build, so Gradle writes it into
 * [BuildConfig.GIT_SHA] and this object is the only reader.
 *
 * The value is normalised here rather than trusted from Gradle because it
 * reaches a user-facing label and a file the user exports and shares. Whatever
 * the build step produced - a longer sha, uppercase, a stray line, an error
 * message from a git that misbehaved - what leaves the app is either seven
 * lowercase hex characters, optionally marked "+" for a tree with uncommitted
 * changes, or the literal "unknown". Nothing else can reach the label or the
 * export, whatever the build environment did.
 */
object BuildIdentity {

    /** Shown when the build could not say which commit it came from. */
    const val UNKNOWN = "unknown"

    /** Length git uses for a short sha, and the length always shown. */
    private const val SHORT_LENGTH = 7

    /** Hex of at least the short length, up to a full sha, then an optional "+". */
    private val SHA_SHAPE = Regex("^([0-9a-fA-F]{7,40})(\\+?)$")

    /**
     * Returns [raw] as seven lowercase hex characters, with "+" kept when the
     * build marked its tree dirty, or [UNKNOWN] for anything that is not a sha.
     */
    fun displaySha(raw: String): String {
        val match = SHA_SHAPE.matchEntire(raw.trim()) ?: return UNKNOWN

        val hex = match.groupValues[1].lowercase().take(SHORT_LENGTH)
        val dirtyMarker = match.groupValues[2]

        return hex + dirtyMarker
    }

    /** This build's commit, normalised by [displaySha]. */
    val shortSha: String = displaySha(BuildConfig.GIT_SHA)
}
