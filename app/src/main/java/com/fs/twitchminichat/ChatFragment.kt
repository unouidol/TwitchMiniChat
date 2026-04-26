package com.fs.twitchminichat

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.MultiAutoCompleteTextView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fs.twitchminichat.pcg.GeckoSessionManager
import com.fs.twitchminichat.pcg.PcgActivity
import org.json.JSONArray
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.text.InputType
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Typeface
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.graphics.drawable.GradientDrawable



private const val HISTORY_BASE_URL = "https://api.ircminichat.party"
private const val HISTORY_SECONDS = 3600



class ChatFragment : Fragment(R.layout.fragment_chat), CatchPresetSettingsBottomSheet.Host {


    private var cfg: AccountConfig? = null
    private var accountId: String = ""

    private var readClient: TwitchChatClient? = null
    private var sendClient: TwitchChatClient? = null

    @Volatile
    private var sendReady = false

    @Volatile
    private var connectInProgress = false

    private var suppressComposerRestore = false
    private var composerTextVersion = 0L

    private var pendingBuddyUsername: String? = null

    private var quickCatchDialog: AlertDialog? = null
    private var quickCatchAdapter: QuickCatchPresetMenuAdapter? = null
    private var quickCatchProfileId: String? = null

    private var quickCatchSpawnTitle: TextView? = null
    private var quickCatchSpawnSubtitle: TextView? = null
    private var channelDropdownManuallyClosed = false
    private var pendingOpenChannelDropdownAfterIme = false
    private var lastImeVisible = false
    private var lastImeBottomInsetPx = 0

    private lateinit var textStatus: TextView
    private lateinit var scrollChat: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var editMessage: MultiAutoCompleteTextView
    private lateinit var btnSend: Button
    private lateinit var btnStartPcg: Button
    private lateinit var btnJumpToBottom: Button

    private lateinit var channelHistory: ChannelHistoryStore
    private lateinit var btnRefreshChat: ImageButton
    private lateinit var btnSafetyPrivacy: ImageButton

    private lateinit var geckoStreamView: GeckoView
    private lateinit var btnToggleStream: Button
    private lateinit var btnMuteStream: Button

    private lateinit var replyBar: LinearLayout
    private lateinit var txtReplyInfo: TextView
    private lateinit var btnCancelReply: Button
    private lateinit var mentionAdapter: ArrayAdapter<String>
    private lateinit var btnTogglePush: ImageButton
    private lateinit var btnCatchPresets: ImageButton

    private data class MentionUserEntry(
        var displayName: String,
        var lastSeenAtMs: Long
    )

    private data class ChatViewMeta(
        val usernameLower: String,
        val messageId: String?,
        val messageText: String,
        val messageTimestampSec: Double
    )


    private fun requestBallPurchase(
        profileId: String,
        preset: CatchPreset,
        quantity: Int,
        onSuccess: (() -> Unit)? = null
    ): Boolean {
        val shopBallName = CatchPresetBallHelper.resolveShopBallNameForPreset(preset) ?: return false
        val boughtBallId = CatchPresetBallHelper.resolveBoughtBallIdForPreset(preset) ?: return false

        val handled = onCatchPresetBuyRequested(
            profileId = profileId,
            ballId = boughtBallId,
            shopBallName = shopBallName,
            quantity = quantity,
            label = preset.label
        )

        if (handled) {
            onSuccess?.invoke()
        }

        return handled
    }

    private val quickCatchRefreshRunnable = object : Runnable {
        override fun run() {
            refreshOpenQuickCatchMenuIfNeeded()

            val dialog = quickCatchDialog
            if (dialog != null && dialog.isShowing && view != null) {
                view?.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCatchPresetBuyRequested(
        profileId: String,
        ballId: String,
        shopBallName: String,
        quantity: Int,
        label: String
    ): Boolean {
        val requestedProfileId = profileId.trim().lowercase()
        val activeProfileId = currentProfileId().trim().lowercase()

        Log.d(
            "CHAT_BUY",
            "buy host fragment=${System.identityHashCode(this)} " +
                    "activeProfileId=$activeProfileId " +
                    "requestProfileId=$requestedProfileId " +
                    "ballId=$ballId " +
                    "shopBallName=$shopBallName " +
                    "quantity=$quantity " +
                    "sendReady=$sendReady " +
                    "sendClientNull=${sendClient == null} " +
                    "sendClientId=${sendClient?.let { System.identityHashCode(it) }}"
        )

        if (activeProfileId.isBlank()) {
            Log.w("CHAT_BUY", "reject: active profile blank")
            return false
        }

        if (activeProfileId != requestedProfileId) {
            Log.w(
                "CHAT_BUY",
                "reject: profile mismatch active=$activeProfileId requested=$requestedProfileId"
            )
            return false
        }

        if (quantity <= 0) {
            Log.w("CHAT_BUY", "reject: invalid quantity=$quantity")
            return false
        }

        val command = "!pokeshop $shopBallName $quantity"
        val sent = sendRawChatCommand(command)
        if (!sent) {
            Log.w("CHAT_BUY", "reject: sendRawChatCommand returned false")
            return false
        }

        InventoryBallStore.noteBallBought(
            context = requireContext(),
            profileId = requestedProfileId,
            ballId = ballId,
            quantity = quantity
        )

        Toast.makeText(
            requireContext(),
            getString(R.string.buy_ball_request_sent, quantity, label),
            Toast.LENGTH_SHORT
        ).show()

        Log.d("CHAT_BUY", "buy handled successfully")
        return true
    }

    private inner class SwipeReplyTextView(context: Context) : AppCompatTextView(context) {

        var onSwipeReply: (() -> Unit)? = null

        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()

        private var downRawX = 0f
        private var downRawY = 0f
        private var swiping = false
        private var replyGestureEnabled = false
        private var longPressTriggered = false

        private val longPressRunnable = Runnable {
            if (!swiping && isPressed) {
                longPressTriggered = performLongClick()
            }
        }

        init {
            isClickable = true
            isLongClickable = true
            isFocusable = false
            isFocusableInTouchMode = false
        }



        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    swiping = false
                    longPressTriggered = false

                    val startFromRightEdge =
                        event.x >= (width - swipeReplyActivationEdgePx)

                    replyGestureEnabled =
                        startFromRightEdge && onSwipeReply != null

                    isPressed = true
                    removeCallbacks(longPressRunnable)
                    postDelayed(longPressRunnable, longPressTimeoutMs)

                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY

                    val movedEnoughForCancel =
                        kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop

                    if (movedEnoughForCancel) {
                        removeCallbacks(longPressRunnable)
                    }

                    if (!replyGestureEnabled) {
                        return super.onTouchEvent(event)
                    }

                    if (!swiping) {
                        val horizontalSwipe =
                            dx < 0 &&
                                    kotlin.math.abs(dx) > touchSlop &&
                                    kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.5f

                        if (horizontalSwipe) {
                            swiping = true
                            isPressed = false
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }

                    if (swiping) {
                        val translation = dx
                            .coerceAtMost(0f)
                            .coerceAtLeast(-swipeReplyMaxPx)

                        translationX = translation
                        return true
                    }

                    return super.onTouchEvent(event)
                }

                MotionEvent.ACTION_UP -> {
                    removeCallbacks(longPressRunnable)

                    val wasSwiping = swiping
                    val shouldReply =
                        !longPressTriggered &&
                                swiping &&
                                translationX <= -swipeReplyTriggerPx &&
                                onSwipeReply != null

                    animate()
                        .translationX(0f)
                        .setDuration(150)
                        .start()

                    parent?.requestDisallowInterceptTouchEvent(false)

                    swiping = false
                    replyGestureEnabled = false
                    isPressed = false

                    if (longPressTriggered) {
                        return true
                    }

                    if (shouldReply) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onSwipeReply?.invoke()
                        return true
                    }

                    if (wasSwiping) {
                        return true
                    }

                    return performClick()
                }

                MotionEvent.ACTION_CANCEL -> {
                    removeCallbacks(longPressRunnable)

                    animate()
                        .translationX(0f)
                        .setDuration(150)
                        .start()

                    parent?.requestDisallowInterceptTouchEvent(false)

                    swiping = false
                    replyGestureEnabled = false
                    longPressTriggered = false
                    isPressed = false

                    return true
                }
            }

            return super.onTouchEvent(event)
        }

        override fun performClick(): Boolean {
            if (translationX != 0f) {
                animate()
                    .translationX(0f)
                    .setDuration(150)
                    .start()
            }
            return super.performClick()
        }
    }

