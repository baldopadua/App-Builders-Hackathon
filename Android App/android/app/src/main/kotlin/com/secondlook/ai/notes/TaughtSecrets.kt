package com.secondlook.ai.notes

import com.secondlook.ai.Detector
import com.secondlook.ai.Redactor
import com.secondlook.ai.RiskCategory
import com.secondlook.ai.RiskResult
import com.secondlook.ai.rules.RuleDetector
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * "Teach me what's private" for secret VALUES: the user's own MPIN, passcode, password or API key.
 *
 * The built-in rules need a clue ("MPIN ko 4821"). A taught value is caught anywhere, even with no clue at all
 * ("eto na yung 4821"). Only a keyed fingerprint (HMAC-SHA256) of the value is kept, never the value itself.
 * Matching is exact (ignoring letter case, and spaces / dashes inside numbers). This part is deliberately not AI:
 * for your real PIN you want certainty, not a guess.
 *
 * JS twin: web-demo/public/core.js (teachValues / secretCandidates / taughtResult). Keep in sync.
 */
object TaughtSecrets {
    const val MIN_CHARS = 4

    private val WORD = Regex("""\S+""")
    private val NUMBER = Regex("""(?<!\d)\d(?:[ -]?\d)+(?!\d)""")
    private const val LEAD = "\"'([{<“‘"
    private const val TRAIL = ".,;:!?)]}>\"'”’"

    /**
     * The secret values in what the user typed into "Teach me": the whole entry when it is one word
     * ("4821", "Lodi1234", "AIza..."), otherwise every value the built-in rules find ("GCash MPIN ko: 4821" -> "4821").
     * Empty = a private fact, matched by meaning + names instead.
     */
    fun valuesIn(entry: String, rules: RuleDetector = RuleDetector()): List<String> {
        val t = entry.trim()
        if (t.isEmpty()) return emptyList()
        if (t.none { it.isWhitespace() }) return if (normalize(t).length >= MIN_CHARS) listOf(t) else emptyList()
        val out = ArrayList<String>()
        var rest = t
        for (i in 0 until 10) {
            val r = rules.detect(rest) ?: break
            val v = rest.substring(r.start, r.end)
            if (normalize(v).length >= MIN_CHARS) out += v
            rest = Redactor.hide(rest, r) ?: break
        }
        return out
    }

    /** Lowercase; a number loses its spaces and dashes ("4532 0151 1283 0366" = "4532015112830366"). */
    fun normalize(v: String): String {
        val t = v.trim()
        val s = if (t.any { it.isDigit() } && t.all { it.isDigit() || it == ' ' || it == '-' }) t.filter { it.isDigit() } else t
        return s.lowercase(Locale.ROOT)
    }

    /**
     * Every place in a draft a taught value could be, sorted by position: each word as typed, without surrounding
     * punctuation, after a ':' or '=' ("MPIN:4821", "pw=Lodi1234"), and each run of digits ("4532-0151-1283-0366").
     */
    fun candidates(draft: String): List<IntRange> {
        val out = ArrayList<IntRange>()
        fun add(s: Int, e: Int) { if (e - s >= MIN_CHARS) out += s until e }
        for (m in WORD.findAll(draft)) {
            val s = m.range.first
            val e = m.range.last + 1
            add(s, e)
            var a = s
            var b = e
            while (a < b && draft[a] in LEAD) a++
            while (b > a && draft[b - 1] in TRAIL) b--
            if (a != s || b != e) add(a, b)
            val cut = draft.lastIndexOfAny(charArrayOf(':', '='), b - 1)
            if (cut >= a && cut < b - 1) add(cut + 1, b)
        }
        for (m in NUMBER.findAll(draft)) add(m.range.first, m.range.last + 1)
        return out.sortedBy { it.first }
    }
}

/** Keyed fingerprint (HMAC-SHA256, hex) of a normalized secret value. Without the key, a fingerprint reveals nothing. */
class Fingerprinter(key: ByteArray) {
    private val key = SecretKeySpec(key.copyOf(), "HmacSHA256")

    fun of(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return mac.doFinal(TaughtSecrets.normalize(value).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun randomKey(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }
    }
}

/** Flags a draft that contains a secret value the user taught. Runs before every other check: exact and cheap. */
class TaughtSecretMatcher(private val store: NotesStore) : Detector {
    override fun detect(text: String): RiskResult? {
        if (text.isBlank()) return null
        val taught = store.notes().filter { it.fingerprints.isNotEmpty() }
        if (taught.isEmpty()) return null
        val fp = store.fingerprinter
        for (r in TaughtSecrets.candidates(text)) {
            val span = text.substring(r)
            if (Redactor.isMasked(span)) continue
            val f = fp.of(span)
            val note = taught.firstOrNull { f in it.fingerprints } ?: continue
            return RiskResult(
                RiskCategory.PRIVATE_NOTE, r.first, r.last + 1,
                "This is a secret you taught SecondLook: ${note.summary.label}.",
                0.99f, note.summary.label, kind = "my_secret",
            )
        }
        return null
    }
}
