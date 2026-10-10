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
    private val btnDismiss: Button = view.findViewById(R.id.btn_dismiss)

    private val holdHandler = Handler(Looper.getMainLooper())
    private var isInterceptActive = false

    init {
        // Start hidden by default
        container.visibility = View.GONE
    }

    private var onDismissListener: (() -> Unit)? = null

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
     * Transforms the banner UI for intercepted Send events: [Send anyway] [Mask text]
     */
    fun showSendIntercept(warning: Warning, onSendAnyway: () -> Unit) {
        cancelHold()
        isInterceptActive = true
        headlineText.text = "Send anyway? ${warning.headline}"
        applyHighAlertStyle()

        tipText.text = warning.tip
        btnDismiss.setOnTouchListener(null)
        btnDismiss.text = "Send anyway"
        btnDismiss.setOnClickListener {
            hide()
            onSendAnyway()
        }
        btnItago.visibility = if (warning.canHide) View.VISIBLE else View.GONE

        if (container.visibility != View.VISIBLE) {
            container.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(container.context, R.anim.banner_slide_up)
            container.startAnimation(anim)
        }
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
    }

    fun setOnItagoClickListener(listener: () -> Unit) {
        btnItago.setOnClickListener { listener() }
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
        bannerIcon?.setImageResource(R.drawable.ic_tanggolin)
        bannerBadge?.visibility = View.GONE
        btnItago.setBackgroundResource(R.drawable.bg_button_itago_high)
        btnItago.text = "Mask text"
        btnItago.backgroundTintList = null
        btnDismiss.text = "Dismiss"
        btnDismiss.setTextColor(Color.parseColor("#991B1B"))
    }

    private fun applyMediumAlertStyle() {
        container.setBackgroundResource(R.drawable.bg_warning_banner_medium)
        headlineText.setTextColor(Color.parseColor("#854D0E"))
        tipText.setTextColor(Color.parseColor("#78350F"))
        bannerIcon?.setImageResource(R.drawable.ic_tanggolin)
        bannerBadge?.visibility = View.GONE
        btnItago.setBackgroundResource(R.drawable.bg_button_itago_medium)
        btnItago.text = "Mask text"
        btnItago.backgroundTintList = null
        btnDismiss.text = "Dismiss"
        btnDismiss.setTextColor(Color.parseColor("#854D0E"))
    }
}
