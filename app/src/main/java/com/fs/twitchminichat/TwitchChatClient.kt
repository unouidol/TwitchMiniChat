package com.fs.twitchminichat

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/** Maintains one Twitch IRC connection without retrying outgoing chat commands. */
class TwitchChatClient(
    private val username: String,
    private val oauthToken: String,
    private val channel: String
) {
    @Volatile
    private var running: Boolean = false

    @Volatile
    private var disconnectRequested: Boolean = false

    @Volatile
    private var socket: SSLSocket? = null

    @Volatile
    private var writer: BufferedWriter? = null

    private val writerLock = Any()

    /** Opens the IRC connection and reports both messages and terminal state. */
    fun connect(
        onConnected: (() -> Unit)? = null,
        onMessage: ((
            user: String,
            message: String,
            emotesRaw: String?,
            clientNonce: String?,
            messageId: String?,
            replyParentUserLogin: String?,
            messageTimestampSec: Double?
        ) -> Unit)? = null,
        onError: (Throwable) -> Unit = {},
        onNotice: ((msgId: String?, message: String) -> Unit)? = null,
        onUserState: (() -> Unit)? = null,
        onSessionMetadata: ((TwitchIrcSessionMetadataUpdate) -> Unit)? = null,
        onDisconnected: ((shouldReconnect: Boolean, cause: Throwable?) -> Unit)? = null
    ) {
        synchronized(this) {
            if (running) return
            running = true
            disconnectRequested = false
        }

        Thread({
            var localSocket: SSLSocket? = null
            var localWriter: BufferedWriter? = null
            var terminalError: Throwable? = null
            var shouldReconnect = false

            try {
                val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val connectedSocket = factory.createSocket(
                    "irc.chat.twitch.tv",
                    6697
                ) as SSLSocket

                localSocket = connectedSocket
                socket = connectedSocket

                val connectedWriter = BufferedWriter(
                    OutputStreamWriter(
                        connectedSocket.outputStream,
                        Charsets.UTF_8
                    )
                )
                val input = BufferedReader(
                    InputStreamReader(
                        connectedSocket.inputStream,
                        Charsets.UTF_8
                    )
                )

                localWriter = connectedWriter
                writer = connectedWriter

                writeLine("CAP REQ :twitch.tv/tags twitch.tv/commands twitch.tv/membership")
                writeLine("PASS oauth:$oauthToken")
                writeLine("NICK $username")
                writeLine("JOIN #$channel")

                var connectionConfirmed = false

                while (running) {
                    val line = input.readLine()
                    if (line == null) {
                        shouldReconnect = !disconnectRequested
                        break
                    }

                    if (
                        !connectionConfirmed &&
                        (line.contains(" 001 ") || line.contains(" JOIN #$channel"))
                    ) {
                        connectionConfirmed = true
                        invokeSafely {
                            onConnected?.invoke()
                        }
                    }

                    when (val event = TwitchIrcProtocolParser.parse(line)) {
                        is TwitchIrcPing -> {
                            if (!writeLine(event.responseLine)) {
                                shouldReconnect = !disconnectRequested
                                break
                            }
                        }

                        TwitchIrcReconnect -> {
                            shouldReconnect = !disconnectRequested
                            break
                        }

                        is TwitchIrcGlobalUserState -> {
                            invokeSafely {
                                onSessionMetadata?.invoke(
                                    TwitchIrcSessionMetadataUpdate(
                                        userId = event.userId,
                                        emoteSetIds = event.emoteSetIds
                                    )
                                )
                            }
                        }

                        is TwitchIrcRoomState -> {
                            invokeSafely {
                                onSessionMetadata?.invoke(
                                    TwitchIrcSessionMetadataUpdate(
                                        channel = event.channel,
                                        roomId = event.roomId
                                    )
                                )
                            }
                        }

                        is TwitchIrcUserState -> {
                            invokeSafely {
                                onUserState?.invoke()
                            }
                            invokeSafely {
                                onSessionMetadata?.invoke(
                                    TwitchIrcSessionMetadataUpdate(
                                        channel = event.channel,
                                        emoteSetIds = event.emoteSetIds
                                    )
                                )
                            }
                        }

                        is TwitchIrcNotice -> {
                            if (onNotice != null) {
                                invokeSafely {
                                    onNotice(event.msgId, event.message)
                                }
                            }
                        }

                        is TwitchIrcPrivMsg -> {
                            if (onMessage != null) {
                                invokeSafely {
                                    onMessage(
                                        event.user,
                                        event.message,
                                        event.emotesRaw,
                                        event.clientNonce,
                                        event.messageId,
                                        event.replyParentUserLogin,
                                        event.messageTimestampSec
                                    )
                                }
                            }
                        }

                        null -> Unit
                    }
                }
            } catch (error: Throwable) {
                terminalError = error
                shouldReconnect = !disconnectRequested

                if (!disconnectRequested) {
                    invokeSafely {
                        onError(error)
                    }
                }
            } finally {
                running = false

                synchronized(writerLock) {
                    if (writer === localWriter) {
                        writer = null
                    }
                }

                if (socket === localSocket) {
                    socket = null
                }

                try {
                    localSocket?.close()
                } catch (_: Throwable) {
                }

                invokeSafely {
                    onDisconnected?.invoke(
                        shouldReconnect && !disconnectRequested,
                        terminalError
                    )
                }
            }
        }, "tmc-irc-$channel").start()
    }

    /** Sends one user-triggered chat message exactly once. */
    fun sendMessage(
        text: String,
        onResult: (TwitchChatWriteResult) -> Unit
    ) {
        sendLineAsync(
            line = "PRIVMSG #$channel :$text",
            onResult = onResult
        )
    }

    /** Sends one user-triggered reply exactly once. */
    fun sendReply(
        parentMsgId: String,
        text: String,
        onResult: (TwitchChatWriteResult) -> Unit
    ) {
        sendLineAsync(
            line = "@reply-parent-msg-id=$parentMsgId PRIVMSG #$channel :$text",
            onResult = onResult
        )
    }

    /** Closes the connection without requesting an automatic reconnect. */
    fun disconnect() {
        disconnectRequested = true
        running = false

        synchronized(writerLock) {
            writer = null
        }

        try {
            socket?.close()
        } catch (_: Throwable) {
        }
    }

    /** Writes one IRC line under a shared writer lock. */
    private fun writeLine(line: String): Boolean {
        return synchronized(writerLock) {
            val currentWriter = writer ?: return@synchronized false
            currentWriter.write(line)
            currentWriter.write("\r\n")
            currentWriter.flush()
            true
        }
    }

    /** Performs one asynchronous write and reports its result without retrying. */
    private fun sendLineAsync(
        line: String,
        onResult: (TwitchChatWriteResult) -> Unit
    ) {
        Thread({
            val result = try {
                if (writeLine(line)) {
                    TwitchChatWriteResult.Written
                } else {
                    TwitchChatWriteResult.NotConnected
                }
            } catch (error: Throwable) {
                try {
                    socket?.close()
                } catch (_: Throwable) {
                }
                TwitchChatWriteResult.Failed(error)
            }

            invokeSafely {
                onResult(result)
            }
        }, "tmc-irc-write-$channel").start()
    }

    /** Prevents callback failures from terminating the IRC reader thread. */
    private inline fun invokeSafely(action: () -> Unit) {
        try {
            action()
        } catch (_: Throwable) {
        }
    }
}
