package com.secondlook

import android.content.Context

/**
 * Manages user preferences for SecondLook keyboard protection and feedback.
 */
object SettingsManager {
    private const val PREFS_NAME = "secondlook_settings"
    private const val KEY_HAPTIC_FEEDBACK = "pref_haptic_feedback"

    fun isHapticFeedbackEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
    }

    fun setHapticFeedbackEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    }
}
