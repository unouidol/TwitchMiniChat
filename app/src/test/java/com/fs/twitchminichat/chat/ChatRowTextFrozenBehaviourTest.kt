package com.fs.twitchminichat.chat

import com.fs.twitchminichat.TwitchEmoteMarker
import com.fs.twitchminichat.TwitchEmoteMessageFormatter.EMOTE_MARKER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Freezes the text decisions the chat page made for a row in ChatFragment's
 * createMessageTextView and appendSystemLine at a382c94: the text, the reply header, the
 * sender's name and its colour, which mentions of the signed-in user are highlighted, the look
 * that gives the row, the emote markers and where links are searched.
 *
 * That code was private to the fragment and built Android views, so it could not run here.
 * These tests were written from it, and pass against [ChatRowText] while the page still makes
 * the decisions itself; they stay unchanged when the page starts taking them from [ChatRowText].
 * That the page then shows what it showed before is the parity harness's to prove, on a device.
 */
class ChatRowTextFrozenBehaviourTest {

    // Mentions that are highlighted

    @Test
    fun frozen_aMentionOfTheSignedInUser_isHighlighted_withABackgroundThenABoldSpan_andTheMentionLook() {
        val row = message(raw = "hi @me there")

        assertEquals("[bob] hi @me there", row.text)
        assertEquals(
            listOf(RowTextSpan.Username(1, 4, BOB)) + mention(9, 12),
            row.spans
        )
        assertEquals(RowLook.MENTION, row.look)
    }

    @Test
    fun frozen_theMentionIgnoresCase_inTheTextAndInTheSignedInName() {
        assertEquals(mention(6, 9), mentionSpans(message(raw = "@ME", signedIn = "me")))
        assertEquals(mention(6, 9), mentionSpans(message(raw = "@me", signedIn = "Me")))
    }

    @Test
    fun frozen_aMentionNextToPunctuationOrAtEitherEndOfTheMessage_isHighlighted() {
        assertEquals(mention(6, 9), mentionSpans(message(raw = "@me, ok")))
        assertEquals(mention(7, 10), mentionSpans(message(raw = "(@me)")))
        assertEquals(mention(9, 12), mentionSpans(message(raw = "ok @me")))
        assertEquals(mention(6, 9), mentionSpans(message(raw = "@me")))
    }

    @Test
    fun frozen_everyMention_isHighlighted_inTheOrderTheyAppear() {
        val row = message(raw = "@me and @me")

        assertEquals(mention(6, 9) + mention(14, 17), mentionSpans(row))
        assertEquals(RowLook.MENTION, row.look)
    }

    @Test
    fun frozen_aMentionAfterAnyCharacterOutsideAsciiLettersDigitsAndUnderscore_isHighlighted() {
        assertEquals(mention(7, 10), mentionSpans(message(raw = "è@me")))
        assertEquals(mention(7, 10), mentionSpans(message(raw = "-@me")))
        assertEquals(mention(7, 10), mentionSpans(message(raw = "@@me")))
    }

    @Test
    fun frozen_aReplyToTheSignedInUser_highlightsTheMentionInTheReplyHeader() {
        val row = message(raw = "thanks", reply = "me")

        assertEquals(mention(14, 17), mentionSpans(row))
        assertEquals(RowLook.MENTION, row.look)
    }

    @Test
    fun frozen_theSignedInNameIsTrimmed_beforeTheMentionSearch() {
        assertEquals(mention(6, 9), mentionSpans(message(raw = "@me", signedIn = "  me ")))
    }

    // Mentions that are not highlighted

    @Test
    fun frozen_theNameWithoutTheAt_isNotAMention() {
        assertNotHighlighted(message(raw = "me too"))
    }

    @Test
    fun frozen_aLongerNameThatStartsWithTheSignedInName_isNotAMention() {
        assertNotHighlighted(message(raw = "@meow"))
        assertNotHighlighted(message(raw = "@me_2"))
        assertNotHighlighted(message(raw = "@me2"))
    }

    @Test
    fun frozen_anAtAfterAnAsciiLetterDigitOrUnderscore_isNotAMention() {
        assertNotHighlighted(message(raw = "x@me"))
        assertNotHighlighted(message(raw = "1@me"))
        assertNotHighlighted(message(raw = "_@me"))
    }

    @Test
    fun frozen_withoutASignedInName_nothingIsAMention_notEvenABareAt() {
        assertNotHighlighted(message(raw = "@ hello @me", signedIn = null))
        assertNotHighlighted(message(raw = "@ hello @me", signedIn = ""))
        assertNotHighlighted(message(raw = "@ hello @me", signedIn = "   "))
    }

    @Test
    fun frozen_theSignedInNameIsMatchedLiterally_notAsAPattern() {
        assertNotHighlighted(message(raw = "@mxe", signedIn = "m.e"))
        assertEquals(mention(6, 10), mentionSpans(message(raw = "@m.e", signedIn = "m.e")))
    }

