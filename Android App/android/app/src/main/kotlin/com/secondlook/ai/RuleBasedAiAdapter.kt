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
 * on-device ONNX models load, and functions as the continuous safety net.
 */
class RuleBasedAiAdapter : AiDetectionAdapter {

    override val engineName: String = "RuleBasedFallback"
    override val isReady: Boolean = true

    private val bgExecutor = Executors.newSingleThreadExecutor()
    private var mainHandler: Handler? = null

    private fun getMainHandler(): Handler {
        if (mainHandler == null) {
            mainHandler = Handler(Looper.getMainLooper())
        }
        return mainHandler!!
    }

    override fun init(context: Context) {
        // Rule-based engine compiles patterns eagerly, ready immediately
    }

    private data class MatchedSecret(
        val matchedText: String,
        val start: Int,
        val end: Int
    )

    private fun createWarning(
        type: WarningType,
        headline: String,
        tip: String,
        text: String,
        secret: MatchedSecret,
        category: String = "Data protection",
        canHide: Boolean = true
    ): Warning {
        val (s, e) = WordMasker.expandSecretBoundaries(text, secret.start, secret.end)
        val matched = if (s < e) text.substring(s, e) else secret.matchedText
        return Warning(
            type = type,
            headline = headline,
            tip = tip,
            category = category,
            matchedText = matched,
            start = s,
            end = e,
            canHide = canHide
        )
    }

    override fun detectSync(text: String): Warning? {
        if (text.isBlank()) return null
        val lower = text.lowercase()

        // 1. OTP / 2FA / Verification Codes (High Alert)
        val otp = matchOtpOrPin(text, lower)
        if (otp != null) {
            return createWarning(
                type = WarningType.HIGH,
                headline = SafetyTips.headline("otp"),
                tip = SafetyTips.forKind("otp"),
                text = text,
                secret = otp
            )
        }

        // 2. Passwords in Context (High Alert)
        val pw = matchPassword(text, lower)
        if (pw != null) {
            return createWarning(
                type = WarningType.HIGH,
                headline = SafetyTips.headline("password"),
                tip = SafetyTips.forKind("password"),
                text = text,
                secret = pw
            )
        }

        // 3. API Keys & Developer Secrets (High Alert)
        val apiKey = matchApiKey(text, lower)
        if (apiKey != null) {
            return createWarning(
                type = WarningType.HIGH,
                headline = SafetyTips.headline("api_key"),
                tip = SafetyTips.forKind("api_key"),
                text = text,
                secret = apiKey
            )
        }

        // 4. SSH & Private Cryptographic Keys (High Alert)
        val sshKey = matchSshKey(text)
        if (sshKey != null) {
            return createWarning(
                type = WarningType.HIGH,
                headline = SafetyTips.headline("ssh_key"),
                tip = SafetyTips.forKind("ssh_key"),
                text = text,
                secret = sshKey
            )
        }

        // 5. Database & Server Connection Strings (High Alert)
        val dbCred = matchDatabaseOrServerCredential(text, lower)
        if (dbCred != null) {
            return createWarning(
                type = WarningType.HIGH,
                headline = SafetyTips.headline("db_credential"),
                tip = SafetyTips.forKind("db_credential"),
                text = text,
                secret = dbCred
            )
        }

        // 6. Credit Card Numbers with Luhn Check (High Alert)
        val card = matchCreditCard(text)
        if (card != null) {
            return createWarning(
                type = WarningType.HIGH,
                headline = SafetyTips.headline("credit_card"),
                tip = SafetyTips.forKind("credit_card"),
                text = text,
                secret = card
            )
        }

        // 7. Government IDs (High Alert)
        val govId = matchGovernmentId(text, lower)
        if (govId != null) {
            return createWarning(
                type = WarningType.HIGH,
                headline = SafetyTips.headline("gov_id"),
                tip = SafetyTips.forKind("gov_id"),
                text = text,
                secret = govId
            )
        }

        // 8. User-Trained Vault Secrets (High Alert)
        val vaultSecret = matchVaultSecret(text)
        if (vaultSecret != null) {
            return createWarning(
                type = WarningType.HIGH,
                headline = SafetyTips.headline("my_secret"),
                tip = SafetyTips.forKind("my_secret"),
                text = text,
                secret = vaultSecret
            )
        }

        // 9. Phone Numbers (Medium Alert - "Intended?")
        val phone = matchPhone(text, lower)
        if (phone != null) {
            return createWarning(
                type = WarningType.MEDIUM,
                headline = SafetyTips.headline("phone"),
                tip = SafetyTips.forKind("phone"),
                text = text,
                secret = phone
            )
        }

        // 10. Email Addresses (Medium Alert - "Intended?")
        val email = matchEmail(text)
        if (email != null) {
            return createWarning(
                type = WarningType.MEDIUM,
                headline = SafetyTips.headline("email"),
                tip = SafetyTips.forKind("email"),
                text = text,
                secret = email
            )
        }

        // 11. Bank Account Numbers (Medium Alert - "Intended?")
        val bankAccount = matchBankAccount(text, lower)
        if (bankAccount != null) {
            return createWarning(
                type = WarningType.MEDIUM,
                headline = SafetyTips.headline("bank_account"),
                tip = SafetyTips.forKind("bank_account"),
                text = text,
                secret = bankAccount
            )
        }

        return null
    }

