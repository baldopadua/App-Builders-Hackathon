package com.secondlook

import android.content.Context

/**
 * Manages user preferences for SecondLook keyboard protection modes.
 *
 * "Ingat Mode": When enabled, high-alert warnings require a 2-second press & hold
 * on the Send button before dispatching, preventing accidental taps by seniors, kids,
 * or busy users. Enabled by default.
 */
object SettingsManager {
    private const val PREFS_NAME = "secondlook_settings"
    private const val KEY_INGAT_MODE = "pref_ingat_mode"

    fun isIngatModeEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_INGAT_MODE, true) // Safe by default
    }

    fun setIngatModeEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_INGAT_MODE, enabled).apply()
    }
}
