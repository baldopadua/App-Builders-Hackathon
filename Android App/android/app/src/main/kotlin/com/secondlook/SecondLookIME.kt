package com.secondlook

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.secondlook.secondlook.R

class SecondLookIME : InputMethodService() {

    private lateinit var warningBanner: WarningBanner
    private var currentWarning: Warning? = null
    private val debounceHandler = Handler(Looper.getMainLooper())

    // Layer 5: Send Shield state & views
    private var rootImeLayout: View? = null
    private var keyboardContainer: View? = null
    private var sendShieldContainer: FrameLayout? = null
    private var sendShieldPill: LinearLayout? = null
    private var tvSendShieldText: TextView? = null
    private var isShieldOn = false
    private val shieldHoldHandler = Handler(Looper.getMainLooper())

    // Layer 2: Auto-Itago state
    // 1. PIN / OTP / Passcode with digits (e.g. 'PIN ko 4821', 'OTP: 123456', 'passcode 9988')
    private val PIN_OTP_REGEX = Regex("""(?i)\b(m?pin|otp|passcode)\b([^\d\n]{0,20}?)(\d{4,8})\b""")

    // 2. Password with explicit value (e.g. 'password ko 1234', 'password: secret', 'pw: 123')
    private val PW_VALUE_REGEX = Regex("""(?i)\b(password|pass|pw)\b(\s*(?:ko(?:\s+ay)?|ay|is|:|:=|=|\s)\s*)([A-Za-z0-9_\-!@#$%^&*]{3,})\b""")

    // Common grammatical particles/stopwords that must never be treated as passwords
    private val PW_STOPWORDS = setOf(
        "ang", "ng", "sa", "ko", "ay", "mo", "na", "ka", "po", "the", "is", "for", "to", "and", "my", "your", "with", "word", "words", "namin", "natin", "nila", "niya"
    )

    // Approved secrets in RAM only (cleared on field finish)
    private val approvedSecrets = mutableSetOf<String>()

    private var restoreText: String? = null
    private var maskedText: String? = null

    private fun maskSecret(text: String): String? {
        var out = text

        // 1. Check PIN / OTP digits first (e.g. 'PIN ko 4821', 'OTP: 123456')
        val pinMatch = PIN_OTP_REGEX.find(out)
        if (pinMatch != null) {
            val digits = pinMatch.groupValues[3]
            if (digits !in approvedSecrets) {
                val bullets = "•".repeat(digits.length)
                return out.substring(0, pinMatch.range.first) +
                       pinMatch.groupValues[1] + pinMatch.groupValues[2] + bullets +
                       out.substring(pinMatch.range.last + 1)
            }
        }

        // 2. Check Password with actual value (e.g. 'password ko 1234', 'password: secret')
        var pwHit = false
        val pwOut = PW_VALUE_REGEX.replace(out) { m ->
            val secretValue = m.groupValues[3]
            if (secretValue.lowercase() in PW_STOPWORDS || secretValue in approvedSecrets) {
                return@replace m.value
            }
            pwHit = true
            val bullets = "•".repeat(secretValue.length)
            m.groupValues[1] + m.groupValues[2] + bullets
        }

        return if (pwHit) pwOut else null
    }

    // Layer 1 + 2: Fast rules check + Auto-Itago
    private val rulesRunnable = Runnable {
        val text = getCurrentText()
        if (text.isEmpty() || text == maskedText) return@Runnable
        if (text == restoreText) return@Runnable

        // If currently in masked state, absorb newly typed or deleted characters smoothly
        val currentMasked = maskedText
        val currentRestore = restoreText
        if (currentMasked != null && currentRestore != null) {
            if (text.startsWith(currentMasked) && text.length > currentMasked.length) {
                val appended = text.substring(currentMasked.length)
                val newRestore = currentRestore + appended
                val newMasked = currentMasked + "•".repeat(appended.length)
                restoreText = newRestore
                maskedText = newMasked
                replaceCurrentText(newMasked)
                return@Runnable
            } else if (currentMasked.startsWith(text) && text.length < currentMasked.length) {
                val diff = currentMasked.length - text.length
                restoreText = currentRestore.dropLast(diff)
                maskedText = text
                return@Runnable
            } else if (!text.contains("•")) {
                restoreText = null
                maskedText = null
            }
        }

        val w = DetectionEngine.detectRulesOnly(text) ?: return@Runnable
        if (w.type != WarningType.HIGH || !SettingsManager.isIngatModeEnabled(this)) return@Runnable

        val masked = maskSecret(text) ?: return@Runnable
        Log.i("SecondLookIME", "Auto-Itago triggered! Masking secret in draft (${text.length} chars)")
        restoreText = text
        maskedText = masked
        replaceCurrentText(masked)
        currentWarning = w
        updatePangolinState(w)
        FeedbackManager.notifyWarning(applicationContext, isHighAlert = true)
        showSendShield()

        if (::warningBanner.isInitialized) {
            warningBanner.showAutoHidden(w) {
                val original = restoreText
                if (original != null && getCurrentText() == maskedText) {
                    PIN_OTP_REGEX.findAll(original).forEach { m ->
                        approvedSecrets += m.groupValues[3]
                    }
                    PW_VALUE_REGEX.findAll(original).forEach { m ->
                        val v = m.groupValues[3]
                        if (v.lowercase() !in PW_STOPWORDS) {
                            approvedSecrets += v
                        }
                    }
                    replaceCurrentText(original)
                    warningBanner.showRestored()
                    hideSendShield()
                }
                restoreText = null
                maskedText = null
            }
        }
    }

