package com.secondlook

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.secondlook.secondlook.R

class SecondLookIME : InputMethodService() {

    private lateinit var warningBanner: WarningBanner

    override fun onCreateInputView(): View {
        val keyboardView = layoutInflater.inflate(R.layout.keyboard_layout, null)
        
        // Setup Warning Banner
        val bannerContainer = keyboardView.findViewById<FrameLayout>(R.id.warning_banner_container)
        bannerContainer.removeAllViews() // Remove the placeholder text
        bannerContainer.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT // Allow it to expand naturally
        
        warningBanner = WarningBanner(layoutInflater, bannerContainer)

        setupKeys(keyboardView)
        return keyboardView
    }

    private fun setupKeys(view: View) {
        val keys = listOf(
            "q", "w", "e", "r", "t", "y", "u", "i", "o", "p",
            "a", "s", "d", "f", "g", "h", "j", "k", "l",
            "z", "x", "c", "v", "b", "n", "m"
        )
        
        for (key in keys) {
            view.findViewWithTag<Button>(key)?.setOnClickListener {
                if (key == "z") {
                    // Temporary test trigger
                    warningBanner.showWarning(
                        Warning(
                            type = WarningType.HIGH,
                            headline = "Teka! OTP o PIN ito — huwag ibigay kahit kanino.",
                            tip = "This looks like a verification code. Sharing it could let someone into your account."
                        )
                    )
                }
                currentInputConnection?.commitText(key, 1)
            }
        }

        view.findViewWithTag<Button>("DEL")?.setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }

        view.findViewWithTag<Button>("SPACE")?.setOnClickListener {
            currentInputConnection?.commitText(" ", 1)
        }

        view.findViewWithTag<Button>("ENTER")?.setOnClickListener {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
            currentInputConnection?.sendKeyEvent(eventDown)
            currentInputConnection?.sendKeyEvent(eventUp)
        }
    }
}
