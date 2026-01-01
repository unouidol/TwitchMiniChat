package com.fs.twitchminichat.v2

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

private const val HISTORY_BASE_URL = "https://api.ircminichat.party"
private const val HISTORY_SECONDS = 3600
private const val HISTORY_SECRET_KEY = "" // <-- metti la tua key (se la usi)

class ChatFragment : Fragment(R.layout.fragment_chat) {

    // Se vuoi history SOLO sul canale cachato dal tuo bot:
    private val CACHED_CHANNEL = "unouidol"

    private var cfg: AccountConfig? = null

    private var readClient: TwitchChatClient? = null
    private var sendClient: TwitchChatClient? = null

    @Volatile private var sendReady = false


    private lateinit var textStatus: TextView
    private lateinit var scrollChat: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var editMessage: EditText
    private lateinit var btnSend: Button

    // Dedup semplice (evita doppioni history/live o glitch)
    private val recentSet = HashSet<String>()
    private val recentQueue = ArrayDeque<String>()
    private val recentMax = 300

    // colori bot fissi
    private val BOT_COLORS = mapOf(
        "elbierro" to 0xFFFFD700.toInt(),
        "pokemoncommunitygame" to 0xFFFF5555.toInt()
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textStatus = view.findViewById(R.id.textStatus)
        scrollChat = view.findViewById(R.id.scrollChat)
        chatContainer = view.findViewById(R.id.chatContainer)
        editMessage = view.findViewById(R.id.editMessage)
        btnSend = view.findViewById(R.id.btnSend)

        val accountId = requireArguments().getString(ARG_ACCOUNT_ID) ?: return
        val repo = AccountRepository(requireContext())
        cfg = repo.getById(accountId)

        textStatus.text = cfg?.let { "Loading @${it.username} on #${it.channel}..." } ?: "Account not found"

        btnSend.setOnClickListener { sendCurrentMessage() }

        editMessage.setOnEditorActionListener { _, actionId, event ->
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_SEND || isEnterKey) {
                sendCurrentMessage()
                true
            } else false
        }
    }

    override fun onStart() {
        super.onStart()
        connectIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        readClient?.disconnect()
        sendClient?.disconnect()
        readClient = null
        sendClient = null
        sendReady = false

        textStatus.text = cfg?.let { "Disconnected (@${it.username})" } ?: "Disconnected"
    }

    private fun connectIfNeeded() {
        val c = cfg ?: return
        if (c.accessToken.isBlank()) {
            textStatus.text = "Missing token for @${c.username} (login again)"
            return
        }

        // history solo se canale supportato dal bot
        if (c.channel.lowercase() == CACHED_CHANNEL.lowercase()) {
            loadHistoryFromBot(c)
        } else {
            appendSystemLine("Storico disponibile solo per #$CACHED_CHANNEL")
        }

        textStatus.text = "Connecting as @${c.username} on #${c.channel}..."

// READ client (riceve chat)
        val rc = TwitchChatClient(
            username = c.username,
            oauthToken = c.accessToken,
            channel = c.channel
        )
        readClient = rc

// SEND client (invia chat)
        val sc = TwitchChatClient(
            username = c.username,
            oauthToken = c.accessToken,
            channel = c.channel
        )
        sendClient = sc
        sendReady = false

        rc.connect(
            onConnected = {
                activity?.runOnUiThread {
                    textStatus.text = "Connected (read) as @${c.username} on #${c.channel}"
                }
            },
            onMessage = { user, msg, emotesRaw, _ ->
                activity?.runOnUiThread {
                    appendChatLine(user, msg, emotesRaw)
                }
            },
            onError = { err ->
                activity?.runOnUiThread {
                    textStatus.text = "Read error: ${err.message}"
                }
            }
        )

        sc.connect(
            onConnected = {
                sendReady = true
                activity?.runOnUiThread {
                    // opzionale: aggiorna status quando anche send è pronto
                    textStatus.text = "Connected as @${c.username} on #${c.channel}"
                }
            },
            onMessage = null, // non ci serve leggere da qui
            onError = { err ->
                sendReady = false
                activity?.runOnUiThread {
                    textStatus.text = "Send error: ${err.message}"
                }
            }
        )

    }

    // ----------- SEND (server-echo only) -----------

    private fun sendCurrentMessage() {
        val c = cfg ?: return
        val client = sendClient ?: return
        if (!sendReady) {
            appendSystemLine("Connection not ready…")
            return
        }


        val text = editMessage.text?.toString().orEmpty().trim()
        if (text.isBlank()) return

        client.sendMessage(text)

        // NON aggiungiamo local-echo: lo vedrai solo quando torna dal server con emotes tag
        editMessage.text?.clear()

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editMessage.windowToken, 0)
    }

    // ----------- HISTORY -----------

    private fun loadHistoryFromBot(config: AccountConfig) {
        if (HISTORY_BASE_URL.isBlank()) return

        thread {
            try {
                val urlString =
                    "$HISTORY_BASE_URL/history" +
                            "?channel=${config.channel}" +
                            "&seconds=$HISTORY_SECONDS" +
                            "&key=$HISTORY_SECRET_KEY"

                val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                }

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    conn.disconnect()
                    return@thread
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val arr = JSONArray(body)
                activity?.runOnUiThread {
                    appendSystemLine("Storico: ${arr.length()} messaggi")
                }

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val user = obj.optString("user", "unknown")
                    val text = obj.optString("text", "")
                    if (text.isBlank()) continue

                    // evita i tuoi (opzionale)
                    if (user.equals(config.username, ignoreCase = true)) continue

                    // NEW: emotes raw dal bot (può mancare/null)
                    val emotesRaw = obj.optString("emotes", "")
                        .takeIf { it.isNotBlank() }

                    activity?.runOnUiThread {
                        appendChatLine(user, text, emotesRaw = emotesRaw)
                    }
                }

            } catch (_: Exception) {
                // silenzio: se bot/tunnel non disponibile, va solo live
            }
        }
    }

    private fun appendSystemLine(text: String) {
        val tv = TextView(requireContext())
        tv.text = "• $text"
        tv.setTextColor(0xFFAAAAAA.toInt())
        tv.textSize = 12f
        chatContainer.addView(tv)
        scrollToBottom()
    }

    // ----------- RENDER CHAT + EMOTES -----------

    private data class EmoteSpanInfo(val id: String, val start: Int, val end: Int)

    private fun parseEmoteSpans(message: String, emotesRaw: String?): List<EmoteSpanInfo> {
        if (emotesRaw.isNullOrEmpty()) return emptyList()

        val result = mutableListOf<EmoteSpanInfo>()
        val emoteSpecs = emotesRaw.split('/')

        for (spec in emoteSpecs) {
            val parts = spec.split(':')
            if (parts.size < 2) continue
            val emoteId = parts[0]
            val positions = parts[1].split(',')

            for (p in positions) {
                val se = p.split('-')
                if (se.size != 2) continue
                val start = se[0].toIntOrNull() ?: continue
                val end = se[1].toIntOrNull() ?: continue
                if (start < 0 || end >= message.length || start > end) continue
                result.add(EmoteSpanInfo(emoteId, start, end))
            }
        }

        return result.sortedBy { it.start }
    }

    private fun appendChatLine(user: String, message: String, emotesRaw: String?) {
        // dedup
        val key = "${user.lowercase()}\u0000$message"
        if (recentSet.contains(key)) return
        recentSet.add(key)
        recentQueue.addLast(key)
        while (recentQueue.size > recentMax) {
            val old = recentQueue.removeFirst()
            recentSet.remove(old)
        }

        val tv = createMessageTextView(user, message, emotesRaw)
        chatContainer.addView(tv)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        scrollChat.post { scrollChat.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun createMessageTextView(user: String, message: String, emotesRaw: String?): TextView {
        val tv = TextView(requireContext())
        tv.setTextColor(0xFFFFFFFF.toInt())
        tv.textSize = 14f

        val prefix = "[$user] "
        val fullText = prefix + message
        val spannable = SpannableStringBuilder(fullText)

        // colore nome
        val lowerUser = user.lowercase()
        val lowerSelf = cfg?.username?.lowercase()
        val botColor = BOT_COLORS[lowerUser]

        val nameColor = when {
            lowerUser == lowerSelf -> 0xFF00FFAA.toInt()
            botColor != null -> botColor
            else -> 0xFFAAAAAA.toInt()
        }

        val openBracket = fullText.indexOf('[')
        val closeBracket = fullText.indexOf(']')
        if (openBracket != -1 && closeBracket > openBracket) {
            spannable.setSpan(
                ForegroundColorSpan(nameColor),
                openBracket + 1,
                closeBracket,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val emoteInfos = parseEmoteSpans(message, emotesRaw)
        if (emoteInfos.isEmpty()) {
            tv.text = spannable
            return tv
        }

        // testo provvisorio
        tv.text = spannable

        for (info in emoteInfos) {
            val startInFull = prefix.length + info.start
            val endInFull = prefix.length + info.end + 1
            if (startInFull < 0 || endInFull > fullText.length) continue

            val url = "https://static-cdn.jtvnw.net/emoticons/v2/${info.id}/static/light/1.0"

            Glide.with(this)
                .asDrawable()
                .load(url)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        val size = (tv.textSize * 1.5).toInt()
                        resource.setBounds(0, 0, size, size)

                        val imageSpan = ImageSpan(resource, ImageSpan.ALIGN_BOTTOM)
                        spannable.setSpan(imageSpan, startInFull, endInFull, 0)

                        tv.text = spannable
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }

        return tv
    }

    companion object {
        private const val ARG_ACCOUNT_ID = "accountId"

        fun newInstance(accountId: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply { putString(ARG_ACCOUNT_ID, accountId) }
            }
        }
    }
}
