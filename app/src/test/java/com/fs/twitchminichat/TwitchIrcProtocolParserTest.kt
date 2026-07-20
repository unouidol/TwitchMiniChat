package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies stable parsing of the Twitch IRC events used by the chat client. */
class TwitchIrcProtocolParserTest {

    @Test
    fun ping_builds_matching_pong() {
        val event = TwitchIrcProtocolParser.parse("PING :tmi.twitch.tv")

        assertEquals(
            TwitchIrcPing("PONG :tmi.twitch.tv"),
            event
        )
    }

    @Test
    fun reconnect_is_exposed_as_protocol_event() {
        val event = TwitchIrcProtocolParser.parse(":tmi.twitch.tv RECONNECT")

        assertSame(TwitchIrcReconnect, event)
    }

    @Test
    fun privmsg_preserves_server_timestamp_and_message_id() {
        val event = TwitchIrcProtocolParser.parse(
            "@display-name=Tester;emotes=25:0-4;id=message-123;" +
                    "reply-parent-user-login=parent;tmi-sent-ts=1712345678123 " +
                    ":tester!tester@tester.tmi.twitch.tv PRIVMSG #channel :Kappa hello"
        )

        assertTrue(event is TwitchIrcPrivMsg)
        event as TwitchIrcPrivMsg

        assertEquals("Tester", event.user)
        assertEquals("Kappa hello", event.message)
        assertEquals("25:0-4", event.emotesRaw)
        assertEquals("message-123", event.messageId)
        assertEquals("parent", event.replyParentUserLogin)
        assertEquals(1712345678.123, event.messageTimestampSec!!, 0.0001)
    }

    @Test
    fun ircv3_tag_escapes_are_decoded() {
        val event = TwitchIrcProtocolParser.parse(
            "@display-name=Name\\sWith\\sSpaces;id=message-1;tmi-sent-ts=1000 " +
                    ":name!name@name.tmi.twitch.tv PRIVMSG #channel :hello"
        ) as TwitchIrcPrivMsg

        assertEquals("Name With Spaces", event.user)
    }

    @Test
    fun userstate_is_exposed_as_outgoing_confirmation() {
        val event = TwitchIrcProtocolParser.parse(
            "@display-name=Tester;emote-sets=0,33 :tmi.twitch.tv USERSTATE #channel"
        ) as TwitchIrcUserState

        assertEquals("channel", event.channel)
        assertEquals(setOf("0", "33"), event.emoteSetIds)
    }

    @Test
    fun notice_preserves_message_identifier() {
        val event = TwitchIrcProtocolParser.parse(
            "@msg-id=msg_rate-limit :tmi.twitch.tv NOTICE #channel :Too fast"
        )

        assertEquals(
            TwitchIrcNotice(
                msgId = "msg_rate-limit",
                message = "Too fast"
            ),
            event
        )
    }

    @Test
    fun unsupported_or_malformed_lines_are_ignored() {
        assertNull(TwitchIrcProtocolParser.parse(":tmi.twitch.tv 001 user :Welcome"))
        assertNull(TwitchIrcProtocolParser.parse("malformed PRIVMSG"))
        assertNull(TwitchIrcProtocolParser.parse(""))
    }
}
