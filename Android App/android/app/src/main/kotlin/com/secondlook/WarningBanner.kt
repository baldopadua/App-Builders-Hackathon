package com.secondlook

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    private val btnItago: Button = view.findViewById(R.id.btn_itago)
    private val btnReview: Button = view.findViewById(R.id.btn_review)
    private val btnDismiss: Button = view.findViewById(R.id.btn_dismiss)

    init {
        // Start hidden by default
        container.visibility = View.GONE
    }

    private var onDismissListener: (() -> Unit)? = null

    fun showWarning(warning: Warning) {
        headlineText.text = warning.headline
        tipText.text = warning.tip
        btnDismiss.text = "Dismiss"
        btnDismiss.setOnClickListener {
            hide()
            onDismissListener?.invoke()
        }

        when (warning.type) {
            WarningType.HIGH -> applyHighAlertStyle()
            WarningType.MEDIUM -> applyMediumAlertStyle()
        }

        container.visibility = View.VISIBLE
    }

    /**
     * Transforms the banner UI for intercepted Send events: [Itago] [Review] [Send anyway]
     */
    fun showSendIntercept(warning: Warning, onSendAnyway: () -> Unit) {
        headlineText.text = "Ipadala pa rin? ${warning.headline}"
        tipText.text = warning.tip
        applyHighAlertStyle()

        btnDismiss.text = "Send anyway"
        btnDismiss.setOnClickListener {
            hide()
            onSendAnyway()
        }

        container.visibility = View.VISIBLE
    }

    fun hide() {
        container.visibility = View.GONE
        btnDismiss.text = "Dismiss"
    }

    fun setOnItagoClickListener(listener: () -> Unit) {
        btnItago.setOnClickListener { listener() }
    }

    fun setOnReviewClickListener(listener: () -> Unit) {
        btnReview.setOnClickListener { listener() }
    }

    fun setOnDismissClickListener(listener: () -> Unit) {
        onDismissListener = listener
        btnDismiss.setOnClickListener {
            hide()
            listener()
        }
    }

    private fun applyHighAlertStyle() {
        container.setBackgroundColor(Color.parseColor("#FEE2E2"))
        headlineText.setTextColor(Color.parseColor("#991B1B"))
        tipText.setTextColor(Color.parseColor("#B91C1C"))
        btnItago.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
    }

    private fun applyMediumAlertStyle() {
        container.setBackgroundColor(Color.parseColor("#FEF08A"))
        headlineText.setTextColor(Color.parseColor("#854D0E"))
        tipText.setTextColor(Color.parseColor("#A16207"))
        btnItago.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EAB308"))
    }
}
