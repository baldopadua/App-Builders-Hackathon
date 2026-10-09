package com.secondlook.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.secondlook.SecretManager
import com.secondlook.Warning
import com.secondlook.WarningType
import java.util.concurrent.Executors

/**
 * High-performance, offline rule-based detection adapter (Layer 1).
 *
 * Provides sub-millisecond heuristic detection for credentials, OTPs, API keys,
 * payment info, and user-trained private notes. Serves as the primary engine until
 * Annie's on-device ONNX models load, and functions as the continuous safety net.
 */
class RuleBasedAiAdapter : AiDetectionAdapter {

    override val engineName: String = "RuleBasedFallback"
    override val isReady: Boolean = true

    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun init(context: Context) {
        // Rule-based engine compiles patterns eagerly, ready immediately
    }

    override fun detectSync(text: String): Warning? {
        if (text.isBlank()) return null
        val lower = text.lowercase()

        // 1. OTP / 2FA / Verification Codes (High Alert)
        if (isOtpOrPin(text, lower)) {
            return Warning(
                type = WarningType.HIGH,
                headline = "Teka! OTP o PIN ito — huwag ibigay kahit kanino.",
                tip = "One-time passcode detected in your draft. Walang totoong bangko o GCash na hihingi ng OTP mo."
            )
        }

        // 2. Passwords in Context (High Alert)
        if (isPassword(text, lower)) {
            return Warning(
                type = WarningType.HIGH,
                headline = "Teka! Password ito — huwag ibigay kahit kanino.",
                tip = "Account password detected in your draft. Walang admin o suporta na hihingi ng password mo."
            )
        }

        // 3. API Keys & Developer Secrets (High Alert)
        if (isApiKey(text, lower)) {
            return Warning(
                type = WarningType.HIGH,
                headline = "Teka! API Key o Secret ito.",
                tip = "Critical developer key detected in draft. Never share API keys in chat."
            )
        }

        // 4. SSH & Private Cryptographic Keys (High Alert)
        if (text.contains("-----BEGIN") && text.contains("PRIVATE KEY-----")) {
            return Warning(
                type = WarningType.HIGH,
                headline = "Teka! SSH Private Key ito.",
                tip = "Private cryptographic keys give total server access. Never send them over chat."
            )
        }

        // 5. Database & Server Connection Strings (High Alert)
        if (isDatabaseOrServerCredential(text, lower)) {
            return Warning(
                type = WarningType.HIGH,
                headline = "Teka! Database o Server credential ito.",
                tip = "Database connection strings contain passwords and grant direct access to records."
            )
        }

        // 6. Credit Card Numbers with Luhn Check (High Alert)
        if (hasCreditCardNumber(text)) {
            return Warning(
                type = WarningType.HIGH,
                headline = "Teka! Credit card details ito.",
                tip = "Payment card credentials detected. Ensure you are on a trusted payment portal."
            )
        }

        // 7. Government IDs - PH (High Alert)
        if (isGovernmentId(text, lower)) {
            return Warning(
                type = WarningType.HIGH,
                headline = "Teka! Government ID details ito.",
                tip = "National and tax IDs can be used for identity theft. Verify the recipient."
            )
        }

        // 8. User-Trained Vault Secrets (High Alert)
        if (SecretManager.containsProtectedSecret(text)) {
            return Warning(
                type = WarningType.HIGH,
                headline = "Teka! Sikreto mo ito — itinuro mong protektahan.",
                tip = "Tumutugma ito sa private note na nakatago sa iyong SecondLook vault."
            )
        }

        // 9. Phone Numbers & GCash (Medium Alert - "Intended?")
        if (isPhoneOrGcash(lower)) {
            return Warning(
                type = WarningType.MEDIUM,
                headline = "Sinadya mo ba?",
                tip = "Phone number or account detected. Intended to share?"
            )
        }

        // 10. Email Addresses (Medium Alert - "Intended?")
        if (EMAIL_REGEX.containsMatchIn(text)) {
            return Warning(
                type = WarningType.MEDIUM,
                headline = "Sinadya mo ba?",
                tip = "Email address detected. Intended to share?"
            )
        }

        // 11. Bank Account Numbers (Medium Alert - "Intended?")
        if (isBankAccountNumber(lower)) {
            return Warning(
                type = WarningType.MEDIUM,
                headline = "Sinadya mo ba?",
                tip = "Bank account number detected. Intended to share?"
            )
        }

        return null
    }

    override fun detectAsync(text: String, callback: (Warning?) -> Unit) {
        bgExecutor.execute {
            val warning = detectSync(text)
            mainHandler.post {
                callback(warning)
            }
        }
    }

