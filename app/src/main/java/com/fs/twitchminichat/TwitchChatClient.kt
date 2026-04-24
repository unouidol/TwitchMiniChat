package com.fs.twitchminichat

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class TwitchChatClient(
    private val username: String,
    private val oauthToken: String,
    private val channel: String
) {
    @Volatile
    private var running: Boolean = false

    private var socket: SSLSocket? = null
    private var writer: BufferedWriter? = null

    fun connect(
        onConnected: (() -> Unit)? = null,
        onMessage: ((String, String, String?, String?, String?, String?) -> Unit)? = null,
        onError: (Throwable) -> Unit = {},
        onNotice: ((msgId: String?, message: String) -> Unit)? = null
    ) {
        if (running) return
        running = true

        Thread {
            try {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sock = factory.createSocket("irc.chat.twitch.tv", 6697) as SSLSocket
                socket = sock

                val out = BufferedWriter(OutputStreamWriter(sock.outputStream, Charsets.UTF_8))
                val input = BufferedReader(InputStreamReader(sock.inputStream, Charsets.UTF_8))
                writer = out

                out.write("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership\r\n")
                out.flush()

                out.write("PASS oauth:$oauthToken\r\n")
                out.write("NICK $username\r\n")
                out.write("JOIN #$channel\r\n")
                out.flush()

                try {
                    onConnected?.invoke()
                } catch (_: Throwable) {
                }

                while (running) {
                    val line = input.readLine() ?: break

                    when {
                        line.startsWith("PING") -> {
                            val response = line.replace("PING", "PONG")
                            out.write("$response\r\n")
                            out.flush()
                        }

                        onNotice != null && line.contains(" NOTICE ") -> {
                            parseNotice(line)?.let { parsed ->
                                try {
                                    onNotice(parsed.msgId, parsed.message)
                                } catch (_: Throwable) {
                                }
                            }
                        }

                        onMessage != null && line.contains(" PRIVMSG ") -> {
                            parsePrivmsg(line)?.let { parsed ->
                                try {
                                    onMessage(
                                        parsed.user,
                                        parsed.message,
                                        parsed.emotesRaw,
                                        parsed.clientNonce,
                                        parsed.msgId,
                                        parsed.replyParentUserLogin
                                    )
                                } catch (_: Throwable) {
                                }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                onError(e)
            } finally {
                running = false
                try {
                    socket?.close()
                } catch (_: Throwable) {
                }
            }
        }.start()
    }

    fun sendMessage(text: String) {
        val w = writer ?: return
        Thread {
            try {
                w.write("PRIVMSG #$channel :$text\r\n")
                w.flush()
            } catch (_: Throwable) {
            }
        }.start()
    }

    fun sendReply(parentMsgId: String, text: String) {
        val w = writer ?: return
        Thread {
            try {
                w.write("@reply-parent-msg-id=$parentMsgId PRIVMSG #$channel :$text\r\n")
                w.flush()
            } catch (_: Throwable) {
            }
        }.start()
    }

    fun disconnect() {
        running = false
        try {
            socket?.close()
        } catch (_: Throwable) {
        }
    }

    private data class ParsedPrivMsg(
        val user: String,
        val message: String,
        val emotesRaw: String?,
        val clientNonce: String?,
        val msgId: String?,
        val replyParentUserLogin: String?
    )

    private data class ParsedNotice(
        val msgId: String?,
        val message: String
    )

    private fun parseTags(tagsPart: String?): Map<String, String> {
        if (tagsPart.isNullOrBlank()) return emptyMap()

        val out = LinkedHashMap<String, String>()

        for (pair in tagsPart.split(';')) {
            val eqIndex = pair.indexOf('=')
            if (eqIndex <= 0) continue

            val key = pair.substring(0, eqIndex)
            val value = pair.substring(eqIndex + 1)

            out[key] = value
        }

        return out
    }

    private fun splitTagsAndRest(line: String): Pair<String?, String> {
        var tagsPart: String? = null
        var rest = line

        if (rest.startsWith("@")) {
            val spaceIndex = rest.indexOf(' ')
            if (spaceIndex > 0) {
                tagsPart = rest.substring(1, spaceIndex)
                rest = rest.substring(spaceIndex + 1)
            }
        }

        return tagsPart to rest
    }

    private fun parseNotice(line: String): ParsedNotice? {
        val (tagsPart, rest) = splitTagsAndRest(line)
        val tags = parseTags(tagsPart)

        val noticeIndex = rest.indexOf(" NOTICE ")
        if (noticeIndex < 0) return null

        val msgColonIndex = rest.indexOf(" :", noticeIndex)
        if (msgColonIndex < 0 || msgColonIndex + 2 >= rest.length) return null

        val message = rest.substring(msgColonIndex + 2).trim()
        if (message.isBlank()) return null

        return ParsedNotice(
            msgId = tags["msg-id"]?.takeIf { it.isNotBlank() },
            message = message
        )
    }

    private fun parsePrivmsg(line: String): ParsedPrivMsg? {
        val (tagsPart, rest) = splitTagsAndRest(line)

        var emotesRaw: String? = null
        var clientNonce: String? = null
        var msgId: String? = null
        var displayName: String? = null
        var replyParentUserLogin: String? = null

        val tags = parseTags(tagsPart)

        for ((key, value) in tags) {
            if (value.isEmpty()) continue

            when (key) {
                "emotes" -> emotesRaw = value
                "client-nonce" -> clientNonce = value
                "id" -> msgId = value
                "display-name" -> displayName = value
                "reply-parent-user-login" -> replyParentUserLogin = value
            }
        }

        val bangIndex = rest.indexOf('!')
        if (bangIndex <= 0 || !rest.startsWith(":")) return null
        val nick = rest.substring(1, bangIndex)

        val userOut = (displayName?.takeIf { it.isNotBlank() } ?: nick).trim()

        val privmsgIndex = rest.indexOf(" PRIVMSG ")
        if (privmsgIndex < 0) return null

        val msgColonIndex = rest.indexOf(" :", privmsgIndex)
        if (msgColonIndex < 0 || msgColonIndex + 2 >= rest.length) return null

        val message = rest.substring(msgColonIndex + 2)

        return ParsedPrivMsg(
            user = userOut,
            message = message,
            emotesRaw = emotesRaw,
            clientNonce = clientNonce,
            msgId = msgId,
            replyParentUserLogin = replyParentUserLogin
        )
    }
}