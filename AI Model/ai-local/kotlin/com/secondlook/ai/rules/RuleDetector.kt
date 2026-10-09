package com.secondlook.ai.rules

import com.secondlook.ai.Detector
import com.secondlook.ai.Redactor
import com.secondlook.ai.RiskCategory
import com.secondlook.ai.RiskResult
import com.secondlook.ai.Severity

/** Luhn checksum used to reject random digit runs that are not payment card numbers. */
object Luhn {
    fun isValid(digits: String): Boolean {
        if (digits.isEmpty() || digits.any { it !in '0'..'9' }) return false
        var sum = 0
        var double = false
        for (i in digits.length - 1 downTo 0) {
            var d = digits[i] - '0'
            if (double) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            double = !double
        }
        return sum % 10 == 0
    }
}

/**
 * Fast, model-free checks. Works with no ONNX model bundled, so it is the Gate 1 fallback.
 *
 * Context matters: a bare six-digit number never triggers; "verification code is 482913" does.
 * Filipino / Taglish phrasing is covered: "password ko ay ...", "password ng wifi namin ay ...", "api key ko ay ...",
 * "SSS number ko ...", "TIN ko: ...".
 *
 * HIGH severity: passwords, PINs / OTPs / 2FA and authentication codes, API keys and tokens, SSH / private keys,
 * database credentials, cards (Luhn) and CVV, government IDs.
 * MEDIUM severity ("Intended?"): email, Philippine mobile numbers and bank account numbers. Small businesses share
 * those on purpose to get paid, so they get a soft warning; each can be switched off
 * ([flagPhoneNumbers], [flagEmails], [flagBankAccounts]).
 *
 * Keep in sync with web-demo/public/rules.js.
 */