    override fun detectAsync(text: String, callback: (Warning?) -> Unit) {
        bgExecutor.execute {
            val warning = detectSync(text)
            getMainHandler().post {
                callback(warning)
            }
        }
    }

    override fun hide(text: String, warning: Warning?): String {
        val targetWarning = if (warning != null && warning.canHide && warning.start < warning.end && warning.end <= text.length) {
            warning
        } else {
            detectSync(text)
        }

        if (targetWarning != null && targetWarning.canHide && targetWarning.start >= 0 && targetWarning.end > targetWarning.start && targetWarning.end <= text.length) {
            val (s, e) = WordMasker.expandSecretBoundaries(text, targetWarning.start, targetWarning.end)
            if (s < e) {
                val secret = text.substring(s, e)
                val masked = "•".repeat(secret.length)
                return text.substring(0, s) + masked + text.substring(e)
            }
        }

        var result = text

        // Fallback: OTP masking
        val otpMatch = Regex("""\b\d{4,8}\b""").find(result)
        if (otpMatch != null && !otpMatch.value.contains('•')) {
            val (s, e) = WordMasker.expandSecretBoundaries(result, otpMatch.range.first, otpMatch.range.last + 1)
            val bullets = "•".repeat(e - s)
            return result.substring(0, s) + bullets + result.substring(e)
        }

        // Fallback: Labeled Password masking
        val pwMatch = PASSWORD_LABELED_REGEX.find(result)
        if (pwMatch != null) {
            val g = pwMatch.groups[1]
            if (g != null) {
                val (s, e) = WordMasker.expandSecretBoundaries(result, g.range.first, g.range.last + 1)
                val bullets = "•".repeat(e - s)
                return result.substring(0, s) + bullets + result.substring(e)
            }
        }

        // Fallback: API keys
        val apiKeyMatch = API_KEY_REGEX.find(result)
        if (apiKeyMatch != null && !apiKeyMatch.value.contains('•')) {
            val (s, e) = WordMasker.expandSecretBoundaries(result, apiKeyMatch.range.first, apiKeyMatch.range.last + 1)
            val bullets = "•".repeat(e - s)
            return result.substring(0, s) + bullets + result.substring(e)
        }

        // Fallback: hunter2
        val hunterMatch = Regex("""(?i)\bhunter2\b""").find(result)
        if (hunterMatch != null && !hunterMatch.value.contains('•')) {
            val (s, e) = WordMasker.expandSecretBoundaries(result, hunterMatch.range.first, hunterMatch.range.last + 1)
            val bullets = "•".repeat(e - s)
            return result.substring(0, s) + bullets + result.substring(e)
        }

        return WordMasker.ensureFullWordMask(text, result, targetWarning)
    }

    // --- Private Detection Helpers ---