    @Test
    fun frozen_theSendersOwnBracketedName_isNotAMention() {
        val row = message(user = "me", raw = "hello")

        assertEquals(listOf(RowTextSpan.Username(1, 3, UsernameColor.SignedIn)), row.spans)
        assertEquals(RowLook.PLAIN, row.look)
    }

    // The reply header

    @Test
    fun frozen_aReply_beginsWithAHeaderInTheSecondaryColour_andTheNameFollowsIt() {
        val row = message(raw = "hi", reply = "carol")
        val header = "↪ replying to @carol\n"

        assertEquals("$header[bob] hi", row.text)
        assertEquals(
            listOf(
                RowTextSpan.Secondary(0, header.length),
                RowTextSpan.Username(header.length + 1, header.length + 4, BOB)
            ),
            row.spans
        )
        assertEquals(header.length + 6, row.linksFrom)
        assertEquals(header.length + 6, row.emoteMarkerOffset)
    }

    @Test
    fun frozen_aMissingEmptyOrBlankReplyLogin_addsNoHeader() {
        for (reply in listOf(null, "", "   ")) {
            val row = message(raw = "hi", reply = reply)

            assertEquals("[bob] hi", row.text)
            assertEquals(listOf(RowTextSpan.Username(1, 4, BOB)), row.spans)
        }
    }

    @Test
    fun frozen_bracketsInTheReplyLogin_doNotMoveTheNameSpan() {
        val row = message(raw = "hi", reply = "[x]")
        val header = "↪ replying to @[x]\n"

        assertEquals(RowTextSpan.Username(header.length + 1, header.length + 4, BOB), row.spans[1])
    }

    // The sender's name and its colour

    @Test
    fun frozen_theSignedInUsersOwnName_takesTheSignedInColour_inAnyCase() {
        val row = message(user = "Me", raw = "hi", signedIn = "mE")

        assertEquals(listOf(RowTextSpan.Username(1, 3, UsernameColor.SignedIn)), row.spans)
    }

    @Test
    fun frozen_theSignedInNameIsNotTrimmed_forTheSendersColour() {
        val row = message(user = "me", raw = "hi", signedIn = " me ")

        assertEquals(RowTextSpan.Username(1, 3, hashed(240f)), row.spans.single())
    }

    @Test
    fun frozen_theBotTable_givesItsNamesAFixedColour_inAnyCase() {
        assertEquals(
            UsernameColor.Fixed(0xFFFF5555.toInt()),
            nameColour(message(user = "PokemonCommunityGame", raw = "A wild Pokemon appeared"))
        )
        assertEquals(
            UsernameColor.Fixed(0xFFFFD700.toInt()),
            nameColour(message(user = "ElBierro", raw = "hi"))
        )
    }

    @Test
    fun frozen_theSignedInColour_winsOverTheBotTable() {
        val row = message(user = "PokemonCommunityGame", raw = "hi", signedIn = "pokemoncommunitygame")

        assertEquals(UsernameColor.SignedIn, nameColour(row))
    }

    @Test
    fun frozen_anyOtherName_takesAHueFromItsLowercasedName_atSaturation075AndValue095() {
        assertEquals(hashed(157f), nameColour(message(user = "bob", raw = "hi")))
        assertEquals(hashed(157f), nameColour(message(user = "BOB", raw = "hi")))
        assertEquals(hashed(211f), nameColour(message(user = "viewer_42", raw = "hi")))
        assertEquals(hashed(0f), nameColour(message(user = "polygenelubricants", raw = "hi")))
    }

    @Test
    fun frozen_withoutASignedInName_theSenderIsColouredByTheTableOrTheHash() {
        assertEquals(hashed(157f), nameColour(message(user = "bob", raw = "hi", signedIn = null)))
        assertEquals(
            UsernameColor.Fixed(0xFFFF5555.toInt()),
            nameColour(message(user = "pokemoncommunitygame", raw = "hi", signedIn = null))
        )
    }

    @Test
    fun frozen_anEmptySender_getsAnEmptyNameSpan_betweenTheBrackets() {
        val row = message(user = "", raw = "hi")

        assertEquals("[] hi", row.text)
        assertEquals(listOf(RowTextSpan.Username(1, 1, hashed(0f))), row.spans)
    }

    // The empty message, and a message

    @Test
    fun frozen_anEmptyMessage_isTheBracketedNameAlone_withLinksSearchedFromItsEnd() {
        val row = message(raw = "")

        assertEquals("[bob] ", row.text)
        assertEquals(listOf(RowTextSpan.Username(1, 4, BOB)), row.spans)
        assertEquals(RowLook.PLAIN, row.look)
        assertEquals(6, row.linksFrom)
        assertEquals(row.text.length, row.linksFrom)
        assertEquals(emptyList<TwitchEmoteMarker>(), row.emoteMarkers)
    }