class RuleDetector(
    private val flagPhoneNumbers: Boolean = true,
    private val flagEmails: Boolean = true,
    private val flagBankAccounts: Boolean = true,
) : Detector {

    override fun detect(text: String): RiskResult? {
        if (text.isBlank()) return null
        val hits = ArrayList<RiskResult>()
        hits += cardHits(text)
        hits += cvvHits(text)
        hits += keyHits(text)
        hits += passwordHits(text)
        hits += codeHits(text)
        hits += govIdHits(text)
        if (flagEmails) hits += softHits(text, EMAIL, "email", "This contains an email address. Intended?")
        if (flagPhoneNumbers) hits += softHits(text, PHONE, "phone", "This contains a phone number. Intended?")
        if (flagBankAccounts) hits += softHits(text, BANK_ACCOUNT, "bank_account", "This contains a bank account number. Intended?")
        hits.removeAll { Redactor.isMasked(text.substring(it.start, it.end)) }   // already hidden with "Itago"
        return hits.maxByOrNull { it.confidence }   // first max wins on ties
    }

    private fun cvvHits(text: String): List<RiskResult> =
        CVV.findAll(text).mapNotNull { m ->
            val g = m.groups[1] ?: return@mapNotNull null
            RiskResult(
                RiskCategory.CARD, g.range.first, g.range.last + 1,
                "This looks like a card security code (CVV). Sharing it could let someone use your card.",
                0.9f, kind = "credit_card",
            )
        }.toList()

    private fun cardHits(text: String): List<RiskResult> =
        CARD.findAll(text).mapNotNull { m ->
            val digits = m.value.filter { it in '0'..'9' }
            if (digits.length in 13..19 && Luhn.isValid(digits) && !isReference(text, m.range.first)) {
                RiskResult(
                    RiskCategory.CARD, m.range.first, m.range.last + 1,
                    "This looks like a payment card number. Sharing it could let someone make charges.",
                    0.95f, kind = "credit_card",
                )
            } else null
        }.toList()

    /** "Ref No. 1023 4567 89012": GCash / bank transaction numbers are shared as proof of payment, not card numbers. */
    private fun isReference(text: String, start: Int): Boolean =
        REFERENCE.containsMatchIn(text.substring(maxOf(0, start - 25), start))

    private fun passwordHits(text: String): List<RiskResult> {
        val out = ArrayList<RiskResult>()
        for (m in PASSWORD.findAll(text)) {
            val g = m.groups[1] ?: continue
            val token = g.value.trimEnd('.', ',', ';', '!', '?', ')')
            if (token.length < 3 || token.lowercase() in NOT_A_SECRET) continue
            out += password(g.range.first, g.range.first + token.length, token.any { it.isDigit() })
        }
        for (m in CONVERSATIONAL_PASSWORD_AFTER.findAll(text) + CONVERSATIONAL_PASSWORD_BEFORE.findAll(text)) {
            val g = m.groups[1] ?: continue
            val token = g.value.trimEnd('.', ',', ';', '!', '?', ')')
            if (token.length < 3 || token.lowercase() in NOT_A_SECRET) continue
            if (out.none { it.start == g.range.first && it.end == g.range.first + token.length }) {
                out += passwordReminder(g.range.first, g.range.first + token.length, token.any { it.isDigit() })
            }
        }
        for (m in ENV_PASSWORD.findAll(text)) {
            val g = m.groups[1] ?: continue
            out += password(g.range.first, g.range.last + 1, true)
        }
        return out
    }

    private fun password(start: Int, end: Int, hasDigit: Boolean) = RiskResult(
        RiskCategory.PASSWORD, start, end,
        "This message appears to include a password. Sharing it could give someone access to your account.",
        if (hasDigit) 0.95f else 0.85f, kind = "password",
    )

    private fun passwordReminder(start: Int, end: Int, hasDigit: Boolean) = RiskResult(
        RiskCategory.PASSWORD, start, end,
        "This message may include a password. Intended?",
        if (hasDigit) 0.75f else 0.7f, kind = "password", severity = Severity.MEDIUM,
    )

    private fun codeHits(text: String): List<RiskResult> {
        val out = ArrayList<RiskResult>()
        for (re in listOf(CODE, BARE_CODE, OTP_SENT, OTP_SENT_BEFORE, APP_CODE, CODE_FROM_APP)) {
            for (m in re.findAll(text)) {
                val g = m.groups[1] ?: continue
                out += RiskResult(
                    RiskCategory.PASSWORD, g.range.first, g.range.last + 1,
                    "This looks like a verification code or PIN. Sharing it could let someone into your account.",
                    if (re === CODE) 0.85f else 0.8f, kind = "otp",
                )
            }
        }
        return out
    }

    private fun keyHits(text: String): List<RiskResult> {
        val out = ArrayList<RiskResult>()
        for ((re, conf) in KEY_PATTERNS) for (m in re.findAll(text)) out += apiKey(m.range.first, m.range.last + 1, conf)
        for (m in KEY_CONTEXT.findAll(text)) {
            val g = m.groups[1] ?: continue
            if (looksLikeSecret(g.value)) out += apiKey(g.range.first, g.range.last + 1, 0.9f)
        }
        for (m in ENV_KEY.findAll(text)) {
            val g = m.groups[1] ?: continue
            out += apiKey(g.range.first, g.range.last + 1, 0.95f)
        }
        return out
    }

    private fun apiKey(start: Int, end: Int, confidence: Float) = RiskResult(
        RiskCategory.API_KEY, start, end,
        "This looks like an API key or access token. Anyone with it could use your account or run up charges.",
        confidence, kind = "api_key",
    )

    /** A long value that mixes digits or letter-case, so plain words after "token" don't count. */
    private fun looksLikeSecret(v: String): Boolean =
        v.length >= 16 && (v.any { it.isDigit() } || (v.any { it.isUpperCase() } && v.any { it.isLowerCase() }))

    private fun govIdHits(text: String): List<RiskResult> =
        GOV_ID.findAll(text).mapNotNull { m ->
            val g = m.groups[1] ?: return@mapNotNull null
            RiskResult(
                RiskCategory.OTHER_PII, g.range.first, g.range.last + 1,
                "This looks like a government ID number. Sharing it could allow identity theft.",
                0.9f, kind = "gov_id",
            )
        }.toList()

    private fun softHits(text: String, re: Regex, kind: String, reason: String): List<RiskResult> =
        re.findAll(text).map {
            val r = if (it.groups.size > 1) it.groups[1]?.range ?: it.range else it.range   // the number itself when grouped
            RiskResult(RiskCategory.OTHER_PII, r.first, r.last + 1, reason, 0.7f, kind = kind, severity = Severity.MEDIUM)
        }.toList()

    private companion object {
        // 13-19 digits, optionally separated by single spaces or dashes
        val CARD = Regex("""(?<!\d)(?:\d[ -]?){12,18}\d(?!\d)""")

        val EMAIL = Regex("""(?i)\b[a-z0-9._%+-]+@[a-z0-9-]+(?:\.[a-z0-9-]+)*\.[a-z]{2,}\b""")

        // Philippine mobile: 09XX XXX XXXX, +63 9XX XXX XXXX
        val PHONE = Regex("""(?<!\d)(?:\+?63|0)9\d{2}[ -]?\d{3}[ -]?\d{4}(?!\d)""")

        // "password is X", "pw: X", "password ko ay X", "password ng wifi namin ay X", "password for the shared drive is X".
        // Needs an explicit connector so "reset your password today" stays quiet.
        val PASSWORD = Regex(
            // "pass" alone only with a ":"/"=" ("pass: x", "pass ng FB ko: x"), so "ang pass ng bus ay libre" stays quiet
            """(?i)\b(?:password|passcode|passwd|pwd|pw|pasword|paswrd|passwrd|pass(?=\s*[:=]|\s+(?:ng|sa)\s+[\w-]+(?:\s+(?:ko|mo|namin|natin))?\s*[:=]))\b""" +
                """(?:\s+(?:ko|mo|nya|niya|namin|natin|po|(?:for|ng|sa|para\s+sa)\s+(?:the\s+|my\s+|our\s+|ang\s+)?[\w-]+(?:\s+[\w-]+)?)){0,3}""" +
                """\s*(?:(?:is|ay|ang)\s*[:=]?|[:=]|->|-)\s*["']?([^\s"']{3,})""",
        )
        private const val PASSWORD_WORD = """(?:password|passcode|passwd|pwd|pw|pasword|paswrd|passwrd)"""
        private const val PASSWORD_CANDIDATE = """(?<![\w])(?:\d{3,8}|[A-Za-z][A-Za-z0-9_#@!.-]*\d[A-Za-z0-9_#@!.-]*)(?![\w])"""
        val CONVERSATIONAL_PASSWORD_AFTER = Regex(
            """(?i)\b$PASSWORD_WORD\b[^\d\n]{0,40}?($PASSWORD_CANDIDATE)""",
        )
        val CONVERSATIONAL_PASSWORD_BEFORE = Regex(
            """(?i)($PASSWORD_CANDIDATE)[^\d\n]{0,40}?\b$PASSWORD_WORD\b""",
        )

        // OTP / PIN / passcode / 2FA words followed (within a short distance) by 4-8 digits: "Passcode ko 0517", "6-digit code 739204"
        val CODE = Regex(
            """(?i)\b(?:otp|one[- ]time\s+(?:password|passcode|code|pin)|verification\s+code|security\s+code|authentication\s+code|auth\s+code|confirmation\s+code|login\s+code|2fa|mfa|two[- ]factor(?:\s+code)?|backup\s+code|recovery\s+code|m-?pin|passcode|atm\s+pin|pin\s+code|(?:my|your)\s+pin|pin\s+(?:ko|mo|namin)|pin\s+(?:ng|sa)\s+[\w-]+|\d[- ]?digits?\s+(?:code|pin|otp))\b[^\d\n]{0,25}?(\d{4,8})(?!\d)""",
        )

        // E-wallet / bank / app names: "GCash code ko 553201", "code galing GCash: 482913", "Maya OTP 739204".
        // The digits must not be glued to letters ("GCash code PROMO2026" stays quiet).
        private const val APPS = """(?:g-?cash|maya|paymaya|gotyme|seabank|shopee(?:pay)?|lazada|grab(?:pay)?|coins(?:\.ph)?|bdo|bpi|unionbank|metrobank|landbank|facebook|fb|gmail|google|messenger|viber|telegram|tiktok|roblox)"""
        private const val CODE_WORD = """(?:code|otp|m?pin|passcode|auth(?:entication)?\s+code|verification\s+code)"""
        val APP_CODE = Regex("""(?i)\b$APPS\s+$CODE_WORD\b[^\d\n]{0,25}?(?<![A-Za-z])(\d{4,8})(?!\d)""")

        // Everyday phrasing (lola, kids): "eto yung code na pinadala sa akin: 482913", "may dumating na code, 739204",
        // "the code they sent me is 551203". Never promo / voucher / tracking / order codes.
        private const val SENT = """(?:pinadala|ipinadala|dumating|natanggap|na-?receive|sinend|sinent|tinext|nag-?text|sent|received|texted|got)"""
        private const val NOT_OTP = """(?<!\b(?:zip|postal|area|promo|tracking|order|product|country|bar|qr|voucher|discount|referral)\s)"""
        val OTP_SENT = Regex("""(?i)$NOT_OTP\b(?:code|otp|pin)\b[^\d\n]{0,40}?\b$SENT\b[^\d\n]{0,25}?(\d{4,8})(?!\d)""")
        val OTP_SENT_BEFORE = Regex("""(?i)\b$SENT\b[^\d\n]{0,25}?$NOT_OTP\b(?:code|otp|pin)\b[^\d\n]{0,15}?(\d{4,8})(?!\d)""")
        val CODE_FROM_APP = Regex("""(?i)$NOT_OTP\b$CODE_WORD\s+(?:ng|sa|galing(?:\s+sa)?|from|para\s+sa)\s+$APPS\b[^\d\n]{0,25}?(?<![A-Za-z])(\d{4,8})(?!\d)""")

        // Words just before a long number that make it a transaction reference, not a card: "Ref No.", "Trace no."
        val REFERENCE = Regex("""(?i)\b(?:ref(?:erence)?|trace|txn|transaction|tracking|order|invoice)\b[^\d\n]{0,12}$""")

        // CVV / CVC: "CVV 123", "yung CVV sa likod ng card ko 123"
        val CVV = Regex("""(?i)\b(?:cvv2?|cvc2?|csc)\b[^\d\n]{0,25}?(\d{3,4})(?!\d)""")

        // Bank account numbers (soft warning): "BDO account no. 0012345678", "Account no. 0012345678 BDO"
        val BANK_ACCOUNT = Regex(
            """(?i)\b(?:account|acct|acc|bank|bdo|bpi|metrobank|unionbank|landbank|pnb|rcbc|security\s*bank|china\s*bank|gotyme|seabank|cimb|eastwest|psbank)\b""" +
                """[^\d\n]{0,25}?(\d[\d -]{7,16}\d)(?!\d)""",
        )

        // "The code is 482916" / "code: 482916" / "code mo ay 482916" / "code ko: 482916", but not zip / promo / tracking / order codes
        val BARE_CODE = Regex(
            """(?i)(?<!\b(?:zip|postal|area|promo|tracking|order|product|country|bar|qr)\s)\bcode\s*(?:(?:is|ay)\s*[:=]?|[:=]|(?:mo|ko)(?:\s+ay|\s*[:=]))\s*(\d{4,8})(?!\d)""",
        )

        // KEY_NAME [ko|namin|...] [is|ay|:|=] VALUE   e.g. "api key ko ay Qm8vXz4TnL2pWc9R", "token: 8f3a9c1d7b2e4f60a5d9"
        val KEY_CONTEXT = Regex(
            """(?i)\b(?:api[ _-]?key|apikey|api[ _-]?secret|secret[ _-]?key|client[ _-]?secret|access[ _-]?token|refresh[ _-]?token|auth(?:orization)?[ _-]?token|bearer|token|private[ _-]?key|service[ _-]?account[ _-]?key|susi)\b""" +
                """(?:\s+(?:ko|mo|nya|niya|namin|natin|po|(?:ng|sa|para\s+sa)\s+[\w-]+))?""" +
                """[^\S\n]*(?:(?:is|ay|ang)\s*)?[:=]?[^\S\n]*["']?([A-Za-z0-9_\-./+=]{16,})""",
        )

        // .env style:  GEMINI_API_KEY=AIza...   DB_PASSWORD=...
        val ENV_KEY = Regex("""\b[A-Z][A-Z0-9_]*(?:API_KEY|APIKEY|SECRET|TOKEN|PRIVATE_KEY|ACCESS_KEY)\s*=\s*["']?([^\s"']{8,})""")
        val ENV_PASSWORD = Regex("""\b[A-Z][A-Z0-9_]*(?:PASSWORD|PASSWD|PWD)\s*=\s*["']?([^\s"']{4,})""")

        // Provider-shaped secrets: no context needed. All examples in test-data are invented.
        val KEY_PATTERNS: List<Pair<Regex, Float>> = listOf(
            Regex("""\bAIza[0-9A-Za-z_-]{35}\b""") to 0.97f,                                          // Google / Gemini
            Regex("""\bsk-(?:ant-|proj-)?[A-Za-z0-9_-]{20,}""") to 0.97f,                              // OpenAI / Anthropic style
            Regex("""\b(?:AKIA|ASIA)[0-9A-Z]{16}\b""") to 0.97f,                                       // AWS access key id
            Regex("""\bgh[pousr]_[A-Za-z0-9]{36,}""") to 0.97f,                                        // GitHub
            Regex("""\b(?:sk|rk)_(?:live|test)_[A-Za-z0-9]{16,}""") to 0.97f,                          // Stripe / PayMongo style
            Regex("""\bxnd_(?:development|production)_[A-Za-z0-9]{20,}""") to 0.97f,                   // Xendit
            Regex("""\bxox[abprs]-[A-Za-z0-9-]{10,}""") to 0.97f,                                      // Slack
            Regex("""\bGOCSPX-[A-Za-z0-9_-]{20,}""") to 0.97f,                                         // Google OAuth client secret
            Regex("""\bhf_[A-Za-z0-9]{30,}""") to 0.97f,                                               // Hugging Face
            Regex("""\bgsk_[A-Za-z0-9]{40,}""") to 0.97f,                                              // Groq
            Regex("""\bnpm_[A-Za-z0-9]{36}\b""") to 0.97f,                                             // npm
            Regex("""\bSG\.[A-Za-z0-9_-]{16,}\.[A-Za-z0-9_-]{16,}""") to 0.97f,                       // SendGrid
            Regex("""\b\d{8,10}:[A-Za-z0-9_-]{35}\b""") to 0.95f,                                      // Telegram bot token
            Regex("""\beyJ[A-Za-z0-9_-]{8,}\.eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}""") to 0.95f,     // JWT
            // PEM / SSH private key: the whole block (to the END line, or to the end of the text) so "Itago" removes it all
            Regex("""-----BEGIN (?:[A-Z]+ )*PRIVATE KEY-----(?:[\s\S]*?-----END (?:[A-Z]+ )*PRIVATE KEY-----|[\s\S]*)""") to 0.99f,
            Regex("""\b[a-z][a-z0-9+.-]{1,15}://[^\s:/@]+:[^\s@/]{3,}@\S+""") to 0.95f,                // URL with user:password@
        )

        // Philippine government IDs + passport/licence: keyword, then the number
        val GOV_ID = Regex(
            """(?i)\b(?:sss|tin|philhealth|phil\s?health|pag-?ibig|hdmf|umid|crn|philsys|psn|national\s+id|passport|driver'?s?\s+licen[sc]e|lisensya)\b""" +
                """[^\d\n]{0,25}?([A-Za-z]?\d[\d -]{5,18}\d[A-Za-z]?)""",
        )

        val NOT_A_SECRET = setOf(
            "required", "wrong", "incorrect", "invalid", "correct", "expired", "changed", "hidden",
            "secret", "safe", "strong", "weak", "not", "the", "your", "this", "that", "here", "reset",
            // Filipino / Taglish filler that can follow "password ay ..."
            "hindi", "dapat", "kailangan", "mali", "tama", "nasa", "para", "sikreto", "lihim",
            "mahina", "malakas", "ishare", "i-share", "nag-expire", "naka-save", "nakalimutan",
        )
    }
}