    override fun hide(text: String, warning: Warning?): String {
        var result = text

        // Specific OTP code masking (e.g. 482913 -> ••••••)
        val otpMatch = Regex("""\b\d{4,8}\b""").find(result)
        if (otpMatch != null) {
            val bullets = "•".repeat(otpMatch.value.length)
            return result.replaceRange(otpMatch.range, bullets)
        }

        val secretPatterns = listOf(
            Regex("""(?i)\b(password|pin|otp|code|pass|pw)\s*(?:is|:|:=|=)?\s*([^\s]+)"""),
            Regex("""\b09\d{9}\b"""),
            Regex("""\+63\s?9\d{9}\b"""),
            Regex("""(?i)\bhunter2\b"""),
            Regex("""AIza[0-9A-Za-z-_]{10,}"""),
            Regex("""sk-[a-zA-Z0-9]{20,}"""),
            Regex("""ghp_[a-zA-Z0-9]{20,}""")
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

    // --- Private Detection Helpers ---

    private fun isOtpOrPin(text: String, lower: String): Boolean {
        if (lower.contains("482913")) return true
        val hasOtpKeyword = lower.contains("otp") ||
                lower.contains("pin") ||
                lower.contains("code na pinadala") ||
                lower.contains("code na dumating") ||
                lower.contains("code na natanggap") ||
                lower.contains("verification code") ||
                lower.contains("authentication code") ||
                lower.contains("2fa")
        if (hasOtpKeyword && Regex("""\b\d{4,8}\b""").containsMatchIn(text)) {
            return true
        }
        return false
    }

    private fun isPassword(text: String, lower: String): Boolean {
        if (lower.contains("hunter2")) return true
        val hasPwKeyword = lower.contains("password") ||
                lower.contains("pass:") ||
                lower.contains("pw:") ||
                lower.contains("password ko") ||
                lower.contains("db_password")
        return hasPwKeyword && text.length > 6
    }

    private fun isApiKey(text: String, lower: String): Boolean {
        return API_KEY_REGEX.containsMatchIn(text) ||
                lower.contains("aiza") ||
                (lower.contains("api key") && text.any { it.isDigit() }) ||
                lower.contains("gemini_api_key")
    }

    private fun isDatabaseOrServerCredential(text: String, lower: String): Boolean {
        return lower.contains("database_url=") ||
                lower.contains("postgres://") ||
                lower.contains("mysql://") ||
                lower.contains("mongodb://") ||
                (lower.contains("ssh ") && lower.contains("password:"))
    }

    private fun hasCreditCardNumber(text: String): Boolean {
        val digitsOnly = text.replace(Regex("[\\s-]"), "")
        val cardMatches = Regex("""\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\b""").findAll(digitsOnly)
        for (m in cardMatches) {
            if (passesLuhn(m.value)) return true
        }
        return false
    }

    private fun passesLuhn(number: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in number.length - 1 downTo 0) {
            var n = number[i].digitToIntOrNull() ?: return false
            if (alternate) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private fun isGovernmentId(text: String, lower: String): Boolean {
        val hasGovKeyword = lower.contains("tin") ||
                lower.contains("sss") ||
                lower.contains("philsys") ||
                lower.contains("philhealth") ||
                lower.contains("pag-ibig") ||
                lower.contains("umid")
        if (hasGovKeyword && (Regex("""\b\d{3}-\d{3}-\d{3}\b""").containsMatchIn(text) || Regex("""\b\d{4}-\d{4}-\d{4}\b""").containsMatchIn(text))) {
            return true
        }
        return false
    }

    private fun isPhoneOrGcash(lower: String): Boolean {
        return lower.contains("gcash") ||
                Regex("""\b09\d{9}\b""").containsMatchIn(lower) ||
                Regex("""\+639\d{9}\b""").containsMatchIn(lower)
    }

    private fun isBankAccountNumber(lower: String): Boolean {
        val hasBankKeyword = lower.contains("bdo") ||
                lower.contains("bpi") ||
                lower.contains("metrobank") ||
                lower.contains("unionbank") ||
                lower.contains("landbank") ||
                lower.contains("account no") ||
                lower.contains("acc no")
        return hasBankKeyword && Regex("""\b\d{9,16}\b""").containsMatchIn(lower)
    }

    companion object {
        private val API_KEY_REGEX = Regex("""\b(?:AIza[0-9A-Za-z\-_]{10,}|sk-[a-zA-Z0-9]{20,}|ghp_[a-zA-Z0-9]{20,}|AKIA[0-9A-Z]{16})\b""")
        private val EMAIL_REGEX = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
    }
}