    // Section 11.1: No draft text in logs (logs only length and warning type)
    private val debounceRunnable = Runnable {
        val text = getCurrentText()
        if (maskedText != null && text == maskedText) {
            return@Runnable
        }
        Log.d("SecondLookIME", "500ms pause reached, analyzing draft (${text.length} chars)")
        DetectionEngine.detectAsync(text) { warning ->
            Log.d("SecondLookIME", "DetectionEngine result: ${warning?.type} - ${warning?.headline}")
            val isNewWarning = warning != null && (currentWarning == null || currentWarning?.type != warning.type || currentWarning?.headline != warning.headline)
            currentWarning = warning
            if (::warningBanner.isInitialized) {
                if (warning != null) {
                    warningBanner.showWarning(warning)
                    if (warning.type == WarningType.HIGH) {
                        showSendShield()
                    } else {
                        hideSendShield()
                    }
                    if (isNewWarning) {
                        FeedbackManager.notifyWarning(applicationContext, warning.type == WarningType.HIGH)
                    }
                } else {
                    hideSendShield()
                    if (maskedText == null || text != maskedText) {
                        warningBanner.hide()
                    }
                }
            }
            updatePangolinState(warning)
        }
    }

    enum class ShiftState {
        UNSHIFTED,
        SHIFTED,
        CAPS_LOCKED
    }

    enum class KeyboardMode {
        QWERTY,
        SYMBOLS,
        SYMBOLS_ALT
    }

    private var shiftState = ShiftState.UNSHIFTED
    private var lastShiftTapTime: Long = 0L
    private var keyboardMode = KeyboardMode.QWERTY

    data class EnterActionInfo(
        val label: String,
        val targetAction: Int,
        val isSendOrSubmit: Boolean,
        val isNewLine: Boolean
    )

    private var shiftButton: Button? = null
    private var delButton: Button? = null
    private var modeToggleButton: Button? = null
    private var enterButton: Button? = null

    private val row1Buttons = mutableListOf<Button>()
    private val row2Buttons = mutableListOf<Button>()
    private val row3MiddleButtons = mutableListOf<Button>()

    private val qwertyRow1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    private val symbolsRow1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    private val symbolsAltRow1 = listOf("~", "`", "|", "^", "\\", "°", "{", "}", "[", "]")

