package com.fs.twitchminichat

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards production diagnostics against accidental reintroduction of raw user data.
 *
 * This defense-in-depth source check complements the focused behavioral tests for
 * the components that produce each diagnostic event.
 */
class PrivacySafeLoggingSourceTest {

    @Test
    fun android_log_calls_exclude_sensitive_values_and_raw_throwables() {
        val moduleDirectory = findAppModuleDirectory()
        val sourceRoot = moduleDirectory.resolve("src/main/java")
        val violations = mutableListOf<String>()

        sourceFiles(sourceRoot, setOf("kt", "java")).forEach { path ->
            val source = readUtf8(path)

            extractCalls(source, ANDROID_LOG_CALL).forEach { call ->
                if (topLevelArgumentCount(call.text) > 2) {
                    violations += violation(path, call, "raw Throwable argument")
                }

                FORBIDDEN_ANDROID_LOG_CONTENT.forEach { forbidden ->
                    if (forbidden.pattern.containsMatchIn(call.text)) {
                        violations += violation(path, call, forbidden.description)
                    }
                }
            }
        }

        assertTrue(violations.joinToString(separator = "\n"), violations.isEmpty())
    }

    @Test
    fun webextension_console_calls_use_one_static_message() {
        val moduleDirectory = findAppModuleDirectory()
        val assetRoot = moduleDirectory.resolve("src/main/assets/pcg_probe")
        val violations = mutableListOf<String>()

        sourceFiles(assetRoot, setOf("js")).forEach { path ->
            val source = readUtf8(path)

            extractCalls(source, CONSOLE_LOG_CALL).forEach { call ->
                val argument = call.text
                    .substringAfter('(')
                    .dropLast(1)
                    .trim()

                if (
                    topLevelArgumentCount(call.text) != 1 ||
                    !STATIC_JAVASCRIPT_STRING.matches(argument)
                ) {
                    violations += violation(path, call, "non-static console payload")
                }
            }
        }

        assertTrue(violations.joinToString(separator = "\n"), violations.isEmpty())
    }

    /** Finds the Android app module from Gradle or Android Studio test working directories. */
    private fun findAppModuleDirectory(): Path {
        val start = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()

        return generateSequence(start) { current -> current.parent }
            .take(MAX_PARENT_SEARCH_DEPTH)
            .flatMap { current -> sequenceOf(current.resolve("app"), current) }
            .firstOrNull { candidate ->
                Files.isDirectory(candidate.resolve("src/main/java"))
            }
            ?: error("Unable to locate app/src/main/java from $start")
    }

    /** Returns regular source files below [root] with one of the requested extensions. */
    private fun sourceFiles(root: Path, extensions: Set<String>): List<Path> {
        if (!Files.isDirectory(root)) return emptyList()

        val files = mutableListOf<Path>()
        Files.walk(root).use { paths ->
            paths
                .filter { path -> Files.isRegularFile(path) }
                .filter { path ->
                    path.fileName.toString().substringAfterLast('.', "") in extensions
                }
                .forEach { path -> files.add(path) }
        }
        return files
    }

    /** Reads one source file as UTF-8 without depending on the Java 11 Files API. */
    private fun readUtf8(path: Path): String {
        return String(Files.readAllBytes(path), Charsets.UTF_8)
    }

    /** Extracts balanced function calls beginning with [callStart]. */
    private fun extractCalls(source: String, callStart: Regex): List<SourceCall> {
        val calls = mutableListOf<SourceCall>()
        var searchFrom = 0

        while (searchFrom < source.length) {
            val match = callStart.find(source, searchFrom) ?: break
            val openingParenthesis = source.indexOf('(', match.range.first)
            val closingParenthesis = findClosingParenthesis(source, openingParenthesis)

            if (closingParenthesis < 0) {
                error("Unbalanced diagnostic call at character ${match.range.first}")
            }

            calls += SourceCall(
                line = source.substring(0, match.range.first).count { it == '\n' } + 1,
                text = source.substring(match.range.first, closingParenthesis + 1)
            )
            searchFrom = closingParenthesis + 1
        }

        return calls
    }

    /** Finds a call's matching parenthesis while ignoring quoted text. */
    private fun findClosingParenthesis(source: String, openingParenthesis: Int): Int {
        var depth = 1
        var quote: Char? = null
        var escaped = false

        for (index in openingParenthesis + 1 until source.length) {
            val character = source[index]

            if (quote != null) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == quote -> quote = null
                }
                continue
            }

            when (character) {
                '"', '\'' -> quote = character
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }

