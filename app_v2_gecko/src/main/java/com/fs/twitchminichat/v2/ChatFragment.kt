package com.fs.twitchminichat.v2

import android.content.Context
import android.graphics.Color
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
import com.fs.twitchminichat.v2.pcg.PcgActivity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread
import android.util.Log
import androidx.activity.OnBackPressedCallback
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.view.ViewGroup
import android.widget.ImageButton
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent


private const val HISTORY_BASE_URL = "https://api.ircminichat.party"
private const val HISTORY_SECONDS = 3600
private val HISTORY_SECRET_KEY = BuildConfig.HISTORY_SECRET_KEY // <-- metti la tua key (se la usi)

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private var cfg: AccountConfig? = null
    private var accountId: String = ""

    private var readClient: TwitchChatClient? = null
    private var sendClient: TwitchChatClient? = null
    @Volatile private var sendReady = false

    private lateinit var textStatus: TextView
    private lateinit var scrollChat: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var editMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnStartPcg: Button
    private lateinit var btnJoinChannel: Button
    private lateinit var savedChannelsContainer: LinearLayout

    private lateinit var channelHistory: ChannelHistoryStore

    private lateinit var btnRefreshChat: ImageButton

    private var lastManualRefreshMs = 0L


    // Dedup "immediato": evita doppioni history+live nello stesso istante
    private var lastDedupKey: String? = null
    private var lastDedupAtMs: Long = 0L
    private val dedupWindowMs = 1500L

    // Dedup "LRU": evita duplicati quando ricarichi history al rientro
    private val seenKeys = LinkedHashMap<String, Unit>(1024, 0.75f, true)
    private val seenMax = 800

    // Evita di ricaricare l'history completa ogni volta che cambi tab
    private var historyLoaded = false

    // Per capire quanto sei stato via (es. apri PCG activity)
    private var lastPausedAtMs: Long = 0L

    // Colori bot fissi
    private val BOT_COLORS = mapOf(
        "elbierro" to 0xFFFFD700.toInt(),
        "pokemoncommunitygame" to 0xFFFF5555.toInt()
    )

    private val userColorCache = HashMap<String, Int>()

    private lateinit var editChannel: AutoCompleteTextView
    private lateinit var channelsAdapter: ArrayAdapter<String>

    private var suppressDropdownReopenUntilMs: Long = 0L

    private fun closeChannelDropdown() {
        if (!this::editChannel.isInitialized) return
        suppressDropdownReopenUntilMs = System.currentTimeMillis() + 250
        editChannel.dismissDropDown()
        editChannel.clearFocus()
    }

    private fun colorForUsername(user: String): Int {
        val key = user.lowercase()
        BOT_COLORS[key]?.let { return it }

        return userColorCache.getOrPut(key) {
            val h = (key.hashCode() and 0x7fffffff) % 360
            Color.HSVToColor(floatArrayOf(h.toFloat(), 0.75f, 0.95f))
        }
    }

    private fun refreshSavedChannelsUi() {
        if (!this::savedChannelsContainer.isInitialized) return
        savedChannelsContainer.post { renderSavedChannels() }
    }
    private fun buildChannelsAdapter(): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            requireContext(),
            R.layout.row_channel_dropdown,
            R.id.txtChannel,
            mutableListOf()
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: layoutInflater.inflate(R.layout.row_channel_dropdown, parent, false)
                val ch = getItem(position).orEmpty()

                val txt = v.findViewById<TextView>(R.id.txtChannel)
                val btn = v.findViewById<ImageButton>(R.id.btnRemove)

                txt.text = ch

                // ✅ tap sulla riga -> GO
                v.setOnClickListener {
                    editChannel.setText(ch, false)
                    editChannel.setSelection(editChannel.text.length)
                    editChannel.dismissDropDown()
                    joinChannelFromChat(ch)
                }

                // ✅ tap sulla X -> remove
                btn.setOnClickListener {
                    channelHistory.remove(accountId, ch)
                    refreshSavedChannelsUi()
                    editChannel.post { editChannel.showDropDown() }
                }

                return v
            }
        }
    }




    private fun joinChannelFromChat(channelRaw: String) {
        val ch = channelRaw.trim().removePrefix("#").lowercase()

        // Validazione Twitch channel name
        val ok = Regex("^[a-z0-9_]{1,25}$").matches(ch)
        if (!ok) {
            appendSystemLine("Invalid Channel Name")
            return
        }

        val c = cfg ?: return
        val current = c.channel.trim().removePrefix("#").lowercase()
        if (ch == current) return

        appendSystemLine("Channel Switch → #$ch")

        // Salva e aggiorna cfg
        AccountRepository(requireContext()).updateChannel(accountId, ch)
        cfg = AccountRepository(requireContext()).getById(accountId)

        Log.d("CHAN", "JOIN-> add recent accountId=$accountId ch=$ch")
        channelHistory.add(accountId, ch)
        Log.d("CHAN", "AFTER add recent list=" + channelHistory.get(accountId).joinToString())
        refreshSavedChannelsUi()

        // Reset “state” utile
        historyLoaded = false
        lastDedupKey = null
        lastDedupAtMs = 0L
        seenKeys.clear()
        chatContainer.removeAllViews()

        // Reconnect pulito
        readClient?.disconnect()
        sendClient?.disconnect()
        readClient = null
        sendClient = null
        sendReady = false

        connectIfNeeded()
    }

    private fun renderSavedChannels() {

        savedChannelsContainer.removeAllViews()
        val list = channelHistory.get(accountId)
        // aggiorna dropdown
        channelsAdapter.clear()
        channelsAdapter.addAll(list)
        channelsAdapter.notifyDataSetChanged()

        if (list.isEmpty()) return

        for (ch in list) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 6, 0, 6)
            }

            val tv = TextView(requireContext()).apply {
                text = "#$ch"
                textSize = 14f
                setTextColor(0xFFCCCCCC.toInt())
                setPadding(8, 8, 8, 8)
                setOnClickListener {
                    editChannel.setText(ch,false)
                    joinChannelFromChat(ch)
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val btnX = Button(requireContext()).apply {
                text = "X"
                setOnClickListener {
                    channelHistory.remove(accountId, ch)
                    renderSavedChannels()
                }
            }

            row.addView(tv)
            row.addView(btnX)
            savedChannelsContainer.addView(row)
        }
    }
    private fun sendOrGo() {
        val c = cfg ?: return

        val ch = editChannel.text?.toString().orEmpty().trim().removePrefix("#").lowercase()
        val current = c.channel.trim().removePrefix("#").lowercase()

        // Se l'utente ha scritto un canale valido e diverso -> GO
        if (ch.isNotBlank() && ch != current && Regex("^[a-z0-9_]{1,25}$").matches(ch)) {
            joinChannelFromChat(ch)
            editChannel.text?.clear()
            return
        }

        // Altrimenti -> SEND
        sendCurrentMessage()
    }
    @Suppress("ClickableViewAccessibility")
    private fun setupChannelDropdownInteractions() {
        editChannel.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
                editChannel.showDropDown()
            }
            false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Find views (TUTTE prima di usarle)
        textStatus = view.findViewById(R.id.textStatus)
        scrollChat = view.findViewById(R.id.scrollChat)
        chatContainer = view.findViewById(R.id.chatContainer)
        editMessage = view.findViewById(R.id.editMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnStartPcg = view.findViewById(R.id.btnStartPcg)
        btnRefreshChat = view.findViewById(R.id.btnRefreshChat)

        savedChannelsContainer = view.findViewById(R.id.savedChannelsContainer)

        // 2) Init store PRIMA di usarlo
        channelHistory = ChannelHistoryStore(requireContext())

        editChannel = view.findViewById(R.id.editChannel)
// Tap sulla chat -> chiudi dropdown al primo tap (senza rompere lo scroll)
        scrollChat.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                closeChannelDropdown()
                scrollChat.requestFocus() // importante: sposta davvero il focus fuori
            }
            false
        }