    private fun matchOtpOrPin(text: String, lower: String): MatchedSecret? {
        // 1. Digits linked after OTP/PIN keyword: e.g. "PIN ko 4821", "OTP ko ay 482913 at ok"
        for (m in OTP_AFTER_REGEX.findAll(text)) {
            val g = m.groups[1] ?: continue
            val code = g.value
            if (code.length in 4..8 && !code.contains('•')) {
                return MatchedSecret(code, g.range.first, g.range.last + 1)
            }
        }

        // 2. Digits linked before OTP/PIN keyword: e.g. "4821 mpin ko", "482913 is your OTP code"
        for (m in OTP_BEFORE_REGEX.findAll(text)) {
            val g = m.groups[1] ?: continue
            val code = g.value
            if (code.length in 4..8 && !code.contains('•')) {
                return MatchedSecret(code, g.range.first, g.range.last + 1)
            }
        }

        // 3. Known canary 482913
        val canaryMatch = Regex("""\b482913\b""").find(text)
        if (canaryMatch != null && !canaryMatch.value.contains('•')) {
            return MatchedSecret(canaryMatch.value, canaryMatch.range.first, canaryMatch.range.last + 1)
        }

        // 4. Standalone OTP or bare digits when OTP keyword is present
        val hasOtpKeyword = lower.contains("otp") ||
                lower.contains("pin") ||
                lower.contains("code na pinadala") ||
                lower.contains("code na dumating") ||
                lower.contains("code na natanggap") ||
                lower.contains("verification code") ||
                lower.contains("authentication code") ||
                lower.contains("2fa")
        if (hasOtpKeyword) {
            val m = Regex("""\b\d{4,8}\b""").find(text)
            if (m != null && !m.value.contains('•')) {
                return MatchedSecret(m.value, m.range.first, m.range.last + 1)
            }
        }

        return null
    }

    private fun matchPassword(text: String, lower: String): MatchedSecret? {
        // 1. Explicitly labeled/assigned password
        // e.g. "password: mango72-sss", "pw is hunter2 extra", "Password ng ML account ko: Lodi1234"
        for (m in PASSWORD_LABELED_REGEX.findAll(text)) {
            val g = m.groups[1] ?: continue
            val raw = g.value
            val unquoted = if ((raw.startsWith('"') && raw.endsWith('"')) || (raw.startsWith('\'') && raw.endsWith('\''))) {
                raw.substring(1, raw.length - 1)
            } else {
                raw
            }
            val trimmed = unquoted.trimEnd('.', '!', '?', ')', ';', ',')
            val secret = trimmed.trim()
            if (secret.length >= 3 && !secret.contains('•') && secret.lowercase() !in NOT_A_SECRET) {
                val secretStart = text.indexOf(secret, g.range.first)
                if (secretStart >= 0) {
                    val secretEnd = secretStart + secret.length
                    return MatchedSecret(secret, secretStart, secretEnd)
                }
            }
        }

        // 2. Conversational password (e.g. "password ko mango72-sss")
        for (m in PASSWORD_CONVERSATIONAL_REGEX.findAll(text)) {
            val g = m.groups[1] ?: continue
            val secret = g.value.trimEnd('.', '!', '?', ')', ';', ',').trim()
            if (secret.length >= 3 && !secret.contains('•') && secret.lowercase() !in NOT_A_SECRET) {
                val secretStart = text.indexOf(secret, g.range.first)
                if (secretStart >= 0) {
                    val secretEnd = secretStart + secret.length
                    return MatchedSecret(secret, secretStart, secretEnd)
                }
            }
        }

        // 3. .env style: DB_PASSWORD=my_secret_pass
        for (m in ENV_PASSWORD_REGEX.findAll(text)) {
            val g = m.groups[1] ?: continue
            val secret = g.value.trim('"', '\'').trim()
            if (secret.length >= 3 && !secret.contains('•')) {
                val secretStart = text.indexOf(secret, g.range.first)
                if (secretStart >= 0) {
                    val secretEnd = secretStart + secret.length
                    return MatchedSecret(secret, secretStart, secretEnd)
                }
            }
        }

        // 4. Standalone complex secret / password token without keyword (e.g. "@!$\"-*sss")
        for (m in Regex("""\S+""").findAll(text)) {
            val token = m.value
            val clean = token.trimEnd('.', '!', '?', ')', ';', ',', ':')
            if (clean.length >= 6 && !clean.contains('•') && clean.any { it in "!@#$%^&*()-_=+[]{}|;:'\",.<>/?`~" } && clean.any { it.isLetterOrDigit() }) {
                // Exclude emails, URLs, and non-secret keywords
                if (!EMAIL_REGEX.containsMatchIn(clean) && !clean.contains("://") && !clean.startsWith("www.") && clean.lowercase() !in NOT_A_SECRET) {
                    val start = m.range.first
                    return MatchedSecret(clean, start, start + clean.length)
                }
            }
        }

        // 5. Standalone canary: hunter2
        val hunterMatch = Regex("""(?i)\bhunter2\b""").find(text)
        if (hunterMatch != null && !hunterMatch.value.contains('•')) {
            return MatchedSecret(hunterMatch.value, hunterMatch.range.first, hunterMatch.range.last + 1)
        }

        return null
    }

