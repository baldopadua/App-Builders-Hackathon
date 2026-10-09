package com.secondlook.ai

/**
 * "Itago" (hide it): masks the risky part of a draft so the rest can still be sent.
 *   password / OTP / taught secret -> ••••••      card / bank / phone -> •••• •••• •••• 0366 (last 4 kept)
 *   API key         -> AIza••••                    gov ID              -> •••-•••-•••-000 (last 3 kept)
 *   private key     -> [private key removed]       email               -> •••@example.com
 *   URL password    -> postgres://admin:••••••@db.example.com/...
 * Private-note matches are not masked: the user reviews them.
 *
 * Pure Kotlin (no Android), so it runs in the keyboard, the test app and the laptop check.
 * Detectors ignore spans that are already masked, so a hidden secret never warns again.
 * JS twin: web-demo/public/core.js (maskSpan / hide / hideAll). Keep in sync.
 */
object Redactor {
    const val DOT = '•'
    const val KEY_REMOVED = "[private key removed]"
    private const val MASK = "••••••"
    private val URL_PASSWORD = Regex("""(://[^\s:/@]+:)[^\s@/]+@""")

    /** Masked replacement for a risky span, or null when masking makes no sense (private notes). */
    fun maskSpan(span: String, kind: String): String? = when (kind) {
        "private_match" -> null
        "password", "otp", "my_secret" -> MASK
        "api_key" -> when {
            span.startsWith("-----BEGIN") -> KEY_REMOVED
            URL_PASSWORD.containsMatchIn(span) -> URL_PASSWORD.replace(span) { it.groupValues[1] + MASK + "@" }
            else -> span.take(4) + "••••"
        }
        "credit_card", "bank_account", "phone" -> keepLastDigits(span, 4)
        "gov_id" -> keepLastDigits(span, 3)
        "email" -> span.indexOf('@').let { at -> if (at > 0) "•••" + span.substring(at) else "•••" }
        else -> "••••"
    }

    /** The whole draft with this result's span masked, or null when it can't be masked (private notes). */
    fun hide(text: String, r: RiskResult): String? {
        val start = r.start.coerceIn(0, text.length)
        val end = r.end.coerceIn(start, text.length)
        val masked = maskSpan(text.substring(start, end), r.kind) ?: return null
        return text.substring(0, start) + masked + text.substring(end)
    }

    /** "Itago lahat": masks every secret the checker finds (at most [maxPasses]). Private-note matches stay for review. */
    fun hideAll(text: String, check: (String) -> RiskResult?, maxPasses: Int = 10): String {
        var t = text
        repeat(maxPasses) {
            val r = check(t) ?: return t
            t = hide(t, r) ?: return t
        }
        return t
    }

    /** True when the span is already masked, so detectors don't warn about their own mask. */
    fun isMasked(span: String): Boolean = DOT in span || KEY_REMOVED in span

    /** True when a model hit starts right after a mask (e.g. "@db.example.com" after "admin:••••••"): leftover of a hidden secret. */
    fun touchesMask(text: String, start: Int): Boolean = (maxOf(0, start - 2) until start.coerceAtMost(text.length)).any { text[it] == DOT }

    private fun keepLastDigits(s: String, keep: Int): String {
        val total = s.count { it.isDigit() }
        var seen = 0
        return buildString {
            for (c in s) {
                if (c.isDigit()) {
                    seen++
                    append(if (total > keep && seen > total - keep) c else DOT)   // short numbers (CVV) fully masked
                } else {
                    append(c)
                }
            }
        }
    }
}
