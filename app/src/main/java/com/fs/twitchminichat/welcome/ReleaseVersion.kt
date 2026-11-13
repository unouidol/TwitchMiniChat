package com.fs.twitchminichat.welcome

/**
 * A `MAJOR.MINOR.PATCH` version, compared as three numbers.
 *
 * Numeric comparison is the point. A string comparison puts `5.10.0` before `5.9.0`.
 *
 * Every parse that fails returns `null`, and [isNewer] treats `null` as "not newer". So a
 * tag or version name this class cannot read never produces an update notice: a shape it
 * does not understand fails toward silence, not toward a wrong prompt.
 */
data class ReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<ReleaseVersion> {

    override fun compareTo(other: ReleaseVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    /** The version as users read it, without a leading `v`: `5.6.1`. */
    override fun toString(): String = "$major.$minor.$patch"

    companion object {

        /*
         * The release contract is vMAJOR.MINOR.PATCH. The v is optional because one old
         * release, 5.2.0, was tagged without it. Anything after the patch, such as -rc1, is
         * not a published release shape, so it does not parse.
         */
        private val TAG = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

        /*
         * versionName as the build writes it: stable 5.6.0, dev 5.6.0-dev through
         * versionNameSuffix. The suffix is accepted and ignored.
         */
        private val INSTALLED = Regex("""^(\d+)\.(\d+)\.(\d+)(-[A-Za-z0-9.]+)?$""")

        /**
         * Reads a release tag, as GitHub's `tag_name` gives it.
         *
         * @return `null` for anything that is not exactly `vMAJOR.MINOR.PATCH`, with an
         *     optional `v`. Pre-release tags, missing parts, whitespace, and numbers too large
         *     for an [Int] are all `null`.
         */
        fun parseTag(tag: String?): ReleaseVersion? = parse(TAG, tag)

        /**
         * Reads the installed `versionName`, ignoring a `-suffix` such as dev's `-dev`.
         */
        fun parseInstalled(versionName: String?): ReleaseVersion? = parse(INSTALLED, versionName)

        /**
         * Whether [latestTag] names a release later than [installedVersionName].
         *
         * An equal or older tag is not newer. Neither is a value that cannot be parsed, on
         * either side.
         */
        fun isNewer(latestTag: String?, installedVersionName: String?): Boolean {
            val latest = parseTag(latestTag) ?: return false
            val installed = parseInstalled(installedVersionName) ?: return false
            return latest > installed
        }

        private fun parse(pattern: Regex, value: String?): ReleaseVersion? {
            val match = pattern.matchEntire(value ?: return null) ?: return null
            val (major, minor, patch) = match.destructured
            return ReleaseVersion(
                major = major.toIntOrNull() ?: return null,
                minor = minor.toIntOrNull() ?: return null,
                patch = patch.toIntOrNull() ?: return null
            )
        }
    }
}
