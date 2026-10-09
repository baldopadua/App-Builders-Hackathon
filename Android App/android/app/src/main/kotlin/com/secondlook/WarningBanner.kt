package com.secondlook

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.secondlook.secondlook.R

enum class WarningType {
    HIGH,
    MEDIUM
}

data class Warning(
    val type: WarningType,
    val headline: String,
    val tip: String
)

class WarningBanner(inflater: LayoutInflater, parent: ViewGroup) {
    
    // Inflate the warning banner XML directly into the parent container
    val view: View = inflater.inflate(R.layout.warning_banner, parent, true)

    private val container: View = view.findViewById(R.id.banner_container)
    private val headlineText: TextView = view.findViewById(R.id.banner_headline)
    private val tipText: TextView = view.findViewById(R.id.banner_tip)
    private val bannerIcon: ImageView? = view.findViewById(R.id.banner_icon)
    private val bannerBadge: TextView? = view.findViewById(R.id.banner_badge)
    private val btnItago: Button = view.findViewById(R.id.btn_itago)
    private val btnReview: Button = view.findViewById(R.id.btn_review)
    private val btnDismiss: Button = view.findViewById(R.id.btn_dismiss)

    private val holdHandler = Handler(Looper.getMainLooper())
    private var isHolding = false
    private var holdStartTime = 0L
    private val holdDurationMs = 2000L
    private var isInterceptActive = false

    init {
        // Start hidden by default
        container.visibility = View.GONE
    }

    private var onDismissListener: (() -> Unit)? = null
    private var onReviewListener: (() -> Unit)? = null

    fun showWarning(warning: Warning) {
        cancelHold()
        isInterceptActive = false
        headlineText.text = warning.headline
        tipText.text = warning.tip
        btnDismiss.setOnTouchListener(null)
        btnDismiss.text = "Dismiss"
        btnDismiss.setOnClickListener {
            hide()
            onDismissListener?.invoke()
        }

        when (warning.type) {
            WarningType.HIGH -> applyHighAlertStyle()
            WarningType.MEDIUM -> applyMediumAlertStyle()
        }

        if (container.visibility != View.VISIBLE) {
            container.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(container.context, R.anim.banner_slide_up)
            container.startAnimation(anim)
        }
    }