    @Test
    fun frozen_aMessage_isPrimaryTextAt14sp_plain_withLinksSearchedAfterTheName() {
        val row = message(raw = "see https://example.com")

        assertEquals("[bob] see https://example.com", row.text)
        assertEquals(RowTextColor.PRIMARY, row.textColor)
        assertEquals(14f, row.textSizeSp)
        assertEquals(RowLook.PLAIN, row.look)
        assertEquals(6, row.linksFrom)
        assertEquals(listOf(RowTextSpan.Username(1, 4, BOB)), row.spans)
    }

    @Test
    fun frozen_anActionMessage_losesItsFraming() {
        assertEquals("[bob] waves", message(raw = "\u0001ACTION waves\u0001").text)
    }

    // A message with emote markers

    @Test
    fun frozen_emotes_becomeMarkers_indexedFromTheEndOfTheNamePrefix() {
        val row = message(raw = "Kappa hi Kappa", emotes = "25:0-4,9-13")

        assertEquals("[bob] ${EMOTE_MARKER} hi ${EMOTE_MARKER}", row.text)
        assertEquals(
            listOf(TwitchEmoteMarker("25", 0), TwitchEmoteMarker("25", 5)),
            row.emoteMarkers
        )
        assertEquals(6, row.emoteMarkerOffset)
        assertEquals(6, row.linksFrom)
        assertEquals(EMOTE_MARKER, row.text[row.emoteMarkerOffset + row.emoteMarkers[1].markerIndex])
    }

    @Test
    fun frozen_inAReply_theEmoteMarkersAreIndexedFromTheEndOfTheHeaderAndName() {
        val row = message(raw = "Kappa", emotes = "25:0-4", reply = "carol")
        val prefix = "↪ replying to @carol\n[bob] "

        assertEquals("$prefix$EMOTE_MARKER", row.text)
        assertEquals(prefix.length, row.emoteMarkerOffset)
        assertEquals(EMOTE_MARKER, row.text[row.emoteMarkerOffset + row.emoteMarkers.single().markerIndex])
    }

    @Test
    fun frozen_aMentionAfterAnEmote_isFoundInTheMarkedText() {
        val row = message(raw = "Kappa @me", emotes = "25:0-4")

        assertEquals("[bob] $EMOTE_MARKER @me", row.text)
        assertEquals(mention(8, 11), mentionSpans(row))
        assertEquals(RowLook.MENTION, row.look)
    }

    // The look

    @Test
    fun frozen_theMentionLook_isARoundedHighlightOfRadius10_withPadding8and6_andThePlainLookIsNothing() {
        assertEquals(
            RowLook(RowBackground.MentionHighlight(cornerRadiusDp = 10), RowPadding(8, 6, 8, 6)),
            RowLook.MENTION
        )
        assertEquals(RowLook(RowBackground.None, RowPadding(0, 0, 0, 0)), RowLook.PLAIN)
    }

    // A system line

    @Test
    fun frozen_aSystemLine_isTheFormattedTextAlone_secondaryAt12sp_plain_withNoSpansLinksOrEmotes() {
        val row = ChatRowText.systemLine("• Joined #channel, see https://example.com @me")

        assertEquals("• Joined #channel, see https://example.com @me", row.text)
        assertEquals(emptyList<RowTextSpan>(), row.spans)
        assertEquals(RowTextColor.SECONDARY, row.textColor)
        assertEquals(12f, row.textSizeSp)
        assertEquals(RowLook.PLAIN, row.look)
        assertNull(row.linksFrom)
        assertEquals(emptyList<TwitchEmoteMarker>(), row.emoteMarkers)
        assertEquals(0, row.emoteMarkerOffset)
    }

    private fun message(
        user: String = "bob",
        raw: String,
        emotes: String? = null,
        reply: String? = null,
        signedIn: String? = "me"
    ): RowText = ChatRowText.message(
        user = user,
        rawMessage = raw,
        emotesRaw = emotes,
        replyParentUserLogin = reply,
        signedInUsername = signedIn
    )

    private fun mention(start: Int, end: Int): List<RowTextSpan> = listOf(
        RowTextSpan.MentionBackground(start, end),
        RowTextSpan.MentionBold(start, end)
    )

    private fun mentionSpans(row: RowText): List<RowTextSpan> = row.spans.filter { span ->
        span is RowTextSpan.MentionBackground || span is RowTextSpan.MentionBold
    }

    private fun nameColour(row: RowText): UsernameColor =
        row.spans.filterIsInstance<RowTextSpan.Username>().single().color

    private fun assertNotHighlighted(row: RowText) {
        assertEquals(emptyList<RowTextSpan>(), mentionSpans(row))
        assertEquals(RowLook.PLAIN, row.look)
        assertTrue(row.spans.single() is RowTextSpan.Username)
    }

    private companion object {
        fun hashed(hue: Float) = UsernameColor.Hashed(hue = hue, saturation = 0.75f, value = 0.95f)

        val BOB = hashed(157f)
    }
}