    private val qwertyRow2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    private val symbolsRow2 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "=")
    private val symbolsAltRow2 = listOf("<", ">", "€", "£", "¥", "₱", "§", "©", "®")

    private val qwertyRow3 = listOf("z", "x", "c", "v", "b", "n", "m")
    private val symbolsRow3 = listOf("/", ":", ";", "_", "\"", "!", "?")
    private val symbolsAltRow3 = listOf("¿", "¡", "«", "»", "•", "…", "÷")

    private val deleteRepeatHandler = Handler(Looper.getMainLooper())
    private val deleteRepeatRunnable = object : Runnable {
        override fun run() {
            FeedbackManager.vibrateKeyPress(applicationContext)
            handleBackspace()
            deleteRepeatHandler.postDelayed(this, 50)
        }
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return

        // 1. If text is currently selected, replace the entire selection with an empty string
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
            onTextChanged()
            return
        }

        // 2. Secondary check via ExtractedText for custom editors where getSelectedText returns null
        try {
            val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
            if (extracted != null && extracted.selectionStart != extracted.selectionEnd) {
                ic.commitText("", 1)
                onTextChanged()
                return
            }
        } catch (_: Exception) {
            // Ignore if extracted text request is unsupported
        }

        // 3. No selection: delete 1 character before cursor, with fallback to KEYCODE_DEL
        if (!ic.deleteSurroundingText(1, 0)) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        onTextChanged()
    }

    private var pangolinStatusDot: View? = null
    private var pangolinStatusLabel: TextView? = null

    override fun onCreate() {
        super.onCreate()
        DetectionEngine.init(applicationContext)
        FeedbackManager.init(applicationContext)
    }

    private fun showSendShield() {
        if (!SettingsManager.isIngatModeEnabled(this)) return
        val isChatApp = currentInputEditorInfo?.packageName in chatApps
        if (!isChatApp) return
        if (isShieldOn) return

        isShieldOn = true
        sendShieldContainer?.visibility = View.VISIBLE
        tvSendShieldText?.text = "🛡️ I-review muna — idiin 2s para i-unlock ang Send"
        window?.window?.decorView?.requestLayout()
    }

    private fun hideSendShield() {
        shieldHoldHandler.removeCallbacksAndMessages(null)
        if (!isShieldOn) return

        isShieldOn = false
        sendShieldContainer?.visibility = View.GONE
        window?.window?.decorView?.requestLayout()
    }

    override fun onComputeInsets(outInsets: Insets) {
        super.onComputeInsets(outInsets)
        val kb = keyboardContainer
        val kbTop = kb?.top ?: 0
        outInsets.contentTopInsets = kbTop
        outInsets.visibleTopInsets = kbTop
        outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
        val top = if (isShieldOn) 0 else kbTop
        val root = rootImeLayout ?: return
        outInsets.touchableRegion.set(0, top, root.width, root.height)
    }

    override fun onCreateInputView(): View {
        val keyboardView = layoutInflater.inflate(R.layout.keyboard_layout, null)
        rootImeLayout = keyboardView
        keyboardContainer = keyboardView.findViewById(R.id.keyboard_container)
        sendShieldContainer = keyboardView.findViewById(R.id.send_shield_container)
        sendShieldPill = keyboardView.findViewById(R.id.send_shield_pill)
        tvSendShieldText = keyboardView.findViewById(R.id.tv_send_shield_text)

        sendShieldPill?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    tvSendShieldText?.text = "Idiin pa (2s) para i-unlock..."
                    shieldHoldHandler.postDelayed({
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        hideSendShield()
                    }, 2000)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    shieldHoldHandler.removeCallbacksAndMessages(null)
                    tvSendShieldText?.text = "🛡️ I-review muna — idiin 2s para i-unlock ang Send"
                    true
                }
                else -> false
            }
        }
        
        // Setup Warning Banner
        val bannerContainer = keyboardView.findViewById<FrameLayout>(R.id.warning_banner_container)
        bannerContainer.removeAllViews() // Remove the placeholder text
        bannerContainer.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT // Allow it to expand naturally
        
        warningBanner = WarningBanner(layoutInflater, bannerContainer)
        warningBanner.setOnItagoClickListener {
            val text = getCurrentText()
            val masked = maskSecret(text) ?: DetectionEngine.hide(text, currentWarning)
            restoreText = text
            maskedText = masked
            replaceCurrentText(masked)
            FeedbackManager.notifyWarning(applicationContext, isHighAlert = false)
            val w = currentWarning ?: Warning(WarningType.HIGH, "Itinago ang secret", "Itinago")
            warningBanner.showAutoHidden(w) {
                val original = restoreText
                if (original != null && getCurrentText() == maskedText) {
                    PIN_OTP_REGEX.findAll(original).forEach { m ->
                        approvedSecrets += m.groupValues[3]
                    }
                    PW_VALUE_REGEX.findAll(original).forEach { m ->
                        val v = m.groupValues[3]
                        if (v.lowercase() !in PW_STOPWORDS) {
                            approvedSecrets += v
                        }
                    }
                    replaceCurrentText(original)
                    warningBanner.showRestored()
                    hideSendShield()
                }
                restoreText = null
                maskedText = null
            }
        }
        warningBanner.setOnDismissClickListener {
            currentWarning = null
            hideSendShield()
            updatePangolinState(null)
            Log.d("SecondLookIME", "Warning dismissed by user")
        }

        pangolinStatusDot = keyboardView.findViewById<View>(R.id.pangolin_status_indicator)
        pangolinStatusLabel = keyboardView.findViewById<TextView>(R.id.tv_pangolin_label)

        // Make Tanggolin header badge actively launch the main app when clicked
        val pangolinBtn = keyboardView.findViewById<View>(R.id.btn_pangolin)
        pangolinBtn?.setOnClickListener { v ->
            FeedbackManager.vibrateKeyPress(applicationContext, v)
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                }
            } catch (e: Exception) {
                Log.e("SecondLookIME", "Failed to launch main app from header: ${e.message}")
            }
        }

        setupKeys(keyboardView)
        updateKeyboardUi()
        updateEnterKeyUi(currentInputEditorInfo)
        return keyboardView
    }

    private fun updatePangolinState(warning: Warning?) {
        val dot = pangolinStatusDot ?: return
        val label = pangolinStatusLabel ?: return
        when (warning?.type) {
            WarningType.HIGH -> {
                dot.setBackgroundColor(android.graphics.Color.parseColor("#EF4444"))
                label.text = "Alert!"
            }
            WarningType.MEDIUM -> {
                dot.setBackgroundColor(android.graphics.Color.parseColor("#F59E0B"))
                label.text = "Intended?"
            }
            null -> {
                dot.setBackgroundResource(R.drawable.bg_status_dot)
                label.text = "Tanggolin"
            }
        }
    }

    private fun replaceCurrentText(newText: String) {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        try {
            val before = ic.getTextBeforeCursor(2000, 0)?.length ?: 0
            val after = ic.getTextAfterCursor(2000, 0)?.length ?: 0
            ic.deleteSurroundingText(before, after)
            ic.commitText(newText, 1)
        } finally {
            ic.endBatchEdit()
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        onTextChanged()
    }

    private fun getCurrentText(): String {
        val ic = currentInputConnection ?: return ""
        val before = ic.getTextBeforeCursor(1000, 0)?.toString() ?: ""
        val after = ic.getTextAfterCursor(1000, 0)?.toString() ?: ""
        return before + after
    }

    private fun onTextChanged(immediate: Boolean = false) {
        debounceHandler.removeCallbacks(rulesRunnable)
        debounceHandler.removeCallbacks(debounceRunnable)

        val text = getCurrentText()
        val inMaskedState = maskedText != null

        val delay = when {
            immediate -> 0L
            inMaskedState -> 50L // While already masked, immediately absorb newly typed characters
            text.endsWith(" ") || text.endsWith("\n") || text.endsWith(",") || text.endsWith(".") -> 250L // Word completed
            else -> 1200L // Still actively typing mid-word: allow 1.2s of inactivity before auto-hiding
        }

        if (delay == 0L) {
            rulesRunnable.run()
        } else {
            debounceHandler.postDelayed(rulesRunnable, delay)
        }
        debounceHandler.postDelayed(debounceRunnable, 500)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        deleteRepeatHandler.removeCallbacks(deleteRepeatRunnable)
        debounceHandler.removeCallbacks(rulesRunnable)
        debounceHandler.removeCallbacks(debounceRunnable)
        delButton?.isPressed = false

        currentWarning = null
        restoreText = null
        maskedText = null
        approvedSecrets.clear()
        hideSendShield()
        updatePangolinState(null)
        if (::warningBanner.isInitialized) {
            warningBanner.hide()
        }
        shiftState = ShiftState.UNSHIFTED
        keyboardMode = KeyboardMode.QWERTY
    }

    override fun onFinishInput() {
        super.onFinishInput()
        deleteRepeatHandler.removeCallbacks(deleteRepeatRunnable)
        debounceHandler.removeCallbacks(rulesRunnable)
        debounceHandler.removeCallbacks(debounceRunnable)
        restoreText = null
        maskedText = null
        approvedSecrets.clear()
        hideSendShield()
        if (::warningBanner.isInitialized) {
            warningBanner.cancelHold()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteRepeatHandler.removeCallbacks(deleteRepeatRunnable)
        debounceHandler.removeCallbacks(rulesRunnable)
        debounceHandler.removeCallbacks(debounceRunnable)
        restoreText = null
        maskedText = null
        approvedSecrets.clear()
        if (::warningBanner.isInitialized) {
            warningBanner.cancelHold()
        }
    }

    private fun setupKeys(view: View) {
        row1Buttons.clear()
        row2Buttons.clear()
        row3MiddleButtons.clear()

        // Populate Row 1 (10 keys)
        val row1Layout = view.findViewById<LinearLayout>(R.id.row_1)
        if (row1Layout != null) {
            for (i in 0 until row1Layout.childCount) {
                val child = row1Layout.getChildAt(i)
                if (child is Button) row1Buttons.add(child)
            }
        } else {
            for (key in qwertyRow1) {
                val b = view.findViewWithTag<Button>(key)
                if (b != null) row1Buttons.add(b)
            }
        }

        // Populate Row 2 (9 keys, spaces are skipped)
        val row2Layout = view.findViewById<LinearLayout>(R.id.row_2)
        if (row2Layout != null) {
            for (i in 0 until row2Layout.childCount) {
                val child = row2Layout.getChildAt(i)
                if (child is Button) row2Buttons.add(child)
            }
        } else {
            for (key in qwertyRow2) {
                val b = view.findViewWithTag<Button>(key)
                if (b != null) row2Buttons.add(b)
            }
        }

        // Populate Row 3 (Shift + 7 middle keys + Del)
        val row3Layout = view.findViewById<LinearLayout>(R.id.row_3)
        if (row3Layout != null) {
            val r3Btns = mutableListOf<Button>()
            for (i in 0 until row3Layout.childCount) {
                val child = row3Layout.getChildAt(i)
                if (child is Button) r3Btns.add(child)
            }
            if (r3Btns.isNotEmpty()) {
                shiftButton = r3Btns.first()
                delButton = r3Btns.last()
                if (r3Btns.size > 2) {
                    row3MiddleButtons.addAll(r3Btns.subList(1, r3Btns.size - 1))
                }
            }
        } else {
            for (key in qwertyRow3) {
                val b = view.findViewWithTag<Button>(key)
                if (b != null) row3MiddleButtons.add(b)
            }
        }

        if (shiftButton == null) {
            shiftButton = view.findViewById(R.id.btn_shift) ?: view.findViewWithTag("SHIFT")
        }
        if (delButton == null) {
            delButton = view.findViewById(R.id.btn_del) ?: view.findViewWithTag("DEL")
        }
        modeToggleButton = view.findViewById(R.id.btn_mode_toggle) ?: view.findViewWithTag("?123") ?: view.findViewWithTag("\\?123")

        // Set click listeners on dynamic rows
        row1Buttons.forEachIndexed { index, btn ->
            btn.setOnClickListener { v ->
                FeedbackManager.vibrateKeyPress(applicationContext, v)
                handleKeyClick(index, 1)
            }
        }
        row2Buttons.forEachIndexed { index, btn ->
            btn.setOnClickListener { v ->
                FeedbackManager.vibrateKeyPress(applicationContext, v)
                handleKeyClick(index, 2)
            }
        }
        row3MiddleButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener { v ->
                FeedbackManager.vibrateKeyPress(applicationContext, v)
                handleKeyClick(index, 3)
            }
        }

        // Shift button listener (handles QWERTY Shift/Caps Lock and symbol page toggles)
        shiftButton?.setOnClickListener { v ->
            FeedbackManager.vibrateKeyPress(applicationContext, v)
            when (keyboardMode) {
                KeyboardMode.QWERTY -> handleShiftClick()
                KeyboardMode.SYMBOLS -> {
                    keyboardMode = KeyboardMode.SYMBOLS_ALT
                    updateKeyboardUi()
                }
                KeyboardMode.SYMBOLS_ALT -> {
                    keyboardMode = KeyboardMode.SYMBOLS
                    updateKeyboardUi()
                }
            }
        }
        shiftButton?.setOnLongClickListener { v ->
            FeedbackManager.vibrateKeyPress(applicationContext, v)
            if (keyboardMode == KeyboardMode.QWERTY) {
                shiftState = ShiftState.CAPS_LOCKED
                updateKeyboardUi()
                true
            } else {
                false
            }
        }

        // Mode toggle (?123 / ABC)
        modeToggleButton?.setOnClickListener { v ->
            FeedbackManager.vibrateKeyPress(applicationContext, v)
            keyboardMode = if (keyboardMode == KeyboardMode.QWERTY) {
                KeyboardMode.SYMBOLS
            } else {
                KeyboardMode.QWERTY
            }
            updateKeyboardUi()
        }

        // Continuous backspace repeat on DEL long-press
        delButton?.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    FeedbackManager.vibrateKeyPress(applicationContext, v)
                    handleBackspace()
                    deleteRepeatHandler.removeCallbacks(deleteRepeatRunnable)
                    deleteRepeatHandler.postDelayed(deleteRepeatRunnable, 400)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    deleteRepeatHandler.removeCallbacks(deleteRepeatRunnable)
                    v.performClick()
                    true
                }
                else -> false
            }
        }

        // Punctuation and functional keys
        (view.findViewById<Button>(R.id.btn_comma) ?: view.findViewWithTag<Button>(","))?.setOnClickListener { v ->
            FeedbackManager.vibrateKeyPress(applicationContext, v)
            currentInputConnection?.commitText(",", 1)
            onTextChanged(immediate = true)
        }

        (view.findViewById<Button>(R.id.btn_period) ?: view.findViewWithTag<Button>("."))?.setOnClickListener { v ->
            FeedbackManager.vibrateKeyPress(applicationContext, v)
            currentInputConnection?.commitText(".", 1)
            onTextChanged(immediate = true)
        }

        (view.findViewById<Button>(R.id.btn_apostrophe) ?: view.findViewWithTag<Button>("'"))?.setOnClickListener { v ->
            FeedbackManager.vibrateKeyPress(applicationContext, v)
            currentInputConnection?.commitText("'", 1)
            onTextChanged(immediate = true)
        }

        (view.findViewById<Button>(R.id.btn_space) ?: view.findViewWithTag<Button>("SPACE"))?.setOnClickListener { v ->
            FeedbackManager.vibrateKeyPress(applicationContext, v)
            currentInputConnection?.commitText(" ", 1)
            onTextChanged(immediate = true)
        }

        enterButton = view.findViewById<Button>(R.id.btn_enter) ?: view.findViewWithTag<Button>("ENTER")
        enterButton?.setOnClickListener { v ->
            FeedbackManager.vibrateKeyPress(applicationContext, v)
            handleEnterClick()
        }
        updateEnterKeyUi(currentInputEditorInfo)
    }

    private fun handleKeyClick(index: Int, row: Int) {
        val charToCommit = when (keyboardMode) {
            KeyboardMode.QWERTY -> {
                val base = when (row) {
                    1 -> qwertyRow1.getOrNull(index) ?: ""
                    2 -> qwertyRow2.getOrNull(index) ?: ""
                    3 -> qwertyRow3.getOrNull(index) ?: ""
                    else -> ""
                }
                if (shiftState != ShiftState.UNSHIFTED) base.uppercase() else base.lowercase()
            }
            KeyboardMode.SYMBOLS -> {
                when (row) {
                    1 -> symbolsRow1.getOrNull(index) ?: ""
                    2 -> symbolsRow2.getOrNull(index) ?: ""
                    3 -> symbolsRow3.getOrNull(index) ?: ""
                    else -> ""
                }
            }
            KeyboardMode.SYMBOLS_ALT -> {
                when (row) {
                    1 -> symbolsAltRow1.getOrNull(index) ?: ""
                    2 -> symbolsAltRow2.getOrNull(index) ?: ""
                    3 -> symbolsAltRow3.getOrNull(index) ?: ""
                    else -> ""
                }
            }
        }
        if (charToCommit.isNotEmpty()) {
            currentInputConnection?.commitText(charToCommit, 1)
            if (keyboardMode == KeyboardMode.QWERTY && shiftState == ShiftState.SHIFTED) {
                shiftState = ShiftState.UNSHIFTED
                updateKeyboardUi()
            }
            onTextChanged()
        }
    }

    // Layer 3: Chat apps list for Enter key hold interception
    private val chatApps = setOf(
        "com.facebook.orca",                 // Messenger
        "com.facebook.katana",               // Facebook
        "com.facebook.lite",                 // FB Lite / Messenger Lite
        "com.google.android.apps.messaging", // Google Messages (SMS)
        "com.whatsapp",
        "com.whatsapp.w4b",                  // WhatsApp Business
        "org.telegram.messenger",
        "org.telegram.plus",
        "com.viber.voip",
        "com.instagram.android",             // Instagram Direct
        "com.discord",                       // Discord
        "com.tencent.mm",                    // WeChat
        "jp.naver.line.android",             // LINE
        "com.skype.raider"                   // Skype
    )

    private fun handleEnterClick() {
        debounceHandler.removeCallbacks(rulesRunnable)
        debounceHandler.removeCallbacks(debounceRunnable)

        // If intercept banner is already active, prompt user with hold vibration
        if (::warningBanner.isInitialized && warningBanner.isInterceptActive()) {
            warningBanner.showHoldPrompt()
            return
        }

        val draft = getCurrentText()
        val warning = DetectionEngine.detectSync(draft)
        currentWarning = warning
        Log.d("SecondLookIME", "Final sync check on Send/Enter: ${warning?.type} - ${warning?.headline}")

        val actionInfo = getEnterActionInfo(currentInputEditorInfo)
        val isChatApp = currentInputEditorInfo?.packageName in chatApps
        val isSendAction = actionInfo.isSendOrSubmit || actionInfo.targetAction == EditorInfo.IME_ACTION_SEND || isChatApp

        // Intercept if Red Warning (High Alert) is active on Send or inside a chat app
        if (warning?.type == WarningType.HIGH && isSendAction) {
            val isIngatMode = SettingsManager.isIngatModeEnabled(this)
            Log.w("SecondLookIME", "Intercepted Send action in ${currentInputEditorInfo?.packageName} (IngatMode=$isIngatMode)")
            FeedbackManager.notifyWarning(applicationContext, isHighAlert = true)
            if (::warningBanner.isInitialized) {
                warningBanner.showSendIntercept(warning, isIngatMode) {
                    Log.i("SecondLookIME", "User authorized send. Dispatching held action.")
                    currentWarning = null
                    dispatchAction(actionInfo)
                }
            }
            return
        }

        // Normal send dispatch
        if (warning != null && ::warningBanner.isInitialized) {
            warningBanner.showWarning(warning)
        }

        dispatchAction(actionInfo)
    }

    private fun dispatchAction(actionInfo: EnterActionInfo) {
        val ic = currentInputConnection ?: return
        if (actionInfo.isNewLine) {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
            if (!ic.sendKeyEvent(eventDown) || !ic.sendKeyEvent(eventUp)) {
                ic.commitText("\n", 1)
            }
        } else if (actionInfo.targetAction != EditorInfo.IME_ACTION_NONE && actionInfo.targetAction != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionInfo.targetAction)
        } else {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
            ic.sendKeyEvent(eventDown)
            ic.sendKeyEvent(eventUp)
        }
    }

    private fun getEnterActionInfo(info: EditorInfo?): EnterActionInfo {
        if (info == null) {
            return EnterActionInfo("↵", EditorInfo.IME_ACTION_NONE, isSendOrSubmit = false, isNewLine = true)
        }

        val inputType = info.inputType
        val isMultiLine = (inputType and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        val imeOptions = info.imeOptions
        val hasNoEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        val effectiveActionId = if (info.actionId != 0) info.actionId else action

        val customLabel = info.actionLabel?.toString()?.trim()
        if (!customLabel.isNullOrEmpty() && !hasNoEnterAction) {
            return EnterActionInfo(
                label = customLabel.uppercase(),
                targetAction = effectiveActionId,
                isSendOrSubmit = true,
                isNewLine = false
            )
        }

        if (hasNoEnterAction || (isMultiLine && (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED))) {
            return EnterActionInfo(
                label = "↵",
                targetAction = EditorInfo.IME_ACTION_NONE,
                isSendOrSubmit = false,
                isNewLine = true
            )
        }

        return when (action) {
            EditorInfo.IME_ACTION_SEND -> EnterActionInfo("SEND", effectiveActionId, isSendOrSubmit = true, isNewLine = false)
            EditorInfo.IME_ACTION_GO -> EnterActionInfo("GO", effectiveActionId, isSendOrSubmit = true, isNewLine = false)
            EditorInfo.IME_ACTION_SEARCH -> EnterActionInfo("SEARCH", effectiveActionId, isSendOrSubmit = true, isNewLine = false)
            EditorInfo.IME_ACTION_DONE -> EnterActionInfo("DONE", effectiveActionId, isSendOrSubmit = false, isNewLine = false)
            EditorInfo.IME_ACTION_NEXT -> EnterActionInfo("NEXT", effectiveActionId, isSendOrSubmit = false, isNewLine = false)
            else -> {
                if (isMultiLine) {
                    EnterActionInfo("↵", EditorInfo.IME_ACTION_NONE, isSendOrSubmit = false, isNewLine = true)
                } else {
                    EnterActionInfo("↵", effectiveActionId, isSendOrSubmit = false, isNewLine = false)
                }
            }
        }
    }

    private fun updateEnterKeyUi(info: EditorInfo?) {
        val btn = enterButton ?: return
        val actionInfo = getEnterActionInfo(info)
        btn.text = actionInfo.label
        when {
            actionInfo.label.length > 4 -> btn.textSize = 12f
            actionInfo.label.length > 2 -> btn.textSize = 13f
            actionInfo.label == "GO" -> btn.textSize = 15f
            actionInfo.label == "↵" || actionInfo.label == "⏎" -> btn.textSize = 28f
            else -> btn.textSize = 26f
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateEnterKeyUi(info)
        if (!restarting) {
            shiftState = ShiftState.UNSHIFTED
            keyboardMode = KeyboardMode.QWERTY
            updateKeyboardUi()
            currentWarning = null
            updatePangolinState(null)
            if (::warningBanner.isInitialized) {
                warningBanner.hide()
            }
        }
        onTextChanged()
    }

    private fun handleShiftClick() {
        val now = System.currentTimeMillis()
        shiftState = when (shiftState) {
            ShiftState.UNSHIFTED -> {
                lastShiftTapTime = now
                ShiftState.SHIFTED
            }
            ShiftState.SHIFTED -> {
                if (now - lastShiftTapTime < 500) {
                    ShiftState.CAPS_LOCKED
                } else {
                    ShiftState.UNSHIFTED
                }
            }
            ShiftState.CAPS_LOCKED -> {
                ShiftState.UNSHIFTED
            }
        }
        updateKeyboardUi()
    }

    private fun updateKeyboardUi() {
        val isUpper = shiftState != ShiftState.UNSHIFTED

        // Row 1
        val r1 = when (keyboardMode) {
            KeyboardMode.QWERTY -> if (isUpper) qwertyRow1.map { it.uppercase() } else qwertyRow1
            KeyboardMode.SYMBOLS -> symbolsRow1
            KeyboardMode.SYMBOLS_ALT -> symbolsAltRow1
        }
        row1Buttons.forEachIndexed { idx, btn ->
            if (idx < r1.size) btn.text = r1[idx]
        }

        // Row 2
        val r2 = when (keyboardMode) {
            KeyboardMode.QWERTY -> if (isUpper) qwertyRow2.map { it.uppercase() } else qwertyRow2
            KeyboardMode.SYMBOLS -> symbolsRow2
            KeyboardMode.SYMBOLS_ALT -> symbolsAltRow2
        }
        row2Buttons.forEachIndexed { idx, btn ->
            if (idx < r2.size) btn.text = r2[idx]
        }

        // Row 3 middle
        val r3 = when (keyboardMode) {
            KeyboardMode.QWERTY -> if (isUpper) qwertyRow3.map { it.uppercase() } else qwertyRow3
            KeyboardMode.SYMBOLS -> symbolsRow3
            KeyboardMode.SYMBOLS_ALT -> symbolsAltRow3
        }
        row3MiddleButtons.forEachIndexed { idx, btn ->
            if (idx < r3.size) btn.text = r3[idx]
        }

        // Row 3 Left (Shift / Symbol Toggle)
        val sBtn = shiftButton
        if (sBtn != null) {
            when (keyboardMode) {
                KeyboardMode.QWERTY -> {
                    sBtn.textSize = 26f
                    when (shiftState) {
                        ShiftState.UNSHIFTED -> {
                            sBtn.text = "⇧"
                            sBtn.setTextColor(android.graphics.Color.parseColor("#1E293B"))
                            sBtn.setBackgroundResource(R.drawable.bg_keyboard_key_functional)
                        }
                        ShiftState.SHIFTED -> {
                            sBtn.text = "⇧"
                            sBtn.setTextColor(android.graphics.Color.parseColor("#7C3AED"))
                            sBtn.setBackgroundResource(R.drawable.bg_keyboard_key_functional)
                        }
                        ShiftState.CAPS_LOCKED -> {
                            sBtn.text = "⇪"
                            sBtn.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                            sBtn.setBackgroundResource(R.drawable.bg_keyboard_key_action)
                        }
                    }
                }
                KeyboardMode.SYMBOLS -> {
                    sBtn.textSize = 14f
                    sBtn.text = "=\\<"
                    sBtn.setTextColor(android.graphics.Color.parseColor("#1E293B"))
                    sBtn.setBackgroundResource(R.drawable.bg_keyboard_key_functional)
                }
                KeyboardMode.SYMBOLS_ALT -> {
                    sBtn.textSize = 14f
                    sBtn.text = "?123"
                    sBtn.setTextColor(android.graphics.Color.parseColor("#1E293B"))
                    sBtn.setBackgroundResource(R.drawable.bg_keyboard_key_functional)
                }
            }
        }

        // Mode Toggle Button (?123 / ABC)
        val mBtn = modeToggleButton
        if (mBtn != null) {
            mBtn.text = if (keyboardMode == KeyboardMode.QWERTY) "?123" else "ABC"
        }
    }
}
