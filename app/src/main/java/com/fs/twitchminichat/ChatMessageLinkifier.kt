package com.fs.twitchminichat

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import androidx.core.text.util.LinkifyCompat

/**
 * Adds manually handled web links to the message portion of a chat row.
 *
 * Android's default URLSpan is never copied to the final row, preventing a link
 * from bypassing TMC's warning and external-browser selection flow.
 */
object ChatMessageLinkifier {

    /**
     * Adds clickable HTTP and HTTPS spans after the username and reply prefix.
     */
    fun addWebLinks(
        text: Spannable,
        messageStartIndex: Int,
        onLinkClick: (String) -> Unit
    ) {
        if (messageStartIndex < 0 || messageStartIndex >= text.length) {
            return
        }

        val messageText = SpannableString(
            text.subSequence(messageStartIndex, text.length)
        )

        LinkifyCompat.addLinks(
            messageText,
            Linkify.WEB_URLS
        )

        messageText
            .getSpans(
                0,
                messageText.length,
                URLSpan::class.java
            )
            .forEach { urlSpan ->
                val start = messageText.getSpanStart(urlSpan)
                val end = messageText.getSpanEnd(urlSpan)

                if (start < 0 || end <= start) {
                    return@forEach
                }

                text.setSpan(
                    ExternalWebLinkSpan(
                        url = urlSpan.url,
                        onLinkClick = onLinkClick
                    ),
                    messageStartIndex + start,
                    messageStartIndex + end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
    }

    /** Delegates one visible link tap to TMC's external-link controller. */
    private class ExternalWebLinkSpan(
        private val url: String,
        private val onLinkClick: (String) -> Unit
    ) : ClickableSpan() {

        /** Opens this link only after a direct user tap. */
        override fun onClick(widget: View) {
            onLinkClick(url)
        }
    }
}
