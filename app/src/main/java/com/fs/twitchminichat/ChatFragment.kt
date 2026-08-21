package com.fs.twitchminichat

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fs.twitchminichat.pcg.GeckoSessionManager
import com.fs.twitchminichat.pcg.PcgActivity
import com.fs.twitchminichat.pcg.mostwanted.PcgMostWantedActivity
import com.fs.twitchminichat.pcg.mostwanted.PcgMostWantedStore
import com.fs.twitchminichat.pcg.mostwanted.PcgMostWantedToggleController
import com.fs.twitchminichat.chat.ChatMessageDeduplicator
import com.fs.twitchminichat.chat.ChatMentionUserTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
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
import com.fs.twitchminichat.ui.input.ChatMessageInputView
import com.fs.twitchminichat.ui.insets.SystemBarsInsetHelper
import java.net.URLEncoder
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.coroutines.resume




private const val HISTORY_SECONDS = 3600
/** Logcat tag for non-sensitive backend history diagnostics. */
private const val HISTORY_LOG_TAG = "TMC_HISTORY"



class ChatFragment : Fragment(R.layout.fragment_chat), CatchPresetSettingsBottomSheet.Host {


    private var cfg: AccountConfig? = null
    private var accountId: String = ""

    private var ircClient: TwitchChatClient? = null

    @Volatile
    private var sendReady = false

    @Volatile
    private var connectInProgress = false

    private val ircReconnectBackoff = TwitchIrcReconnectBackoff()
    private val ircReconnectHandler = Handler(Looper.getMainLooper())
    private var ircReconnectRunnable: Runnable? = null
    private var ircConnectionGeneration = 0L
    private var outgoingWriteInProgress = false

    private val outgoingChatMessageTracker = OutgoingChatMessageTracker()
    private val outgoingMessageHandler = Handler(Looper.getMainLooper())
    private val pendingOutgoingViews = LinkedHashMap<String, PendingOutgoingView>()
    private val pendingOutgoingTimeouts = HashMap<String, Runnable>()

    private var suppressComposerRestore = false
    private var composerTextVersion = 0L

    /**
     * Timestamp until which the message composer is allowed to reclaim focus.
     *
     * This is intentionally short: it protects only the initial keyboard opening race,
     * not normal typing after the keyboard is already open.
     */
    private var composerFocusGuardUntilMs = 0L

    /**
     * Last ACTION_DOWN timestamp on the message composer.
     *
     * This lets outside-click handling distinguish between a transient layout click
     * caused by the composer tap and a real user tap outside the input.
     */
    private var lastComposerTouchDownAtMs = 0L


    /**
     * Delayed callbacks used to restore composer focus while the keyboard is opening.
     */
    private val composerFocusRestoreRunnables = mutableListOf<Runnable>()

    private var pendingBuddyUsername: String? = null

    private var quickCatchDialog: AlertDialog? = null
    private var quickCatchAdapter: QuickCatchPresetMenuAdapter? = null
    private var quickCatchProfileId: String? = null

    private var quickCatchSpawnTitle: TextView? = null
    private var quickCatchSpawnSubtitle: TextView? = null
    private var channelDropdownManuallyClosed = false
    private var pendingOpenChannelDropdownAfterIme = false
    private var lastImeVisible = false

    private var keyboardLayoutController: ChatKeyboardLayoutController? = null

    /**
     * Approximate row height used by the mention dropdown.
     *
     * The platform dropdown row includes text, selector padding, and popup decoration.
     * A slightly larger value avoids clipping when two suggestions are visible.
     */
    private val mentionDropdownRowHeightPx: Int
        get() = dp(56)

    /**
     * Extra vertical space reserved for popup padding and selector decoration.
     */
    private val mentionDropdownExtraHeightPx: Int
        get() = dp(12)

    /**
     * Maximum number of mention rows visible before the dropdown starts scrolling.
     */
    private val mentionDropdownMaxVisibleRows = 5

    private lateinit var channelSwitchBox: LinearLayout
    private lateinit var textStatus: TextView
    private lateinit var scrollChat: ScrollView
    private lateinit var chatContainer: LinearLayout
    /** Orders asynchronous history and live rows by Twitch timestamp. */
    private lateinit var chatTimelineController: ChatTimelineController
    /** Owns inline emote requests and animated drawable lifecycles for chat rows. */
    private var emoteImageLoader: TwitchEmoteImageLoader? = null
    /** Resolves the authenticated account's own outgoing emotes from a cached catalog. */
    private var emoteCatalogController: TwitchEmoteCatalogController? = null

    /** Owns the visible emote grid without adding picker logic to ChatFragment. */
    private var emotePickerController: TwitchEmotePickerController? = null
    /** Owns warnings and explicit external-browser launches for chat links. */
    private var externalBrowserLinkController: ExternalBrowserLinkController? = null
    private lateinit var editMessage: ChatMessageInputView
    private lateinit var btnSend: Button
    private lateinit var btnEmotes: ImageButton
    private lateinit var btnStartPcg: Button
    private lateinit var btnJumpToBottom: Button

    private lateinit var channelHistory: ChannelHistoryStore
    /** Loads profile-scoped history without legacy-key fallback. */
    private lateinit var backendHistoryClient: BackendHistoryClient
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

    /** Prevents overlapping backend writes from repeated alert-menu taps. */
    private var profileAlertSyncInProgress = false

    private data class ChatViewMeta(
        val usernameLower: String,
        val messageId: String?,
        val messageText: String,
        val messageTimestampSec: Double
    )

    private data class PendingOutgoingView(
        val row: LinearLayout,
        val status: TextView
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
            "buy requested profileMatches=${activeProfileId == requestedProfileId} " +
                    "quantity=$quantity sendReady=$sendReady clientReady=${ircClient != null}"
        )

        if (activeProfileId.isBlank()) {
            Log.w("CHAT_BUY", "reject: active profile blank")
            return false
        }

