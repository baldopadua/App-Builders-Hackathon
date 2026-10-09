package com.secondlook

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.FrameLayout
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
        }
    }

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
        }
        warningBanner.setOnDismissClickListener {
            currentWarning = null
            Log.d("SecondLookIME", "Warning dismissed by user")
        }

        setupKeys(keyboardView)
        return keyboardView
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
        
        for (key in keys) {
            view.findViewWithTag<Button>(key)?.setOnClickListener {
                currentInputConnection?.commitText(key, 1)
                onTextChanged()
            }
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
}