    /**
     * Transforms the banner UI for intercepted Send events: [Itago] [Review] [Send anyway]
     * In Ingat Mode, requires a 2-second press & hold to prevent accidental sends.
     */
    fun showSendIntercept(warning: Warning, isIngatMode: Boolean = true, onSendAnyway: () -> Unit) {
        cancelHold()
        isInterceptActive = true
        headlineText.text = "Ipadala pa rin? ${warning.headline}"
        applyHighAlertStyle()

        if (isIngatMode) {
            tipText.text = "Ingat Mode: Idiin ang 'Hold 2s to send' nang 2 segundo."
            btnDismiss.text = "Hold 2s to send"

            val holdProgressRunnable = object : Runnable {
                override fun run() {
                    if (!isHolding) return
                    val elapsed = System.currentTimeMillis() - holdStartTime
                    val remaining = ((holdDurationMs - elapsed + 999) / 1000).coerceAtLeast(0).toInt()
                    if (elapsed >= holdDurationMs) {
                        isHolding = false
                        btnDismiss.text = "Pinadala!"
                        btnDismiss.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        hide()
                        onSendAnyway()
                    } else {
                        btnDismiss.text = "Hold (${remaining}s)..."
                        holdHandler.postDelayed(this, 100)
                    }
                }
            }

            btnDismiss.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isHolding = true
                        holdStartTime = System.currentTimeMillis()
                        btnDismiss.text = "Hold (2s)..."
                        btnDismiss.isPressed = true
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        holdHandler.removeCallbacks(holdProgressRunnable)
                        holdHandler.postDelayed(holdProgressRunnable, 100)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (isHolding) {
                            val x = event.x
                            val y = event.y
                            if (x < 0 || x > v.width || y < 0 || y > v.height) {
                                isHolding = false
                                holdHandler.removeCallbacks(holdProgressRunnable)
                                btnDismiss.isPressed = false
                                btnDismiss.text = "Hold 2s to send"
                                tipText.text = "Kinansela ang pagpadala. Protektado ang iyong impormasyon."
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isHolding) {
                            isHolding = false
                            holdHandler.removeCallbacks(holdProgressRunnable)
                            btnDismiss.isPressed = false
                            btnDismiss.text = "Hold 2s to send"
                            tipText.text = "Kinansela ang pagpadala. Protektado ang iyong impormasyon."
                        }
                        true
                    }
                    else -> false
                }
            }
        } else {
            tipText.text = warning.tip
            btnDismiss.setOnTouchListener(null)
            btnDismiss.text = "Send anyway"
            btnDismiss.setOnClickListener {
                hide()
                onSendAnyway()
            }
        }

        btnReview.visibility = View.VISIBLE
        btnReview.text = "Review"
        btnReview.setOnClickListener {
            hide()
            onReviewListener?.invoke()
        }

        if (container.visibility != View.VISIBLE) {
            container.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(container.context, R.anim.banner_slide_up)
            container.startAnimation(anim)
        }
    }

    fun isInterceptActive(): Boolean = isInterceptActive

    fun showHoldPrompt() {
        if (isInterceptActive) {
            headlineText.text = "Teka! Ingat Mode is active."
            tipText.text = "Idiin ang 'Hold 2s to send' nang buong 2 segundo para ipadala."
            btnDismiss.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            val anim = AnimationUtils.loadAnimation(container.context, R.anim.banner_slide_up)
            container.startAnimation(anim)
        }
    }

    fun cancelHold() {
        isHolding = false
        holdHandler.removeCallbacksAndMessages(null)
    }

    fun hide() {
        cancelHold()
        isInterceptActive = false
        if (container.visibility == View.VISIBLE) {
            val anim = AnimationUtils.loadAnimation(container.context, R.anim.banner_slide_down)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    container.visibility = View.GONE
                }
            })
            container.startAnimation(anim)
        } else {
            container.visibility = View.GONE
        }
        btnDismiss.text = "Dismiss"
        btnDismiss.setOnTouchListener(null)
    }

    fun setOnItagoClickListener(listener: () -> Unit) {
        btnItago.setOnClickListener { listener() }
    }

    fun setOnReviewClickListener(listener: () -> Unit) {
        onReviewListener = listener
        btnReview.setOnClickListener {
            hide()
            listener()
        }
    }

    fun setOnDismissClickListener(listener: () -> Unit) {
        onDismissListener = listener
        btnDismiss.setOnClickListener {
            hide()
            listener()
        }
    }

    private fun applyHighAlertStyle() {
        container.setBackgroundResource(R.drawable.bg_warning_banner_high)
        headlineText.setTextColor(Color.parseColor("#991B1B"))
        tipText.setTextColor(Color.parseColor("#7F1D1D"))
        bannerIcon?.setImageResource(R.drawable.ic_warning_high)
        bannerBadge?.text = "BABALA"
        bannerBadge?.setTextColor(Color.parseColor("#991B1B"))
        bannerBadge?.setBackgroundResource(R.drawable.bg_badge_high)
        btnItago.setBackgroundResource(R.drawable.bg_button_itago_high)
        btnItago.text = "Itago"
        btnItago.backgroundTintList = null
        btnReview.setTextColor(Color.parseColor("#991B1B"))
        btnDismiss.setTextColor(Color.parseColor("#991B1B"))
    }

    private fun applyMediumAlertStyle() {
        container.setBackgroundResource(R.drawable.bg_warning_banner_medium)
        headlineText.setTextColor(Color.parseColor("#854D0E"))
        tipText.setTextColor(Color.parseColor("#78350F"))
        bannerIcon?.setImageResource(R.drawable.ic_warning_medium)
        bannerBadge?.text = "PANSIN · INTENDED?"
        bannerBadge?.setTextColor(Color.parseColor("#854D0E"))
        bannerBadge?.setBackgroundResource(R.drawable.bg_badge_medium)
        btnItago.setBackgroundResource(R.drawable.bg_button_itago_medium)
        btnItago.text = "Itago"
        btnItago.backgroundTintList = null
        btnReview.setTextColor(Color.parseColor("#854D0E"))
        btnDismiss.setTextColor(Color.parseColor("#854D0E"))
    }
}
