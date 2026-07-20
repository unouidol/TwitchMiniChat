package com.fs.twitchminichat

/** Represents one parsed Twitch IRC protocol event. */
sealed interface TwitchIrcEvent

/** Carries the PONG command required for one Twitch PING line. */
data class TwitchIrcPing(
    val responseLine: String
) : TwitchIrcEvent

/** Requests a fresh IRC connection after Twitch sends RECONNECT. */
data object TwitchIrcReconnect : TwitchIrcEvent

/** Carries authenticated-user metadata sent immediately after IRC login. */
data class TwitchIrcGlobalUserState(
    val userId: String?,
    val emoteSetIds: Set<String>
) : TwitchIrcEvent

/** Confirms one outgoing message and carries the latest channel user state. */
data class TwitchIrcUserState(
    val channel: String?,
    val emoteSetIds: Set<String>
) : TwitchIrcEvent

/** Carries the numeric identity of one joined Twitch chat room. */
data class TwitchIrcRoomState(
    val channel: String?,
    val roomId: String?
) : TwitchIrcEvent

/** Represents one Twitch NOTICE event. */
data class TwitchIrcNotice(
    val msgId: String?,
    val message: String
) : TwitchIrcEvent

/** Represents one Twitch PRIVMSG event with stable server metadata. */
data class TwitchIrcPrivMsg(
    val user: String,
    val message: String,
    val emotesRaw: String?,
    val clientNonce: String?,
    val messageId: String?,
    val replyParentUserLogin: String?,
    val messageTimestampSec: Double?
) : TwitchIrcEvent

/** Parses the Twitch IRC lines used by Twitch Mini Chat. */
object TwitchIrcProtocolParser {

    /** Parses one complete Twitch IRC line into a supported protocol event. */
    fun parse(line: String): TwitchIrcEvent? {
        val normalizedLine = line.trimEnd('\r', '\n')
        if (normalizedLine.isBlank()) return null

        if (normalizedLine.startsWith("PING")) {
            return TwitchIrcPing(
                responseLine = normalizedLine.replaceFirst("PING", "PONG")
            )
        }

        val (tagsPart, rest) = splitTagsAndRest(normalizedLine)

        if (rest == ":tmi.twitch.tv RECONNECT" || rest.endsWith(" RECONNECT")) {
            return TwitchIrcReconnect
        }

        return when {
            rest.contains(" GLOBALUSERSTATE") -> parseGlobalUserState(tagsPart)
            rest.contains(" ROOMSTATE ") -> parseRoomState(tagsPart, rest)
            rest.contains(" USERSTATE ") -> parseUserState(tagsPart, rest)
            rest.contains(" NOTICE ") -> parseNotice(tagsPart, rest)
            rest.contains(" PRIVMSG ") -> parsePrivMsg(tagsPart, rest)
            else -> null
        }
    }

    /** Splits optional IRCv3 tags from the remaining command line. */
    private fun splitTagsAndRest(line: String): Pair<String?, String> {
        if (!line.startsWith("@")) return null to line

        val spaceIndex = line.indexOf(' ')
        if (spaceIndex <= 1 || spaceIndex + 1 >= line.length) {
            return null to line
        }

        return line.substring(1, spaceIndex) to line.substring(spaceIndex + 1)
    }

    /** Parses IRCv3 tags and decodes Twitch escape sequences. */
    private fun parseTags(tagsPart: String?): Map<String, String> {
        if (tagsPart.isNullOrBlank()) return emptyMap()

        val tags = LinkedHashMap<String, String>()

        for (pair in tagsPart.split(';')) {
            val equalsIndex = pair.indexOf('=')
            if (equalsIndex <= 0) continue

            val key = pair.substring(0, equalsIndex)
            val value = decodeTagValue(pair.substring(equalsIndex + 1))
            tags[key] = value
        }

        return tags
    }

    /** Decodes the IRCv3 tag escaping used by Twitch. */
    private fun decodeTagValue(rawValue: String): String {
        if ('\\' !in rawValue) return rawValue

        val decoded = StringBuilder(rawValue.length)
        var index = 0

        while (index < rawValue.length) {
            val current = rawValue[index]
            if (current != '\\' || index + 1 >= rawValue.length) {
                decoded.append(current)
                index += 1
                continue
            }

            val escaped = rawValue[index + 1]
            decoded.append(
                when (escaped) {
                    's' -> ' '
                    ':' -> ';'
                    '\\' -> '\\'
                    'r' -> '\r'
                    'n' -> '\n'
                    else -> escaped
                }
            )
            index += 2
        }

        return decoded.toString()
    }