    private val mentionUsers = LinkedHashMap<String, MentionUserEntry>()
    private val mentionTimeoutMs = 10 * 60 * 1000L

    private val swipeReplyTriggerPx: Float
        get() = 72f * resources.displayMetrics.density

    private val swipeReplyMaxPx: Float
        get() = 112f * resources.displayMetrics.density

    private val swipeReplyActivationEdgePx: Float
        get() = 64f * resources.displayMetrics.density

    private var pendingReplyMessageId: String? = null
    private var pendingReplyUser: String? = null
    private var pendingReplyBody: String? = null

    private var pendingBuddyProfileId: String? = null
    private var pendingBuddyRequestedAtMs: Long = 0L

    private var streamSession: GeckoSession? = null
    private var streamEnabled = false
    private var streamMuted = true

    private var lastManualRefreshMs = 0L

    private var stickToBottom = true
    private var unseenMessages = 0

    private val bottomThresholdPx: Int
        get() = (72 * resources.displayMetrics.density).toInt()

    private var lastDedupKey: String? = null
    private var lastDedupAtMs: Long = 0L
    private val dedupWindowMs = 1500L

    private val seenKeys = LinkedHashMap<String, Unit>(1024, 0.75f, true)
    private val seenMax = 800

    private var historyLoaded = false
    private var lastPausedAtMs: Long = 0L

    private var lastBallRecoLogSignature: String? = null

    private val botcolors = mapOf(
        "elbierro" to 0xFFFFD700.toInt(),
        "pokemoncommunitygame" to 0xFFFF5555.toInt()
    )

    private val userColorCache = HashMap<String, Int>()

    private lateinit var editChannel: ChannelAutoCompleteTextView
    private lateinit var channelsAdapter: ArrayAdapter<String>

    private fun handleFriendBallBuddyAction(preset: CatchPreset) {
        if (preset.ballId != "friend_ball") return
        requestBuddyInfo()
    }

    private fun resolveDexEntryForSpawnName(rawName: String): PokemonTypeEntry? {
        return PokemonTypeDex.findByPokemonName(requireContext(), rawName)
    }

    private fun requestBuddyInfo(): Boolean {
        val profileId = currentProfileId().trim().lowercase()
        val username = cfg?.username?.trim()?.lowercase().orEmpty()

        if (profileId.isBlank() || username.isBlank()) return false

        val sent = sendRawChatCommand("!pokebuddy")
        if (!sent) return false

        pendingBuddyProfileId = profileId
        pendingBuddyUsername = username
        pendingBuddyRequestedAtMs = System.currentTimeMillis()

        Toast.makeText(
            requireContext(),
            getString(R.string.buddy_request_sent),
            Toast.LENGTH_SHORT
        ).show()

        return true
    }

    private fun startQuickCatchAutoRefresh() {
        view?.removeCallbacks(quickCatchRefreshRunnable)
        view?.post(quickCatchRefreshRunnable)
    }

    private fun stopQuickCatchAutoRefresh() {
        view?.removeCallbacks(quickCatchRefreshRunnable)
    }

    private fun refreshChannelsDropdown() {
        if (!this::channelsAdapter.isInitialized) return
        if (!this::channelHistory.isInitialized) return
        if (accountId.isBlank()) return

        val list = channelHistory.get(accountId)

        channelsAdapter.clear()
        channelsAdapter.addAll(list)
        channelsAdapter.notifyDataSetChanged()
    }



    private fun updateChannelDropdownHeight() {
        if (!this::editChannel.isInitialized) return

        // Con ADJUST_NOTHING la tastiera copre la parte bassa dello schermo.
        // Quindi limitiamo la dropdown per evitare che finisca dietro la tastiera.
        val root = view ?: return

        val rootLocation = IntArray(2)
        val fieldLocation = IntArray(2)

        root.getLocationOnScreen(rootLocation)
        editChannel.getLocationOnScreen(fieldLocation)

        val rootBottom = rootLocation[1] + root.height
        val safeBottom = rootBottom - lastImeBottomInsetPx

        val fieldBottom = fieldLocation[1] + editChannel.height
        val availableBelow = safeBottom - fieldBottom - dp(8)

        editChannel.dropDownHeight = availableBelow
            .coerceAtLeast(dp(96))
            .coerceAtMost(dp(260))
    }

    private fun showChannelDropdownNow() {
        if (!this::editChannel.isInitialized) return
        if (!isAdded) return
        if (!editChannel.hasFocus()) return
        if (channelDropdownManuallyClosed) return

        refreshChannelsDropdown()
        updateChannelDropdownHeight()

        editChannel.showDropDown()

        Log.d(
            "CHAN_DROPDOWN",
            "show requested hasFocus=${editChannel.hasFocus()} " +
                    "popup=${editChannel.isPopupShowing} " +
                    "adapterCount=${channelsAdapter.count} " +
                    "text='${editChannel.text}'"
        )
    }

    private fun scheduleChannelDropdownOpen(delayMs: Long) {
        if (!this::editChannel.isInitialized) return

        editChannel.postDelayed({
            if (!isAdded) return@postDelayed
            if (!editChannel.hasFocus()) return@postDelayed
            if (channelDropdownManuallyClosed) return@postDelayed

            refreshChannelsDropdown()
            updateChannelDropdownHeight()

            if (!editChannel.isPopupShowing) {
                editChannel.showDropDown()
            }
        }, delayMs)
    }

    private fun openChannelFieldInput() {
        if (!this::editChannel.isInitialized) return

        channelDropdownManuallyClosed = false
        pendingOpenChannelDropdownAfterIme = true

        refreshChannelsDropdown()

        editChannel.requestFocus()
        ViewCompat.requestApplyInsets(requireView())

        editChannel.post {
            if (!isAdded) return@post
            if (!editChannel.hasFocus()) return@post

            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            imm.showSoftInput(editChannel, InputMethodManager.SHOW_IMPLICIT)

            // Se la tastiera era già visibile, possiamo aprire subito.
            // Se invece sta ancora comparendo, aspettiamo gli insets o i delay sotto.
            if (lastImeVisible && !channelDropdownManuallyClosed) {
                pendingOpenChannelDropdownAfterIme = false
                showChannelDropdownNow()
            }
        }

        // Backup contro il caso in cui l'evento insets arrivi tardi o non arrivi.
        scheduleChannelDropdownOpen(220L)
        scheduleChannelDropdownOpen(420L)
    }

