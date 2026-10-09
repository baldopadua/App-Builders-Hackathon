package com.secondlook

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
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
    private val debounceRunnable = Runnable {
        val text = getCurrentText()
        Log.d("SecondLookIME", "500ms pause reached, analyzing draft: $text")
        DetectionEngine.detectAsync(text) { warning ->
            Log.d("SecondLookIME", "DetectionEngine result: $warning")
            currentWarning = warning
            if (::warningBanner.isInitialized) {
                if (warning != null) {
                    warningBanner.showWarning(warning)
                } else {
                    warningBanner.hide()
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

    private var shiftButton: Button? = null
    private var delButton: Button? = null
    private var modeToggleButton: Button? = null

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
            currentInputConnection?.deleteSurroundingText(1, 0)
            onTextChanged()
            deleteRepeatHandler.postDelayed(this, 50)
        }
    }

    private var pangolinStatusDot: View? = null
    private var pangolinStatusLabel: TextView? = null

    override fun onCreateInputView(): View {
        val keyboardView = layoutInflater.inflate(R.layout.keyboard_layout, null)
        
        // Setup Warning Banner
        val bannerContainer = keyboardView.findViewById<FrameLayout>(R.id.warning_banner_container)
        bannerContainer.removeAllViews() // Remove the placeholder text
        bannerContainer.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT // Allow it to expand naturally
        
        warningBanner = WarningBanner(layoutInflater, bannerContainer)
        warningBanner.setOnItagoClickListener {
            val text = getCurrentText()
            val masked = DetectionEngine.hide(text, currentWarning)
            replaceCurrentText(masked)
            warningBanner.hide()
            currentWarning = null
            updatePangolinState(null)
        }
        warningBanner.setOnDismissClickListener {
            currentWarning = null
            updatePangolinState(null)
            Log.d("SecondLookIME", "Warning dismissed by user")
        }

        pangolinStatusDot = keyboardView.findViewById<View>(R.id.pangolin_status_indicator)
        pangolinStatusLabel = keyboardView.findViewById<TextView>(R.id.tv_pangolin_label)

        setupKeys(keyboardView)
        updateKeyboardUi()
        return keyboardView
    }

    private fun updatePangolinState(warning: Warning?) {
        val dot = pangolinStatusDot ?: return
        val label = pangolinStatusLabel ?: return
        when (warning?.type) {
            WarningType.HIGH -> {
                dot.setBackgroundColor(android.graphics.Color.parseColor("#EF4444"))
                label.text = "Ingat!"
            }
            WarningType.MEDIUM -> {
                dot.setBackgroundColor(android.graphics.Color.parseColor("#F59E0B"))
                label.text = "Intended?"
            }
            null -> {
                dot.setBackgroundResource(R.drawable.bg_status_dot)
                label.text = "SecondLook"
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

    private fun onTextChanged() {
        debounceHandler.removeCallbacks(debounceRunnable)
        debounceHandler.postDelayed(debounceRunnable, 500)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        deleteRepeatHandler.removeCallbacks(deleteRepeatRunnable)
        debounceHandler.removeCallbacks(debounceRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteRepeatHandler.removeCallbacks(deleteRepeatRunnable)
        debounceHandler.removeCallbacks(debounceRunnable)
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
            btn.setOnClickListener { handleKeyClick(index, 1) }
        }
        row2Buttons.forEachIndexed { index, btn ->
            btn.setOnClickListener { handleKeyClick(index, 2) }
        }
        row3MiddleButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener { handleKeyClick(index, 3) }
        }

        // Shift button listener (handles QWERTY Shift/Caps Lock and symbol page toggles)
        shiftButton?.setOnClickListener {
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
        shiftButton?.setOnLongClickListener {
            if (keyboardMode == KeyboardMode.QWERTY) {
                shiftState = ShiftState.CAPS_LOCKED
                updateKeyboardUi()
                true
            } else {
                false
            }
        }

        // Mode toggle (?123 / ABC)
        modeToggleButton?.setOnClickListener {
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
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    onTextChanged()
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
        (view.findViewById<Button>(R.id.btn_comma) ?: view.findViewWithTag<Button>(","))?.setOnClickListener {
            currentInputConnection?.commitText(",", 1)
            onTextChanged()
        }

        (view.findViewById<Button>(R.id.btn_period) ?: view.findViewWithTag<Button>("."))?.setOnClickListener {
            currentInputConnection?.commitText(".", 1)
            onTextChanged()
        }

        (view.findViewById<Button>(R.id.btn_space) ?: view.findViewWithTag<Button>("SPACE"))?.setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
            onTextChanged()
        }

        (view.findViewById<Button>(R.id.btn_enter) ?: view.findViewWithTag<Button>("ENTER"))?.setOnClickListener {
            debounceHandler.removeCallbacks(debounceRunnable)
            val draft = getCurrentText()
            val warning = DetectionEngine.detectSync(draft)
            currentWarning = warning
            Log.d("SecondLookIME", "Final sync check on Send/Enter: $warning")

            val editorInfo = currentInputEditorInfo
            val imeAction = (editorInfo?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
            val actionId = editorInfo?.actionId ?: 0
            val isSendAction = (imeAction == EditorInfo.IME_ACTION_SEND) || (actionId == EditorInfo.IME_ACTION_SEND)

            val targetAction = if (actionId != 0) actionId else imeAction

            // Intercept if Red Warning (High Alert) is active: Hold send and transform banner UI
            if (warning?.type == WarningType.HIGH) {
                Log.w("SecondLookIME", "Intercepted Send action! Transforming UI to [Itago] [Review] [Send anyway]")
                if (::warningBanner.isInitialized) {
                    warningBanner.showSendIntercept(warning) {
                        Log.i("SecondLookIME", "User selected [Send anyway]. Dispatching held send action.")
                        currentWarning = null
                        dispatchSend(targetAction, isSendAction)
                    }
                }
                return@setOnClickListener // Hold/Block the send event
            }

            // Normal send dispatch
            if (warning != null && ::warningBanner.isInitialized) {
                warningBanner.showWarning(warning)
            }

            dispatchSend(targetAction, isSendAction)
        }
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

    private fun dispatchSend(targetAction: Int, isSendAction: Boolean) {
        val ic = currentInputConnection ?: return
        if (isSendAction || (targetAction != EditorInfo.IME_ACTION_NONE && targetAction != EditorInfo.IME_ACTION_UNSPECIFIED)) {
            ic.performEditorAction(targetAction)
        } else {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
            ic.sendKeyEvent(eventDown)
            ic.sendKeyEvent(eventUp)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!restarting) {
            shiftState = ShiftState.UNSHIFTED
            keyboardMode = KeyboardMode.QWERTY
            updateKeyboardUi()
        }
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
                    sBtn.text = "=\\<"
                    sBtn.setTextColor(android.graphics.Color.parseColor("#1E293B"))
                    sBtn.setBackgroundResource(R.drawable.bg_keyboard_key_functional)
                }
                KeyboardMode.SYMBOLS_ALT -> {
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