        return -1
    }

    /** Counts arguments at the outermost call level. */
    private fun topLevelArgumentCount(call: String): Int {
        val openingParenthesis = call.indexOf('(')
        if (openingParenthesis < 0 || call.last() != ')') return 0

        var parenthesisDepth = 1
        var bracketDepth = 0
        var braceDepth = 0
        var quote: Char? = null
        var escaped = false
        var commaCount = 0
        var hasContent = false

        for (index in openingParenthesis + 1 until call.lastIndex) {
            val character = call[index]

            if (quote != null) {
                hasContent = true
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == quote -> quote = null
                }
                continue
            }

            when (character) {
                '"', '\'' -> {
                    quote = character
                    hasContent = true
                }

                '(' -> parenthesisDepth++
                ')' -> parenthesisDepth--
                '[' -> bracketDepth++
                ']' -> bracketDepth--
                '{' -> braceDepth++
                '}' -> braceDepth--
                ',' -> if (
                    parenthesisDepth == 1 &&
                    bracketDepth == 0 &&
                    braceDepth == 0
                ) {
                    commaCount++
                }

                else -> if (!character.isWhitespace()) hasContent = true
            }
        }

        return if (hasContent) commaCount + 1 else 0
    }

    /** Formats one actionable source-level privacy violation. */
    private fun violation(path: Path, call: SourceCall, reason: String): String {
        return "${path.fileName}:${call.line}: $reason"
    }

    /** Source location and exact text of one logging call. */
    private data class SourceCall(
        val line: Int,
        val text: String
    )

    /** Named forbidden expression for readable test failures. */
    private data class ForbiddenContent(
        val description: String,
        val pattern: Regex
    )

    private companion object {
        private const val MAX_PARENT_SEARCH_DEPTH = 6

        private val ANDROID_LOG_CALL =
            Regex("""\bLog\.(?:v|d|i|w|e|wtf)\s*\(""")

        private val CONSOLE_LOG_CALL =
            Regex("""\bconsole\.(?:log|debug|info|warn|error)\s*\(""")

        private val STATIC_JAVASCRIPT_STRING =
            Regex("""(?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')""")

        private val FORBIDDEN_ANDROID_LOG_CONTENT = listOf(
            ForbiddenContent(
                description = "direct sensitive interpolation",
                pattern = Regex(
                    """\$(?:accountId|profileId|profileLabel|username|channel|ch|user|userId|roomId|sessionKey|message|messageText|messageUser|noticeMessage|payload|json|rawBody|responseBody|responseText|body|requestId|auditLogPath|targetProfileId|profiles|pokemon|balls|url|extension|nativeApp|token|accessToken|refreshToken|loginToken|codeVerifier|codeChallenge|authorizationHeader|deviceCredential|credential|cookie|state|slot|returnScheme)\b"""
                )
            ),
            ForbiddenContent(
                description = "braced sensitive interpolation",
                pattern = Regex(
                    """\$\{\s*(?:accountId|profileId|profileLabel|username|channel|ch|user|userId|roomId|sessionKey|message|messageText|messageUser|noticeMessage|payload|json|rawBody|responseBody|responseText|body|requestId|auditLogPath|targetProfileId|profiles|pokemon|balls|url|extension|nativeApp|token|accessToken|refreshToken|loginToken|codeVerifier|codeChallenge|authorizationHeader|deviceCredential|credential|cookie|state|slot|returnScheme)\s*}"""
                )
            ),
            ForbiddenContent(
                description = "raw sensitive object field",
                pattern = Regex(
                    """\$\{\s*[A-Za-z_][A-Za-z0-9_?]*\.(?:id|accountId|profileId|profileLabel|username|channel|user|userId|roomId|message|messageText|messageUser|rawResponse|responseBody|responseText|body|requestId|auditLogPath|targetProfileId|profiles|pokemon|balls|url|activeTab|reason|activeNonSpawnableFilters|token|accessToken|refreshToken|loginToken|codeVerifier|codeChallenge|authorizationHeader|deviceCredential|credential|cookie|state|slot|returnScheme)\s*}"""
                )
            ),
            ForbiddenContent(
                description = "raw Firebase notification content",
                pattern = Regex(
                    """\$\{\s*(?:remoteMessage\.(?:from|data)|notification\.(?:title|body))\s*}"""
                )
            ),
            ForbiddenContent(
                description = "raw Document Object Model string",
                pattern = Regex("""\bpayload\.optString\s*\(""")
            ),
            ForbiddenContent(
                description = "rendered collection",
                pattern = Regex("""\.joinToString\s*\(""")
            ),
            ForbiddenContent(
                description = "exception message",
                pattern = Regex(
                    """\b(?:error|failure|exception|throwable|cause|t)\??\.message\b""",
                    RegexOption.IGNORE_CASE
                )
            )
        )
    }
}