    private fun clearChannelFieldUi(hideKeyboard: Boolean = true) {
        if (!this::editChannel.isInitialized) return

        dismissChannelDropdown()
        editChannel.clearFocus()

        if (hideKeyboard) {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editChannel.windowToken, 0)
        }
    }

    private fun dismissChannelDropdown() {
        if (!this::editChannel.isInitialized) return
        editChannel.dismissDropDown()
    }


    private fun colorForUsername(user: String): Int {
        val key = user.lowercase()
        botcolors[key]?.let { return it }

        return userColorCache.getOrPut(key) {
            val h = (key.hashCode() and 0x7fffffff) % 360
            Color.HSVToColor(floatArrayOf(h.toFloat(), 0.75f, 0.95f))
        }
    }

    private fun normalizeChatUser(user: String?): String {
        return user?.trim()?.lowercase().orEmpty()
    }

    private fun isUserHidden(user: String): Boolean {
        return HiddenUsersStore.isHidden(requireContext(), user)
    }

    private fun removeMessagesOfHiddenUser(user: String) {
        val normalized = normalizeChatUser(user)
        if (normalized.isBlank()) return

        for (i in chatContainer.childCount - 1 downTo 0) {
            val child = chatContainer.getChildAt(i)
            val meta = child.tag as? ChatViewMeta ?: continue

            if (meta.usernameLower == normalized) {
                chatContainer.removeViewAt(i)
            }
        }

        val pendingReplyUserNormalized = normalizeChatUser(pendingReplyUser)
        if (pendingReplyUserNormalized == normalized) {
            clearPendingReply()
        }

        updateJumpToBottomButton()
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

    private fun resolveThemeColor(attr: Int, fallback: Int): Int {
        val typedValue = TypedValue()
        val ok = requireContext().theme.resolveAttribute(attr, typedValue, true)
        if (!ok) return fallback

        return if (typedValue.resourceId != 0) {
            ContextCompat.getColor(requireContext(), typedValue.resourceId)
        } else {
            typedValue.data
        }
    }

    private fun colorOnSurface(): Int {
        return resolveThemeColor(android.R.attr.textColorPrimary, Color.WHITE)
    }

    private fun colorOnSurfaceVariant(): Int {
        return resolveThemeColor(android.R.attr.textColorSecondary, 0xFFAAAAAA.toInt())
    }

    private fun colorPrimarySafe(): Int {
        return resolveThemeColor(android.R.attr.textColorLink, 0xFF00FFAA.toInt())
    }

    private fun colorMentionHighlight(): Int {
        val base = resolveThemeColor(
            android.R.attr.textColorHighlight,
            0x66FFD54F
        )

        return Color.argb(
            110,
            Color.red(base),
            Color.green(base),
            Color.blue(base)
        )
    }

    private fun applyMentionHighlightSpans(text: SpannableStringBuilder): Boolean {
        val username = cfg?.username?.trim().orEmpty()
        if (username.isBlank()) return false

        val regex = Regex(
            pattern = "(?i)(?<![A-Za-z0-9_])@${Regex.escape(username)}(?![A-Za-z0-9_])"
        )

        val highlightColor = colorMentionHighlight()
        var found = false

        regex.findAll(text.toString()).forEach { match ->
            found = true

            text.setSpan(
                BackgroundColorSpan(highlightColor),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            text.setSpan(
                StyleSpan(Typeface.BOLD),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return found
    }

    private fun colorMentionRowHighlight(): Int {
        val base = resolveThemeColor(
            android.R.attr.textColorHighlight,
            0x66FFD54F
        )

        return Color.argb(
            54,
            Color.red(base),
            Color.green(base),
            Color.blue(base)
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun applyMentionRowHighlight(tv: TextView, hasMention: Boolean) {
        if (!hasMention) return

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10).toFloat()
            setColor(colorMentionRowHighlight())
        }

        tv.background = bg
        tv.setPaddingRelative(
            dp(8),
            dp(6),
            dp(8),
            dp(6)
        )
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
                val row = convertView ?: layoutInflater.inflate(
                    R.layout.row_channel_dropdown,
                    parent,
                    false
                )

                val ch = getItem(position).orEmpty()

                val txt = row.findViewById<TextView>(R.id.txtChannel)
                val btn = row.findViewById<ImageButton>(R.id.btnRemove)

                txt.text = ch

                row.setOnClickListener {
                    dismissChannelDropdown()

                    val joined = joinChannelFromChat(ch)
                    if (joined) {
                        editChannel.setText("", false)
                        clearChannelFieldUi(hideKeyboard = false)
                    }
                }

                btn.setOnClickListener {
                    channelHistory.remove(accountId, ch)

                    dismissChannelDropdown()
                    refreshChannelsDropdown()

                    editChannel.post {
                        if (!isAdded) return@post

                        if (!editChannel.hasFocus()) {
                            editChannel.requestFocus()
                        }

                        val currentText = editChannel.text?.toString().orEmpty().trim()

                        if (currentText.isEmpty() || currentText == "#") {
                            openChannelFieldInput()
                        }
                    }
                }

                return row
            }
        }
    }

    private fun joinChannelFromChat(channelRaw: String): Boolean {
        val ch = channelRaw.trim().removePrefix("#").lowercase()

        val ok = Regex("^[a-z0-9_]{1,25}$").matches(ch)
        if (!ok) {
            appendSystemLine(getString(R.string.invalid_channel_name))
            return false
        }

        val c = cfg ?: return false
        val current = c.channel.trim().removePrefix("#").lowercase()
        if (ch == current) {
            return false
        }

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
        connectInProgress = false

        connectIfNeeded()

        return true
    }

    private fun sendOrGo() {
        val c = cfg ?: return

        val ch = editChannel.text?.toString().orEmpty().trim().removePrefix("#").lowercase()
        val current = c.channel.trim().removePrefix("#").lowercase()

        if (ch.isNotBlank() && ch != current) {
            val joined = joinChannelFromChat(ch)
            if (joined) {
                editChannel.setText("", false)
                dismissChannelDropdown()
                clearChannelFieldUi(hideKeyboard = false)
            }
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
        val explicit = cfg?.profileId?.trim().orEmpty()
        if (explicit.isNotBlank()) return explicit.lowercase()

        val username = cfg?.username?.trim().orEmpty()
        if (username.isBlank()) return ""

        return ProfileIdUtil.fromUsername(username).trim().lowercase()
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

            FcmRegistrationUploader.fetchDevicePushStateWithRetry(
                context = ctx,
                knownProfileIds = allProfileIds
            ) { state ->
                if (!isAdded) return@fetchDevicePushStateWithRetry

                btnTogglePush.isEnabled = true

                if (state == null) {
                    refreshPushToggleUi()

                    Toast.makeText(
                        requireContext(),
                        "Switch sent, but status check failed",
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
                        "Alerts ON for ${c.username}"
                    } else {
                        "Alerts OFF for ${c.username}"
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

        Log.d(
            "CHAT_INSTANCE",
            "onViewCreated fragment=${System.identityHashCode(this)} " +
                    "accountId=$accountId " +
                    "username=${cfg?.username.orEmpty()} " +
                    "profile=${currentProfileId()}"
        )

        textStatus = view.findViewById(R.id.textStatus)
        scrollChat = view.findViewById(R.id.scrollChat)
        chatContainer = view.findViewById(R.id.chatContainer)
        editMessage = view.findViewById(R.id.editMessage)
        btnSend = view.findViewById(R.id.btnSend)
        val chatComposerBar = editMessage.parent as? View
        btnStartPcg = view.findViewById(R.id.btnStartPcg)
        btnRefreshChat = view.findViewById(R.id.btnRefreshChat)
        btnTogglePush = view.findViewById(R.id.btnTogglePush)
        btnSafetyPrivacy = view.findViewById(R.id.btnSafetyPrivacy)
        editChannel = view.findViewById(R.id.editChannel)
        btnJumpToBottom = view.findViewById(R.id.btnJumpToBottom)
        geckoStreamView = view.findViewById(R.id.geckoStreamView)
        btnToggleStream = view.findViewById(R.id.btnToggleStream)
        btnMuteStream = view.findViewById(R.id.btnMuteStream)
        replyBar = view.findViewById(R.id.replyBar)
        txtReplyInfo = view.findViewById(R.id.txtReplyInfo)
        btnCancelReply = view.findViewById(R.id.btnCancelReply)
        btnCatchPresets = view.findViewById(R.id.btnCatchPresets)


        view.isFocusable = true
        view.isFocusableInTouchMode = true
        btnJumpToBottom.isFocusable = false
        btnJumpToBottom.isFocusableInTouchMode = false

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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {

                composerTextVersion++

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
        editChannel.setSingleLine(true)
        editChannel.imeOptions = EditorInfo.IME_ACTION_SEND

        editChannel.onChannelFieldTapped = { wasDropdownOpenBeforeTap ->
            if (wasDropdownOpenBeforeTap) {
                channelDropdownManuallyClosed = true
                pendingOpenChannelDropdownAfterIme = false
                editChannel.dismissDropDown()
            } else {
                openChannelFieldInput()
            }
        }

        editChannel.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (!channelDropdownManuallyClosed) {
                    pendingOpenChannelDropdownAfterIme = true
                    scheduleChannelDropdownOpen(220L)
                    scheduleChannelDropdownOpen(420L)
                }
            } else {
                channelDropdownManuallyClosed = false
                pendingOpenChannelDropdownAfterIme = false
                editChannel.dismissDropDown()
            }

            ViewCompat.requestApplyInsets(view)
        }


        editChannel.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN

            val isSendAction =
                actionId == EditorInfo.IME_ACTION_SEND ||
                        actionId == EditorInfo.IME_ACTION_GO ||
                        actionId == EditorInfo.IME_ACTION_DONE

            if (isSendAction || isEnter) {
                dismissChannelDropdown()
                sendOrGo()
                true
            } else {
                false
            }
        }

        cfg?.channel?.let { channelHistory.add(accountId, it) }
        refreshChannelsDropdown()

        textStatus.text = cfg?.let {
            getString(R.string.status_loading, it.username, it.channel)
        } ?: getString(R.string.account_not_found)


        val clearChannelFocusClickListener = View.OnClickListener {
            view.requestFocus()
            clearChannelFieldUi(hideKeyboard = false)
            closeComposerKeyboard()
        }

        view.setOnClickListener(clearChannelFocusClickListener)
        scrollChat.setOnClickListener(clearChannelFocusClickListener)
        chatContainer.setOnClickListener(clearChannelFocusClickListener)
        textStatus.setOnClickListener(clearChannelFocusClickListener)
        replyBar.setOnClickListener(clearChannelFocusClickListener)

        editMessage.setOnClickListener {
            clearChannelFieldUi(hideKeyboard = false)
            ViewCompat.requestApplyInsets(view)
        }

        editMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                clearChannelFieldUi(hideKeyboard = false)
                ViewCompat.requestApplyInsets(view)
            }
        }

        btnStartPcg.setOnClickListener {
            PcgActivity.start(requireContext(), accountId)
        }

        btnSend.setOnClickListener { sendOrGo() }

        editMessage.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_SEND || isEnter) {
                sendOrGo()
                true
            } else {
                false
            }


        }

        btnRefreshChat.setOnClickListener {
            manualRefresh()
        }


        btnSafetyPrivacy.setOnClickListener {
            SafetyPrivacyActivity.start(requireContext())
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

        val initialLeft = view.paddingLeft
        val initialTop = view.paddingTop
        val initialRight = view.paddingRight
        val initialBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            lastImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            lastImeBottomInsetPx = imeInsets.bottom

            val channelFieldHasFocus =
                this::editChannel.isInitialized && editChannel.hasFocus()

            val chatComposerHasFocus =
                this::editMessage.isInitialized && editMessage.hasFocus()

            v.setPadding(
                initialLeft,
                initialTop,
                initialRight,
                initialBottom + systemInsets.bottom
            )

            chatComposerBar?.translationY = 0f

            if (chatComposerHasFocus && !channelFieldHasFocus && lastImeVisible) {
                scrollChat.post {
                    if (stickToBottom) {
                        scrollToBottom()
                    }
                }
            }

            if (channelFieldHasFocus) {
                if (
                    pendingOpenChannelDropdownAfterIme &&
                    lastImeVisible &&
                    !channelDropdownManuallyClosed
                ) {
                    pendingOpenChannelDropdownAfterIme = false

                    editChannel.post {
                        if (!isAdded) return@post
                        showChannelDropdownNow()
                    }

                    editChannel.postDelayed({
                        if (!isAdded) return@postDelayed
                        if (!editChannel.hasFocus()) return@postDelayed
                        if (channelDropdownManuallyClosed) return@postDelayed
                        if (editChannel.isPopupShowing) return@postDelayed

                        showChannelDropdownNow()
                    }, 120L)
                } else if (editChannel.isPopupShowing) {
                    updateChannelDropdownHeight()
                }
            }

            insets
        }

        ViewCompat.requestApplyInsets(view)

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

        btnCatchPresets.setOnClickListener {
            showCatchPresetsMenu()
        }

        btnCatchPresets.setOnLongClickListener {
            CatchPresetSettingsBottomSheet
                .newInstance(currentProfileId().ifBlank { null })
                .show(childFragmentManager, CatchPresetSettingsBottomSheet.TAG)
            true
        }

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
        dismissChannelDropdown()

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
        connectInProgress = false
        historyLoaded = false
        textStatus.text = cfg?.let {
            getString(R.string.status_disconnected_account, it.username)
        } ?: getString(R.string.status_disconnected)
    }

    private fun openIrcClients(chatUsername: String, ircToken: String, ch: String) {
        val rc = TwitchChatClient(chatUsername, ircToken, ch)
        val sc = TwitchChatClient(chatUsername, ircToken, ch)

        Log.d("CHAT", "accountId=$accountId cfgChannel=$ch")

        readClient = rc
        sendClient = sc
        Log.d(
            "CHAT_INSTANCE",
            "openIrcClients fragment=${System.identityHashCode(this)} " +
                    "sendClient=${System.identityHashCode(sc)} " +
                    "profile=${currentProfileId()}"
        )
        sendReady = false

        rc.connect(
            onConnected = {
                runUiIfAlive {
                    val ctx = context ?: return@runUiIfAlive
                    textStatus.text = ctx.getString(R.string.status_connected, chatUsername, ch)
                }
            },
            onMessage = { user, msg, emotesRaw, _, msgId, replyParentUserLogin ->
                val key = msgId?.takeIf { it.isNotBlank() }?.let { "id:$it" }
                    ?: "live:${System.nanoTime()}:${user.lowercase()}:${msg.hashCode()}"

                val forceScroll = user.equals(chatUsername, ignoreCase = true)

                runUiIfAlive {
                    appendChatLine(
                        user = user,
                        message = msg,
                        emotesRaw = emotesRaw,
                        dedupKey = key,
                        replyParentUserLogin = replyParentUserLogin,
                        forceScroll = forceScroll,
                        messageTimestampSec = System.currentTimeMillis().toDouble() / 1000.0
                    )
                }
            },
            onError = { err ->
                runUiIfAlive {
                    val ctx = context ?: return@runUiIfAlive
                    textStatus.text = ctx.getString(R.string.status_read_error, err.message ?: "unknown")
                }
            }
        )

        sc.connect(
            onConnected = {
                sendReady = true
                runUiIfAlive {
                    val ctx = context ?: return@runUiIfAlive
                    textStatus.text = ctx.getString(R.string.status_connected, chatUsername, ch)
                }
            },
            onMessage = null,
            onError = { err ->
                sendReady = false
                runUiIfAlive {
                    val ctx = context ?: return@runUiIfAlive
                    textStatus.text = ctx.getString(R.string.status_send_error, err.message ?: "unknown")
                }
            },
            onNotice = { msgId, noticeMessage ->
                runUiIfAlive {
                    showTwitchSendNoticeToast(
                        msgId = msgId,
                        noticeMessage = noticeMessage
                    )
                }
            }
        )
    }

    private fun connectIfNeeded() {
        if (readClient != null || sendClient != null || connectInProgress) return

        val c = cfg ?: return

        if (!historyLoaded) {
            historyLoaded = true
            loadHistoryFromBot(c, seconds = HISTORY_SECONDS)
        }

        val ch = c.channel.trim().removePrefix("#").lowercase()
        textStatus.text = getString(R.string.status_connecting, c.username, ch)

        val profileId = c.profileId.trim()
        val localToken = c.accessToken.trim()

        if (profileId.isBlank()) {
            if (localToken.isBlank()) {
                textStatus.text = getString(R.string.status_missing_token, c.username)
                return
            }

            openIrcClients(
                chatUsername = c.username,
                ircToken = localToken,
                ch = ch
            )
            return
        }

        connectInProgress = true

        thread {
            try {
                val fresh = OAuthBackendApi.tokenForIrc(profileId)

                val ircToken = when {
                    fresh?.accessToken?.isNotBlank() == true -> fresh.accessToken
                    localToken.isNotBlank() -> localToken
                    else -> ""
                }

                val chatUsername = when {
                    fresh?.username?.isNotBlank() == true -> fresh.username
                    c.username.isNotBlank() -> c.username
                    else -> ""
                }

                runUiIfAlive {
                    connectInProgress = false

                    if (readClient != null || sendClient != null) return@runUiIfAlive

                    if (ircToken.isBlank() || chatUsername.isBlank()) {
                        val ctx = context ?: return@runUiIfAlive
                        textStatus.text = ctx.getString(R.string.status_missing_token, c.username)
                        return@runUiIfAlive
                    }

                    openIrcClients(
                        chatUsername = chatUsername,
                        ircToken = ircToken,
                        ch = ch
                    )
                }
            } catch (_: Exception) {
                runUiIfAlive {
                    connectInProgress = false

                    if (readClient != null || sendClient != null) return@runUiIfAlive

                    if (localToken.isBlank()) {
                        val ctx = context ?: return@runUiIfAlive
                        textStatus.text = ctx.getString(R.string.status_missing_token, c.username)
                        return@runUiIfAlive
                    }

                    openIrcClients(
                        chatUsername = c.username,
                        ircToken = localToken,
                        ch = ch
                    )
                }
            }
        }
    }

    private fun showTwitchSendNoticeToast(
        msgId: String?,
        noticeMessage: String
    ) {
        val normalizedMsgId = msgId?.trim()?.lowercase().orEmpty()
        val normalizedMessage = noticeMessage.trim()
        val lowerMessage = normalizedMessage.lowercase()

        val text = when {
            normalizedMsgId == "msg_duplicate" -> {
                getString(R.string.chat_send_filtered_duplicate)
            }

            normalizedMsgId == "msg_rate-limit" ||
                    normalizedMsgId == "msg_timedout" -> {
                getString(R.string.chat_send_filtered_rate_limit)
            }

            lowerMessage.contains("duplicate") ||
                    lowerMessage.contains("same message") ||
                    lowerMessage.contains("30 seconds") ||
                    lowerMessage.contains("identical") -> {
                getString(R.string.chat_send_filtered_duplicate)
            }

            lowerMessage.contains("too quickly") ||
                    lowerMessage.contains("rate") ||
                    lowerMessage.contains("slow down") -> {
                getString(R.string.chat_send_filtered_rate_limit)
            }

            normalizedMessage.isNotBlank() -> {
                getString(R.string.chat_send_filtered_generic, normalizedMessage)
            }

            else -> {
                getString(R.string.chat_send_filtered_rate_limit)
            }
        }

        Toast.makeText(
            requireContext(),
            text,
            Toast.LENGTH_SHORT
        ).show()

        Log.d(
            "TWITCH_NOTICE",
            "send notice msgId=$msgId message=$noticeMessage"
        )
    }

    private fun sendCurrentMessage() {
        val text = editMessage.text?.toString().orEmpty()
        sendMessageText(
            message = text,
            clearComposerOnSuccess = true,
            allowPendingReply = true
        )
    }

    private fun sendMessageText(
        message: String,
        clearComposerOnSuccess: Boolean,
        allowPendingReply: Boolean
    ): Boolean {
        val client = sendClient ?: return false
        if (!sendReady) {
            appendSystemLine(getString(R.string.connection_not_ready))
            return false
        }

        val text = message.trim()
        if (text.isBlank()) return false

        val replyParentId = pendingReplyMessageId
        if (allowPendingReply && !replyParentId.isNullOrBlank()) {
            client.sendReply(replyParentId, text)
            clearPendingReply()
        } else {
            if (!replyParentId.isNullOrBlank()) {
                clearPendingReply()
            }
            client.sendMessage(text)
        }

        if (clearComposerOnSuccess) {
            suppressComposerRestore = true

            editMessage.text?.clear()
            editMessage.clearFocus()

            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editMessage.windowToken, 0)

            editMessage.postDelayed({
                suppressComposerRestore = false
            }, 400)
        }

        stickToBottom = true
        scrollToBottom()
        return true
    }

    private fun loadHistoryFromBot(config: AccountConfig, seconds: Int) {
        if (HISTORY_BASE_URL.isBlank()) return

        val channelNorm = config.channel.trim().removePrefix("#").lowercase()
        val secondsSafe = seconds.coerceIn(30, HISTORY_SECONDS)

        thread {
            try {
                val chan = URLEncoder.encode(channelNorm, "UTF-8")
                val urlString = "$HISTORY_BASE_URL/history" +
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

                    val timestampSec = obj.optDouble("timestamp", 0.0)

                    runUiIfAlive {
                        appendChatLine(
                            user = user,
                            message = text,
                            emotesRaw = emotesRaw,
                            dedupKey = key,
                            replyParentUserLogin = null,
                            messageTimestampSec = if (timestampSec > 0.0) timestampSec else null
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

    private fun closeComposerKeyboard() {
        if (!this::editMessage.isInitialized) return

        editMessage.clearFocus()

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editMessage.windowToken, 0)
    }

    private fun restoreComposerFocusIfNeeded(
        hadFocus: Boolean,
        selectionStart: Int,
        selectionEnd: Int,
        expectedTextVersion: Long,
        expectedTextSnapshot: String
    ) {
        if (!hadFocus) return
        if (suppressComposerRestore) return
        if (!this::editMessage.isInitialized) return

        editMessage.post {
            if (!isAdded) return@post
            if (suppressComposerRestore) return@post
            if (!editMessage.hasFocus()) return@post

            val currentText = editMessage.text?.toString().orEmpty()

            // Se l'utente ha digitato nel frattempo, non ripristinare una selezione vecchia.
            if (composerTextVersion != expectedTextVersion) return@post
            if (currentText != expectedTextSnapshot) return@post

            val length = currentText.length
            val safeStart = selectionStart.coerceIn(0, length)
            val safeEnd = selectionEnd.coerceIn(0, length)

            editMessage.setSelection(
                minOf(safeStart, safeEnd),
                maxOf(safeStart, safeEnd)
            )
        }
    }

    private fun appendChatView(view: View, forceScroll: Boolean = false, countAsUnread: Boolean = true) {
        val hadComposerFocus = this::editMessage.isInitialized && editMessage.hasFocus()
        val oldSelectionStart = if (hadComposerFocus) editMessage.selectionStart else 0
        val oldSelectionEnd = if (hadComposerFocus) editMessage.selectionEnd else 0
        val oldTextSnapshot = if (hadComposerFocus) editMessage.text?.toString().orEmpty() else ""
        val oldTextVersion = composerTextVersion

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

        restoreComposerFocusIfNeeded(
            hadFocus = hadComposerFocus,
            selectionStart = oldSelectionStart,
            selectionEnd = oldSelectionEnd,
            expectedTextVersion = oldTextVersion,
            expectedTextSnapshot = oldTextSnapshot
        )
    }

    private fun appendSystemLine(text: String) {
        val tv = TextView(requireContext())
        tv.text = getString(R.string.system_bullet, text)
        tv.setTextColor(colorOnSurfaceVariant())
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

    private fun onCopyMessageRequested(message: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Twitch chat message", message)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            requireContext(),
            "Message copied",
            Toast.LENGTH_SHORT
        ).show()
    }


    private fun ensureStreamSession() {
        if (streamSession != null) {
            geckoStreamView.setSession(streamSession!!)
            return
        }

        val s = GeckoSessionManager.getOrCreateStreamSession(requireContext(), accountId)
        streamSession = s
        geckoStreamView.setSession(s)
        s.setActive(false)
    }

    private fun onChatMessageLongPressed(
        messageId: String?,
        user: String,
        message: String,
        messageTimestampSec: Double
    ) {
        Log.d(
            "CHAT_LONG_PRESS",
            "messageId=$messageId user=$user message=$message ts=$messageTimestampSec"
        )

        showMessageActions(
            messageId = messageId,
            user = user,
            message = message,
            messageTimestampSec = messageTimestampSec
        )
    }

    private fun showMessageActions(
        messageId: String?,
        user: String,
        message: String,
        messageTimestampSec: Double
    ) {
        val labels = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (!messageId.isNullOrBlank()) {
            labels += "Reply"
            actions += {
                beginReply(messageId, user, message)
            }
        }

        labels += "Copy message"
        actions += {
            onCopyMessageRequested(message)
        }

        labels += "Block user in app"
        actions += {
            onHideUserRequested(user)
        }

        labels += "Report message"
        actions += {
            onReportMessageRequested(
                messageId = messageId,
                user = user,
                message = message,
                messageTimestampSec = messageTimestampSec
            )
        }

        AlertDialog.Builder(requireContext())
            .setTitle("@$user")
            .setItems(labels.toTypedArray()) { _, which ->
                actions.getOrNull(which)?.invoke()
            }
            .show()
    }

    private fun onHideUserRequested(user: String) {
        val added = HiddenUsersStore.add(requireContext(), user)

        Log.d("CHAT_ACTION", "hide user requested user=$user added=$added")

        removeMessagesOfHiddenUser(user)

        Toast.makeText(
            requireContext(),
            if (added) {
                "User blocked in app: @$user"
            } else {
                "User already blocked: @$user"
            },
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun onReportMessageRequested(
        messageId: String?,
        user: String,
        message: String,
        messageTimestampSec: Double
    ) {
        val reporterProfileId = currentProfileId()
        val channel = currentChannelNormalized()

        Log.d(
            "CHAT_ACTION",
            "report message requested " +
                    "reporterProfileId=$reporterProfileId " +
                    "channel=$channel messageId=$messageId user=$user ts=$messageTimestampSec"
        )

        FcmRegistrationUploader.reportMessage(
            context = requireContext(),
            reporterProfileId = reporterProfileId,
            channel = channel,
            messageUser = user,
            messageText = message,
            messageId = messageId,
            messageTimestampSec = messageTimestampSec,
            reason = "user_report"
        ) { result ->
            if (!isAdded) return@reportMessage

            Toast.makeText(
                requireContext(),
                if (result.ok) {
                    "Message reported"
                } else {
                    "Report failed"
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun beginReply(messageId: String, user: String, message: String) {
        setPendingReply(messageId, user, message)

        editMessage.postDelayed({
            if (!isAdded) return@postDelayed

            clearChannelFieldUi(hideKeyboard = false)

            editMessage.requestFocus()
            editMessage.setSelection(editMessage.text?.length ?: 0)

            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            imm.showSoftInput(editMessage, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
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

    private inline fun runUiIfAlive(crossinline action: () -> Unit) {
        val act = activity ?: return
        act.runOnUiThread {
            if (!isAdded) return@runOnUiThread
            if (view == null) return@runOnUiThread
            action()
        }
    }

    private fun attachSwipeReplyAction(
        tv: SwipeReplyTextView,
        messageId: String?,
        user: String,
        message: String
    ) {
        tv.onSwipeReply = messageId
            ?.takeIf { it.isNotBlank() }
            ?.let { replyId ->
                { beginReply(replyId, user, message) }
            }
    }

    private fun maybeCaptureBuddyInfoFromChat(user: String, message: String) {
        val profileId = pendingBuddyProfileId ?: return
        val expectedUsername = pendingBuddyUsername ?: return
        val now = System.currentTimeMillis()

        if (now - pendingBuddyRequestedAtMs > 15_000L) {
            pendingBuddyProfileId = null
            pendingBuddyUsername = null
            pendingBuddyRequestedAtMs = 0L
            return
        }

        val normalizedUser = user.trim().lowercase()
        if (normalizedUser != "pokemoncommunitygame") return

        val parsed = BuddyMessageParser.parse(message)
        if (parsed == null) {
            Log.d("BUDDY_PARSE", "unparsed buddy response: $message")
            return
        }

        if (parsed.addressedUsername != expectedUsername) {
            Log.d(
                "BUDDY_PARSE",
                "ignored buddy response for other user expected=$expectedUsername actual=${parsed.addressedUsername}"
            )
            return
        }

        val dexEntry = PokemonTypeDex.findByPokemonName(requireContext(), parsed.rawName)

        val info = BuddyInfo(
            rawName = parsed.rawName,
            level = parsed.level,
            avgIv = parsed.avgIv,
            primaryType = dexEntry?.type1,
            secondaryType = dexEntry?.type2,
            isKnownPokemon = dexEntry != null,
            updatedAtMs = System.currentTimeMillis()
        )

        BuddyInfoStore.save(requireContext(), profileId, info)
        refreshOpenQuickCatchMenuIfNeeded()

        pendingBuddyProfileId = null
        pendingBuddyUsername = null
        pendingBuddyRequestedAtMs = 0L

        Log.d(
            "BUDDY_PARSE",
            "saved profileId=$profileId username=$expectedUsername " +
                    "rawName=${info.rawName} level=${info.level} avgIv=${info.avgIv} " +
                    "knownPokemon=${info.isKnownPokemon} type1=${info.primaryType} type2=${info.secondaryType}"
        )
    }

    private fun maybeCaptureSpawnInfoFromChat(
        user: String,
        message: String,
        messageTimestampSec: Double?
    ) {
        val normalizedUser = user.trim().lowercase()
        if (normalizedUser != "pokemoncommunitygame") return

        Log.d("SPAWN_PARSE", "candidate message=$message")

        val parsed = SpawnMessageParser.parse(message)
        if (parsed == null) {
            Log.d("SPAWN_PARSE", "not a spawn message")
            return
        }

        Log.d("SPAWN_PARSE", "parsed rawName=${parsed.rawName}")

        val dexEntry = resolveDexEntryForSpawnName(parsed.rawName)

        val seenAtMs = when {
            messageTimestampSec != null && messageTimestampSec > 0.0 ->
                (messageTimestampSec * 1000.0).toLong()
            else ->
                System.currentTimeMillis()
        }

        val newSnapshot = SpawnSnapshot(
            rawName = parsed.rawName,
            dexKey = dexEntry?.key,
            displayName = dexEntry?.pcgName ?: parsed.rawName,
            type1 = dexEntry?.type1,
            type2 = dexEntry?.type2,
            weightKg = dexEntry?.weightKg,
            baseSpeed = dexEntry?.baseSpeed,
            baseHp = dexEntry?.baseHp,
            evolvesTwice = dexEntry?.evolvesTwice,
            seenAtMs = seenAtMs,
            isAlreadyCaught = null
        )

        val channel = currentChannelNormalized()
        val existing = CurrentSpawnStore.loadForChannel(requireContext(), channel)

        if (existing != null && existing.seenAtMs > newSnapshot.seenAtMs) {
            Log.d(
                "SPAWN_PARSE",
                "ignored older spawn rawName=${newSnapshot.rawName} existing=${existing.rawName} " +
                        "existingSeenAtMs=${existing.seenAtMs} newSeenAtMs=${newSnapshot.seenAtMs}"
            )
            return
        }

        CurrentSpawnStore.saveForChannel(
            context = requireContext(),
            channel = channel,
            spawn = newSnapshot
        )

        val reloaded = CurrentSpawnStore.loadForChannel(requireContext(), channel)

        Log.d(
            "SPAWN_PARSE",
            "saved channel=$channel rawName=${newSnapshot.rawName} dexKey=${newSnapshot.dexKey} " +
                    "type1=${newSnapshot.type1} type2=${newSnapshot.type2} " +
                    "weightKg=${newSnapshot.weightKg} baseSpeed=${newSnapshot.baseSpeed} " +
                    "baseHp=${newSnapshot.baseHp} evolvesTwice=${newSnapshot.evolvesTwice} " +
                    "seenAtMs=${newSnapshot.seenAtMs} reloadOk=${reloaded != null} reloadName=${reloaded?.rawName}"
        )

        refreshOpenQuickCatchMenuIfNeeded()
    }

    private fun appendChatLine(
        user: String,
        message: String,
        emotesRaw: String?,
        dedupKey: String,
        replyParentUserLogin: String? = null,
        forceScroll: Boolean = false,
        messageTimestampSec: Double? = null
    ) {
        val now = System.currentTimeMillis()

        if (dedupKey == lastDedupKey && (now - lastDedupAtMs) < dedupWindowMs) return
        lastDedupKey = dedupKey
        lastDedupAtMs = now

        val stable = dedupKey.startsWith("id:")
        if (stable && rememberKey(dedupKey)) return

        val msgId = dedupKey.removePrefix("id:").takeIf { stable && it.isNotBlank() }
        addMentionUser(user)

        val resolvedMessageTimestampSec = messageTimestampSec
            ?: (System.currentTimeMillis().toDouble() / 1000.0)

        maybeCaptureBuddyInfoFromChat(user, message)
        maybeCaptureSpawnInfoFromChat(
            user = user,
            message = message,
            messageTimestampSec = messageTimestampSec
        )

        if (isUserHidden(user)) {
            Log.d("CHAT_HIDE", "skip hidden user=$user")
            return
        }

        val tv = createMessageTextView(
            user = user,
            rawMessage = message,
            emotesRaw = emotesRaw,
            replyParentUserLogin = replyParentUserLogin
        )

        tv.tag = ChatViewMeta(
            usernameLower = normalizeChatUser(user),
            messageId = msgId,
            messageText = message,
            messageTimestampSec = resolvedMessageTimestampSec
        )

        tv.setOnLongClickListener {
            val meta = tv.tag as? ChatViewMeta

            onChatMessageLongPressed(
                messageId = meta?.messageId ?: msgId,
                user = user,
                message = meta?.messageText ?: message,
                messageTimestampSec = meta?.messageTimestampSec
                    ?: (System.currentTimeMillis().toDouble() / 1000.0)
            )
            true
        }

        tv.setOnClickListener {
            clearChannelFieldUi(hideKeyboard = false)
            closeComposerKeyboard()
        }

        attachSwipeReplyAction(
            tv = tv,
            messageId = msgId,
            user = user,
            message = message
        )

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

    private fun isDarkTheme(): Boolean {
        val nightMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun pickEmoteThemeMode(): String {
        return if (isDarkTheme()) "dark" else "light"
    }

    private fun pickEmoteScale(textSizePx: Float): String {
        return when {
            textSizePx >= 54f -> "3.0"
            textSizePx >= 34f -> "2.0"
            else -> "3.0"
        }
    }

    private fun buildTwitchEmoteUrl(
        emoteId: String,
        themeMode: String,
        scale: String,
        format: String = "static"
    ): String {
        return "https://static-cdn.jtvnw.net/emoticons/v2/$emoteId/$format/$themeMode/$scale"
    }

    private fun targetEmoteRenderSizePx(textSizePx: Float): Int {
        return (textSizePx * 1.5f).toInt()
    }

    private fun showCatchPresetsMenu() {
        val context = requireContext()

        val profileId = currentProfileId().ifBlank { null }
        val rawPresets = candidateQuickPresetsForCurrentContext(profileId)
        val recommendations = recommendQuickPresetsForCurrentContext(rawPresets, profileId)
        val visibleRecommendations = visibleQuickRecommendations(recommendations)

        val presets = visibleRecommendations.map { it.preset }

        if (presets.isEmpty()) {
            Toast.makeText(
                context,
                getString(R.string.no_enabled_catch_presets),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val countsByBallId = if (profileId != null) {
            InventoryBallStore.getDisplayCounts(context, profileId)
        } else {
            emptyMap()
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_quick_catch_presets, null, false)
        val recycler = dialogView.findViewById<RecyclerView>(R.id.recyclerQuickCatchPresets)
        val spawnTitle = dialogView.findViewById<TextView>(R.id.txtQuickCatchSpawnTitle)
        val spawnSubtitle = dialogView.findViewById<TextView>(R.id.txtQuickCatchSpawnSubtitle)

        recycler.layoutManager = LinearLayoutManager(context)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val adapter = QuickCatchPresetMenuAdapter(
            items = buildQuickCatchRows(
                presets = presets,
                countsByBallId = countsByBallId,
                profileId = profileId,
                recommendations = visibleRecommendations
            ),
            onPresetClicked = { preset ->
                sendPresetCommand(preset, profileId)
                dialog.dismiss()
            },
            onBuyClicked = buyClick@ { preset ->
                if (profileId.isNullOrBlank()) return@buyClick

                showBuyBallQuantityDialog(
                    profileId = profileId,
                    preset = preset,
                    onInventoryChanged = {
                        refreshOpenQuickCatchMenuIfNeeded()
                    }
                )
            },
            onBuddyClicked = { preset ->
                handleFriendBallBuddyAction(preset)
            }
        )

        recycler.adapter = adapter

        quickCatchDialog = dialog
        quickCatchAdapter = adapter
        quickCatchProfileId = profileId
        quickCatchSpawnTitle = spawnTitle
        quickCatchSpawnSubtitle = spawnSubtitle

        dialog.setOnDismissListener {
            stopQuickCatchAutoRefresh()
            quickCatchDialog = null
            quickCatchAdapter = null
            quickCatchProfileId = null
            quickCatchSpawnTitle = null
            quickCatchSpawnSubtitle = null
        }

        dialog.show()
        updateQuickCatchHeader()
        startQuickCatchAutoRefresh()
    }

    private fun refreshOpenQuickCatchMenuIfNeeded() {
        val dialog = quickCatchDialog ?: return
        if (!dialog.isShowing) return

        val context = context ?: return
        val profileId = quickCatchProfileId

        val rawPresets = candidateQuickPresetsForCurrentContext(profileId)
        val recommendations = recommendQuickPresetsForCurrentContext(rawPresets, profileId)
        val visibleRecommendations = visibleQuickRecommendations(recommendations)

        val presets = visibleRecommendations.map { it.preset }

        val countsByBallId = if (!profileId.isNullOrBlank()) {
            InventoryBallStore.getDisplayCounts(context, profileId)
        } else {
            emptyMap()
        }

        quickCatchAdapter?.updateItems(
            buildQuickCatchRows(
                presets = presets,
                countsByBallId = countsByBallId,
                profileId = profileId,
                recommendations = visibleRecommendations
            )
        )

        updateQuickCatchHeader()
    }

    private fun resolveDisplayedCountForPreset(
        preset: CatchPreset,
        countsByBallId: Map<String, Int>
    ): Int? {
        return when (preset.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC -> {
                val poke = countsByBallId["poke_ball"] ?: 0
                if (poke > 0) {
                    poke
                } else {
                    countsByBallId["premier_ball"]
                }
            }

            null -> null
            else -> countsByBallId[preset.ballId]
        }
    }


    private fun buildQuickCatchRows(
        presets: List<CatchPreset>,
        countsByBallId: Map<String, Int>,
        profileId: String?,
        recommendations: List<CatchBallRecommendation>
    ): List<QuickCatchPresetRow> {
        val recommendationByPresetId = recommendations.associateBy { it.preset.id }

        return presets.map { preset ->
            val count = resolveDisplayedCountForPreset(preset, countsByBallId)
            val recommendation = recommendationByPresetId[preset.id]

            val subtitle = when {
                CatchPresetBallHelper.isFriendBallPreset(preset) ->
                    buildFriendBallSubtitle(profileId)

                else ->
                    CatchBallReasonFormatter.format(
                        context = requireContext(),
                        reasonKeys = recommendation?.reasonKeys.orEmpty()
                    )
            }

            QuickCatchPresetRow(
                preset = preset,
                label = preset.label,
                countText = count?.toString() ?: "-",
                subtitle = subtitle,
                showBuyButton = CatchPresetBallHelper.canBuyFromPreset(preset),
                showBuddyButton = CatchPresetBallHelper.isFriendBallPreset(preset)
            )
        }
    }

    private fun buildFriendBallSubtitle(profileId: String?): String? {
        if (profileId.isNullOrBlank()) return null

        val info = BuddyInfoStore.load(requireContext(), profileId)
            ?: return getString(R.string.buddy_unknown)

        val typeText = when {
            !info.primaryType.isNullOrBlank() && !info.secondaryType.isNullOrBlank() ->
                "${info.primaryType} / ${info.secondaryType}"

            !info.primaryType.isNullOrBlank() ->
                info.primaryType

            else -> null
        }

        return when {
            info.level != null && typeText != null -> getString(
                R.string.friend_ball_buddy_subtitle_level_types,
                info.rawName,
                info.level,
                typeText
            )

            info.level != null -> getString(
                R.string.friend_ball_buddy_subtitle_level,
                info.rawName,
                info.level
            )

            typeText != null -> getString(
                R.string.friend_ball_buddy_subtitle_name_types,
                info.rawName,
                typeText
            )

            else -> getString(
                R.string.friend_ball_buddy_subtitle_name_only,
                info.rawName
            )
        }
    }

    private fun sendRawChatCommand(command: String): Boolean {
        return sendMessageText(
            message = command,
            clearComposerOnSuccess = false,
            allowPendingReply = false
        )
    }

    private fun currentSpawnSnapshot(): SpawnSnapshot? {
        return CurrentSpawnStore.loadForChannel(
            requireContext(),
            currentChannelNormalized()
        )
    }
    private fun currentSpawnAgeSec(spawn: SpawnSnapshot?): Int? {
        if (spawn == null) return null
        val ageMs = System.currentTimeMillis() - spawn.seenAtMs
        if (ageMs <= 0L) return 0
        return (ageMs / 1000L).toInt()
    }

    private fun currentSpawnRemainingSec(spawn: SpawnSnapshot?): Int? {
        val ageSec = currentSpawnAgeSec(spawn) ?: return null
        return (90 - ageSec).coerceAtLeast(0)
    }

    private fun currentSpawnTypesText(spawn: SpawnSnapshot?): String? {
        if (spawn == null) return null

        return when {
            !spawn.type1.isNullOrBlank() && !spawn.type2.isNullOrBlank() ->
                "${spawn.type1} / ${spawn.type2}"

            !spawn.type1.isNullOrBlank() ->
                spawn.type1

            else -> null
        }
    }

    private fun updateQuickCatchHeader() {
        val titleView = quickCatchSpawnTitle ?: return
        val subtitleView = quickCatchSpawnSubtitle ?: return

        val spawn = currentSpawnSnapshot()

        if (spawn == null) {
            titleView.text = getString(R.string.quick_catch_no_spawn_title)
            subtitleView.text = getString(R.string.quick_catch_no_spawn_subtitle)
            return
        }

        titleView.text = spawn.displayName

        val remainingSec = currentSpawnRemainingSec(spawn) ?: 0
        val typesText = currentSpawnTypesText(spawn)

        subtitleView.text = if (!typesText.isNullOrBlank()) {
            getString(
                R.string.quick_catch_spawn_subtitle_types_time,
                typesText,
                remainingSec
            )
        } else {
            getString(
                R.string.quick_catch_spawn_subtitle_time,
                remainingSec
            )
        }
    }

    private fun recommendQuickPresetsForCurrentContext(
        presets: List<CatchPreset>,
        profileId: String?
    ): List<CatchBallRecommendation> {
        val buddy = if (!profileId.isNullOrBlank()) {
            BuddyInfoStore.load(requireContext(), profileId)
        } else {
            null
        }

        val spawn = currentSpawnSnapshot()
        val spawnAgeSec = if (spawn != null) {
            ((System.currentTimeMillis() - spawn.seenAtMs) / 1000L).toInt()
        } else {
            null
        }

        val recommendations = CatchBallRecommender.recommend(
            presets = presets,
            spawn = spawn,
            buddy = buddy
        )

        val signature = buildString {
            append("channel=").append(currentChannelNormalized())
            append("|spawn=").append(spawn?.displayName)
            append("|age=").append(spawnAgeSec)
            append("|buddy=").append(buddy?.rawName)
            append("|items=")
            recommendations.forEach { r ->
                append(r.preset.label)
                    .append(":")
                    .append(CatchPresetBallHelper.effectiveBallId(r.preset))
                    .append(":")
                    .append(r.score)
                    .append(":")
                    .append(r.reasonKeys.joinToString(","))
                    .append(";")
            }
        }

        if (signature != lastBallRecoLogSignature) {
            lastBallRecoLogSignature = signature

            Log.d(
                "BALL_RECO",
                "channel=${currentChannelNormalized()} spawn=${spawn?.displayName} ageSec=$spawnAgeSec " +
                        "type1=${spawn?.type1} type2=${spawn?.type2} weightKg=${spawn?.weightKg} " +
                        "speed=${spawn?.baseSpeed} hp=${spawn?.baseHp} evolvesTwice=${spawn?.evolvesTwice}"
            )

            for (r in recommendations) {
                Log.d(
                    "BALL_RECO",
                    "label=${r.preset.label} ballId=${CatchPresetBallHelper.effectiveBallId(r.preset)} " +
                            "score=${r.score} reasons=${r.reasonKeys}"
                )
            }
        }

        return recommendations
    }

    private fun visibleQuickRecommendations(
        recommendations: List<CatchBallRecommendation>
    ): List<CatchBallRecommendation> {
        return recommendations.filter { recommendation ->
            recommendation.score > 0 ||
                    CatchPresetBallHelper.isCoreStandardPreset(recommendation.preset)
        }.filterNot { recommendation ->
            CatchPresetBallHelper.shouldHideFromQuickMenu(recommendation.preset)
        }
    }

    private fun candidateQuickPresetsForCurrentContext(
        profileId: String?
    ): List<CatchPreset> {
        val context = requireContext()

        val savedPresets = CatchPresetStore.loadAll(context)
        val savedByBallId = LinkedHashMap<String, CatchPreset>()

        for (preset in savedPresets) {
            val effectiveBallId = CatchPresetBallHelper.effectiveBallId(preset) ?: continue
            if (!preset.enabled) continue
            if (!savedByBallId.containsKey(effectiveBallId)) {
                savedByBallId[effectiveBallId] = preset
            }
        }

        val countsByBallId = if (!profileId.isNullOrBlank()) {
            InventoryBallStore.getDisplayCounts(context, profileId)
        } else {
            emptyMap()
        }

        val candidateEntries = CatchBallCatalog.entries.filter { entry ->
            entry.keepAlways || (countsByBallId[entry.ballId] ?: 0) > 0
        }

        val result = mutableListOf<CatchPreset>()

        candidateEntries.forEachIndexed { index, entry ->
            val preset = savedByBallId[entry.ballId]?.copy(
                enabled = true,
                ballId = entry.ballId
            ) ?: CatchBallCatalog.createDefaultPreset(entry, index)

            result += preset
        }

        Log.d("QUICK_SRC", "profileId=$profileId savedPresetCount=${savedPresets.size}")
        Log.d("QUICK_SRC", "profileId=$profileId candidateEntryCount=${candidateEntries.size}")
        Log.d("QUICK_SRC", "profileId=$profileId finalCandidateCount=${result.size}")

        for (preset in result) {
            Log.d(
                "QUICK_SRC",
                "candidate preset label=${preset.label} command=${preset.command} " +
                        "ballId=${preset.ballId} effectiveBallId=${CatchPresetBallHelper.effectiveBallId(preset)} enabled=${preset.enabled}"
            )
        }

        return result
    }

    private fun showBuyBallQuantityDialog(
        profileId: String,
        preset: CatchPreset,
        onInventoryChanged: () -> Unit
    ) {

        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.buy_ball_quantity_hint)
            setText("1")
            setSelection(text.length)
        }


        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.buy_ball_title, preset.label))
            .setMessage(getString(R.string.buy_ball_message))
            .setView(input)
            .setNegativeButton(android.R.string.cancel) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.buy_ball_confirm) { _: DialogInterface, _: Int ->
                val quantity = input.text?.toString()?.trim()?.toIntOrNull()
                if (quantity == null || quantity <= 0) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.invalid_quantity),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val handled = requestBallPurchase(
                    profileId = profileId,
                    preset = preset,
                    quantity = quantity,
                    onSuccess = onInventoryChanged
                )

                if (!handled) return@setPositiveButton
            }
            .show()
    }

    private fun resolveBallIdToSpendForPreset(
        preset: CatchPreset,
        countsByBallId: Map<String, Int>
    ): String? {
        return when (preset.ballId) {
            CatchPresetStore.BALL_ID_AUTO_CATCH_BASIC -> {
                val poke = countsByBallId["poke_ball"] ?: 0
                when {
                    poke > 0 -> "poke_ball"
                    (countsByBallId["premier_ball"] ?: 0) > 0 -> "premier_ball"
                    else -> null
                }
            }

            null -> null
            else -> preset.ballId
        }
    }

    private fun noteCatchPresetUsedOptimistically(
        profileId: String,
        preset: CatchPreset
    ) {
        val context = requireContext()
        val countsByBallId = InventoryBallStore.getDisplayCounts(context, profileId)

        val spentBallId = resolveBallIdToSpendForPreset(preset, countsByBallId) ?: return

        InventoryBallStore.noteBallUsed(
            context = context,
            profileId = profileId,
            ballId = spentBallId
        )
    }

    private fun sendPresetCommand(
        preset: CatchPreset,
        profileId: String?
    ) {
        val sent = sendRawChatCommand(preset.command)
        if (!sent) return

        if (!profileId.isNullOrBlank()) {
            noteCatchPresetUsedOptimistically(profileId, preset)
        }
    }

    private fun createMessageTextView(
        user: String,
        rawMessage: String,
        emotesRaw: String?,
        replyParentUserLogin: String?
    ): SwipeReplyTextView {
        val tv = SwipeReplyTextView(requireContext()).apply {
            setTextColor(colorOnSurface())
            textSize = 14f
        }

        val spans = parseEmoteSpans(emotesRaw)
        val message = normalizeMessageForEmotes(rawMessage, spans.isNotEmpty())

        val lowerUser = user.lowercase()
        val lowerSelf = cfg?.username?.lowercase()
        val nameColor = if (lowerUser == lowerSelf) colorPrimarySafe() else colorForUsername(user)

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
                    ForegroundColorSpan(colorOnSurfaceVariant()),
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

            val hasMention = applyMentionHighlightSpans(plain)
            applyMentionRowHighlight(tv, hasMention)

            tv.text = plain
            return tv
        }

        val builder = SpannableStringBuilder(fullPrefix + message)

        for (s in spans.sortedByDescending { it.start }) {
            val startInMsg = s.start
            val endExclusiveInMsg = s.endInclusive + 1

            if (startInMsg !in 0 until message.length || endExclusiveInMsg !in (startInMsg + 1)..message.length) {
                continue
            }

            val start = fullPrefix.length + startInMsg
            val end = fullPrefix.length + endExclusiveInMsg

            if (start < fullPrefix.length || end > builder.length || start >= end) continue

            builder.replace(start, end, EMOTE_MARKER.toString())
        }

        if (replyHeader.isNotEmpty()) {
            builder.setSpan(
                ForegroundColorSpan(colorOnSurfaceVariant()),
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

        val hasMention = applyMentionHighlightSpans(builder)
        applyMentionRowHighlight(tv, hasMention)

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
            val themeMode = pickEmoteThemeMode()
            val scale = pickEmoteScale(tv.textSize)
            val url = buildTwitchEmoteUrl(
                emoteId = emoteId,
                themeMode = themeMode,
                scale = scale,
                format = "static"
            )

            Glide.with(this)
                .asDrawable()
                .load(url)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        val size = targetEmoteRenderSizePx(tv.textSize)
                        resource.setBounds(0, 0, size, size)

                        val imageSpan = ImageSpan(resource, ImageSpan.ALIGN_BOTTOM)
                        if (idx >= 0 && idx + 1 <= builder.length) {
                            builder.setSpan(
                                imageSpan,
                                idx,
                                idx + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            tv.text = builder
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
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