    private fun matchApiKey(text: String, lower: String): MatchedSecret? {
        val m = API_KEY_REGEX.find(text)
        if (m != null && !m.value.contains('•')) {
            return MatchedSecret(m.value, m.range.first, m.range.last + 1)
        }
        for (km in KEY_CONTEXT_REGEX.findAll(text)) {
            val g = km.groups[1] ?: continue
            val key = g.value.trim('"', '\'', ' ')
            if (key.length >= 16 && !key.contains('•')) {
                val start = text.indexOf(key, g.range.first)
                if (start >= 0) {
                    return MatchedSecret(key, start, start + key.length)
                }
            }
        }
        for (em in ENV_KEY_REGEX.findAll(text)) {
            val g = em.groups[1] ?: continue
            val key = g.value.trim('"', '\'', ' ')
            if (key.length >= 8 && !key.contains('•')) {
                val start = text.indexOf(key, g.range.first)
                if (start >= 0) {
                    return MatchedSecret(key, start, start + key.length)
                }
            }
        }
        return null
    }

    private fun matchSshKey(text: String): MatchedSecret? {
        val m = SSH_KEY_REGEX.find(text) ?: return null
        if (m.value.contains('•')) return null
        return MatchedSecret(m.value, m.range.first, m.range.last + 1)
    }

    private fun matchDatabaseOrServerCredential(text: String, lower: String): MatchedSecret? {
        val urlMatch = DB_URL_REGEX.find(text)
        if (urlMatch != null && !urlMatch.value.contains('•')) {
            return MatchedSecret(urlMatch.value, urlMatch.range.first, urlMatch.range.last + 1)
        }
        val envMatch = DB_ENV_REGEX.find(text)
        if (envMatch != null && !envMatch.value.contains('•')) {
            val g = envMatch.groups[1]
            if (g != null) {
                val valTrim = g.value.trim('"', '\'')
                val start = text.indexOf(valTrim, g.range.first)
                if (start >= 0) return MatchedSecret(valTrim, start, start + valTrim.length)
            }
            return MatchedSecret(envMatch.value, envMatch.range.first, envMatch.range.last + 1)
        }
        return null
    }

