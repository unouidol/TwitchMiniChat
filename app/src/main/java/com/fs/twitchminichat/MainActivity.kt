package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

private const val HISTORY_BASE_URL = "https://api.ircminichat.party"
private const val HISTORY_SECONDS = 3600
private const val HISTORY_SECRET_KEY = "HCJbQrlivoHyTtpBoNESePO7RIbxKUHK" // <-- metti la tua key

private const val CLEAR_CHAT_ON_SWITCH = false

class MainActivity : AppCompatActivity() {

    private var currentAccountSlot: Int = 1
    private var currentAccount: AccountConfig? = null
    private var chatClient: TwitchChatClient? = null

    // nonce -> TextView pending
    private val pendingByNonce = mutableMapOf<String, TextView>()

    // login Twitch dei bot → colore fisso (ARGB)
    private val BOT_COLORS = mapOf(
        "elbierro" to 0xFFFFD700.toInt(),
        "pokemoncommunitygame" to 0xFFFF5555.toInt()
    )

    private lateinit var textStatus: TextView
    private lateinit var editMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var radioAccounts: RadioGroup
    private lateinit var radioAccount1: RadioButton
    private lateinit var radioAccount2: RadioButton

    private lateinit var scrollChat: ScrollView
    private lateinit var chatContainer: LinearLayout

