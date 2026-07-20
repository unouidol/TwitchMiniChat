package com.fs.twitchminichat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TwitchIrcSessionMetadataStoreTest {

    @Test
    fun parser_extracts_global_user_emote_sets_and_room_identity() {
        val globalEvent = TwitchIrcProtocolParser.parse(
            "@display-name=Tester;emote-sets=0,33,50;user-id=12345678 " +
                    ":tmi.twitch.tv GLOBALUSERSTATE"
        ) as TwitchIrcGlobalUserState

        val roomEvent = TwitchIrcProtocolParser.parse(
            "@emote-only=0;room-id=87654321;slow=0 " +
                    ":tmi.twitch.tv ROOMSTATE #ExampleChannel"
        ) as TwitchIrcRoomState

        assertEquals("12345678", globalEvent.userId)
        assertEquals(setOf("0", "33", "50"), globalEvent.emoteSetIds)
        assertEquals("examplechannel", roomEvent.channel)
        assertEquals("87654321", roomEvent.roomId)
    }

    @Test
    fun store_merges_partial_updates_without_losing_previous_identity() {
        val accountId = "metadata-test-account"
        TwitchIrcSessionMetadataStore.remove(accountId)

        TwitchIrcSessionMetadataStore.merge(
            accountId = accountId,
            update = TwitchIrcSessionMetadataUpdate(
                userId = "12345678",
                emoteSetIds = setOf("0", "33")
            )
        )

        val snapshot = TwitchIrcSessionMetadataStore.merge(
            accountId = accountId,
            update = TwitchIrcSessionMetadataUpdate(
                channel = "#ExampleChannel",
                roomId = "87654321"
            )
        )

        assertEquals("12345678", snapshot.userId)
        assertEquals(setOf("0", "33"), snapshot.emoteSetIds)
        assertEquals("87654321", snapshot.roomIdFor("examplechannel"))
        assertNull(snapshot.roomIdFor("otherchannel"))

        TwitchIrcSessionMetadataStore.remove(accountId)
    }

    @Test
    fun userstate_replaces_the_emote_set_snapshot() {
        val accountId = "metadata-emote-update-test"
        TwitchIrcSessionMetadataStore.remove(accountId)

        TwitchIrcSessionMetadataStore.merge(
            accountId = accountId,
            update = TwitchIrcSessionMetadataUpdate(
                emoteSetIds = setOf("0", "33")
            )
        )

        val snapshot = TwitchIrcSessionMetadataStore.merge(
            accountId = accountId,
            update = TwitchIrcSessionMetadataUpdate(
                channel = "channel",
                emoteSetIds = setOf("0", "50", "237")
            )
        )

        assertEquals(setOf("0", "50", "237"), snapshot.emoteSetIds)

        TwitchIrcSessionMetadataStore.remove(accountId)
    }
}
