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
    val tip: String,
    val category: String = "Data protection",
    val matchedText: String = "",
    val start: Int = 0,
    val end: Int = 0,
    val canHide: Boolean = true
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
    private var isInterceptActive = false

    init {
        // Start hidden by default
        container.visibility = View.GONE
    }

    private var onDismissListener: (() -> Unit)? = null
    private var onReviewListener: (() -> Unit)? = null
    private var onItagoListener: (() -> Unit)? = null

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

        // Restore Itago button configuration cleanly
        btnItago.text = "Itago"
        btnItago.setOnTouchListener(null)
        btnItago.setOnClickListener { onItagoListener?.invoke() }
        btnItago.visibility = if (warning.canHide) View.VISIBLE else View.GONE

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
     * Transforms the banner for Auto-Itago state:
     * Explains secret is masked, offers [Ibalik (idiin 2s)] with 2-second press hold.
     */
    fun showAutoHidden(warning: Warning, onRestore: () -> Unit) {
        cancelHold()
        isInterceptActive = false
        applyHighAlertStyle()

        val isOtp = warning.headline.contains("OTP", ignoreCase = true) || 
                    warning.tip.contains("OTP", ignoreCase = true)
        val isPassword = warning.headline.contains("password", ignoreCase = true) ||
                         warning.tip.contains("password", ignoreCase = true)
        val secretName = when {
            isOtp -> "OTP"
            isPassword -> "password"
            else -> "PIN"
        }
        headlineText.text = "Itinago ko muna ang $secretName mo."
        tipText.text = "Kahit pindutin mo ang Send ng app, •••• lang ang maipapadala."

        // Configure Ibalik button with 2s hold requirement
        btnItago.visibility = View.VISIBLE
        btnItago.text = "Ibalik (idiin 2s)"
        btnItago.setOnClickListener(null)
        btnItago.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    btnItago.text = "Idiin pa (2s)..."
                    holdHandler.postDelayed({
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        btnItago.text = "Ibalik (idiin 2s)"
                        onRestore()
                    }, 2000)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    holdHandler.removeCallbacksAndMessages(null)
                    btnItago.text = "Ibalik (idiin 2s)"
                    true
                }
                else -> false
            }
        }

        // Configure OK button to dismiss banner
        btnDismiss.visibility = View.VISIBLE
        btnDismiss.text = "OK"
        btnDismiss.setOnTouchListener(null)
        btnDismiss.setOnClickListener {
            hide()
            onDismissListener?.invoke()
        }

        btnReview.visibility = View.GONE

        if (container.visibility != View.VISIBLE) {
            container.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(container.context, R.anim.banner_slide_up)
            container.startAnimation(anim)
        }
    }

    /**
     * Shows confirmation that secret was restored to original draft text.
     */
    fun showRestored() {
        cancelHold()
        isInterceptActive = false
        applyMediumAlertStyle()

        headlineText.text = "Naibalik na."
        tipText.text = "Pindutin ang Send ng app kung sigurado ka. Ingat sa pagpapadala."

        btnItago.visibility = View.GONE
        btnItago.setOnTouchListener(null)
        btnReview.visibility = View.GONE

        btnDismiss.visibility = View.VISIBLE
        btnDismiss.text = "OK"
        btnDismiss.setOnTouchListener(null)
        btnDismiss.setOnClickListener {
            hide()
            onDismissListener?.invoke()
        }

        if (container.visibility != View.VISIBLE) {
            container.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(container.context, R.anim.banner_slide_up)
            container.startAnimation(anim)
        }
    }

    /**
     * Transforms the banner UI for intercepted Send events: [Review] [Send anyway / 2s hold]
     */
    fun showSendIntercept(warning: Warning, isIngatMode: Boolean = true, onSendAnyway: () -> Unit) {
        cancelHold()
        isInterceptActive = true
        headlineText.text = "Babala! I-send pa rin ba?"
        applyHighAlertStyle()

        tipText.text = warning.tip

        btnReview.visibility = View.VISIBLE
        btnReview.text = "Review"
        btnReview.setOnClickListener {
            hide()
            onReviewListener?.invoke()
        }

        btnItago.visibility = View.GONE
        btnItago.setOnTouchListener(null)

        btnDismiss.visibility = View.VISIBLE
        if (isIngatMode) {
            btnDismiss.text = "I-send (idiin 2s)"
            btnDismiss.setOnClickListener(null)
            btnDismiss.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        btnDismiss.text = "Idiin pa..."
                        holdHandler.postDelayed({
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            hide()
                            onSendAnyway()
                        }, 2000)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        holdHandler.removeCallbacksAndMessages(null)
                        btnDismiss.text = "I-send (idiin 2s)"
                        true
                    }
                    else -> false
                }
            }
        } else {
            btnDismiss.text = "Send anyway"
            btnDismiss.setOnTouchListener(null)
            btnDismiss.setOnClickListener {
                hide()
                onSendAnyway()
            }
        }

        if (container.visibility != View.VISIBLE) {
            container.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(container.context, R.anim.banner_slide_up)
            container.startAnimation(anim)
        }
    }

    fun showHoldPrompt() {
        btnDismiss.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val anim = AnimationUtils.loadAnimation(container.context, R.anim.banner_slide_up)
        container.startAnimation(anim)
    }

    fun isInterceptActive(): Boolean = isInterceptActive

    fun cancelHold() {
        isInterceptActive = false
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
        btnItago.setOnTouchListener(null)
        btnItago.text = "Itago"
        btnItago.setOnClickListener { onItagoListener?.invoke() }
    }

    fun setOnItagoClickListener(listener: () -> Unit) {
        onItagoListener = listener
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
        bannerBadge?.text = "ALERT"
        bannerBadge?.setTextColor(Color.parseColor("#991B1B"))
        bannerBadge?.setBackgroundResource(R.drawable.bg_badge_high)
        btnItago.setBackgroundResource(R.drawable.bg_button_itago_high)
        btnItago.text = "Hide"
        btnItago.backgroundTintList = null
        btnReview.setTextColor(Color.parseColor("#991B1B"))
        btnDismiss.setTextColor(Color.parseColor("#991B1B"))
    }

    private fun applyMediumAlertStyle() {
        container.setBackgroundResource(R.drawable.bg_warning_banner_medium)
        headlineText.setTextColor(Color.parseColor("#854D0E"))
        tipText.setTextColor(Color.parseColor("#78350F"))
        bannerIcon?.setImageResource(R.drawable.ic_warning_medium)
        bannerBadge?.text = "NOTICE · INTENDED?"
        bannerBadge?.setTextColor(Color.parseColor("#854D0E"))
        bannerBadge?.setBackgroundResource(R.drawable.bg_badge_medium)
        btnItago.setBackgroundResource(R.drawable.bg_button_itago_medium)
        btnItago.text = "Hide"
        btnItago.backgroundTintList = null
        btnReview.setTextColor(Color.parseColor("#854D0E"))
        btnDismiss.setTextColor(Color.parseColor("#854D0E"))
    }
}
