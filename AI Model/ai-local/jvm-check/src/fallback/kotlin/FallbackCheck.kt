package com.secondlook.check

import com.secondlook.WarningType
import com.secondlook.ai.RuleBasedAiAdapter
import com.secondlook.ai.SecondLookChecker
import com.secondlook.ai.Severity
import com.secondlook.ai.model.ModelConfig
import com.secondlook.ai.model.PiiDetector
import com.secondlook.ai.rules.RuleDetector
import org.json.JSONObject
import java.io.File
import java.util.Locale

/*
 * gradlew fallbackCheck: on the same messages, compares
 *   - the ai-local core (rules + PII model, what OnnxAiAdapter runs),
 *   - the keyboard's RuleBasedAiAdapter (read from Android App, copied at build time, not modified),
 *   - the keyboard's DetectionEngine today: activeAdapter.detectSync(text) ?: fallbackAdapter.detectSync(text).
 * No notes are saved, so the vault rule never fires. Answers handoff/android-app/README.md step 4.
 * Verdicts: red (HIGH), yellow (MEDIUM "Intended?"), quiet. Also writes ../eval/results/keyboard-fallback-predictions.csv.
 */
private class Msg(val set: String, val text: String, val expect: String)

fun main() {
    val msgs = ArrayList<Msg>()
    fun load(path: String, set: String) {
        val a = JSONObject(File(path).readText()).getJSONArray("detect")
        for (i in 0 until a.length()) a.getJSONObject(i).let { msgs += Msg(set, it.getString("text"), it.getString("expect")) }
    }
    load("../test-data/samples.json", "synthetic (samples.json)")
    load("../test-data/external/ph-sms-onzero0.json", "real PH SMS")
    load("../test-data/external/ph-sms-scottleechua.json", "real PH SMS")

    val models = File("../assets/models")
    val pii = PiiDetector(
        File(models, "pii/model_quantized.onnx").readBytes(), File(models, "pii/vocab.txt").readLines().filter { it.isNotEmpty() },
        ModelConfig.parseId2Label(File(models, "pii/config.json").readText()),
    )
    val checker = SecondLookChecker(RuleDetector(), pii)
    val fallbackAdapter = RuleBasedAiAdapter()

    class V(val core: String, val regex: String) { val engine get() = if (core != "quiet") core else regex }
    val v = msgs.map { m ->
        V(checker.check(m.text)?.let { if (it.severity == Severity.MEDIUM) "yellow" else "red" } ?: "quiet",
            fallbackAdapter.detectSync(m.text)?.let { if (it.type == WarningType.MEDIUM) "yellow" else "red" } ?: "quiet")
    }
    for (set in msgs.map { it.set }.distinct()) {
        val idx = msgs.indices.filter { msgs[it].set == set }
        val risky = idx.filter { msgs[it].expect != "none" }
        val safe = idx.filter { msgs[it].expect == "none" }
        println("== $set: ${risky.size} risky, ${safe.size} safe")
        for ((name, pick) in listOf<Pair<String, (V) -> String>>(
            "ai-local core (OnnxAiAdapter)" to { it.core }, "keyboard regex (RuleBasedAiAdapter)" to { it.regex },
            "DetectionEngine today (core ?: regex)" to { it.engine },
        )) {
            println(String.format(Locale.ROOT, "   %-40s caught %4d/%d   red false alarms %4d   yellow on safe %4d", name,
                risky.count { pick(v[it]) != "quiet" }, risky.size, safe.count { pick(v[it]) == "red" }, safe.count { pick(v[it]) == "yellow" }))
        }
        val added = safe.filter { v[it].core == "quiet" && v[it].regex == "red" }
        val rescued = risky.filter { v[it].core == "quiet" && v[it].regex != "quiet" }
        println("   red false alarms the regex fallback adds: ${added.size}; risky messages only the regex catches: ${rescued.size}")
        added.take(5).forEach { println("      e.g. ${msgs[it].text.replace("\n", " ").take(110)}") }
        rescued.forEach { println("      only regex: ${msgs[it].text.replace("\n", " ").take(110)}") }
    }
    // one row per message, like eval/results/predictions.csv (eval/ is not committed)
    val csv = { s: String -> "\"" + s.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"" }
    val out = File("../eval/results/keyboard-fallback-predictions.csv")
    out.parentFile.mkdirs()
    out.writeText("dataset,expected,ai_core,keyboard_regex,detection_engine_today,text\n" + msgs.indices.joinToString("") { i ->
        "${csv(msgs[i].set)},${msgs[i].expect},${v[i].core},${v[i].regex},${v[i].engine},${csv(msgs[i].text)}\n"
    })
    println("\nWrote ${out.canonicalPath}")
}