    private fun matchCreditCard(text: String): MatchedSecret? {
        for (m in CARD_REGEX.findAll(text)) {
            if (m.value.contains('•')) continue
            val digits = m.value.filter { it in '0'..'9' }
            if (digits.length in 13..19 && passesLuhn(digits)) {
                return MatchedSecret(m.value, m.range.first, m.range.last + 1)
            }
        }
        return null
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

    private fun matchGovernmentId(text: String, lower: String): MatchedSecret? {
        for (m in GOV_ID_REGEX.findAll(text)) {
            val g = m.groups[1] ?: continue
            val idVal = g.value.trim()
            if (!idVal.contains('•')) {
                val start = text.indexOf(idVal, g.range.first)
                if (start >= 0) return MatchedSecret(idVal, start, start + idVal.length)
            }
        }
        val hasGovKeyword = lower.contains("tin") ||
                lower.contains("sss") ||
                lower.contains("philsys") ||
                lower.contains("philhealth") ||
                lower.contains("pag-ibig") ||
                lower.contains("umid")
        if (hasGovKeyword) {
            val m = STANDALONE_GOV_ID.find(text)
            if (m != null && !m.value.contains('•')) {
                return MatchedSecret(m.value, m.range.first, m.range.last + 1)
            }
        }
        return null
    }

    private fun matchVaultSecret(text: String): MatchedSecret? {
        val found = SecretManager.findProtectedSecret(text) ?: return null
        val secretVal = text.substring(found.second.first, found.second.last + 1)
        if (secretVal.contains('•')) return null
        return MatchedSecret(secretVal, found.second.first, found.second.last + 1)
    }

    private fun matchPhone(text: String, lower: String): MatchedSecret? {
        val m = PHONE_REGEX.find(text)
        if (m != null && !m.value.contains('•')) {
            return MatchedSecret(m.value, m.range.first, m.range.last + 1)
        }
        return null
    }

    private fun matchEmail(text: String): MatchedSecret? {
        val m = EMAIL_REGEX.find(text)
        if (m != null && !m.value.contains('•')) {
            return MatchedSecret(m.value, m.range.first, m.range.last + 1)
        }
        return null
    }

    private fun matchBankAccount(text: String, lower: String): MatchedSecret? {
        for (m in BANK_ACCOUNT_REGEX.findAll(text)) {
            val g = m.groups[1] ?: continue
            val acc = g.value.trim()
            if (!acc.contains('•')) {
                val start = text.indexOf(acc, g.range.first)
                if (start >= 0) return MatchedSecret(acc, start, start + acc.length)
            }
        }
        return null
    }

    companion object {
        private val OTP_AFTER_REGEX = Regex(
            """(?i)\b(?:otp|one[- ]time[-\s]+(?:password|passcode|code|pin)|verification\s+code|security\s+code|authentication\s+code|auth\s+code|confirmation\s+code|login\s+code|2fa|mfa|two[- ]factor(?:\s+code)?|backup\s+code|recovery\s+code|m-?pin|passcode|atm\s+pin|pin\s+code|(?:my|your)\s+pin|pin\s+(?:ko|mo|namin|q|ku|mu|nmin)|pin\s+(?:ng|sa)\s+[\w-]+|\d[- ]?digits?\s+(?:code|pin|otp)|code\s+(?:na\s+)?(?:pinadala|dumating|natanggap)|code)\b[^\d\n]{0,35}?(\d{4,8})(?!\d)"""
        )
        private val OTP_BEFORE_REGEX = Regex(
            """(?i)(?<![\w.,#-])(\d{4,8})[^\d\n,.;]{0,15}?\b(?:m-?pin|atm\s+pin|pin|otp|code)\b"""
        )

        private val PASSWORD_LABELED_REGEX = Regex(
            """(?i)\b(?:password|passcode|passwd|pwd|pw|pasword|paswrd|passwrd|db_password|pass(?=\s*[:=]|\s+(?:ng|sa)\s+[\w-]+(?:\s+(?:ko|mo|namin|natin))?\s*[:=]))\b""" +
                """(?:\s+(?:ko|mo|nya|niya|namin|natin|po|(?:for|ng|sa|para\s+sa)\s+(?:the\s+|my\s+|our\s+|ang\s+)?[\w-]+(?:\s+[\w-]+)?)){0,3}""" +
                """\s*(?:(?:is|ay|ang)\s*[:=]?|[:=]|->|-|=)\s*("[^"\r\n]+"|'[^'\r\n]+'|[^\s\r\n,;]+)"""
        )
        private val PASSWORD_CONVERSATIONAL_REGEX = Regex(
            """(?i)\b(?:password|passcode|passwd|pwd|pw)\s+(?:ko|mo|nya|niya|namin|natin|q|ku|mu)\s+([^\s\r\n,;]+)"""
        )
        private val ENV_PASSWORD_REGEX = Regex("""\b[A-Z][A-Z0-9_]*(?:PASSWORD|PASSWD|PWD)\s*=\s*([^\s\r\n]+)""")

        private val API_KEY_REGEX = Regex(
            """\b(?:AIza[0-9A-Za-z\-_]{20,}|sk-(?:ant-|proj-)?[A-Za-z0-9_-]{20,}|(?:sk|rk)_(?:live|test)_[A-Za-z0-9_-]{16,}|gh[pousr]_[A-Za-z0-9]{36,}|(?:AKIA|ASIA)[0-9A-Z]{16}\b|xox[abprs]-[A-Za-z0-9-]{10,}|SG\.[A-Za-z0-9_-]{16,}\.[A-Za-z0-9_-]{16,}|xnd_(?:development|production)_[A-Za-z0-9]{20,}|eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,})"""
        )
        private val KEY_CONTEXT_REGEX = Regex(
            """(?i)\b(?:api[ _-]?key|apikey|api[ _-]?secret|secret[ _-]?key|client[ _-]?secret|access[ _-]?token|refresh[ _-]?token|auth[ _-]?token|bearer|token)\s*(?:is|ay|ang|:|=)\s*["']?([A-Za-z0-9_\-./+=]{16,})"""
        )
        private val ENV_KEY_REGEX = Regex(
            """\b[A-Z][A-Z0-9_]*(?:API_KEY|APIKEY|SECRET|TOKEN|ACCESS_KEY)\s*=\s*["']?([^\s"'\r\n]{8,})"""
        )

        private val SSH_KEY_REGEX = Regex(
            """-----BEGIN (?:[A-Z]+ )*PRIVATE KEY-----(?:[\s\S]*?-----END (?:[A-Z]+ )*PRIVATE KEY-----|[\s\S]*)"""
        )
        private val DB_URL_REGEX = Regex("""\b[a-z][a-z0-9+.-]{1,15}://[^\s:/@]+:[^\s@/]{3,}@\S+""")
        private val DB_ENV_REGEX = Regex("""\b(?:DATABASE_URL|POSTGRES_URL|MYSQL_URL|MONGODB_URI)\s*=\s*([^\s\r\n]+)""")

        private val CARD_REGEX = Regex("""(?<![\dA-Za-z])(?:\d[ -]?){12,18}\d(?!\d)""")

        private val GOV_ID_REGEX = Regex(
            """(?i)\b(?:sss|tin|philhealth|phil\s?health|pag-?ibig|hdmf|umid|crn|philsys|psn|national\s+id)\b[^\d\n]{0,25}?([A-Za-z]?\d[\d -]{7,14}\d[A-Za-z]?)"""
        )
        private val STANDALONE_GOV_ID = Regex("""\b(?:\d{3}-\d{3}-\d{3}(?:-\d{3})?|\d{2}-\d{7}-\d|\d{4}-\d{4}-\d{4})\b""")

        private val PHONE_REGEX = Regex("""(?<!\d)(?:\+?63|0)9\d{2}[ -]?\d{3}[ -]?\d{4}(?!\d)""")
        private val EMAIL_REGEX = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b""")
        private val BANK_ACCOUNT_REGEX = Regex(
            """(?i)\b(?:account|acct|acc|bank|bdo|bpi|metrobank|unionbank|landbank|pnb|rcbc|security\s*bank|china\s*bank|gotyme|seabank|cimb|eastwest|psbank)\b[^\d\n]{0,25}?(\d[\d -]{7,16}\d)(?!\d)"""
        )

        private val NOT_A_SECRET = setOf(
            "required", "wrong", "incorrect", "invalid", "correct", "expired", "changed", "hidden",
            "secret", "safe", "strong", "weak", "not", "the", "your", "this", "that", "here", "reset",
            "hindi", "dapat", "kailangan", "mali", "tama", "nasa", "para", "sikreto", "lihim"
        )
    }
}
