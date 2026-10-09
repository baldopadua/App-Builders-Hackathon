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
            lower.contains("482913") || (lower.contains("code na pinadala") && Regex("""\b\d{4,8}\b""").containsMatchIn(text)) || lower.contains("otp") || lower.contains("pin") -> {
                Warning(
                    type = WarningType.HIGH,
                    headline = "Teka! OTP o PIN ito — huwag ibigay kahit kanino.",
                    tip = "One-time passcode detected in your draft.\nWalang totoong bangko o GCash na hihingi ng OTP mo."
                )
            }
            lower.contains("hunter2") || lower.contains("password") -> {
                Warning(
                    type = WarningType.HIGH,
                    headline = "Teka! Password ito — huwag ibigay kahit kanino.",
                    tip = "Account password detected in your draft. Walang admin o suporta na hihingi ng password mo."
                )
            }
            lower.contains("aiza") || lower.contains("api key") || lower.contains("gemini") -> {
                Warning(
                    type = WarningType.HIGH,
                    headline = "Teka! API Key o Secret ito.",
                    tip = "Critical developer key detected in draft. Never share API keys in chat."
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
                    headline = "Sinadya mo ba?",
                    tip = "Phone number or account detected. Intended to share?"
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
        // Specific OTP code masking (e.g. 482913 -> ••••••)
        val otpMatch = Regex("""\b\d{4,8}\b""").find(result)
        if (otpMatch != null) {
            val bullets = "•".repeat(otpMatch.value.length)
            return result.replaceRange(otpMatch.range, bullets)
        }

        val secretPatterns = listOf(
            Regex("""(?i)\b(password|pin|otp|code)\s*(?:is|:|:=|=)?\s*([^\s]+)"""),
            Regex("""\b09\d{9}\b"""),
            Regex("""(?i)\bhunter2\b"""),
            Regex("""AIza[0-9A-Za-z-_]{10,}""")
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
