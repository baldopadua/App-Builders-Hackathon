package com.secondlook

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.secondlook.ai.Redactor
import com.secondlook.ai.RiskResult
import com.secondlook.ai.SafetyTips
import com.secondlook.ai.SecondLookChecker
import com.secondlook.ai.Severity
import com.secondlook.ai.android.AssetModels
import com.secondlook.ai.android.EncryptedNotesStore
import com.secondlook.ai.model.PiiDetector
import com.secondlook.ai.notes.NoteMatcher
import com.secondlook.ai.notes.NotesStore
import com.secondlook.ai.notes.TaughtSecretMatcher
import com.secondlook.ai.rules.RuleDetector
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * The API the team manual (section 10) calls: `DetectionEngine(context).detect(text): List<Warning>`.
 * It is a thin adapter over the ai-local core ([SecondLookChecker]); the detection logic lives in com.secondlook.ai.
 *
 *  - Models load on a background thread. Until they are ready, [detect] uses the rule checks only,
 *    so typing never blocks and the keyboard never waits.
 *  - Use [get] so the keyboard service and the Flutter activity (same process) share ONE instance
 *    and the 50 MB of models are loaded once.
 *  - [detect] can take tens of ms with the models: call it off the main thread, or use [detectAsync].
 *  - Nothing is logged or stored. No network permission is needed.
 */
class DetectionEngine private constructor(context: Context) : AutoCloseable {

    /** The shape the manual's keyboard code expects. [type] is the fine-grained kind (see docs). */
    data class Warning(
        val type: String,         // password | otp | api_key | credit_card | gov_id | email | phone | bank_account | pii | private_match | my_secret
        val message: String,      // user-facing English default; strings.xml may override by [type]
        val severity: String,     // "high" (red banner) | "medium" (soft yellow, "Intended?")
        val matchedText: String,  // the part of the text that triggered
        val start: Int = 0,       // where matchedText starts in the checked text
        val end: Int = 0,         // where it ends (exclusive)
        val tip: String = "",     // one line of Taglish advice (SafetyTips); strings.xml may override by type
        val canHide: Boolean = false,  // true -> show the "Itago" button (everything except private-note matches)
        val headline: String = "",     // short Filipino-first title, e.g. "Teka! OTP o PIN ito — huwag ibigay kahit kanino."
    )

    /**
     * Save / list / delete what the user taught ("Teach me what's private"): private facts (matched by meaning + names)
     * and secret values such as their MPIN or password (matched exactly, kept only as a fingerprint). Encrypted on the phone.
     */
    val secrets = SecretManager(this)

    @Volatile private var checker = SecondLookChecker(RuleDetector())
    @Volatile var isReady = false
        private set
    @Volatile var loadError: String? = null
        private set

    private val ready = CountDownLatch(1)
    private var store: NotesStore? = null
    private var pii: PiiDetector? = null
    private var matcher: NoteMatcher? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    init {
        val appContext = context.applicationContext
        Thread({
            try {
                val p = AssetModels.piiDetector(appContext)
                val e = AssetModels.embeddingModel(appContext)
                val s = EncryptedNotesStore(appContext, e)
                val m = NoteMatcher(e, s)
                pii = p; store = s; matcher = m
                checker = SecondLookChecker(RuleDetector(), p, m, TaughtSecretMatcher(s))
                isReady = true
            } catch (t: Throwable) {
                loadError = "${t.javaClass.simpleName}: ${t.message}"   // rules keep working
            } finally {
                ready.countDown()
            }
        }, "secondlook-model-loader").start()
    }

    /** Blocks until the models are loaded (or failed). Returns [isReady]. Never call on the keyboard's UI thread. */
    fun awaitReady(timeoutMs: Long = 20_000): Boolean {
        ready.await(timeoutMs, TimeUnit.MILLISECONDS)
        return isReady
    }

    /** At most one warning (the most important). Empty list = nothing found. Blocking; see class doc. */
    fun detect(text: String): List<Warning> {
        val r = checker.check(text) ?: return emptyList()
        return listOf(r.toWarning(text))
    }

    /** Runs [detect] on a background thread and delivers the result on the main thread. Newer calls queue behind older ones. */
    fun detectAsync(text: String, onResult: (List<Warning>) -> Unit) {
        worker.execute {
            val result = detect(text)
            main.post { onResult(result) }
        }
    }

    /** "Itago": [text] with this warning's secret masked (e.g. AIza••••), or null for private-note matches. */
    fun hide(text: String, warning: Warning): String? {
        val s = warning.start.coerceIn(0, text.length)
        val e = warning.end.coerceIn(s, text.length)
        val masked = Redactor.maskSpan(text.substring(s, e), warning.type) ?: return null
        return text.substring(0, s) + masked + text.substring(e)
    }

    /** "Itago lahat": masks every secret found. Blocking (runs the detectors): call off the main thread. */
    fun hideAll(text: String): String = Redactor.hideAll(text, { checker.check(it) })

    /** How similar (0..1) a draft must be to a private note when no private word appears. Default 0.75. */
    fun setNoteSimilarityThreshold(value: Float) { matcher?.threshold = value }

    internal fun requireStore(): NotesStore {
        awaitReady()
        return store ?: throw IllegalStateException("AI models not loaded: ${loadError ?: "timeout"}")
    }

    override fun close() { worker.shutdown(); pii?.close() }

    private fun RiskResult.toWarning(text: String): Warning {
        val s = start.coerceIn(0, text.length)
        val e = end.coerceIn(s, text.length)
        return Warning(
            type = kind,
            message = reason,
            severity = if (severity == Severity.HIGH) "high" else "medium",
            matchedText = text.substring(s, e),
            start = s,
            end = e,
            tip = SafetyTips.forKind(kind),
            canHide = kind != "private_match",
            headline = SafetyTips.headline(kind, noteLabel),
        )
    }

    companion object {
        @Volatile private var instance: DetectionEngine? = null

        /** Process-wide singleton. Safe to call from the IME's onCreate (it returns immediately; models load in the background). */
        fun get(context: Context): DetectionEngine =
            instance ?: synchronized(this) { instance ?: DetectionEngine(context.applicationContext).also { instance = it } }
    }
}
