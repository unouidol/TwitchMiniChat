package com.fs.twitchminichat.v2

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
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
        onMessage: ((String, String, String?, String?, String?) -> Unit)? = null,
        onError: (Throwable) -> Unit = {}
    )

    {
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

                try { onConnected?.invoke() } catch (_: Throwable) {}

                while (running) {
                    val line = input.readLine() ?: break

                    if (line.startsWith("PING")) {
                        val response = line.replace("PING", "PONG")
                        out.write("$response\r\n")
                        out.flush()
                    } else if (onMessage != null && line.contains(" PRIVMSG ")) {
                        parsePrivmsg(line)?.let { parsed ->
                            try {
                                onMessage(
                                    parsed.user,
                                    parsed.message,
                                    parsed.emotesRaw,
                                    parsed.clientNonce,
                                    parsed.msgId
                                )
                            } catch (_: Throwable) {}
                        }
                    }
                }
            } catch (e: Throwable) {
                onError(e)
            } finally {
                running = false
                try { socket?.close() } catch (_: Throwable) {}
            }
        }.start()
    }

    fun sendMessage(text: String) {
        val w = writer ?: return
        Thread {
            try {
                w.write("PRIVMSG #$channel :$text\r\n")
                w.flush()
            } catch (_: Throwable) {}
        }.start()
    }

    fun sendMessageWithNonce(text: String): String {
        val w = writer ?: return ""
        val nonce = UUID.randomUUID().toString().replace("-", "").take(16)
        Thread {
            try {
                w.write("@client-nonce=$nonce PRIVMSG #$channel :$text\r\n")
                w.flush()
            } catch (_: Throwable) {}
        }.start()
        return nonce
    }

    fun disconnect() {
        running = false
        try { socket?.close() } catch (_: Throwable) {}
    }

    private data class ParsedPrivMsg(
        val user: String,
        val message: String,
        val emotesRaw: String?,
        val clientNonce: String?,
        val msgId: String?
    )

    private fun parsePrivmsg(line: String): ParsedPrivMsg? {
        var tagsPart: String? = null
        var rest = line

        // @tags ... :nick!user@host PRIVMSG #chan :msg
        if (rest.startsWith("@")) {
            val spaceIndex = rest.indexOf(' ')
            if (spaceIndex > 0) {
                tagsPart = rest.substring(1, spaceIndex)
                rest = rest.substring(spaceIndex + 1)
            }
        }

        var emotesRaw: String? = null
        var clientNonce: String? = null
        var msgId: String? = null
        var displayName: String? = null

        if (!tagsPart.isNullOrBlank()) {
            for (pair in tagsPart.split(';')) {
                val eqIndex = pair.indexOf('=')
                if (eqIndex <= 0) continue
                val key = pair.substring(0, eqIndex)
                val value = pair.substring(eqIndex + 1)
                if (value.isEmpty()) continue

                when (key) {
                    "emotes" -> emotesRaw = value
                    "client-nonce" -> clientNonce = value
                    "id" -> msgId = value            // ✅ Twitch message-id
                    "display-name" -> displayName = value
                }
            }
        }

        // parse nick/user from "rest"
        val bangIndex = rest.indexOf('!')
        if (bangIndex <= 0 || !rest.startsWith(":")) return null
        val nick = rest.substring(1, bangIndex)

        val userOut = (displayName?.takeIf { it.isNotBlank() } ?: nick).trim()

        val privmsgIndex = rest.indexOf(" PRIVMSG ")
        if (privmsgIndex < 0) return null

        val msgColonIndex = rest.indexOf(" :", privmsgIndex)
        if (msgColonIndex < 0 || msgColonIndex + 2 >= rest.length) return null

        val message = rest.substring(msgColonIndex + 2)

        return ParsedPrivMsg(userOut, message, emotesRaw, clientNonce, msgId)
    }
}
