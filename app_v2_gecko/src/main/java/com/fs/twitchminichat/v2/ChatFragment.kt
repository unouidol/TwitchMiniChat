package com.fs.twitchminichat.v2

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fs.twitchminichat.v2.pcg.GeckoSessionManager
import com.fs.twitchminichat.v2.pcg.PcgActivity
import org.json.JSONArray
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread
import android.widget.MultiAutoCompleteTextView
import android.os.SystemClock
import android.widget.Toast

private const val HISTORY_BASE_URL = "https://api.ircminichat.party"
private const val HISTORY_SECONDS = 3600


class ChatFragment : Fragment(R.layout.fragment_chat) {

    private var cfg: AccountConfig? = null
    private var accountId: String = ""

    private var readClient: TwitchChatClient? = null
    private var sendClient: TwitchChatClient? = null
    @Volatile private var sendReady = false

    private lateinit var textStatus: TextView
    private lateinit var scrollChat: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var editMessage: MultiAutoCompleteTextView
    private lateinit var btnSend: Button
    private lateinit var btnStartPcg: Button
    private lateinit var btnJumpToBottom: Button

    private lateinit var channelHistory: ChannelHistoryStore
    private lateinit var btnRefreshChat: ImageButton

    private lateinit var geckoStreamView: GeckoView
    private lateinit var btnToggleStream: Button
    private lateinit var btnMuteStream: Button

    private lateinit var replyBar: LinearLayout
    private lateinit var txtReplyInfo: TextView
    private lateinit var btnCancelReply: Button
    private lateinit var mentionAdapter: ArrayAdapter<String>
    private lateinit var btnTogglePush: ImageButton

    private data class MentionUserEntry(
        var displayName: String,
        var lastSeenAtMs: Long
    )

    private val mentionUsers = LinkedHashMap<String, MentionUserEntry>()

    private val mentionTimeoutMs = 10 * 60 * 1000L // 10 minuti


    private var pendingReplyMessageId: String? = null
    private var pendingReplyUser: String? = null
    private var pendingReplyBody: String? = null

    private var streamSession: GeckoSession? = null
    private var streamEnabled = false
    private var streamMuted = true

    private var lastManualRefreshMs = 0L

    // Smart chat state
    private var stickToBottom = true
    private var unseenMessages = 0

    private val bottomThresholdPx: Int
        get() = (72 * resources.displayMetrics.density).toInt()

    // Dedup "immediato"
    private var lastDedupKey: String? = null
    private var lastDedupAtMs: Long = 0L
    private val dedupWindowMs = 1500L

    // Dedup "LRU"
    private val seenKeys = LinkedHashMap<String, Unit>(1024, 0.75f, true)
    private val seenMax = 800

    // Evita reload history completo a ogni tab switch
    private var historyLoaded = false

    // Per capire quanto sei stato via
    private var lastPausedAtMs: Long = 0L

    // Colori bot fissi
    private val botcolors = mapOf(
        "elbierro" to 0xFFFFD700.toInt(),
        "pokemoncommunitygame" to 0xFFFF5555.toInt()
    )

    private val userColorCache = HashMap<String, Int>()

    private lateinit var editChannel: AutoCompleteTextView
    private lateinit var channelsAdapter: ArrayAdapter<String>

    private var suppressDropdownReopenUntilMs: Long = 0L

    private fun dismissChannelDropdown(clearFocus: Boolean = false) {
        if (!this::editChannel.isInitialized) return
        suppressDropdownReopenUntilMs = System.currentTimeMillis() + 250
        editChannel.dismissDropDown()
        if (clearFocus) {
            editChannel.clearFocus()
        }
    }

    private fun colorForUsername(user: String): Int {
        val key = user.lowercase()
        botcolors[key]?.let { return it }

        return userColorCache.getOrPut(key) {
            val h = (key.hashCode() and 0x7fffffff) % 360
            Color.HSVToColor(floatArrayOf(h.toFloat(), 0.75f, 0.95f))
        }
    }

