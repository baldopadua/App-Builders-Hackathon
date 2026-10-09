package com.secondlook

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

object DetectionEngine {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Synchronously analyzes draft text for immediate pre-send checks.
     */
    fun detectSync(text: String): Warning? {
        val lower = text.lowercase()
        return when {
            lower.contains("password") || lower.contains("pin") || lower.contains("otp") || lower.contains("hunter2") -> {
                Warning(
                    type = WarningType.HIGH,
                    headline = "Teka! OTP o PIN ito — huwag ibigay kahit kanino.",
                    tip = "This looks like a verification code or password. Sharing it could compromise your account."
                )
            }
            SecretManager.containsProtectedSecret(text) -> {
                Warning(
                    type = WarningType.HIGH,
                    headline = "Teka! Sikreto mo ito — itinuro mong protektahan.",
                    tip = "This matches a private note you protected in SecondLook."
                )
            }
            lower.contains("gcash") || Regex("""\b09\d{9}\b""").containsMatchIn(lower) -> {
                Warning(
                    type = WarningType.MEDIUM,
                    headline = "Pansin: Posibleng personal o payment info ito.",
                    tip = "Double-check kung sinasadya mong ibahagi ang iyong contact o payment details."
                )
            }
            else -> null
        }
    }

    /**
     * Analyzes draft text asynchronously after debounce pause.
     */
    fun detectAsync(text: String, callback: (Warning?) -> Unit) {
        executor.execute {
            val warning = detectSync(text)
            mainHandler.post {
                callback(warning)
            }
        }
    }

    /**
     * Masks detected sensitive secrets in the draft text.
     */
    fun hide(text: String, warning: Warning? = null): String {
        var result = text
        val secretPatterns = listOf(
            Regex("""(?i)\b(password|pin|otp|code)\s*(?:is|:|:=|=)?\s*([^\s]+)"""),
            Regex("""\b09\d{9}\b"""),
            Regex("""(?i)\bhunter2\b""")
        )

        var replaced = false
        for (pattern in secretPatterns) {
            if (pattern.containsMatchIn(result)) {
                result = pattern.replace(result) { matchResult ->
                    if (matchResult.groupValues.size > 2 && matchResult.groupValues[2].isNotEmpty()) {
                        val label = matchResult.groupValues[1]
                        "$label: ••••••••"
                    } else {
                        "••••••••"
                    }
                }
                replaced = true
            }
        }

        if (!replaced) {
            return "••••••••"
        }
        return result
    }
}