        if (activeProfileId != requestedProfileId) {
            Log.w("CHAT_BUY", "reject: profile mismatch")
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

    override fun onCatchPresetBuddyInfoRequested(): Boolean {
        /*
         * The preset settings sheet exposes a Pokébuddy button, but the actual
         * request must use ChatFragment's existing buddy flow.
         *
         * requestBuddyInfo() sends !pokebuddy and stores the pending profile/user
         * information needed to parse and save the response correctly.
         */
        return requestBuddyInfo()
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
        private var clickableSpanGestureInProgress = false

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
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                clickableSpanGestureInProgress =
                    hasClickableSpanAt(event)
            }

            if (clickableSpanGestureInProgress) {
                val handled = super.onTouchEvent(event)

                if (
                    event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
                ) {
                    clickableSpanGestureInProgress = false
                }

                return handled
            }

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

        /**
         * Returns true when the initial touch targets one clickable text span.
         */
        private fun hasClickableSpanAt(event: MotionEvent): Boolean {
            val spannedText = text as? Spanned
                ?: return false
            val textLayout = layout
                ?: return false
            val localX = event.x - totalPaddingLeft + scrollX
            val localY = event.y - totalPaddingTop + scrollY

            if (
                localX < 0f ||
                localY < 0f ||
                localY > textLayout.height.toFloat()
            ) {
                return false
            }

            val line = textLayout.getLineForVertical(
                localY.toInt()
            )

            if (
                localX < textLayout.getLineLeft(line) ||
                localX > textLayout.getLineRight(line)
            ) {
                return false
            }

            val characterOffset = textLayout.getOffsetForHorizontal(
                line,
                localX
            )

            return spannedText
                .getSpans(
                    characterOffset,
                    characterOffset,
                    android.text.style.ClickableSpan::class.java
                )
                .isNotEmpty()
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

    /** Owns channel-scoped mention candidates and their inactivity expiry. */
    private val mentionUserTracker = ChatMentionUserTracker(
        monotonicTimeMillis = { SystemClock.elapsedRealtime() }
    )

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

    /** Owns bounded duplicate-delivery state independently from Fragment UI state. */
    private val chatMessageDeduplicator = ChatMessageDeduplicator()

    private var historyLoaded = false
    private var lastPausedAtMs: Long = 0L


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

    /**
     * Keeps the chat top controls below the Android status bar.
     *
     * This mirrors the PCG screen behaviour, where only the top manual controls row
     * receives the status bar inset. Android's native resize handles the Input Method
     * Editor (IME), while ChatKeyboardLayoutController protects the navigation-bar edge.
     */
    private fun setupChatTopBarInsets() {
        if (!this::channelSwitchBox.isInitialized) return

        SystemBarsInsetHelper.keepBelowStatusBar(channelSwitchBox)
    }

    private fun updateChannelDropdownHeight() {
        if (!this::editChannel.isInitialized) return

        /*
         * With adjustResize, the Fragment root already ends above a docked keyboard.
         * Limiting the popup to that visible root keeps its actions reachable.
         */

        val root = view ?: return

        val rootLocation = IntArray(2)
        val fieldLocation = IntArray(2)

        root.getLocationOnScreen(rootLocation)
        editChannel.getLocationOnScreen(fieldLocation)

        val rootBottom = rootLocation[1] + root.height
        val fieldBottom = fieldLocation[1] + editChannel.height
        val availableBelow = rootBottom - fieldBottom - dp(8)

        editChannel.dropDownHeight = availableBelow
            .coerceAtLeast(dp(96))
            .coerceAtMost(dp(260))
    }

    /**
     * Positions and sizes the chat mention dropdown above the message input.
     *
     * The popup is kept above the composer for consistent placement while Android
     * resizes the Activity around the Input Method Editor (IME).
     */
    private fun updateMessageMentionDropdownGeometry(
        visibleItemCount: Int = mentionAdapter.count
    ) {
        if (!this::editMessage.isInitialized) return
        if (!this::mentionAdapter.isInitialized) return

        val root = view ?: return

        val rootLocation = IntArray(2)
        val fieldLocation = IntArray(2)

        root.getLocationOnScreen(rootLocation)
        editMessage.getLocationOnScreen(fieldLocation)

        val rootTop = rootLocation[1]
        val fieldTop = fieldLocation[1]

        /*
         * The mention popup belongs to the bottom composer, so the safest direction
         * is upward. This avoids the soft keyboard covering the suggestions.
         */
        val availableAbove = fieldTop - rootTop - dp(8)

        val safeVisibleRows = visibleItemCount
            .coerceAtLeast(1)
            .coerceAtMost(mentionDropdownMaxVisibleRows)

        val desiredHeight = (safeVisibleRows * mentionDropdownRowHeightPx) +
                mentionDropdownExtraHeightPx

        val dropdownHeight = desiredHeight
            .coerceAtMost(availableAbove.coerceAtLeast(mentionDropdownRowHeightPx))
            .coerceAtLeast(mentionDropdownRowHeightPx)

        editMessage.dropDownHeight = dropdownHeight

        /*
         * AutoCompleteTextView positions the popup below the anchor by default.
         * A negative vertical offset moves it above the input field.
         */
        editMessage.dropDownVerticalOffset = -(dropdownHeight + editMessage.height + dp(4))
    }

    /**
     * Refreshes composer-dependent geometry after Android has scheduled a layout pass.
     */
    private fun scheduleComposerLayoutRefresh() {
        if (!this::editMessage.isInitialized) return

        editMessage.post {
            if (!isAdded) return@post
            if (!this::editMessage.isInitialized) return@post
            if (!editMessage.hasFocus()) return@post

            editMessage.requestFocus()
            updateMessageMentionDropdownGeometry()

            if (stickToBottom) {
                scrollToBottom()
            }
        }
    }

    /**
     * Resets the AutoCompleteTextView adapter filter when the channel field is empty.
     *
     * AutoCompleteTextView can keep the adapter filtered by the last selected channel.
     * This means the visible text field may be empty, but the dropdown still shows
     * only the previously selected item. Filtering with an empty constraint restores
     * the full saved-channel list before the popup is shown.
     */
    private fun resetChannelDropdownFilterIfFieldEmpty(onFilterReady: () -> Unit) {
        if (!this::editChannel.isInitialized) return

        val currentText = editChannel.text?.toString().orEmpty()

        if (currentText.isBlank()) {
            channelsAdapter.filter.filter("") {
                editChannel.post {
                    if (!isAdded) return@post
                    onFilterReady()
                }
            }
        } else {
            onFilterReady()
        }
    }

    private fun showChannelDropdownNow() {
        if (!this::editChannel.isInitialized) return
        if (!isAdded) return
        if (!editChannel.hasFocus()) return
        if (channelDropdownManuallyClosed) return

        refreshChannelsDropdown()
        updateChannelDropdownHeight()

        resetChannelDropdownFilterIfFieldEmpty {
            if (!isAdded) return@resetChannelDropdownFilterIfFieldEmpty
            if (!editChannel.hasFocus()) return@resetChannelDropdownFilterIfFieldEmpty
            if (channelDropdownManuallyClosed) return@resetChannelDropdownFilterIfFieldEmpty

            updateChannelDropdownHeight()
            editChannel.showDropDown()

            Log.d(
                "CHAN_DROPDOWN",
                "show requested hasFocus=${editChannel.hasFocus()} " +
                        "popup=${editChannel.isPopupShowing} " +
                        "adapterCount=${channelsAdapter.count}"
            )
        }
    }

    private fun scheduleChannelDropdownOpen(delayMs: Long) {
        if (!this::editChannel.isInitialized) return

        editChannel.postDelayed({
            if (!isAdded) return@postDelayed
            if (!editChannel.hasFocus()) return@postDelayed
            if (channelDropdownManuallyClosed) return@postDelayed

            refreshChannelsDropdown()
            updateChannelDropdownHeight()

            resetChannelDropdownFilterIfFieldEmpty {
                if (!isAdded) return@resetChannelDropdownFilterIfFieldEmpty
                if (!editChannel.hasFocus()) return@resetChannelDropdownFilterIfFieldEmpty
                if (channelDropdownManuallyClosed) return@resetChannelDropdownFilterIfFieldEmpty

                updateChannelDropdownHeight()

                if (!editChannel.isPopupShowing) {
                    editChannel.showDropDown()
                }
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

            // If the keyboard is visible, it's opened immediately.
            // if it is still loading, we wait for it to be stable.
            if (lastImeVisible && !channelDropdownManuallyClosed) {
                pendingOpenChannelDropdownAfterIme = false
                showChannelDropdownNow()
            }
        }

        // Backup against when the insets event arrives late or not at all.
        scheduleChannelDropdownOpen(220L)
        scheduleChannelDropdownOpen(420L)
    }

    private fun clearChannelFieldUi(hideKeyboard: Boolean = true) {
        if (!this::editChannel.isInitialized) return

        /*
         * Capture the window token before clearing focus. Some devices are pickier
         * about hiding the keyboard after focus has already moved away.
         */
        val channelWindowToken = editChannel.windowToken

        dismissChannelDropdown()

        /*
         * Clear the visible channel field without triggering AutoCompleteTextView to
         * filter again using the last selected channel.
         */
        editChannel.setText("", false)

        /*
         * Reset the adapter filter too, otherwise the dropdown can stay narrowed to
         * the last selected channel even while the field looks empty.
         */
        channelsAdapter.filter.filter("")

        editChannel.clearFocus()

        if (hideKeyboard) {
            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            imm.hideSoftInputFromWindow(channelWindowToken, 0)

            view?.let { root ->
                ViewCompat.requestApplyInsets(root)
            }
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

    /**
     * Hides all currently rendered messages from a locally hidden user.
     *
     * The views are kept inside chatContainer instead of being removed. This allows
     * them to become visible again immediately if the user is later unblocked.
     */
    private fun removeMessagesOfHiddenUser(user: String) {
        val normalized = normalizeChatUser(user)
        if (normalized.isBlank()) return

        var changedAnyView = false

        for (i in chatContainer.childCount - 1 downTo 0) {
            val child = chatContainer.getChildAt(i)
            val meta = child.tag as? ChatViewMeta ?: continue

            if (meta.usernameLower == normalized && child.visibility != View.GONE) {
                child.visibility = View.GONE
                changedAnyView = true
            }
        }

        val pendingReplyUserNormalized = normalizeChatUser(pendingReplyUser)
        if (pendingReplyUserNormalized == normalized) {
            clearPendingReply()
        }

        if (changedAnyView) {
            updateJumpToBottomButton()
        }
    }

    /**
     * Re-applies the current local hidden-user state to all rendered chat messages.
     *
     * This is useful when returning from the blocked-users screen: users may have
     * been unblocked while ChatFragment was paused, so previously hidden views can
     * be made visible again without reopening the whole chat.
     */
    private fun refreshHiddenUserVisibilityInChat() {
        if (!this::chatContainer.isInitialized) return
        if (!isAdded) return

        var changedAnyView = false

        for (i in 0 until chatContainer.childCount) {
            val child = chatContainer.getChildAt(i)
            val meta = child.tag as? ChatViewMeta ?: continue

            val shouldBeHidden = HiddenUsersStore.isHidden(
                requireContext(),
                meta.usernameLower
            )

            val targetVisibility = if (shouldBeHidden) {
                View.GONE
            } else {
                View.VISIBLE
            }

            if (child.visibility != targetVisibility) {
                child.visibility = targetVisibility
                changedAnyView = true
            }
        }

        val pendingReplyUserNormalized = normalizeChatUser(pendingReplyUser)
        if (
            pendingReplyUserNormalized.isNotBlank() &&
            HiddenUsersStore.isHidden(requireContext(), pendingReplyUserNormalized)
        ) {
            clearPendingReply()
        }

        if (changedAnyView) {
            updateJumpToBottomButton()
        }
    }

    private fun addMentionUser(user: String) {
        val recorded = mentionUserTracker.record(
            username = user,
            authenticatedUsername = cfg?.username
        )
        if (!recorded) return

        refreshMentionSuggestions()
    }

    private fun resetMentionUsersForCurrentChannel() {
        mentionUserTracker.reset(authenticatedUsername = cfg?.username)
        refreshMentionSuggestions()
    }

    private fun refreshMentionSuggestions() {
        if (!this::mentionAdapter.isInitialized) return

        val items = mentionUserTracker
            .activeDisplayNames(authenticatedUsername = cfg?.username)
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
                        clearChannelFieldUi(hideKeyboard = true)
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

    /**
     * Releases all channel-bound emote rendering before the timeline is replaced.
     *
     * Clearing Glide targets stops animated drawables from the previous channel.
     * Closing the picker also prevents its previous adapter requests from remaining
     * visible while the next channel catalog is selected.
     */
    private fun resetChannelBoundChatUi() {
        emotePickerController?.closeIfOpen()
        emoteImageLoader?.clearAll()

        if (this::chatTimelineController.isInitialized) {
            chatTimelineController.clear()
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

        /*
         * Select the new channel immediately. If OAuth lacks user:read:emotes,
         * the picker now shows this channel's cache or an empty catalog instead
         * of retaining emotes belonging to the previous channel.
         */
        emoteCatalogController?.selectChannel(ch)

        reloadStreamForCurrentChannel()

        Log.d("CHAN", "JOIN recorded in recent channels")
        channelHistory.add(accountId, ch)
        Log.d("CHAN", "Recent channel count=${channelHistory.get(accountId).size}")
        refreshChannelsDropdown()

        historyLoaded = false
        chatMessageDeduplicator.clear()

        resetMentionUsersForCurrentChannel()

        unseenMessages = 0
        stickToBottom = true
        clearPendingOutgoingState(removeViews = false)
        resetChannelBoundChatUi()
        updateJumpToBottomButton()

        closeIrcClient(resetBackoff = true)
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
                clearChannelFieldUi(hideKeyboard = true)
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

    /** Returns the canonical backend profile identifier for the active account. */
    private fun currentProfileId(): String {
        return cfg
            ?.let(AccountProfileIdResolver::resolve)
            .orEmpty()
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountId = requireArguments().getString(ARG_ACCOUNT_ID).orEmpty()
        if (accountId.isBlank()) return

        channelHistory = ChannelHistoryStore(requireContext())
        backendHistoryClient = BackendHistoryClient(
            sessionReader = BackendSessionStore(requireContext())
        )
        externalBrowserLinkController = ExternalBrowserLinkController(
            context = requireContext()
        )
        cfg = AccountRepository(requireContext()).getById(accountId)

        Log.d(
            "CHAT_INSTANCE",
            "onViewCreated accountConfigured=${cfg != null} " +
                    "profileConfigured=${currentProfileId().isNotBlank()}"
        )

        channelSwitchBox = view.findViewById(R.id.channelSwitchBox)
        textStatus = view.findViewById(R.id.textStatus)
        scrollChat = view.findViewById(R.id.scrollChat)
        chatContainer = view.findViewById(R.id.chatContainer)
        chatTimelineController = ChatTimelineController(chatContainer)
        emoteImageLoader = TwitchEmoteImageLoader(
            requestManager = Glide.with(this),
            chatPageView = view
        )
        emoteCatalogController = TwitchEmoteCatalogController(
            context = requireContext(),
            accountId = accountId
        ).also { controller ->
            controller.selectChannel(cfg?.channel.orEmpty())
        }
        editMessage = view.findViewById(R.id.editMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnEmotes = view.findViewById(R.id.btnEmotes)
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

        emotePickerController = TwitchEmotePickerController(
            panel = view.findViewById(R.id.emotePickerPanel),
            recyclerView = view.findViewById(R.id.recyclerEmotes),
            searchInput = view.findViewById(R.id.editEmoteSearch),
            emptyText = view.findViewById(R.id.textEmotePickerEmpty),
            toggleButton = btnEmotes,
            closeButton = view.findViewById(R.id.btnCloseEmotePicker),
            composer = editMessage,
            requestManager = Glide.with(this),
            accountId = accountId,
            recentStore = TwitchEmoteRecentStore(requireContext()),
            catalogProvider = {
                emoteCatalogController?.currentCatalogSnapshot()
                    ?: TwitchEmoteCatalog.EMPTY
            },
            onBeforeOpen = {
                editMessage.clearFocus()

                val inputMethodManager = requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                inputMethodManager.hideSoftInputFromWindow(
                    editMessage.windowToken,
                    0
                )

                view.requestFocus()
            },
            onAfterClose = { keepInputMethod ->
                if (!keepInputMethod) {
                    val inputMethodManager = requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                    inputMethodManager.hideSoftInputFromWindow(
                        view.windowToken,
                        0
                    )

                    view.requestFocus()
                }
            }
        )

        emoteCatalogController?.setOnCatalogChangedListener { catalog ->
            emotePickerController?.submitCatalog(catalog)
        }

        setupChatTopBarInsets()

        editMessage.onComposerTouchDown = {
            /*
             * Close the picker without hiding the Input Method Editor because this
             * touch is immediately transferring focus back to the composer.
             */
            emotePickerController?.closeForComposerTouch()

            val wasAlreadyFocused = editMessage.hasFocus()
            lastComposerTouchDownAtMs = SystemClock.elapsedRealtime()

            armComposerFocusGuard()

            /*
             * Request focus immediately on ACTION_DOWN, before regular click dispatch.
             * This is the earliest safe moment to prepare the Input Method Editor. The
             * selection is intentionally left to the platform so a tap can position the
             * cursor inside existing text.
             */
            editMessage.requestFocus()

            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            if (!wasAlreadyFocused) {
                imm.restartInput(editMessage)
            }

            imm.showSoftInput(editMessage, InputMethodManager.SHOW_IMPLICIT)
        }

        keyboardLayoutController = ChatKeyboardLayoutController(
            root = view,
            onKeyboardShown = {
                if (stickToBottom) {
                    scrollToBottom()
                }
            }
        )

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        btnJumpToBottom.isFocusable = false
        btnJumpToBottom.isFocusableInTouchMode = false

        refreshPushToggleUi()

        btnTogglePush.setOnClickListener {
            showSpawnAlertModeMenu()
        }

        mentionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )

        editMessage.setAdapter(mentionAdapter)
        editMessage.setTokenizer(MentionTokenizer())
        editMessage.threshold = 1
        updateMessageMentionDropdownGeometry()

        resetMentionUsersForCurrentChannel()

        editMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                composerTextVersion++

                scheduleComposerLayoutRefresh()

                val query = currentMentionQuery()
                if (query == null) {
                    editMessage.dismissDropDown()
                    return
                }

                refreshMentionSuggestions()

                editMessage.post {
                    if (!isAdded) return@post
                    if (!editMessage.hasFocus()) return@post

                    mentionAdapter.filter.filter(query) {
                        if (!isAdded) return@filter
                        if (!editMessage.hasFocus()) return@filter

                        val resultCount = mentionAdapter.count
                        updateMessageMentionDropdownGeometry(
                            visibleItemCount = resultCount
                        )

                        if (resultCount > 0 && currentMentionQuery() != null) {
                            editMessage.showDropDown()
                        } else {
                            editMessage.dismissDropDown()
                        }
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
            val now = SystemClock.elapsedRealtime()
            val elapsedSinceComposerTouchMs = now - lastComposerTouchDownAtMs

            /*
             * Ignore only the tiny synthetic/out-of-order click window right after the
             * composer ACTION_DOWN. Any later outside tap is intentional and must close
             * the keyboard immediately.
             */
            if (
                isComposerFocusGuardActive() &&
                elapsedSinceComposerTouchMs in 0L..COMPOSER_OUTSIDE_TAP_IGNORE_MS
            ) {
                restoreComposerFocusIfGuarded("outside_click_ignored_initial_tap")
                return@OnClickListener
            }

            disarmComposerFocusGuard("outside_click")
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

            editMessage.requestFocus()

            scheduleComposerLayoutRefresh()
            updateMessageMentionDropdownGeometry()
            ViewCompat.requestApplyInsets(view)
        }

        editMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                clearChannelFieldUi(hideKeyboard = false)

                scheduleComposerLayoutRefresh()
                updateMessageMentionDropdownGeometry()
                ViewCompat.requestApplyInsets(view)
            } else {
                val now = SystemClock.elapsedRealtime()
                val elapsedSinceComposerTouchMs = now - lastComposerTouchDownAtMs

                /*
                 * Reclaim focus only during the very short initial tap window. If the user
                 * taps outside after that, the focus loss is intentional and must be allowed.
                 */
                if (
                    isComposerFocusGuardActive() &&
                    elapsedSinceComposerTouchMs in 0L..COMPOSER_OUTSIDE_TAP_IGNORE_MS
                ) {
                    restoreComposerFocusIfGuarded("message_focus_lost_initial_tap")
                } else {
                    editMessage.dismissDropDown()
                }
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


        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            keyboardLayoutController?.applyWindowInsets(insets)

            Log.d(
                "CHAT_IME",
                "insets imeVisible=${insets.isVisible(WindowInsetsCompat.Type.ime())} " +
                        "imeBottom=${imeInsets.bottom} " +
                        "systemBottom=${systemInsets.bottom} " +
                        "editMessageFocus=${this::editMessage.isInitialized && editMessage.hasFocus()} " +
                        "editChannelFocus=${this::editChannel.isInitialized && editChannel.hasFocus()} " +
                        "rootHeight=${view.height} " +
                        "rootBottomPadding=${view.paddingBottom}"
            )

            lastImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())

            if (this::editMessage.isInitialized && editMessage.hasFocus()) {
                updateMessageMentionDropdownGeometry(
                    visibleItemCount = mentionAdapter.count.coerceAtLeast(1)
                )
            }

            val channelFieldHasFocus =
                this::editChannel.isInitialized && editChannel.hasFocus()

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
                    if (emotePickerController?.closeIfOpen() == true) {
                        return
                    }

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

    override fun onDestroyView() {
        clearPendingOutgoingState(removeViews = false)
        cancelScheduledIrcReconnect()
        clearComposerFocusRestoreCallbacks()
        composerFocusGuardUntilMs = 0L

        emotePickerController?.release()
        emotePickerController = null

        externalBrowserLinkController?.release()
        externalBrowserLinkController = null

        emoteImageLoader?.clearAll()
        emoteImageLoader = null
        emoteCatalogController?.close()
        emoteCatalogController = null

        keyboardLayoutController?.stop()
        keyboardLayoutController = null

        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()

        val newCfg = AccountRepository(requireContext()).getById(accountId) ?: return

        val oldChannel = cfg?.channel
        cfg = newCfg
        emoteCatalogController?.selectChannel(newCfg.channel)

        if (oldChannel != null && !oldChannel.equals(newCfg.channel, ignoreCase = true)) {
            clearPendingOutgoingState(removeViews = true)
            resetChannelBoundChatUi()
            closeIrcClient(resetBackoff = true)
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

        refreshHiddenUserVisibilityInChat()
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

        if (awaySec >= 1 || ircClient == null) {
            val refreshSec = (awaySec + 10).coerceIn(30, HISTORY_SECONDS)
            loadHistoryFromBot(c, seconds = refreshSec)
        }
    }

    override fun onStop() {
        super.onStop()

        streamSession?.setActive(false)
        closeIrcClient(resetBackoff = true)

        textStatus.text = cfg?.let {
            getString(R.string.status_disconnected_account, it.username)
        } ?: getString(R.string.status_disconnected)
    }

    /** Cancels one pending reconnect callback. */
    private fun cancelScheduledIrcReconnect() {
        val pending = ircReconnectRunnable ?: return
        ircReconnectHandler.removeCallbacks(pending)
        ircReconnectRunnable = null
    }

    /** Closes the current IRC session and invalidates all callbacks from it. */
    private fun closeIrcClient(resetBackoff: Boolean) {
        cancelScheduledIrcReconnect()
        ircConnectionGeneration += 1L

        val oldClient = ircClient

        ircClient = null
        sendReady = false
        connectInProgress = false

        oldClient?.disconnect()

        if (resetBackoff) {
            ircReconnectBackoff.reset()
        }
    }

    /** Schedules one bounded reconnect while the chat Fragment is visible. */
    private fun scheduleIrcReconnect() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        if (ircReconnectRunnable != null || connectInProgress) return

        val delayMs = ircReconnectBackoff.consumeDelayMs()
        val reconnect = Runnable {
            ircReconnectRunnable = null

            if (!isAdded || view == null) return@Runnable
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                return@Runnable
            }

            Log.d(
                "TWITCH_IRC",
                "reconnect start reason=session"
            )
            connectIfNeeded()
        }

        ircReconnectRunnable = reconnect
        ircReconnectHandler.postDelayed(reconnect, delayMs)

        Log.w(
            "TWITCH_IRC",
            "reconnect scheduled delayMs=$delayMs reason=session"
        )
    }

    /** Handles the terminal callback from the active bidirectional IRC session. */
    private fun handleIrcConnectionEnded(
        generation: Long,
        shouldReconnect: Boolean,
        cause: Throwable?
    ) {
        runUiIfAlive {
            if (generation != ircConnectionGeneration) return@runUiIfAlive

            Log.w(
                "TWITCH_IRC",
                "connection ended source=session " +
                        "reconnect=$shouldReconnect error=${cause?.javaClass?.simpleName ?: "none"}"
            )

            closeIrcClient(resetBackoff = false)

            if (shouldReconnect) {
                scheduleIrcReconnect()
            }
        }
    }

    private fun openIrcClient(
        chatUsername: String,
        ircToken: String,
        ch: String
    ) {
        val applicationContext = requireContext().applicationContext

        cancelScheduledIrcReconnect()

        val generation = ircConnectionGeneration + 1L
        ircConnectionGeneration = generation

        val client = TwitchChatClient(
            chatUsername,
            ircToken,
            ch
        )

        Log.d(
            "CHAT",
            "Opening chat channelConfigured=${ch.isNotBlank()}"
        )

        ircClient = client

        Log.d(
            "CHAT_INSTANCE",
            "openIrcClient profileConfigured=${currentProfileId().isNotBlank()} " +
                    "generation=$generation"
        )

        sendReady = false

        client.connect(
            onConnected = {
                runUiIfAlive {
                    if (generation != ircConnectionGeneration) {
                        return@runUiIfAlive
                    }

                    ircReconnectBackoff.reset()
                    sendReady = true

                    val ctx = context
                        ?: return@runUiIfAlive

                    textStatus.text = ctx.getString(
                        R.string.status_connected,
                        chatUsername,
                        ch
                    )

                    Log.d(
                        "TWITCH_IRC",
                        "session ready generation=$generation"
                    )
                }
            },
            onMessage = {
                    user,
                    msg,
                    emotesRaw,
                    _,
                    msgId,
                    replyParentUserLogin,
                    messageTimestampSec ->

                /*
                 * Persist the spawn before entering the Fragment/UI callback. A valid
                 * PCG observation must survive a temporarily detached chat view.
                 */
                val spawnIngestion = SmartCatchSpawnIngestion.ingestIrcMessage(
                    context = applicationContext,
                    user = user,
                    message = msg,
                    messageTimestampSec = messageTimestampSec
                )

                val resolvedTimestampSec = (
                    messageTimestampSec
                        ?: (
                            System.currentTimeMillis()
                                .toDouble()
                                / 1000.0
                            )
                    )

                val fallbackTimestampMs = (
                    resolvedTimestampSec
                        * 1000.0
                    ).toLong()

                val key = msgId
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        "id:$it"
                    }
                    ?: (
                        "live:$fallbackTimestampMs:" +
                                "${user.lowercase()}:" +
                                msg.hashCode()
                        )

                val forceScroll = user.equals(
                    chatUsername,
                    ignoreCase = true
                )

                runUiIfAlive {
                    if (generation != ircConnectionGeneration) {
                        return@runUiIfAlive
                    }

                    appendChatLine(
                        user = user,
                        message = msg,
                        emotesRaw = emotesRaw,
                        dedupKey = key,
                        replyParentUserLogin = replyParentUserLogin,
                        forceScroll = forceScroll,
                        messageTimestampSec = resolvedTimestampSec
                    )

                    if (spawnIngestion.snapshotChanged) {
                        refreshOpenQuickCatchMenuIfNeeded()
                        updateQuickCatchHeader()
                    }
                }
            },
            onError = { error ->
                runUiIfAlive {
                    if (generation != ircConnectionGeneration) {
                        return@runUiIfAlive
                    }

                    sendReady = false

                    val ctx = context
                        ?: return@runUiIfAlive

                    textStatus.text = ctx.getString(
                        R.string.status_read_error,
                        error.message ?: "unknown"
                    )
                }
            },
            onNotice = {
                    msgId,
                    noticeMessage ->

                runUiIfAlive {
                    if (generation != ircConnectionGeneration) {
                        return@runUiIfAlive
                    }

                    when (
                        TwitchIrcNoticeClassifier.classify(
                            msgId = msgId,
                            message = noticeMessage
                        )
                    ) {
                        TwitchIrcNoticeCategory.AUTHENTICATION_FAILED -> {
                            Log.w(
                                "TWITCH_NOTICE",
                                "authentication failed reconnect=false"
                            )

                            closeIrcClient(resetBackoff = true)
                            textStatus.text = getString(
                                R.string.status_backend_session_reauthorize,
                                chatUsername
                            )
                        }

                        TwitchIrcNoticeCategory.OTHER -> {
                            showTwitchSendNoticeToast(
                                msgId = msgId,
                                noticeMessage = noticeMessage
                            )
                        }
                    }
                }
            },
            onUserState = {
                runUiIfAlive {
                    if (generation != ircConnectionGeneration) {
                        return@runUiIfAlive
                    }

                    confirmOldestPendingOutgoingFromUserState()
                }
            },
            onSessionMetadata = { update ->
                runUiIfAlive {
                    if (generation != ircConnectionGeneration) {
                        return@runUiIfAlive
                    }

                    val snapshot = TwitchIrcSessionMetadataStore.merge(
                        accountId = accountId,
                        update = update
                    )

                    snapshot.roomIdFor(ch)?.let { roomId ->
                        emoteCatalogController?.refresh(
                            accessToken = ircToken,
                            channel = ch,
                            broadcasterId = roomId
                        )
                    }

                    Log.d(
                        "TWITCH_EMOTES",
                        "session metadata hasUserId=${snapshot.userId != null} " +
                                "hasRoomId=${snapshot.roomIdFor(ch) != null} " +
                                "emoteSetCount=${snapshot.emoteSetIds.size}"
                    )
                }
            },
            onDisconnected = {
                    shouldReconnect,
                    cause ->

                handleIrcConnectionEnded(
                    generation = generation,

                    shouldReconnect = shouldReconnect,
                    cause = cause
                )
            }
        )
    }

    private fun connectIfNeeded() {
        if (ircClient != null || connectInProgress) return

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

            openIrcClient(
                chatUsername = c.username,
                ircToken = localToken,
                ch = ch
            )
            return
        }

        connectInProgress = true
        val connectGeneration = ircConnectionGeneration
        val ircTokenProvider = BackendIrcTokenProvider(
            sessionReader = BackendSessionStore(requireContext())
        )

        thread {
            val tokenResult = ircTokenProvider.acquire(
                profileId = profileId,
                localAccessToken = localToken,
                localUsername = c.username
            )

            runUiIfAlive {
                if (connectGeneration != ircConnectionGeneration) {
                    return@runUiIfAlive
                }

                connectInProgress = false

                if (ircClient != null) return@runUiIfAlive

                when (tokenResult) {
                    is BackendIrcTokenResult.Success -> {
                        openIrcClient(
                            chatUsername = tokenResult.username,
                            ircToken = tokenResult.accessToken,
                            ch = ch
                        )
                    }

                    BackendIrcTokenResult.ReauthorizationRequired -> {
                        val ctx = context ?: return@runUiIfAlive
                        textStatus.text = ctx.getString(
                            R.string.status_backend_session_reauthorize,
                            c.username
                        )
                    }

                    BackendIrcTokenResult.Failed -> {
                        val ctx = context ?: return@runUiIfAlive
                        textStatus.text = ctx.getString(
                            R.string.status_backend_auth_failed,
                            c.username
                        )
                    }
                }
            }
        }
    }

    /** Adds one immediate local echo after the socket write succeeds. */
    private fun appendPendingOutgoingMessage(
        pending: PendingOutgoingChatMessage,
        replyParentUserLogin: String?
    ) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            alpha = 0.72f
        }

        val messageView = createMessageTextView(
            user = cfg?.username.orEmpty(),
            rawMessage = pending.message,
            emotesRaw = emoteCatalogController
                ?.buildOutgoingIrcTag(pending.message),
            replyParentUserLogin = replyParentUserLogin
        )

        val statusView = TextView(requireContext()).apply {
            text = getString(R.string.chat_send_pending)
            setTextColor(colorOnSurfaceVariant())
            textSize = 10f
        }

        row.addView(messageView)
        row.addView(statusView)

        pendingOutgoingViews[pending.localId] = PendingOutgoingView(
            row = row,
            status = statusView
        )

        appendChatView(
            view = row,
            forceScroll = true,
            countAsUnread = false,
            messageTimestampSec = pending.sentAtSec
        )

        val timeout = Runnable {
            if (!outgoingChatMessageTracker.contains(pending.localId)) {
                return@Runnable
            }

            pendingOutgoingViews[pending.localId]?.let { pendingView ->
                pendingView.status.text = getString(
                    R.string.chat_send_unconfirmed
                )
                pendingView.row.alpha = 0.62f
            }
        }

        pendingOutgoingTimeouts[pending.localId] = timeout
        outgoingMessageHandler.postDelayed(timeout, 10_000L)
    }

    /** Marks the oldest recent local echo as confirmed by Twitch USERSTATE. */
    private fun confirmOldestPendingOutgoingFromUserState() {
        val confirmed = outgoingChatMessageTracker.confirmOldestFromUserState(
            channel = currentChannelNormalized(),
            confirmedAtSec = System.currentTimeMillis().toDouble() / 1000.0
        ) ?: return

        cancelPendingOutgoingTimeout(confirmed.localId)

        pendingOutgoingViews[confirmed.localId]?.let { pendingView ->
            pendingView.status.visibility = View.GONE
            pendingView.row.alpha = 1f
        }

        Log.d(
            "TWITCH_IRC",
            "outgoing message confirmed"
        )
    }

    /** Replaces one local echo when canonical live/history data becomes available. */
    private fun reconcilePendingOutgoingMessage(
        user: String,
        message: String,
        messageTimestampSec: Double
    ): ChatTimelinePosition? {
        val confirmed = outgoingChatMessageTracker.confirmCanonical(
            channel = currentChannelNormalized(),
            username = user,
            message = message,
            messageTimestampSec = messageTimestampSec
        ) ?: return null

        cancelPendingOutgoingTimeout(confirmed.localId)
        val preservedPosition = pendingOutgoingViews.remove(confirmed.localId)?.row?.let { row ->
            chatTimelineController.removeAndTakePosition(row)
        }

        Log.d(
            "TWITCH_IRC",
            "outgoing message reconciled"
        )

        return preservedPosition
    }

    /** Marks the newest unresolved local echo as rejected by a Twitch NOTICE. */
    private fun rejectNewestPendingOutgoing() {
        val rejected = outgoingChatMessageTracker.removeNewestAwaiting(
            currentChannelNormalized()
        ) ?: return

        cancelPendingOutgoingTimeout(rejected.localId)

        pendingOutgoingViews[rejected.localId]?.let { pendingView ->
            pendingView.status.text = getString(R.string.chat_send_rejected)
            pendingView.row.alpha = 0.5f
        }

        if (editMessage.text.isNullOrBlank()) {
            editMessage.setText(rejected.message)
            editMessage.setSelection(editMessage.text?.length ?: 0)
        }
    }

    /** Cancels one local outgoing status timeout. */
    private fun cancelPendingOutgoingTimeout(localId: String) {
        val timeout = pendingOutgoingTimeouts.remove(localId) ?: return
        outgoingMessageHandler.removeCallbacks(timeout)
    }

    /** Clears pending outgoing state when the view or channel is replaced. */
    private fun clearPendingOutgoingState(removeViews: Boolean) {
        pendingOutgoingTimeouts.values.forEach { timeout ->
            outgoingMessageHandler.removeCallbacks(timeout)
        }
        pendingOutgoingTimeouts.clear()
        outgoingChatMessageTracker.clear()

        if (removeViews && this::chatTimelineController.isInitialized) {
            pendingOutgoingViews.values.forEach { pendingView ->
                chatTimelineController.remove(pendingView.row)
            }
        }

        pendingOutgoingViews.clear()
        outgoingWriteInProgress = false
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

        rejectNewestPendingOutgoing()

        Toast.makeText(
            requireContext(),
            text,
            Toast.LENGTH_SHORT
        ).show()

        Log.d(
            "TWITCH_NOTICE",
            "send notice received hasMessageId=${!msgId.isNullOrBlank()}"
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
        val client = ircClient ?: return false
        if (!sendReady) {
            appendSystemLine(getString(R.string.connection_not_ready))
            return false
        }

        if (outgoingWriteInProgress) {
            return false
        }

        val text = message.trim()
        if (text.isBlank()) return false

        val replyParentId = pendingReplyMessageId
        val shouldSendReply = allowPendingReply && !replyParentId.isNullOrBlank()
        val localReplyParentUser = if (shouldSendReply) pendingReplyUser else null
        val generation = ircConnectionGeneration

        outgoingWriteInProgress = true

        val onWriteResult: (TwitchChatWriteResult) -> Unit = { result ->
            runUiIfAlive {
                outgoingWriteInProgress = false

                if (generation != ircConnectionGeneration || client !== ircClient) {
                    return@runUiIfAlive
                }

                when (result) {
                    TwitchChatWriteResult.Written -> {
                        val sentAtSec = System.currentTimeMillis().toDouble() / 1000.0
                        val pending = outgoingChatMessageTracker.register(
                            channel = currentChannelNormalized(),
                            username = cfg?.username.orEmpty(),
                            message = text,
                            sentAtSec = sentAtSec
                        )

                        appendPendingOutgoingMessage(
                            pending = pending,
                            replyParentUserLogin = localReplyParentUser
                        )

                        if (!replyParentId.isNullOrBlank()) {
                            clearPendingReply()
                        }

                        if (clearComposerOnSuccess) {
                            /*
                             * A successful manual composer send closes the emote
                             * picker just like the Input Method Editor.
                             */
                            emotePickerController?.closeIfOpen()
                        }

                        if (
                            clearComposerOnSuccess &&
                            editMessage.text?.toString()?.trim() == text
                        ) {
                            suppressComposerRestore = true

                            editMessage.text?.clear()
                            editMessage.clearFocus()

                            val imm = requireContext().getSystemService(
                                Context.INPUT_METHOD_SERVICE
                            ) as InputMethodManager
                            imm.hideSoftInputFromWindow(
                                editMessage.windowToken,
                                0
                            )

                            view?.let { root ->
                                ViewCompat.requestApplyInsets(root)
                            }

                            editMessage.postDelayed({
                                suppressComposerRestore = false
                            }, 400)
                        }

                        stickToBottom = true
                        scrollToBottom()
                    }

                    TwitchChatWriteResult.NotConnected -> {
                        sendReady = false
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.chat_send_write_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is TwitchChatWriteResult.Failed -> {
                        sendReady = false
                        Log.w(
                            "TWITCH_IRC",
                            "write failed errorType=${DiagnosticError.typeOf(result.cause)}"
                        )
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.chat_send_write_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        if (shouldSendReply) {
            client.sendReply(
                parentMsgId = replyParentId,
                text = text,
                onResult = onWriteResult
            )
        } else {
            client.sendMessage(
                text = text,
                onResult = onWriteResult
            )
        }

        return true
    }

    /** Loads history with the backend session bound to [config]. */
    private fun loadHistoryFromBot(config: AccountConfig, seconds: Int) {
        val applicationContext = requireContext().applicationContext
        val profileId = AccountProfileIdResolver.resolve(config)

        thread {
            when (
                val result = backendHistoryClient.load(
                    profileId = profileId,
                    channel = config.channel,
                    seconds = seconds
                )
            ) {
                is BackendHistoryResult.Success -> {
                    Log.d(
                        HISTORY_LOG_TAG,
                        "History loaded messageCount=${result.messages.size} seconds=$seconds"
                    )

                    result.messages.forEach { message ->
                        /* History is also a spawn source even if its UI row is stale. */
                        val spawnIngestion =
                            SmartCatchSpawnIngestion.ingestIrcMessage(
                                context = applicationContext,
                                user = message.user,
                                message = message.text,
                                messageTimestampSec = message.timestampSec
                                    .takeIf { timestamp -> timestamp > 0.0 }
                            )

                        val key = message.messageId?.let { messageId ->
                            "id:$messageId"
                        } ?: run {
                            "hist:${(message.timestampSec * 1000).toLong()}:" +
                                    "${message.user.lowercase()}:${message.text.hashCode()}"
                        }

                        runUiIfAlive {
                            appendChatLine(
                                user = message.user,
                                message = message.text,
                                emotesRaw = message.emotesRaw,
                                dedupKey = key,
                                replyParentUserLogin = null,
                                messageTimestampSec = message.timestampSec
                                    .takeIf { timestamp -> timestamp > 0.0 }
                            )

                            if (spawnIngestion.snapshotChanged) {
                                refreshOpenQuickCatchMenuIfNeeded()
                                updateQuickCatchHeader()
                            }
                        }
                    }
                }

                BackendHistoryResult.SessionRequired -> {
                    Log.w(
                        HISTORY_LOG_TAG,
                        "History skipped: backend session missing"
                    )
                }

                BackendHistoryResult.ReauthorizationRequired -> {
                    Log.w(
                        HISTORY_LOG_TAG,
                        "History rejected: manual reauthorization required"
                    )
                }

                BackendHistoryResult.Failed -> {
                    Log.w(
                        HISTORY_LOG_TAG,
                        "History request failed"
                    )
                }
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

    /**
     * Enables a short focus-protection window for the message composer.
     *
     * The guard only covers the initial keyboard opening race. It is not renewed for
     * every typed character, otherwise outside taps would feel delayed.
     */
    private fun armComposerFocusGuard() {
        val now = SystemClock.elapsedRealtime()

        composerFocusGuardUntilMs = now + COMPOSER_FOCUS_GUARD_MS

        Log.d(
            "CHAT_FOCUS",
            "composer focus guard armed"
        )

        scheduleComposerFocusRestoreBurst()
    }

    /**
     * Returns true while the composer is protected from transient focus loss.
     */
    private fun isComposerFocusGuardActive(): Boolean {
        return SystemClock.elapsedRealtime() <= composerFocusGuardUntilMs
    }

    /**
     * Disables the composer focus guard immediately.
     *
     * This is used for intentional outside taps, where the user's intent is to close
     * the keyboard rather than keep the composer focused.
     */
    private fun disarmComposerFocusGuard(reason: String) {
        composerFocusGuardUntilMs = 0L
        clearComposerFocusRestoreCallbacks()

        Log.d(
            "CHAT_FOCUS",
            "composer focus guard disarmed reason=$reason"
        )
    }

    /**
     * Cancels delayed focus restore callbacks.
     */
    private fun clearComposerFocusRestoreCallbacks() {
        composerFocusRestoreRunnables.forEach { runnable ->
            view?.removeCallbacks(runnable)
            editMessage.removeCallbacks(runnable)
        }

        composerFocusRestoreRunnables.clear()
    }

    /**
     * Reclaims focus for the composer if the focus guard is still active.
     */
    private fun restoreComposerFocusIfGuarded(reason: String) {
        if (!isAdded) return
        if (!this::editMessage.isInitialized) return
        if (!isComposerFocusGuardActive()) return

        if (!editMessage.hasFocus()) {
            editMessage.requestFocus()

            val imm = requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            /*
             * Restart the input connection before showing the keyboard again. This helps
             * when the cursor disappears but the keyboard remains visible.
             */
            imm.restartInput(editMessage)
            imm.showSoftInput(editMessage, InputMethodManager.SHOW_IMPLICIT)

            Log.d(
                "CHAT_FOCUS",
                "composer focus restored reason=$reason"
            )
        }

        scheduleComposerLayoutRefresh()
        updateMessageMentionDropdownGeometry()

        view?.let { root ->
            ViewCompat.requestApplyInsets(root)
        }
    }

    /**
     * Repeatedly restores composer focus while the keyboard animation settles.
     *
     * A single requestFocus() is not always enough because the Input Method Editor and
     * root layout can dispatch several focus/layout events during the opening animation.
     */
    private fun scheduleComposerFocusRestoreBurst() {
        clearComposerFocusRestoreCallbacks()

        val delaysMs = longArrayOf(0L, 40L, 90L, 160L, 260L, 380L)

        delaysMs.forEach { delayMs ->
            val runnable = Runnable {
                restoreComposerFocusIfGuarded("composer_guard_burst")
            }

            composerFocusRestoreRunnables += runnable
            editMessage.postDelayed(runnable, delayMs)
        }
    }

    /**
     * Closes the message composer keyboard when the user intentionally taps outside it.
     */
    private fun closeComposerKeyboard() {
        if (!this::editMessage.isInitialized) return

        disarmComposerFocusGuard("close_composer_keyboard")

        editMessage.dismissDropDown()
        editMessage.clearFocus()

        val imm = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        imm.hideSoftInputFromWindow(editMessage.windowToken, 0)

        view?.let { root ->
            ViewCompat.requestApplyInsets(root)
        }
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

            // If the user already digited , do not restore the older selection
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

    /** Inserts one chat row chronologically without disturbing composer focus. */
    private fun appendChatView(
        view: View,
        forceScroll: Boolean = false,
        countAsUnread: Boolean = true,
        messageTimestampSec: Double? = null,
        preservedTimelinePosition: ChatTimelinePosition? = null
    ) {
        val hadComposerFocus = this::editMessage.isInitialized && editMessage.hasFocus()
        val oldSelectionStart = if (hadComposerFocus) editMessage.selectionStart else 0
        val oldSelectionEnd = if (hadComposerFocus) editMessage.selectionEnd else 0
        val oldTextSnapshot = if (hadComposerFocus) editMessage.text?.toString().orEmpty() else ""
        val oldTextVersion = composerTextVersion

        val shouldAutoScroll = forceScroll || stickToBottom || isNearBottom()

        chatTimelineController.insert(
            view = view,
            messageTimestampSec = messageTimestampSec,
            preservedPosition = preservedTimelinePosition
        )

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
            "message actions opened hasMessageId=${!messageId.isNullOrBlank()}"
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

        Log.d("CHAT_ACTION", "hidden-user rule updated added=$added")

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
            "message report requested hasMessageId=${!messageId.isNullOrBlank()}"
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
            Log.d("BUDDY_PARSE", "Buddy response could not be parsed")
            return
        }

        if (parsed.addressedUsername != expectedUsername) {
            Log.d(
                "BUDDY_PARSE",
                "Buddy response ignored for non-active user"
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
            "Buddy snapshot saved knownPokemon=${info.isKnownPokemon}"
        )
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
        if (chatMessageDeduplicator.shouldSuppress(dedupKey)) return

        val stable = dedupKey.startsWith("id:")
        val msgId = dedupKey.removePrefix("id:").takeIf { stable && it.isNotBlank() }
        addMentionUser(user)

        val resolvedMessageTimestampSec = messageTimestampSec
            ?: (System.currentTimeMillis().toDouble() / 1000.0)

        val preservedTimelinePosition = reconcilePendingOutgoingMessage(
            user = user,
            message = message,
            messageTimestampSec = resolvedMessageTimestampSec
        )

        maybeCaptureBuddyInfoFromChat(user, message)
        if (isUserHidden(user)) {
            Log.d("CHAT_HIDE", "skip message from hidden user")
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

        appendChatView(
            view = tv,
            forceScroll = forceScroll,
            countAsUnread = true,
            messageTimestampSec = resolvedMessageTimestampSec,
            preservedTimelinePosition = preservedTimelinePosition
        )
    }

    private fun scrollToBottom() {
        scrollChat.post {
            scrollChat.scrollTo(0, chatContainer.bottom)
            stickToBottom = true
            unseenMessages = 0
            updateJumpToBottomButton()
        }
    }

    private fun isDarkTheme(): Boolean {
        val nightMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun targetEmoteRenderSizePx(textSizePx: Float): Int {
        return (textSizePx * 1.5f).toInt()
    }


    private fun showCatchPresetsMenu() {
        /**
         * This menu is opened from the chat screen, so here we need a valid Fragment
         * context. If the Fragment is not attached, requireContext() will fail,
         * which is fine for a direct user-triggered UI action.
         */
        val context = requireContext()

        /**
         * The active profile controls profile-scoped data such as:
         * - ball inventory counts;
         * - buddy info;
         * - per-profile catch recommendation context.
         *
         * currentProfileId() can return an empty string, so normalize it to null
         * when no profile is available.
         */
        val profileId = currentProfileId().ifBlank { null }

        /**
         * Build the complete quick catch menu model.
         *
         * The factory handles:
         * - enabled User Presets;
         * - Smart Presets from the current spawn;
         * - inventory counts;
         * - recommendation filtering;
         * - section/header row creation.
         *
         * ChatFragment only needs the final list to show in the RecyclerView.
         */
        val menuEntries = QuickCatchMenuModelFactory.build(
            context = context,
            profileId = profileId,
            spawn = currentSpawnSnapshot()
        )


        /**
         * Inflate the dialogue layout.
         *
         * The layout contains:
         * - a header area for spawn title/subtitle;
         * - a RecyclerView for section headers and preset rows.
         */
        val dialogView = layoutInflater.inflate(R.layout.dialog_quick_catch_presets, null, false)
        val recycler = dialogView.findViewById<RecyclerView>(R.id.recyclerQuickCatchPresets)
        val spawnTitle = dialogView.findViewById<TextView>(R.id.txtQuickCatchSpawnTitle)
        val spawnSubtitle = dialogView.findViewById<TextView>(R.id.txtQuickCatchSpawnSubtitle)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnQuickCatchClose)

        /**
         * The quick catch menu is a vertical list.
         *
         * The adapter itself decides whether each item is a section header or a
         * preset row.
         */
        recycler.layoutManager = LinearLayoutManager(context)

        /**
         * Create the AlertDialog but do not show it yet.
         *
         * We need the dialogue reference before creating callbacks because the preset
         * click callback dismisses the dialogue after sending the command.
         */
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialog.setCanceledOnTouchOutside(true)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        /**
         * Create the RecyclerView adapter.
         *
         * The adapter only renders rows and forwards click events.
         * It does not send chat commands directly and does not decide catch logic.
         */
        val adapter = QuickCatchPresetMenuAdapter(
            items = menuEntries,

            /**
             * Main preset row tap.
             *
             * This keeps the actual chat/game command behaviour in ChatFragment,
             * where the existing sendPresetCommand(...) logic already lives.
             */
            onPresetClicked = { preset ->
                sendPresetCommand(preset, profileId)
                dialog.dismiss()
            },

            /**
             * Buy button tap.
             *
             * This is only useful when we have a profile, because inventory updates
             * are profile-scoped.
             */
            onBuyClicked = buyClick@ { preset ->
                if (profileId.isNullOrBlank()) return@buyClick

                showBuyBallQuantityDialog(
                    profileId = profileId,
                    preset = preset,
                    onInventoryChanged = {
                        /**
                         * If the user buys balls while the menu is open, rebuild the
                         * visible rows so counts and recommendations stay current.
                         */
                        refreshOpenQuickCatchMenuIfNeeded()
                    }
                )
            },

            /**
             * Buddy button tap.
             *
             * Currently used for Friend Ball-related buddy handling.
             */
            onBuddyClicked = { preset ->
                handleFriendBallBuddyAction(preset)
            }
        )

        recycler.adapter = adapter

        /**
         * Store references to the open quick catch UI.
         *
         * These are used by:
         * - refreshOpenQuickCatchMenuIfNeeded();
         * - updateQuickCatchHeader();
         * - the auto-refresh timer.
         */
        quickCatchDialog = dialog
        quickCatchAdapter = adapter
        quickCatchProfileId = profileId
        quickCatchSpawnTitle = spawnTitle
        quickCatchSpawnSubtitle = spawnSubtitle

        /**
         * Clean up dialogue references when it closes.
         *
         * This prevents stale Fragment/UI references from being used after the menu
         * has been dismissed.
         */
        dialog.setOnDismissListener {
            stopQuickCatchAutoRefresh()
            quickCatchDialog = null
            quickCatchAdapter = null
            quickCatchProfileId = null
            quickCatchSpawnTitle = null
            quickCatchSpawnSubtitle = null
        }

        /**
         * Show the dialogue, then immediately refresh the header and start periodic
         * refresh.
         *
         * Periodic refresh matters because some recommendations are time-sensitive:
         * - Quick Ball changes after the early window;
         * - Timer Ball becomes relevant near the end of the spawn.
         */
        dialog.show()
        updateQuickCatchHeader()
        startQuickCatchAutoRefresh()
    }

    private fun refreshOpenQuickCatchMenuIfNeeded() {
        /**
         * If the quick catch dialogue is not open, there is nothing to refresh.
         *
         * This function may be called after inventory changes or timer ticks, so it
         * must safely exit when the popup is not currently visible.
         */
        val dialog = quickCatchDialog ?: return
        if (!dialog.isShowing) return

        /**
         * Use the nullable Fragment context here because this can be called by
         * delayed refresh/timer logic.
         *
         * If the Fragment is detached, we should not try to rebuild UI state.
         */
        val context = context ?: return

        /**
         * The profile used when the dialogue was opened.
         *
         * We intentionally reuse quickCatchProfileId instead of recalculating it
         * from the active account, because the open dialogue should stay tied to the
         * profile it was opened for.
         */
        val profileId = quickCatchProfileId

        /**
         * Rebuild the full quick catch menu model from the current state.
         *
         * This keeps the open dialogue updated when:
         * - inventory changes;
         * - spawn timing changes;
         * - user preset settings change;
         * - Smart Preset recommendations change.
         */
        val menuEntries = QuickCatchMenuModelFactory.build(
            context = context,
            profileId = profileId,
            spawn = currentSpawnSnapshot()
        )

        /**
         * Push the rebuilt menu model into the adapter.
         *
         * The adapter uses DiffUtil, so it should update only the rows that changed
         * instead of redrawing the entire list blindly.
         */
        quickCatchAdapter?.updateItems(menuEntries)

        /**
         * Refresh the header above the list.
         *
         * This keeps the spawn title/subtitle/timer aligned with the same current
         * spawn snapshot used to build Smart Presets.
         */
        updateQuickCatchHeader()
    }


    private fun sendRawChatCommand(command: String): Boolean {
        return sendMessageText(
            message = command,
            clearComposerOnSuccess = false,
            allowPendingReply = false
        )
    }

    private fun currentSpawnSnapshot(): SpawnSnapshot? {
        return CurrentSpawnStore.load(requireContext())
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

    /**
     * Returns seconds until the next expected PCG spawn.
     *
     * This uses the last known spawn timestamp as the anchor.
     *
     * PCG spawn model:
     * - spawn is active for 90 seconds;
     * - a new spawn starts every 15 minutes.
     *
     * If the app has never seen a spawn, we cannot calculate the next one.
     */
    private fun nextSpawnRemainingSecFromLastKnownSpawn(): Int? {
        val lastSpawn = CurrentSpawnStore.loadLastKnown(requireContext()) ?: return null

        val nowMs = System.currentTimeMillis()
        val ageMs = nowMs - lastSpawn.seenAtMs

        if (ageMs < 0L) return null

        /**
         * If several cycles passed while the app was closed, jump to the next
         * future 15-minute boundary based on the last known spawn.
         */
        val completedCycles = ageMs / PCG_SPAWN_INTERVAL_MS
        val nextSpawnAtMs = lastSpawn.seenAtMs + ((completedCycles + 1) * PCG_SPAWN_INTERVAL_MS)

        val remainingMs = nextSpawnAtMs - nowMs
        if (remainingMs <= 0L) return 0

        return (remainingMs / 1000L).toInt()
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

    private fun formatCountdownMmSs(totalSeconds: Int): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val seconds = safeSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    private fun updateQuickCatchHeader() {
        val titleView = quickCatchSpawnTitle ?: return
        val subtitleView = quickCatchSpawnSubtitle ?: return

        val spawn = currentSpawnSnapshot()

        if (spawn == null) {
            val nextSpawnRemainingSec = nextSpawnRemainingSecFromLastKnownSpawn()

            if (nextSpawnRemainingSec != null) {
                titleView.text = getString(R.string.quick_catch_next_spawn_title)
                subtitleView.text = getString(
                    R.string.quick_catch_next_spawn_subtitle,
                    formatCountdownMmSs(nextSpawnRemainingSec)
                )
            } else {
                titleView.text = getString(R.string.quick_catch_no_spawn_title)
                subtitleView.text = getString(R.string.quick_catch_no_spawn_subtitle)
            }

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

    private fun noteCatchPresetUsedOptimistically(
        profileId: String,
        preset: CatchPreset
    ) {
        QuickCatchInventoryUsageTracker.notePresetUsedOptimistically(
            context = requireContext(),
            profileId = profileId,
            preset = preset
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
            linksClickable = true
            movementMethod =
                android.text.method.LinkMovementMethod.getInstance()
        }

        val emoteLayout = TwitchEmoteMessageFormatter.format(
            rawMessage = rawMessage,
            emotesRaw = emotesRaw
        )
        val message = emoteLayout.text

        val lowerUser = user.lowercase()
        val lowerSelf = cfg?.username?.lowercase()
        val nameColor = if (lowerUser == lowerSelf) colorPrimarySafe() else colorForUsername(user)

        val replyHeader = replyParentUserLogin
            ?.takeIf { it.isNotBlank() }
            ?.let { "↪ replying to @$it\n" }
            .orEmpty()

        val prefix = "[$user] "
        val fullPrefix = replyHeader + prefix

        if (emoteLayout.markers.isEmpty()) {
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
            ChatMessageLinkifier.addWebLinks(
                text = plain,
                messageStartIndex = fullPrefix.length
            ) { url ->
                externalBrowserLinkController?.openLink(url)
            }

            tv.text = plain
            return tv
        }

        val builder = SpannableStringBuilder(fullPrefix + message)

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
        ChatMessageLinkifier.addWebLinks(
            text = builder,
            messageStartIndex = fullPrefix.length
        ) { url ->
            externalBrowserLinkController?.openLink(url)
        }

        tv.text = builder

        emoteImageLoader?.loadInto(
            textView = tv,
            text = builder,
            markers = emoteLayout.markers,
            markerOffset = fullPrefix.length,
            renderSizePx = targetEmoteRenderSizePx(tv.textSize),
            darkTheme = isDarkTheme()
        )

        return tv
    }

    companion object {
        private const val ARG_ACCOUNT_ID = "accountId"

        /**
         * Duration of the focus-protection window after the composer is touched.
         *
         * Keep this short: it protects the keyboard opening race without making outside
         * taps feel delayed.
         */
        private const val COMPOSER_FOCUS_GUARD_MS = 420L

        /**
         * Maximum time after composer ACTION_DOWN where an outside click can be considered
         * a synthetic/out-of-order event caused by layout movement.
         */
        private const val COMPOSER_OUTSIDE_TAP_IGNORE_MS = 140L



        private const val PCG_SPAWN_INTERVAL_MS = 15 * 60 * 1000L

        fun newInstance(accountId: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply { putString(ARG_ACCOUNT_ID, accountId) }
            }
        }
    }

    /**
     * Opens Most Wanted and handles only its explicit navigation result.
     *
     * Returning from the editor reopens the existing bell menu. It does not
     * save data, retry synchronization or send gameplay commands.
     */
    private val mostWantedActivityLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val shouldReopenAlertMenu =
                PcgMostWantedActivity.shouldReturnToAlertMenu(
                    resultCode = result.resultCode,
                    data = result.data
                )

            if (shouldReopenAlertMenu) {
                view?.post {
                    if (isAdded && view != null) {
                        showSpawnAlertModeMenu()
                    }
                }
            }
        }

    /** Opens the complete profile-scoped PCG alert menu. */
    private fun showSpawnAlertModeMenu() {
        val profileId = currentProfileId().trim().lowercase()

        if (profileId.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.spawn_alert_profile_missing,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val currentSelection = readProfileAlertSelection(profileId)

        PcgSpawnAlertModeMenu.show(
            anchor = btnTogglePush,
            currentSelection = currentSelection,
            onMostWantedRequested = {
                mostWantedActivityLauncher.launch(
                    PcgMostWantedActivity.createIntent(
                        context = requireContext(),
                        profileId = profileId
                    )
                )
            }
        ) { requestedSelection ->
            applyProfileAlertSelection(
                profileId = profileId,
                requested = requestedSelection
            )
        }
    }

    /**
     * Applies one complete user-confirmed alert selection in a safe order.
     *
     * Most Wanted registration and ordinary/event delivery share one backend
     * profile record. Serializing their requests prevents the previous race in
     * which an all-off ordinary mode could unregister a just-enabled watchlist.
     */
    private fun applyProfileAlertSelection(
        profileId: String,
        requested: PcgProfileAlertSelection
    ) {
        if (profileAlertSyncInProgress) {
            Toast.makeText(
                requireContext(),
                R.string.pcg_alert_settings_update_in_progress,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val current = readProfileAlertSelection(profileId)
        val plan = PcgProfileAlertSyncPlanner.buildPlan(
            current = current,
            requested = requested
        )
        if (plan.isEmpty()) return

        val appContext = requireContext().applicationContext
        val mostWantedController =
            PcgMostWantedToggleController(appContext)

        profileAlertSyncInProgress = true
        btnTogglePush.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var failed = false
                var temporaryDeliveryActivated = false

                for (step in plan) {
                    val ok = when (step) {
                        PcgProfileAlertSyncStep.FIREBASE_DELIVERY -> {
                            val deliveryOk =
                                syncProfileFirebaseDelivery(
                                    context = appContext,
                                    profileId = profileId,
                                    selection = requested
                                )

                            if (deliveryOk) {
                                persistSpawnAlertSettings(
                                    profileId = profileId,
                                    settings = requested.spawnSettings
                                )
                                temporaryDeliveryActivated =
                                    !current.requiresFirebaseDelivery &&
                                    requested.mostWantedEnabled &&
                                    !requested.spawnSettings
                                        .hasOrdinaryOrEventAlerts
                            }
                            deliveryOk
                        }

                        PcgProfileAlertSyncStep.MOST_WANTED -> {
                            withContext(Dispatchers.IO) {
                                mostWantedController.setEnabled(
                                    profileId = profileId,
                                    enabled =
                                        requested.mostWantedEnabled
                                ).ok
                            }
                        }
                    }

                    if (!ok) {
                        failed = true
                        break
                    }
                }

                if (
                    failed &&
                    temporaryDeliveryActivated
                ) {
                    /*
                     * Firebase was activated only to enable Most Wanted. If the
                     * watchlist request failed, remove that temporary delivery
                     * registration so local and backend state do not drift.
                     */
                    syncProfileFirebaseDelivery(
                        context = appContext,
                        profileId = profileId,
                        selection = requested.copy(
                            mostWantedEnabled = false
                        )
                    )
                }

                if (isAdded && view != null) {
                    val effective =
                        readProfileAlertSelection(profileId)
                    updateSpawnAlertBellUi(profileId)
                    val fullyApplied =
                        !failed && effective == requested

                    val message = when {
                        fullyApplied ->
                            R.string.pcg_alert_settings_saved

                        effective != current ->
                            R.string.pcg_alert_settings_partially_saved

                        else ->
                            R.string.pcg_alert_settings_save_failed
                    }

                    Toast.makeText(
                        requireContext(),
                        message,
                        if (fullyApplied) {
                            Toast.LENGTH_SHORT
                        } else {
                            Toast.LENGTH_LONG
                        }
                    ).show()
                }
            } finally {
                profileAlertSyncInProgress = false
                if (isAdded && ::btnTogglePush.isInitialized) {
                    btnTogglePush.isEnabled = true
                }
            }
        }
    }

    /** Reads the complete local selection for one normalized profile id. */
    private fun readProfileAlertSelection(
        profileId: String
    ): PcgProfileAlertSelection {
        return PcgProfileAlertSelectionStore.read(
            requireContext(),
            profileId
        )
    }

    /** Persists ordinary/event settings only after backend confirmation. */
    private fun persistSpawnAlertSettings(
        profileId: String,
        settings: PcgSpawnAlertSettings
    ) {
        PcgSpawnAlertModeStore.setMode(
            requireContext(),
            profileId,
            settings.regularMode
        )
        PcgEventSpawnAlertStore.setEnabled(
            requireContext(),
            profileId,
            settings.eventSpawnsEnabled
        )
    }

    /** Awaits the existing callback-based Firebase profile update once. */
    private suspend fun syncProfileFirebaseDelivery(
        context: Context,
        profileId: String,
        selection: PcgProfileAlertSelection
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            FcmRegistrationUploader.setProfileSpawnAlertMode(
                context = context,
                profileId = profileId,
                selection = selection
            ) { ok ->
                if (continuation.isActive) {
                    continuation.resume(ok)
                }
            }
        }
    }

    /**
     * Updates the bell button to reflect the locally selected spawn alert mode.
     *
     * The icon remains the same for now; the important feedback is the checked item
     * inside the bell menu. NONE is dimmed to make the disabled state visible.
     */
    private fun updateSpawnAlertBellUi(profileId: String) {
        if (!this::btnTogglePush.isInitialized) return

        val settings = PcgSpawnAlertSettings(
            regularMode = PcgSpawnAlertModeStore.getMode(
                requireContext(),
                profileId
            ),
            eventSpawnsEnabled = PcgEventSpawnAlertStore.isEnabled(
                requireContext(),
                profileId
            )
        )

        val mostWantedEnabled = PcgMostWantedStore(
            requireContext()
        ).isEnabled(profileId)

        val selection = PcgProfileAlertSelection(
            spawnSettings = settings,
            mostWantedEnabled = mostWantedEnabled
        )

        btnTogglePush.alpha = if (selection.requiresFirebaseDelivery) {
            1.0f
        } else {
            0.45f
        }

        val activeDescriptions = buildList {
            if (settings.regularMode != PcgSpawnAlertMode.NONE) {
                add(getString(settings.regularMode.titleRes))
            }
            if (settings.eventSpawnsEnabled) {
                add(getString(R.string.pcg_event_spawn_alert_label))
            }
            if (mostWantedEnabled) {
                add(getString(R.string.pcg_most_wanted_alert_label))
            }
        }

        btnTogglePush.contentDescription =
            activeDescriptions.joinToString(separator = "; ")
                .ifBlank {
                    getString(PcgSpawnAlertMode.NONE.titleRes)
                }

    }
}