    private fun refreshChannelsDropdown(keepOpen: Boolean = true) {
        if (!this::channelsAdapter.isInitialized) return
        if (!this::channelHistory.isInitialized) return
        if (accountId.isBlank()) return

        editChannel.post {
            val list = channelHistory.get(accountId)

            channelsAdapter.clear()
            channelsAdapter.addAll(list)
            channelsAdapter.notifyDataSetChanged()

            channelsAdapter.filter.filter(editChannel.text)

            if (keepOpen && editChannel.hasFocus()) {
                editChannel.dismissDropDown()
                editChannel.showDropDown()
            }
        }
    }
    private fun addMentionUser(user: String) {
        val trimmed = user.trim()
        if (trimmed.isBlank()) return

        val key = trimmed.lowercase()
        val now = SystemClock.elapsedRealtime()

        val existing = mentionUsers[key]
        if (existing == null) {
            mentionUsers[key] = MentionUserEntry(
                displayName = trimmed,
                lastSeenAtMs = now
            )
        } else {
            existing.displayName = trimmed
            existing.lastSeenAtMs = now
        }

        pruneMentionUsers()
        refreshMentionSuggestions()
    }
    private fun pruneMentionUsers() {
        val now = SystemClock.elapsedRealtime()
        val selfKey = cfg?.username?.trim()?.lowercase()

        val it = mentionUsers.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()

            val isSelf = entry.key == selfKey
            val expired = (now - entry.value.lastSeenAtMs) > mentionTimeoutMs

            if (!isSelf && expired) {
                it.remove()
            }
        }
    }

    private fun resetMentionUsersForCurrentChannel() {
        mentionUsers.clear()
        cfg?.username?.let { addMentionUser(it) }
        refreshMentionSuggestions()
    }

    private fun refreshMentionSuggestions() {
        if (!this::mentionAdapter.isInitialized) return

        pruneMentionUsers()

        val items = mentionUsers.values
            .map { it.displayName }
            .sortedBy { it.lowercase() }
            .map { "@$it" }

        mentionAdapter.clear()
        mentionAdapter.addAll(items)
        mentionAdapter.notifyDataSetChanged()
    }

    private fun currentMentionQuery(): String? {
        if (!this::editMessage.isInitialized) return null

        val text = editMessage.text?.toString().orEmpty()
        val cursor = editMessage.selectionStart
        if (cursor < 0 || cursor > text.length) return null

        var start = cursor
        while (start > 0 && !text[start - 1].isWhitespace()) {
            start--
        }

        if (start >= text.length) return null
        if (text[start] != '@') return null

        return text.substring(start, cursor)
    }

    private class MentionTokenizer : MultiAutoCompleteTextView.Tokenizer {
        override fun findTokenStart(text: CharSequence, cursor: Int): Int {
            var i = cursor
            while (i > 0 && !text[i - 1].isWhitespace()) {
                i--
            }

            return if (i < text.length && text[i] == '@') {
                i
            } else {
                cursor
            }
        }

        override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
            var i = cursor
            while (i < text.length) {
                if (text[i].isWhitespace()) {
                    return i
                }
                i++
            }
            return text.length
        }

        override fun terminateToken(text: CharSequence): CharSequence {
            return "$text "
        }
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

                v.setOnClickListener {
                    editChannel.setText(ch, false)
                    editChannel.setSelection(editChannel.text.length)
                    editChannel.dismissDropDown()
                    joinChannelFromChat(ch)
                }

                btn.setOnClickListener {
                    channelHistory.remove(accountId, ch)
                    refreshChannelsDropdown(keepOpen = true)
                }

                return v
            }
        }
    }

    private fun joinChannelFromChat(channelRaw: String) {
        val ch = channelRaw.trim().removePrefix("#").lowercase()

        val ok = Regex("^[a-z0-9_]{1,25}$").matches(ch)
        if (!ok) {
            appendSystemLine(getString(R.string.invalid_channel_name))
            return
        }

        val c = cfg ?: return
        val current = c.channel.trim().removePrefix("#").lowercase()
        if (ch == current) return

        appendSystemLine(getString(R.string.channel_switch, ch))

        AccountRepository(requireContext()).updateChannel(accountId, ch)
        cfg = AccountRepository(requireContext()).getById(accountId)
        reloadStreamForCurrentChannel()

        Log.d("CHAN", "JOIN-> add recent accountId=$accountId ch=$ch")
        channelHistory.add(accountId, ch)
        Log.d("CHAN", "AFTER add recent list=" + channelHistory.get(accountId).joinToString())
        refreshChannelsDropdown()

        historyLoaded = false
        lastDedupKey = null
        lastDedupAtMs = 0L
        seenKeys.clear()

        resetMentionUsersForCurrentChannel()

        unseenMessages = 0
        stickToBottom = true
        chatContainer.removeAllViews()
        updateJumpToBottomButton()

        readClient?.disconnect()
        sendClient?.disconnect()
        readClient = null
        sendClient = null
        sendReady = false

        connectIfNeeded()
    }

    private fun sendOrGo() {
        val c = cfg ?: return

        val ch = editChannel.text?.toString().orEmpty().trim().removePrefix("#").lowercase()
        val current = c.channel.trim().removePrefix("#").lowercase()

        if (ch.isNotBlank() && ch != current && Regex("^[a-z0-9_]{1,25}$").matches(ch)) {
            joinChannelFromChat(ch)
            editChannel.text?.clear()
            return
        }

        sendCurrentMessage()
    }

    private fun manualRefresh() {
        val now = System.currentTimeMillis()
        if (now - lastManualRefreshMs < 1500) return
        lastManualRefreshMs = now

        val c = cfg ?: return
        appendSystemLine(getString(R.string.refreshing))
        loadHistoryFromBot(c, 120)
    }
    private fun currentProfileId(): String {
        val username = cfg?.username?.trim().orEmpty()
        if (username.isBlank()) return ""
        return ProfileIdUtil.fromUsername(username)
    }

    private fun knownProfileIdsForDevice(): List<String> {
        return AccountRepository(requireContext())
            .loadAccounts()
            .map { ProfileIdUtil.fromUsername(it.username) }
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    private fun refreshPushToggleUi() {
        if (!this::btnTogglePush.isInitialized) return
        if (!isAdded) return

        val profileId = currentProfileId()
        val enabled = profileId.isNotBlank() &&
                PushSettingsStore.isPushEnabled(requireContext(), profileId)

        btnTogglePush.setImageResource(
            if (enabled) {
                R.drawable.ic_bell_on
            } else {
                R.drawable.ic_bell_off
            }
        )

        btnTogglePush.setColorFilter(
            if (enabled) 0xFF66FF99.toInt() else 0xFFFF6666.toInt()
        )

        btnTogglePush.alpha = 1f

        btnTogglePush.contentDescription = if (enabled) {
            "Alerts ON for this account"
        } else {
            "Alerts OFF for this account"
        }
    }

    private fun togglePushForCurrentProfile() {
        val c = cfg ?: return
        val ctx = requireContext()
        val profileId = currentProfileId()
        if (profileId.isBlank()) return

        val previousEnabled = PushSettingsStore.isPushEnabled(ctx, profileId)
        val targetEnabled = !previousEnabled
        val allProfileIds = knownProfileIdsForDevice()

        Log.d("PUSH_TOGGLE", "tap profileId=$profileId targetEnabled=$targetEnabled")

        // update ottimistico immediato
        PushSettingsStore.setPushEnabled(ctx, profileId, targetEnabled)
        refreshPushToggleUi()
        btnTogglePush.isEnabled = false

        FcmRegistrationUploader.setProfilePushEnabled(
            context = ctx,
            profileId = profileId,
            enabled = targetEnabled
        ) { ok ->
            if (!isAdded) return@setProfilePushEnabled

            Log.d("PUSH_TOGGLE", "toggle response profileId=$profileId ok=$ok")

            // Readback sempre, anche se ok=false
            FcmRegistrationUploader.fetchDevicePushStateWithRetry(
                context = ctx,
                knownProfileIds = allProfileIds
            ) { state ->
                if (!isAdded) return@fetchDevicePushStateWithRetry

                btnTogglePush.isEnabled = true

                if (state == null) {
                    // NON fare rollback cieco:
                    // il server potrebbe aver già applicato il cambio davvero
                    refreshPushToggleUi()

                    Toast.makeText(
                        requireContext(),
                        "Cambio inviato, ma verifica stato non riuscita",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@fetchDevicePushStateWithRetry
                }

                PushSettingsStore.syncProfilesForDevice(
                    context = requireContext(),
                    allProfileIds = allProfileIds,
                    enabledProfileIds = state.enabledProfileIds
                )

                refreshPushToggleUi()

                val nowEnabled = PushSettingsStore.isPushEnabled(requireContext(), profileId)

                Log.d(
                    "PUSH_TOGGLE",
                    "readback profileId=$profileId nowEnabled=$nowEnabled enabledProfileIds=${state.enabledProfileIds}"
                )

                Toast.makeText(
                    requireContext(),
                    if (nowEnabled) {
                        "Notifiche attivate per ${c.username}"
                    } else {
                        "Notifiche disattivate per ${c.username}"
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountId = requireArguments().getString(ARG_ACCOUNT_ID).orEmpty()
        if (accountId.isBlank()) return

        channelHistory = ChannelHistoryStore(requireContext())
        cfg = AccountRepository(requireContext()).getById(accountId)

        textStatus = view.findViewById(R.id.textStatus)
        scrollChat = view.findViewById(R.id.scrollChat)
        chatContainer = view.findViewById(R.id.chatContainer)
        editMessage = view.findViewById(R.id.editMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnStartPcg = view.findViewById(R.id.btnStartPcg)
        btnRefreshChat = view.findViewById(R.id.btnRefreshChat)
        btnTogglePush = view.findViewById(R.id.btnTogglePush)
        editChannel = view.findViewById(R.id.editChannel)
        btnJumpToBottom = view.findViewById(R.id.btnJumpToBottom)
        geckoStreamView = view.findViewById(R.id.geckoStreamView)
        btnToggleStream = view.findViewById(R.id.btnToggleStream)
        btnMuteStream = view.findViewById(R.id.btnMuteStream)
        replyBar = view.findViewById(R.id.replyBar)
        txtReplyInfo = view.findViewById(R.id.txtReplyInfo)
        btnCancelReply = view.findViewById(R.id.btnCancelReply)

        refreshPushToggleUi()

        btnTogglePush.setOnClickListener {
            togglePushForCurrentProfile()
        }

        mentionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )

        editMessage.setAdapter(mentionAdapter)
        editMessage.setTokenizer(MentionTokenizer())
        editMessage.threshold = 1

        resetMentionUsersForCurrentChannel()

        editMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = currentMentionQuery() ?: return

                refreshMentionSuggestions()

                editMessage.post {
                    mentionAdapter.filter.filter(query)
                    if (editMessage.hasFocus()) {
                        editMessage.showDropDown()
                    }
                }
            }
        })


        channelsAdapter = buildChannelsAdapter()
        editChannel.setAdapter(channelsAdapter)
        editChannel.threshold = 0

        cfg?.channel?.let { channelHistory.add(accountId, it) }
        refreshChannelsDropdown(keepOpen = false)

        textStatus.text = cfg?.let {
            getString(R.string.status_loading, it.username, it.channel)
        } ?: getString(R.string.account_not_found)

        editChannel.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                //closeChannelDropdown()
            } else {
                val t = editChannel.text?.toString().orEmpty().trim()
                if (t.isEmpty() || t == "#") {
                    editChannel.post { editChannel.showDropDown() }
                }
            }
        }

        editChannel.setOnClickListener {
            val t = editChannel.text?.toString().orEmpty().trim()
            if (t.isEmpty() || t == "#") {
                editChannel.showDropDown()
            }
        }

        editChannel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val t = s?.toString().orEmpty().trim()
                if (editChannel.hasFocus() && (t.isEmpty() || t == "#")) {
                    editChannel.post {
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
                    val now2 = System.currentTimeMillis()
                    val t2 = editChannel.text?.toString().orEmpty().trim()
                    if (now2 >= suppressDropdownReopenUntilMs && editChannel.hasFocus() && (t2.isEmpty() || t2 == "#")) {
                        editChannel.clearListSelection()
                        editChannel.showDropDown()
                    }
                }, 80)
            }
        }

        view.isFocusableInTouchMode = true
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.requestFocus()
                    dismissChannelDropdown(clearFocus = true)
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                }
            }
            false
        }


        editMessage.setOnClickListener {
            dismissChannelDropdown(clearFocus = true)
        }


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

        btnRefreshChat.setOnClickListener {
            manualRefresh()
        }

        btnJumpToBottom.setOnClickListener {
            scrollToBottom()
        }

        btnToggleStream.setOnClickListener {
            toggleStream()
        }

        btnMuteStream.setOnClickListener {
            toggleStreamMute()
        }

        btnCancelReply.setOnClickListener {
            clearPendingReply()
        }

        updateStreamUi()

        scrollChat.setOnScrollChangeListener { _, _, _, _, _ ->
            val nearBottom = isNearBottom()
            stickToBottom = nearBottom

            if (nearBottom) {
                unseenMessages = 0
            }

            updateJumpToBottomButton()
        }

        updateJumpToBottomButton()

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (activity as? MainActivity)?.goToLoginPage()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()

        val newCfg = AccountRepository(requireContext()).getById(accountId) ?: return

        val oldChannel = cfg?.channel
        cfg = newCfg

        if (oldChannel != null && !oldChannel.equals(newCfg.channel, ignoreCase = true)) {
            readClient?.disconnect()
            sendClient?.disconnect()
            readClient = null
            sendClient = null
            sendReady = false
            historyLoaded = false
        }

        connectIfNeeded()
    }

    override fun onPause() {
        super.onPause()
        lastPausedAtMs = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        dismissChannelDropdown(clearFocus = false)

        if (streamEnabled) {
            ensureStreamSession()
            streamSession?.setActive(true)
            updateStreamUi()
        }

        val c = cfg ?: return

        val pausedAt = lastPausedAtMs
        if (pausedAt == 0L) return

        val awaySec = ((System.currentTimeMillis() - pausedAt) / 1000).toInt()
        lastPausedAtMs = 0L

        if (awaySec >= 1 || readClient == null) {
            val refreshSec = (awaySec + 10).coerceIn(30, HISTORY_SECONDS)
            loadHistoryFromBot(c, seconds = refreshSec)
        }
    }

    override fun onStop() {
        super.onStop()

        streamSession?.setActive(false)

        readClient?.disconnect()
        sendClient?.disconnect()
        readClient = null
        sendClient = null
        sendReady = false
        historyLoaded = false
        textStatus.text = cfg?.let {
            getString(R.string.status_disconnected_account, it.username)
        } ?: getString(R.string.status_disconnected)
    }

    private fun connectIfNeeded() {
        if (readClient != null || sendClient != null) return
        val c = cfg ?: return
        if (c.accessToken.isBlank()) {
            textStatus.text = getString(R.string.status_missing_token, c.username)
            return
        }

        if (!historyLoaded) {
            historyLoaded = true
            loadHistoryFromBot(c, seconds = HISTORY_SECONDS)
        }

        val ch = c.channel.trim().removePrefix("#").lowercase()

        textStatus.text = getString(R.string.status_connecting, c.username, ch)

        val rc = TwitchChatClient(c.username, c.accessToken, ch)
        val sc = TwitchChatClient(c.username, c.accessToken, ch)

        Log.d("CHAT", "accountId=$accountId cfgChannel=${c.channel}")
        readClient = rc
        sendClient = sc
        sendReady = false

        rc.connect(
            onConnected = {
                activity?.runOnUiThread {
                    textStatus.text = getString(R.string.status_connected, c.username, ch)
                }
            },
            onMessage = { user, msg, emotesRaw, _, msgId, replyParentUserLogin ->
                val key = msgId?.takeIf { it.isNotBlank() }?.let { "id:$it" }
                    ?: "live:${System.nanoTime()}:${user.lowercase()}:${msg.hashCode()}"

                val forceScroll = user.equals(c.username, ignoreCase = true)

                activity?.runOnUiThread {
                    appendChatLine(
                        user = user,
                        message = msg,
                        emotesRaw = emotesRaw,
                        dedupKey = key,
                        replyParentUserLogin = replyParentUserLogin,
                        forceScroll = forceScroll
                    )
                }
            },
            onError = { err ->
                activity?.runOnUiThread {
                    textStatus.text = getString(R.string.status_read_error, err.message ?: "unknown")
                }
            }
        )

        sc.connect(
            onConnected = {
                sendReady = true
                activity?.runOnUiThread {
                    textStatus.text = getString(R.string.status_connected, c.username, ch)
                }
            },
            onMessage = null,
            onError = { err ->
                sendReady = false
                activity?.runOnUiThread {
                    textStatus.text = getString(R.string.status_send_error, err.message ?: "unknown")
                }
            }
        )
    }

    private fun sendCurrentMessage() {
        val client = sendClient ?: return
        if (!sendReady) {
            appendSystemLine(getString(R.string.connection_not_ready))
            return
        }

        val text = editMessage.text?.toString().orEmpty().trim()
        if (text.isBlank()) return

        val replyParentId = pendingReplyMessageId
        if (!replyParentId.isNullOrBlank()) {
            client.sendReply(replyParentId, text)
            clearPendingReply()
        } else {
            client.sendMessage(text)
        }

        editMessage.text?.clear()

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editMessage.windowToken, 0)

        stickToBottom = true
        scrollToBottom()
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
                            "&key=${BuildConfig.HISTORY_SECRET_KEY}"

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

                    val msgId = obj.optString("id", "").takeIf { it.isNotBlank() && it != "null" }
                    val key = msgId?.let { "id:$it" } ?: run {
                        val ts = obj.optDouble("timestamp", 0.0)
                        "hist:${(ts * 1000).toLong()}:${user.lowercase()}:${text.hashCode()}"
                    }

                    activity?.runOnUiThread {
                        appendChatLine(
                            user = user,
                            message = text,
                            emotesRaw = emotesRaw,
                            dedupKey = key,
                            replyParentUserLogin = null
                        )
                    }
                }
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    private fun isNearBottom(): Boolean {
        if (!this::scrollChat.isInitialized || !this::chatContainer.isInitialized) return true

        val contentBottom = chatContainer.bottom
        val viewportBottom = scrollChat.scrollY + scrollChat.height
        val distanceFromBottom = contentBottom - viewportBottom

        return distanceFromBottom <= bottomThresholdPx
    }

    private fun updateJumpToBottomButton() {
        if (!this::btnJumpToBottom.isInitialized) return

        val show = unseenMessages > 0 && !isNearBottom()
        btnJumpToBottom.visibility = if (show) View.VISIBLE else View.GONE

        btnJumpToBottom.text = if (unseenMessages <= 0) {
            getString(R.string.new_messages)
        } else {
            resources.getQuantityString(
                R.plurals.new_messages_count,
                unseenMessages,
                unseenMessages
            )
        }
    }

    private fun appendChatView(view: View, forceScroll: Boolean = false, countAsUnread: Boolean = true) {
        val shouldAutoScroll = forceScroll || stickToBottom || isNearBottom()

        chatContainer.addView(view)

        if (shouldAutoScroll) {
            scrollToBottom()
        } else {
            if (countAsUnread) {
                unseenMessages++
            }
            updateJumpToBottomButton()
        }
    }

    private fun appendSystemLine(text: String) {
        val tv = TextView(requireContext())
        tv.text = getString(R.string.system_bullet, text)
        tv.setTextColor(0xFFAAAAAA.toInt())
        tv.textSize = 12f

        appendChatView(tv, forceScroll = false, countAsUnread = false)
    }

    private fun currentChannelNormalized(): String {
        return cfg?.channel
            ?.trim()
            ?.removePrefix("#")
            ?.lowercase()
            ?.ifBlank { "unouidol" }
            ?: "unouidol"
    }

    private fun buildStreamUrl(channel: String = currentChannelNormalized()): String {
        val ch = URLEncoder.encode(channel, "UTF-8")
        return "https://unouidol.github.io/ircminichat/player.html?channel=$ch&muted=$streamMuted"
    }

    private fun ensureStreamSession() {
        if (streamSession != null) {
            geckoStreamView.setSession(streamSession!!)
            return
        }

        //val sessionKey = "stream:$accountId"
        val s = GeckoSessionManager.getOrCreateStreamSession(requireContext(), accountId)
        streamSession = s
        geckoStreamView.setSession(s)
        s.setActive(false)
    }
    private fun setPendingReply(messageId: String, user: String, message: String) {
        pendingReplyMessageId = messageId
        pendingReplyUser = user
        pendingReplyBody = message

        val preview = if (message.length > 40) {
            message.take(40) + "…"
        } else {
            message
        }

        txtReplyInfo.text = getString(R.string.replying_to, user, preview)
        replyBar.visibility = View.VISIBLE
    }

    private fun clearPendingReply() {
        pendingReplyMessageId = null
        pendingReplyUser = null
        pendingReplyBody = null
        replyBar.visibility = View.GONE
    }

    private fun prefillReplyMention(user: String) {
        if (!this::editMessage.isInitialized) return

        val mention = "@$user "
        val current = editMessage.text?.toString().orEmpty()

        val alreadyStartsWithMention = current.startsWith(mention, ignoreCase = true)
        if (alreadyStartsWithMention) return

        if (current.isBlank()) {
            editMessage.setText(mention)
            editMessage.setSelection(editMessage.text?.length ?: 0)
        }
    }

    private fun updateStreamUi() {
        if (!this::btnToggleStream.isInitialized) return

        btnToggleStream.text = if (streamEnabled) {
            getString(R.string.stream_on)
        } else {
            getString(R.string.stream_off)
        }

        geckoStreamView.visibility = if (streamEnabled) View.VISIBLE else View.GONE
        btnMuteStream.visibility = if (streamEnabled) View.VISIBLE else View.GONE

        btnMuteStream.text = if (streamMuted) {
            getString(R.string.unmute)
        } else {
            getString(R.string.mute)
        }
    }

    private fun enableStream() {
        ensureStreamSession()
        streamEnabled = true
        geckoStreamView.visibility = View.VISIBLE
        streamSession?.setActive(true)
        streamSession?.loadUri(buildStreamUrl())
        updateStreamUi()
    }

    private fun disableStream() {
        streamEnabled = false
        geckoStreamView.visibility = View.GONE
        streamSession?.setActive(false)
        updateStreamUi()
    }

    private fun toggleStream() {
        if (streamEnabled) {
            disableStream()
        } else {
            enableStream()
        }
    }

    private fun toggleStreamMute() {
        streamMuted = !streamMuted
        updateStreamUi()

        if (streamEnabled) {
            streamSession?.loadUri(buildStreamUrl())
        }
    }

    private fun reloadStreamForCurrentChannel() {
        if (!streamEnabled) return
        streamSession?.loadUri(buildStreamUrl())
    }

    private fun rememberKey(key: String): Boolean {
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

    private fun appendChatLine(
        user: String,
        message: String,
        emotesRaw: String?,
        dedupKey: String,
        replyParentUserLogin: String? = null,
        forceScroll: Boolean = false
    )

    {
        val now = System.currentTimeMillis()

        if (dedupKey == lastDedupKey && (now - lastDedupAtMs) < dedupWindowMs) return
        lastDedupKey = dedupKey
        lastDedupAtMs = now

        val stable = dedupKey.startsWith("id:")
        if (stable && rememberKey(dedupKey)) return

        val msgId = dedupKey.removePrefix("id:").takeIf { stable && it.isNotBlank() }
        addMentionUser(user)
        val tv = createMessageTextView(
            user = user,
            rawMessage = message,
            emotesRaw = emotesRaw,
            replyParentUserLogin = replyParentUserLogin
        )

        if (!msgId.isNullOrBlank()) {
            tv.setOnLongClickListener {
                setPendingReply(msgId, user, message)
                true
            }
        }
        appendChatView(tv, forceScroll = forceScroll, countAsUnread = true)
    }

    private fun scrollToBottom() {
        scrollChat.post {
            scrollChat.scrollTo(0, chatContainer.bottom)
            stickToBottom = true
            unseenMessages = 0
            updateJumpToBottomButton()
        }
    }

    private data class EmoteSpanInfo(val id: String, val start: Int, val endInclusive: Int)

    private fun parseEmoteSpans(emotesRaw: String?): List<EmoteSpanInfo> {
        if (emotesRaw.isNullOrBlank()) return emptyList()
        val out = mutableListOf<EmoteSpanInfo>()

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

    private fun normalizeMessageForEmotes(raw: String, hasEmotes: Boolean): String {
        if (raw.startsWith("\u0001ACTION ") && raw.endsWith("\u0001") && raw.length > 8) {
            return raw.substring(8, raw.length - 1)
        }
        return if (!hasEmotes) raw.replace("\u0001", "") else raw
    }

    private fun createMessageTextView(
        user: String,
        rawMessage: String,
        emotesRaw: String?,
        replyParentUserLogin: String?
    ): TextView {
        val tv = TextView(requireContext()).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
        }

        val spans = parseEmoteSpans(emotesRaw)
        val message = normalizeMessageForEmotes(rawMessage, spans.isNotEmpty())

        val lowerUser = user.lowercase()
        val lowerSelf = cfg?.username?.lowercase()
        val nameColor = if (lowerUser == lowerSelf) 0xFF00FFAA.toInt() else colorForUsername(user)

        val replyHeader = replyParentUserLogin
            ?.takeIf { it.isNotBlank() }
            ?.let { "↪ replying to @$it\n" }
            .orEmpty()

        val prefix = "[$user] "
        val fullPrefix = replyHeader + prefix

        if (spans.isEmpty()) {
            val plain = SpannableStringBuilder(fullPrefix + message)

            if (replyHeader.isNotEmpty()) {
                plain.setSpan(
                    ForegroundColorSpan(0xFFAAAAAA.toInt()),
                    0,
                    replyHeader.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val open = plain.indexOf("[", replyHeader.length)
            val close = plain.indexOf("]", replyHeader.length)
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

        val builder = SpannableStringBuilder(fullPrefix + message)

        for (s in spans.sortedByDescending { it.start }) {
            val startInMsg = s.start
            val endExclusiveInMsg = s.endInclusive + 1

            if (startInMsg !in 0 until message.length || endExclusiveInMsg !in (startInMsg + 1)..message.length) continue

            val start = fullPrefix.length + startInMsg
            val end = fullPrefix.length + endExclusiveInMsg

            if (start < fullPrefix.length || end > builder.length || start >= end) continue

            builder.replace(start, end, EMOTE_MARKER.toString())
        }

        if (replyHeader.isNotEmpty()) {
            builder.setSpan(
                ForegroundColorSpan(0xFFAAAAAA.toInt()),
                0,
                replyHeader.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val open = builder.indexOf("[", replyHeader.length)
        val close = builder.indexOf("]", replyHeader.length)
        if (open != -1 && close > open) {
            builder.setSpan(
                ForegroundColorSpan(nameColor),
                open + 1,
                close,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        tv.text = builder

        val markerPositions = ArrayList<Int>()
        for (i in 0 until builder.length) {
            if (builder[i] == EMOTE_MARKER) markerPositions.add(i)
        }

        val spansInOrder = spans.sortedBy { it.start }
        val count = minOf(markerPositions.size, spansInOrder.size)

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
        private const val EMOTE_MARKER: Char = '\u2063'
        private const val ARG_ACCOUNT_ID = "accountId"

        fun newInstance(accountId: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply { putString(ARG_ACCOUNT_ID, accountId) }
            }
        }
    }
}