    /** Parses authenticated-user metadata sent after successful IRC authentication. */
    private fun parseGlobalUserState(tagsPart: String?): TwitchIrcGlobalUserState {
        val tags = parseTags(tagsPart)

        return TwitchIrcGlobalUserState(
            userId = tags["user-id"]?.takeIf { it.isNotBlank() },
            emoteSetIds = parseEmoteSetIds(tags["emote-sets"])
        )
    }

    /** Parses the channel-specific state sent after JOIN and outgoing PRIVMSG. */
    private fun parseUserState(
        tagsPart: String?,
        rest: String
    ): TwitchIrcUserState {
        val tags = parseTags(tagsPart)

        return TwitchIrcUserState(
            channel = parseCommandChannel(rest, " USERSTATE "),
            emoteSetIds = parseEmoteSetIds(tags["emote-sets"])
        )
    }

    /** Parses the numeric room identity sent after joining a channel. */
    private fun parseRoomState(
        tagsPart: String?,
        rest: String
    ): TwitchIrcRoomState {
        val tags = parseTags(tagsPart)

        return TwitchIrcRoomState(
            channel = parseCommandChannel(rest, " ROOMSTATE "),
            roomId = tags["room-id"]?.takeIf { it.isNotBlank() }
        )
    }

    /** Extracts and normalizes one channel name from an IRC command line. */
    private fun parseCommandChannel(
        rest: String,
        commandToken: String
    ): String? {
        val commandIndex = rest.indexOf(commandToken)
        if (commandIndex < 0) return null

        val channelStart = rest.indexOf('#', commandIndex + commandToken.length)
        if (channelStart < 0 || channelStart + 1 >= rest.length) return null

        val channelEnd = rest.indexOf(' ', channelStart).let { index ->
            if (index < 0) rest.length else index
        }

        return rest.substring(channelStart + 1, channelEnd)
            .trim()
            .lowercase()
            .takeIf { it.isNotBlank() }
    }

    /** Splits the comma-separated Twitch emote-set tag into stable unique IDs. */
    private fun parseEmoteSetIds(rawValue: String?): Set<String> {
        if (rawValue.isNullOrBlank()) return emptySet()

        return rawValue
            .split(',')
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toCollection(LinkedHashSet())
    }

    /** Parses one NOTICE line. */
    private fun parseNotice(tagsPart: String?, rest: String): TwitchIrcNotice? {
        val noticeIndex = rest.indexOf(" NOTICE ")
        if (noticeIndex < 0) return null

        val messageColonIndex = rest.indexOf(" :", noticeIndex)
        if (messageColonIndex < 0 || messageColonIndex + 2 >= rest.length) {
            return null
        }

        val message = rest.substring(messageColonIndex + 2).trim()
        if (message.isBlank()) return null

        val tags = parseTags(tagsPart)

        return TwitchIrcNotice(
            msgId = tags["msg-id"]?.takeIf { it.isNotBlank() },
            message = message
        )
    }

    /** Parses one PRIVMSG line and preserves Twitch server metadata. */
    private fun parsePrivMsg(tagsPart: String?, rest: String): TwitchIrcPrivMsg? {
        val tags = parseTags(tagsPart)

        val bangIndex = rest.indexOf('!')
        if (bangIndex <= 1 || !rest.startsWith(":")) return null

        val nick = rest.substring(1, bangIndex)
        val user = tags["display-name"]
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?: nick.trim()

        if (user.isBlank()) return null

        val privMsgIndex = rest.indexOf(" PRIVMSG ")
        if (privMsgIndex < 0) return null

        val messageColonIndex = rest.indexOf(" :", privMsgIndex)
        if (messageColonIndex < 0 || messageColonIndex + 2 >= rest.length) {
            return null
        }

        val message = rest.substring(messageColonIndex + 2)

        val sentAtMillis = tags["tmi-sent-ts"]
            ?.toLongOrNull()
            ?.takeIf { it > 0L }

        return TwitchIrcPrivMsg(
            user = user,
            message = message,
            emotesRaw = tags["emotes"]?.takeIf { it.isNotBlank() },
            clientNonce = tags["client-nonce"]?.takeIf { it.isNotBlank() },
            messageId = tags["id"]?.takeIf { it.isNotBlank() },
            replyParentUserLogin = tags["reply-parent-user-login"]
                ?.takeIf { it.isNotBlank() },
            messageTimestampSec = sentAtMillis?.toDouble()?.div(1000.0)
        )
    }
}
