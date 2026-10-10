package com.secondlook

import android.content.Context

/**
 * Manages user preferences for SecondLook keyboard protection and feedback.
 */
object SettingsManager {
    private const val PREFS_NAME = "secondlook_settings"
    private const val KEY_HAPTIC_FEEDBACK = "pref_haptic_feedback"
    private const val KEY_INGAT_MODE = "pref_ingat_mode"
    private const val KEY_TERMS_ACCEPTED = "pref_terms_accepted"

    fun hasAcceptedTerms(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    fun setAcceptedTerms(context: Context, accepted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, accepted).apply()
    }

    fun isHapticFeedbackEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
    }

    fun setHapticFeedbackEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    }

    fun isIngatModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_INGAT_MODE, true)
    }

    fun setIngatModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_INGAT_MODE, enabled).apply()
    }
}