// Tap nel box messaggio -> chiudi dropdown AL PRIMO TAP (niente doppio tap)
        editMessage.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                closeChannelDropdown()
            }
            false
        }

        channelsAdapter = buildChannelsAdapter()

        editChannel.setAdapter(channelsAdapter)

        editChannel.threshold = 0

        editChannel.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                editChannel.dismissDropDown()
            } else {
                // se entri in focus e il campo è vuoto, mostra subito
                val t = editChannel.text?.toString().orEmpty().trim()
                if (t.isEmpty() || t == "#") {
                    editChannel.post { editChannel.showDropDown() }
                }
            }
        }
        view.isFocusableInTouchMode = true
        view.setOnTouchListener { _, _ ->
            view.requestFocus()            // toglie focus dall’editChannel
            editChannel.dismissDropDown()
            false
        }

        editChannel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val t = s?.toString().orEmpty().trim()
                if (editChannel.hasFocus() && (t.isEmpty() || t == "#")) {
                    editChannel.post {
                        // forza il refresh dei suggerimenti con API pubbliche
                        editChannel.clearListSelection()
                        editChannel.showDropDown()
                    }
                }
            }
        })

        editChannel.setOnDismissListener {
            val now = System.currentTimeMillis()
            if (now < suppressDropdownReopenUntilMs) return@setOnDismissListener

            val t = editChannel.text?.toString().orEmpty().trim()
            if (editChannel.hasFocus() && (t.isEmpty() || t == "#")) {
                editChannel.postDelayed({
                    // ricontrollo (nel frattempo potresti aver tappato fuori)
                    val now2 = System.currentTimeMillis()
                    val t2 = editChannel.text?.toString().orEmpty().trim()
                    if (now2 >= suppressDropdownReopenUntilMs && editChannel.hasFocus() && (t2.isEmpty() || t2 == "#")) {
                        editChannel.clearListSelection()
                        editChannel.showDropDown()
                    }
                }, 80)
            }
        }



        editChannel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // quando svuoti il campo, riapre il dropdown automaticamente
                if (editChannel.hasFocus() && s?.isEmpty() == true) {
                    editChannel.post { editChannel.showDropDown() }
                }
            }
        })
        // se clicchi mentre è già in focus -> riapre
        editChannel.setOnClickListener {
            editChannel.showDropDown()
        }

        // anche su tap (alcune tastiere/skin non triggerano onClick sempre)
        editChannel.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()           // ✅ accessibility
                editChannel.showDropDown()
            }
            false
        }


        // se svuoti il campo -> mostra subito la lista (se è in focus)
        editChannel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (editChannel.hasFocus() && (s == null || s.isEmpty())) {
                    editChannel.post { editChannel.showDropDown() }
                }
            }
        })
        editChannel.setOnItemClickListener { parent, _, position, _ ->
            val ch = (parent.getItemAtPosition(position) as? String).orEmpty()
                .trim()
                .removePrefix("#")

            if (ch.isNotBlank()) {
                editChannel.dismissDropDown()
                joinChannelFromChat(ch)
            }
        }

        // 3) Leggi args + carica cfg
        accountId = requireArguments().getString(ARG_ACCOUNT_ID).orEmpty()
        if (accountId.isBlank()) return

        cfg = AccountRepository(requireContext()).getById(accountId)

        // 4) Seed: aggiungi subito il canale corrente tra i recenti (se esiste)
        cfg?.channel?.let { channelHistory.add(accountId, it) }
        refreshSavedChannelsUi()

        Log.d("CHAT", "ChatFragment opened with accountId=$accountId")

        textStatus.text = cfg?.let { "Loading @${it.username} on #${it.channel}..." } ?: "Account not found"

        // 5) Listener UI
        btnStartPcg.setOnClickListener {
            PcgActivity.start(requireContext(), accountId)
        }

        btnSend.setOnClickListener { sendOrGo() }

        editMessage.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_SEND || isEnter) {
                sendOrGo()
                true
            } else false
        }

        // 6) Back: quando sei nella chat, back -> login (pagina 0)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? MainActivity)?.goToLoginPage()
                }
            }
        )

        btnRefreshChat.setOnClickListener {
            val c = cfg ?: return@setOnClickListener
            appendSystemLine("Refreshing…")
            loadHistoryFromBot(c, seconds = 120) // o 60 se vuoi leggerissimo
        }
        scrollChat.isFocusableInTouchMode = true

        scrollChat.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                closeChannelDropdown()
                v.requestFocus()
            }
            false
        }

        chatContainer.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                closeChannelDropdown()
                v.requestFocus()
            }
            false
        }

        editMessage.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                closeChannelDropdown()
            }
            false
        }


    }

    private fun manualRefresh(seconds: Int = 120) {
        val now = System.currentTimeMillis()
        if (now - lastManualRefreshMs < 1500) return
        lastManualRefreshMs = now

        val c = cfg ?: return
        appendSystemLine("Refreshing…")
        loadHistoryFromBot(c, seconds)
    }


    override fun onStart() {
        super.onStart()

        // ricarica config sempre
        val newCfg = AccountRepository(requireContext()).getById(accountId) ?: return

        val oldChannel = cfg?.channel
        cfg = newCfg

        // se è cambiato canale o account, resetta lo stato e riconnetti pulito
        if (oldChannel != null && !oldChannel.equals(newCfg.channel, ignoreCase = true)) {
            readClient?.disconnect()
            sendClient?.disconnect()
            readClient = null
            sendClient = null
            sendReady = false
            historyLoaded = false
        }
        refreshSavedChannelsUi()

        connectIfNeeded()
    }


    override fun onPause() {
        super.onPause()
        lastPausedAtMs = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()

        // Se torni da PCG (o sei stato fuori), recupera i messaggi mancanti via history
        val c = cfg ?: return
        //if (!c.channel.equals(CACHED_CHANNEL, ignoreCase = true)) return

        val pausedAt = lastPausedAtMs
        if (pausedAt == 0L) return

        val awaySec = ((System.currentTimeMillis() - pausedAt) / 1000).toInt()
        lastPausedAtMs = 0L

        if (awaySec >= 3) {
            val refreshSec = (awaySec + 10).coerceIn(30, HISTORY_SECONDS)
            loadHistoryFromBot(c, seconds = refreshSec)
        }
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

        if (!historyLoaded) {
            historyLoaded = true
            loadHistoryFromBot(c, seconds = HISTORY_SECONDS)
        }

        val ch = c.channel.trim().removePrefix("#").lowercase()

        textStatus.text = "Connecting as @${c.username} on #$ch..."

        val rc = TwitchChatClient(c.username, c.accessToken, ch)
        val sc = TwitchChatClient(c.username, c.accessToken, ch)

        Log.d("CHAT", "accountId=$accountId cfgChannel=${c.channel}")
        readClient = rc
        sendClient = sc
        sendReady = false

        rc.connect(
            onConnected = {
                activity?.runOnUiThread {
                    textStatus.text = "Connected as @${c.username} on #$ch"
                }
            },
            onMessage = { user, msg, emotesRaw, _, msgId ->
                val key = msgId?.takeIf { it.isNotBlank() }?.let { "id:$it" }
                    ?: "live:${System.nanoTime()}:${user.lowercase()}:${msg.hashCode()}"

                activity?.runOnUiThread {
                    appendChatLine(user, msg, emotesRaw, key)
                }
            },
            onError = { err ->
                activity?.runOnUiThread { textStatus.text = "Read error: ${err.message}" }
            }
        )


        sc.connect(
            onConnected = {
                sendReady = true
                activity?.runOnUiThread {
                    textStatus.text = "Connected as @${c.username} on #$ch"
                }
            },
            onMessage = null,
            onError = { err ->
                sendReady = false
                activity?.runOnUiThread { textStatus.text = "Send error: ${err.message}" }
            }
        )
    }

    private fun sendCurrentMessage() {
        val client = sendClient ?: return
        if (!sendReady) {
            appendSystemLine("Connection not ready…")
            return
        }

        val text = editMessage.text?.toString().orEmpty().trim()
        if (text.isBlank()) return

        client.sendMessage(text)
        editMessage.text?.clear()

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editMessage.windowToken, 0)
    }

    private fun loadHistoryFromBot(config: AccountConfig, seconds: Int) {
        if (HISTORY_BASE_URL.isBlank()) return

        val channelNorm = config.channel.trim().removePrefix("#").lowercase()
        val secondsSafe = seconds.coerceIn(30, HISTORY_SECONDS)

        thread {
            try {
                val chan = URLEncoder.encode(channelNorm, "UTF-8")
                val urlString =
                    "$HISTORY_BASE_URL/history" +
                            "?channel=$chan" +
                            "&seconds=$secondsSafe" +
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
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val user = obj.optString("user", "unknown")
                    val text = obj.optString("text", "")
                    if (text.isBlank()) continue

                    val emotesRaw = obj.optString("emotes", "")
                        .takeIf { it.isNotBlank() && it != "null" }

                    // ✅ dedup perfetto: usa l'id Twitch se presente
                    val msgId = obj.optString("id", "").takeIf { it.isNotBlank() && it != "null" }
                    val key = msgId?.let { "id:$it" } ?: run {
                        val ts = obj.optDouble("timestamp", 0.0)
                        "hist:${(ts * 1000).toLong()}:${user.lowercase()}:${text.hashCode()}"
                    }

                    activity?.runOnUiThread { appendChatLine(user, text, emotesRaw, key) }
                }
            } catch (_: Exception) {
                // ignore
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

    private fun rememberKey(key: String): Boolean {
        // return true se già visto (quindi skip)
        if (seenKeys.containsKey(key)) return true
        seenKeys[key] = Unit
        if (seenKeys.size > seenMax) {
            val it = seenKeys.entries.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
            }
        }
        return false
    }

    private fun appendChatLine(user: String, message: String, emotesRaw: String?, dedupKey: String) {
        if (rememberKey(dedupKey)) return
        val tv = createMessageTextView(user, message, emotesRaw)
        chatContainer.addView(tv)
        scrollToBottom()
    }

    private fun scrollToBottom() {
        // Se l'utente sta usando il dropdown canali, non autoscrollare (evita che si chiuda)
        if (this::editChannel.isInitialized && (editChannel.isPopupShowing || editChannel.hasFocus())) return

        scrollChat.post {
            // Evita fullScroll(FOCUS_DOWN) perché può giocare col focus
            scrollChat.scrollTo(0, chatContainer.bottom)
        }
    }


    // ---------------- EMOTES (marker-based, robust) ----------------

    private data class EmoteSpanInfo(val id: String, val start: Int, val endInclusive: Int)

    private fun parseEmoteSpans(emotesRaw: String?): List<EmoteSpanInfo> {
        if (emotesRaw.isNullOrBlank()) return emptyList()
        val out = mutableListOf<EmoteSpanInfo>()

        // es: 166263:0-8,33-41/123:...
        for (spec in emotesRaw.split('/')) {
            val parts = spec.split(':')
            if (parts.size < 2) continue
            val id = parts[0]
            for (p in parts[1].split(',')) {
                val se = p.split('-')
                if (se.size != 2) continue
                val s = se[0].toIntOrNull() ?: continue
                val e = se[1].toIntOrNull() ?: continue
                out.add(EmoteSpanInfo(id, s, e))
            }
        }

        return out.sortedBy { it.start }
    }

    /**
     * Twitch IRC: i messaggi /me arrivano come "\u0001ACTION ...\u0001"
     * Gli indici emote sono calcolati sul testo interno (senza wrapper).
     */
    private fun normalizeMessageForEmotes(raw: String, hasEmotes: Boolean): String {
        if (raw.startsWith("\u0001ACTION ") && raw.endsWith("\u0001") && raw.length > 8) {
            return raw.substring(8, raw.length - 1) // dentro ACTION
        }
        return if (!hasEmotes) raw.replace("\u0001", "") else raw
    }

    private fun createMessageTextView(user: String, rawMessage: String, emotesRaw: String?): TextView {
        val tv = TextView(requireContext()).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
        }

        val spans = parseEmoteSpans(emotesRaw)
        val message = normalizeMessageForEmotes(rawMessage, spans.isNotEmpty())

        val lowerUser = user.lowercase()
        val lowerSelf = cfg?.username?.lowercase()
        val nameColor = if (lowerUser == lowerSelf) 0xFF00FFAA.toInt() else colorForUsername(user)

        val prefix = "[$user] "

        // Nessuna emote => testo semplice
        if (spans.isEmpty()) {
            val plain = SpannableStringBuilder(prefix + message)
            val open = plain.indexOf("[")
            val close = plain.indexOf("]")
            if (open != -1 && close > open) {
                plain.setSpan(
                    ForegroundColorSpan(nameColor),
                    open + 1,
                    close,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            tv.text = plain
            return tv
        }

        // 1) Costruisci testo e rimpiazza range emote con marker invisibile (1 char)
        val builder = SpannableStringBuilder(prefix + message)

        // IMPORTANTISSIMO: replace da destra a sinistra
        for (s in spans.sortedByDescending { it.start }) {
            val startInMsg = s.start
            val endExclusiveInMsg = s.endInclusive + 1

            if (startInMsg < 0 || endExclusiveInMsg <= startInMsg || endExclusiveInMsg > message.length) continue

            val start = prefix.length + startInMsg
            val end = prefix.length + endExclusiveInMsg

            if (start < prefix.length || end > builder.length || start >= end) continue

            builder.replace(start, end, EMOTE_MARKER.toString())
        }

        // 2) Colora il nome utente
        val open = builder.indexOf("[")
        val close = builder.indexOf("]")
        if (open != -1 && close > open) {
            builder.setSpan(
                ForegroundColorSpan(nameColor),
                open + 1,
                close,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        tv.text = builder

        // 3) Trova i marker reali nel testo finale (posizioni corrette) e mappa 1:1 con spans ordinati
        val markerPositions = ArrayList<Int>()
        for (i in 0 until builder.length) {
            if (builder[i] == EMOTE_MARKER) markerPositions.add(i)
        }

        val spansInOrder = spans.sortedBy { it.start }
        val count = minOf(markerPositions.size, spansInOrder.size)

        // 4) Applica ImageSpan su ciascun marker
        for (i in 0 until count) {
            val emoteId = spansInOrder[i].id
            val idx = markerPositions[i]
            val url = "https://static-cdn.jtvnw.net/emoticons/v2/$emoteId/static/light/1.0"

            Glide.with(this)
                .asDrawable()
                .load(url)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        val size = (tv.textSize * 1.5f).toInt()
                        resource.setBounds(0, 0, size, size)

                        val imageSpan = ImageSpan(resource, ImageSpan.ALIGN_BOTTOM)
                        if (idx >= 0 && idx + 1 <= builder.length) {
                            builder.setSpan(imageSpan, idx, idx + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            tv.text = builder
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }

        return tv
    }

    companion object {
        private const val EMOTE_MARKER: Char = '\u2063' // Invisible Separator (non mostra "OBJ")
        private const val ARG_ACCOUNT_ID = "accountId"

        fun newInstance(accountId: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply { putString(ARG_ACCOUNT_ID, accountId) }
            }
        }
    }
}
