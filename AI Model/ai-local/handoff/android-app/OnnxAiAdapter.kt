package com.secondlook.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.secondlook.Warning
import com.secondlook.WarningType
import com.secondlook.ai.android.AssetModels
import com.secondlook.ai.notes.NoteMatcher
import com.secondlook.ai.notes.TaughtSecretMatcher
import com.secondlook.ai.rules.RuleDetector
import java.util.concurrent.Executors

/**
 * The ai-local AI core behind the keyboard's [AiDetectionAdapter] contract (handoff from AI Model/ai-local).
 *
 * DetectionEngine.discoverAnnieAiAdapter() looks for this exact class name, com.secondlook.ai.OnnxAiAdapter, so the
 * keyboard switches from RuleBasedAiAdapter to it without any change to DetectionEngine, SecondLookIME or Flutter.
 * If the models are missing or fail to load, init() throws and DetectionEngine keeps the rule-based fallback.
 *
 * Checks, cheapest first (same code and order as ai-local's jvm-check and test-app):
 *   taught secret values (exact, fingerprint) -> rules -> PII model -> private notes (meaning + names)
 * plus the vault's old exact rule: a saved value typed word for word is always flagged.
 * Notes are the ones saved through SecretManager (the Flutter "Teach me" screen), see [SecretManagerNotesStore].
 *
 * Do not edit this copy in the Android app: change it in AI Model/ai-local/handoff/android-app and copy it again.
 */
class OnnxAiAdapter : AiDetectionAdapter {

    override val engineName: String = "OnnxAiAdapter"
    override val isReady: Boolean get() = checker != null

    @Volatile private var checker: SecondLookChecker? = null
    @Volatile private var store: SecretManagerNotesStore? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** Called by DetectionEngine on its background thread. Loads both models (about 1-2 s on a phone). */
    override fun init(context: Context) {
        val app = context.applicationContext
        val t0 = System.currentTimeMillis()
        val pii = AssetModels.piiDetector(app)
        val embedder = AssetModels.embeddingModel(app)
        val s = SecretManagerNotesStore(app, embedder)
        store = s
        checker = SecondLookChecker(RuleDetector(), pii, NoteMatcher(embedder, s), TaughtSecretMatcher(s))
        Log.i(TAG, "AI core ready in ${System.currentTimeMillis() - t0} ms")
    }

    /**
     * Full check. The keyboard calls this on Send (main thread) right after the 500 ms-pause check of the same text,
     * so that answer is reused from [recent] and Send never waits for a model twice.
     */
    override fun detectSync(text: String): Warning? {
        if (text.isBlank()) return null
        val c = checker ?: return null
        notesVersion = store?.version() ?: 0   // a note added or deleted in Flutter invalidates the cached answers
        val key = cacheKey(text)
        synchronized(recent) { recent[key] }?.let { return it.warning }
        val risk = c.check(text) ?: store?.verbatimMatch(text)
        val warning = risk?.let(::toWarning)
        synchronized(recent) { recent[key] = Answer(risk, warning) }
        return warning
    }

    override fun detectAsync(text: String, callback: (Warning?) -> Unit) {
        worker.execute {
            val w = try { detectSync(text) } catch (e: Exception) { Log.e(TAG, "check failed", e); null }
            main.post { callback(w) }
        }
    }

    /** "Itago": masks every secret the core finds (cards keep the last 4 digits). Private facts cannot be masked. */
    override fun hide(text: String, warning: Warning?): String {
        val c = checker ?: return text
        var out = Redactor.hideAll(text, { c.check(it) })
        store?.let { s -> for (v in s.verbatimValues()) out = out.replace(v, "••••••", ignoreCase = true) }
        return out
    }

    private class Answer(val risk: RiskResult?, val warning: Warning?)

    private fun toWarning(r: RiskResult): Warning {
        val tip = when {
            r.kind == "private_match" && !r.noteLabel.isNullOrBlank() ->
                "Kahawig ito ng private note mo na \"${r.noteLabel}\". " + SafetyTips.forKind(r.kind)
            else -> SafetyTips.forKind(r.kind)
        }
        return Warning(
            type = if (r.severity == Severity.MEDIUM) WarningType.MEDIUM else WarningType.HIGH,
            headline = SafetyTips.headline(r.kind, r.noteLabel),
            tip = tip,
        )
    }

    companion object {
        private const val TAG = "OnnxAiAdapter"

        /** Last answers by (saved notes, draft text); shared, so MainActivity can ask for the category of what it checked. */
        private val recent = object : LinkedHashMap<String, Answer>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Answer>?) = size > 32
        }
        @Volatile private var notesVersion = 0
        private fun cacheKey(text: String) = "$notesVersion\u0000$text"

        /** What kind of secret the last check of [text] found (password, otp, my_secret, private_match, ...), or null. */
        fun kindFor(text: String): String? = synchronized(recent) { recent[cacheKey(text)]?.risk?.kind }

        /** The Flutter pill label for the last check of [text], in the Practice screen's style ("BABALA · OTP"), or null. */
        fun categoryFor(text: String): String? = when (kindFor(text)) {
            null -> null
            "otp" -> "BABALA · OTP"
            "password" -> "BABALA · PASSWORD"
            "api_key" -> "BABALA · API KEY"
            "credit_card" -> "BABALA · CARD"
            "gov_id" -> "BABALA · GOV ID"
            "my_secret", "private_match" -> "BABALA · VAULT"
            "phone", "email", "bank_account" -> "PANSIN · INTENDED?"
            else -> "BABALA · PERSONAL INFO"
        }
    }
}