    private var lastConnectedSignature: String? = null
    private var lastHistorySignature: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textStatus = findViewById(R.id.textStatus)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)
        radioAccounts = findViewById(R.id.radioAccounts)
        radioAccount1 = findViewById(R.id.radioAccount1)
        radioAccount2 = findViewById(R.id.radioAccount2)
        scrollChat = findViewById(R.id.scrollChat)
        chatContainer = findViewById(R.id.chatContainer)

        val btnConfigureAccount: Button = findViewById(R.id.btnConfigureAccount)
        val editUsername: EditText = findViewById(R.id.editUsername)
        val editChannel: EditText = findViewById(R.id.editChannel)
        val btnSaveAccount: Button = findViewById(R.id.btnSaveAccount)

        fun refreshAccountFields(slot: Int) {
            val cfg = AccountStorage.loadAccount(this, slot)
            if (cfg != null) {
                editUsername.setText(cfg.username)
                editChannel.setText(cfg.channel)
            } else {
                editUsername.setText("")
                editChannel.setText("")
            }
        }

        ensureDefaultAccounts()

        currentAccountSlot = AccountStorage.getActiveAccount(this)

        radioAccounts.setOnCheckedChangeListener { _, checkedId ->
            val slot = if (checkedId == R.id.radioAccount1) 1 else 2
            if (slot != currentAccountSlot) {
                switchToAccount(slot)
            }
            refreshAccountFields(slot)
        }

        if (currentAccountSlot == 1) radioAccount1.isChecked = true else radioAccount2.isChecked = true
        refreshAccountFields(currentAccountSlot)

        btnSaveAccount.setOnClickListener {
            val username = editUsername.text.toString().trim()
            val channel = editChannel.text.toString().trim()

            if (username.isEmpty() || channel.isEmpty()) {
                Toast.makeText(this, "Insert username and channel", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val existing = AccountStorage.loadAccount(this, currentAccountSlot)
            val accessToken = existing?.accessToken ?: ""

            val newCfg = AccountConfig(username = username, channel = channel, accessToken = accessToken)
            AccountStorage.saveAccount(this, currentAccountSlot, newCfg)

            Toast.makeText(this, "Account $currentAccountSlot saved as $username (#$channel)", Toast.LENGTH_LONG).show()
            switchToAccount(currentAccountSlot)
        }

        btnConfigureAccount.setOnClickListener {
            startTwitchLoginForCurrentSlot()
        }

        btnSend.setOnClickListener { sendCurrentMessage() }

        editMessage.setOnEditorActionListener { _, actionId, event ->
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_SEND || isEnterKey) {
                sendCurrentMessage()
                true
            } else false
        }

        switchToAccount(currentAccountSlot)
    }

    override fun onResume() {
        super.onResume()
        val cfg = AccountStorage.loadAccount(this, currentAccountSlot)
        if (cfg != null) {
            val sig = signatureOf(cfg)
            if (sig != lastConnectedSignature) {
                switchToAccount(currentAccountSlot)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatClient?.disconnect()
    }

    private fun signatureOf(cfg: AccountConfig): String =
        "${cfg.username.lowercase()}|${cfg.channel.lowercase()}|${cfg.accessToken}"

    private fun startTwitchLoginForCurrentSlot() {
        val clientId = "7tvgt6i65b58k3e8lhxxv1p0b2vrib"
        val redirectUri = "https://unouidol.github.io/ircminichat/callback.html"

        val slot = currentAccountSlot
        val state = "slot${slot}-${System.currentTimeMillis()}"

        val uri = Uri.parse("https://id.twitch.tv/oauth2/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", TWITCH_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.TWITCH_REDIRECT_URI)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("scope", "chat:read chat:edit")
            .appendQueryParameter("state", state)
            .build()

        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun ensureDefaultAccounts() {
        // tutto via login
    }

    // ---------- Invio con pending + nonce ----------

    private fun sendCurrentMessage() {
        val text = editMessage.text.toString()
        if (text.isBlank()) return

        val account = currentAccount
        val client = chatClient
        if (account == null || client == null) {
            Toast.makeText(this, "Not connected to Twitch", Toast.LENGTH_SHORT).show()
            return
        }

        // 1) riga pending (testo puro)
        val pendingTv = appendChatLine(account.username, text, emotesRaw = null, isPending = true)

        // 2) invio con nonce
        val nonce = client.sendMessageWithNonce(text)
        if (nonce.isNotBlank()) {
            pendingByNonce[nonce] = pendingTv
        }

        editMessage.text.clear()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editMessage.windowToken, 0)
    }

    // ---------- Gestione account ----------

    private fun switchToAccount(slot: Int) {
        AccountStorage.setActiveAccount(this, slot)
        currentAccountSlot = slot

        pendingByNonce.clear()

        chatClient?.disconnect()
        chatClient = null
        currentAccount = null

        val config = AccountStorage.loadAccount(this, slot)
        if (config == null) {
            textStatus.text = "Account $slot not configured"
            return
        }

        currentAccount = config
        lastConnectedSignature = signatureOf(config)

        if (CLEAR_CHAT_ON_SWITCH) {
            chatContainer.removeAllViews()
        }

        val historySig = "slot=$slot|chan=${config.channel.lowercase()}|secs=$HISTORY_SECONDS"
        if (historySig != lastHistorySignature) {
            lastHistorySignature = historySig
            loadHistoryFromBot(config)
        }

        textStatus.text = "Connecting as ${config.username}..."

        val client = TwitchChatClient(
            username = config.username,
            oauthToken = config.accessToken,
            channel = config.channel
        )
        chatClient = client

        client.connect(
            onConnected = {
                runOnUiThread {
                    textStatus.text = "Connected as ${config.username} on #${config.channel}"
                }
            },
            onMessage = { user, msg, emotesRaw, clientNonce ->
                runOnUiThread {
                    // Se è il mio messaggio e ha client-nonce -> aggiorna il pending invece di aggiungere una nuova riga
                    val selfLower = currentAccount?.username?.lowercase()
                    val isMe = selfLower != null && user.lowercase() == selfLower

                    if (isMe && !clientNonce.isNullOrBlank()) {
                        val pendingTv = pendingByNonce.remove(clientNonce)
                        if (pendingTv != null) {
                            applyMessageToExistingTextView(pendingTv, user, msg, emotesRaw, isPending = false)
                            return@runOnUiThread
                        }
                    }

                    appendChatLine(user, msg, emotesRaw, isPending = false)
                }
            },
            onError = { error ->
                runOnUiThread {
                    textStatus.text = "Connection Error (${config.username}): ${error.message}"
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // ---------- Storico dal bot (con emotes se presenti) ----------

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
                val selfLower = config.username.lowercase()

                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val user = obj.optString("user", "unknown")
                    val text = obj.optString("text", "")
                    if (text.isBlank()) continue
                    if (user.lowercase() == selfLower) continue

                    val emotesRaw = if (obj.has("emotes") && !obj.isNull("emotes")) obj.optString("emotes", null) else null
                    val emotes = emotesRaw?.takeIf { it.isNotBlank() }

                    runOnUiThread { appendChatLine(user, text, emotes, isPending = false) }
                }
            } catch (_: Exception) {
                // ignoriamo
            }
        }
    }

    // ---------- Rendering testo + emote ----------

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

    private fun buildSpannableAndLoadEmotesIntoTextView(
        tv: TextView,
        user: String,
        message: String,
        emotesRaw: String?
    ): SpannableStringBuilder {
        val prefix = "[$user] "
        val fullText = prefix + message
        val spannable = SpannableStringBuilder(fullText)

        // Colore nome
        val lowerUser = user.lowercase()
        val lowerSelf = currentAccount?.username?.lowercase()
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
        if (emoteInfos.isEmpty()) return spannable

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

        return spannable
    }

    private fun applyMessageToExistingTextView(
        tv: TextView,
        user: String,
        message: String,
        emotesRaw: String?,
        isPending: Boolean
    ) {
        tv.alpha = if (isPending) 0.6f else 1.0f
        tv.setTypeface(null, if (isPending) Typeface.ITALIC else Typeface.NORMAL)

        val spannable = buildSpannableAndLoadEmotesIntoTextView(tv, user, message, emotesRaw)
        tv.text = spannable
    }

    private fun appendChatLine(
        user: String,
        message: String,
        emotesRaw: String?,
        isPending: Boolean
    ): TextView {
        val tv = TextView(this)
        tv.setTextColor(0xFFFFFFFF.toInt())
        tv.textSize = 14f

        applyMessageToExistingTextView(tv, user, message, emotesRaw, isPending)

        chatContainer.addView(tv)
        scrollChat.post { scrollChat.fullScroll(ScrollView.FOCUS_DOWN) }
        return tv
    }
}
