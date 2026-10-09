package com.secondlook

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
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

    private var isShifted = false
    private val letterButtons = mutableListOf<Button>()
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

    override fun onDestroy() {
        super.onDestroy()
        debounceHandler.removeCallbacks(debounceRunnable)
    }

    private fun setupKeys(view: View) {
        val keys = listOf(
            "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
            "a", "s", "d", "f", "g", "h", "j", "k", "l",
            "z", "x", "c", "v", "b", "n", "m"
        )
        
        letterButtons.clear()
        for (key in keys) {
            val btn = view.findViewWithTag<Button>(key)
            if (btn != null) {
                letterButtons.add(btn)
                btn.setOnClickListener {
                    val charToCommit = if (isShifted) key.uppercase() else key
                    currentInputConnection?.commitText(charToCommit, 1)
                    if (isShifted) {
                        isShifted = false
                        updateShiftUi(view)
                    }
                    onTextChanged()
                }
            }
        }

        view.findViewWithTag<Button>("SHIFT")?.setOnClickListener {
            isShifted = !isShifted
            updateShiftUi(view)
        }

        view.findViewWithTag<Button>(",")?.setOnClickListener {
            currentInputConnection?.commitText(",", 1)
            onTextChanged()
        }

        view.findViewWithTag<Button>(".")?.setOnClickListener {
            currentInputConnection?.commitText(".", 1)
            onTextChanged()
        }

        view.findViewWithTag<Button>("DEL")?.setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
            onTextChanged()
        }

        view.findViewWithTag<Button>("SPACE")?.setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
            onTextChanged()
        }

        view.findViewWithTag<Button>("ENTER")?.setOnClickListener {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
            currentInputConnection?.sendKeyEvent(eventDown)
            currentInputConnection?.sendKeyEvent(eventUp)
            onTextChanged()
        }
    }

    private fun updateShiftUi(view: View) {
        for (btn in letterButtons) {
            val tag = btn.tag?.toString() ?: continue
            btn.text = if (isShifted) tag.uppercase() else tag.lowercase()
        }
        val shiftBtn = view.findViewWithTag<Button>("SHIFT")
        if (isShifted) {
            shiftBtn?.setTextColor(android.graphics.Color.parseColor("#7C3AED"))
        } else {
            shiftBtn?.setTextColor(android.graphics.Color.parseColor("#1E293B"))
        }
    }
}
