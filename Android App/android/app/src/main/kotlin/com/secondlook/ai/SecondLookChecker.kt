package com.secondlook.ai

import com.secondlook.ai.rules.RuleDetector

/**
 * Entry point of the AI core. The keyboard and the Flutter bridge both call [check].
 * Everything here runs on-device; there is no network access.
 *
 * Call from a background thread / coroutine, never the keyboard's UI thread.
 */
class SecondLookChecker(
    private val rules: Detector = RuleDetector(),
    private val pii: Detector? = null,          // PiiDetector once the model is loaded
    private val notes: Detector? = null,        // NoteMatcher once "Teach me" is enabled
    private val taught: Detector? = null,       // TaughtSecretMatcher: values the user taught (MPIN, password, key)
) {
    class StageReport(val name: String, val result: RiskResult?, val millis: Long)
    class Report(val result: RiskResult?, val stage: String?, val stages: List<StageReport>)

    /** Stages run cheapest-first; the first hit wins and later stages are skipped. A taught secret is the most specific. */
    fun check(text: String): RiskResult? {
        if (text.isBlank()) return null
        taught?.detect(text)?.let { return it }
        rules.detect(text)?.let { return it }
        pii?.detect(text)?.let { return it }
        return notes?.detect(text)
    }

    /** Runs every stage (no short-circuit) and reports timings. For test apps and tuning, not the keyboard. */
    fun checkDetailed(text: String): Report {
        val stages = ArrayList<StageReport>()
        fun run(name: String, d: Detector?) {
            if (d == null) return
            val t0 = System.nanoTime()
            val r = if (text.isBlank()) null else d.detect(text)
            stages += StageReport(name, r, (System.nanoTime() - t0) / 1_000_000)
        }
        run("taught-secrets", taught)
        run("rules", rules)
        run("pii-model", pii)
        run("private-notes", notes)
        val first = stages.firstOrNull { it.result != null }
        return Report(first?.result, first?.name, stages)
    }
}
