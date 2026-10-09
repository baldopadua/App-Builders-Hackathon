package com.secondlook.check

import com.secondlook.ai.Redactor
import com.secondlook.ai.SecondLookChecker
import com.secondlook.ai.model.EmbeddingModel
import com.secondlook.ai.model.ModelConfig
import com.secondlook.ai.model.PiiDetector
import com.secondlook.ai.model.WordPieceTokenizer
import com.secondlook.ai.notes.InMemoryNotesStore
import com.secondlook.ai.notes.NoteMatcher
import com.secondlook.ai.notes.TaughtSecretMatcher
import com.secondlook.ai.rules.Luhn
import com.secondlook.ai.rules.RuleDetector
import java.io.File

private class Sample(val text: String, val expect: String, val tags: String)
private class NoteSet(val label: String, val text: String, val positives: List<String>, val negatives: List<String>)
private class Taught(val label: String, val teach: String, val kind: String, val positives: List<String>, val negatives: List<String>)

private val STRING = "\"((?:[^\"\\\\]|\\\\.)*)\""

/** Reads the "detect" entries of ../test-data/samples.json ({ "text": ..., "expect": ... }) without a JSON library. */
private fun loadSamples(file: File): List<Sample> {
    val re = Regex("\"text\"\\s*:\\s*$STRING\\s*,\\s*\"expect\"\\s*:\\s*\"(\\w+)\"(?:\\s*,\\s*\"tags\"\\s*:\\s*\\[([^\\]]*)\\])?")
    return re.findAll(file.readText()).map { Sample(unescape(it.groupValues[1]), it.groupValues[2], it.groupValues[3]) }.toList()
}

/** Reads the "notes" sets (id, label, text, positives[], negatives[]). */
private fun loadNoteSets(file: File): List<NoteSet> {
    val items = Regex(STRING)
    val re = Regex(
        "\"id\"\\s*:\\s*\"\\w+\"\\s*,\\s*\"label\"\\s*:\\s*$STRING\\s*,\\s*\"text\"\\s*:\\s*$STRING\\s*,\\s*" +
            "\"positives\"\\s*:\\s*\\[(.*?)\\]\\s*,\\s*\"negatives\"\\s*:\\s*\\[(.*?)\\]",
        RegexOption.DOT_MATCHES_ALL,
    )
    return re.findAll(file.readText()).map { m ->
        NoteSet(
            unescape(m.groupValues[1]), unescape(m.groupValues[2]),
            items.findAll(m.groupValues[3]).map { unescape(it.groupValues[1]) }.toList(),
            items.findAll(m.groupValues[4]).map { unescape(it.groupValues[1]) }.toList(),
        )
    }.toList()
}

/** Reads the "taught" entries (label, teach, kind, positives[], negatives[]). */
private fun loadTaught(file: File): List<Taught> {
    val items = Regex(STRING)
    val re = Regex(
        "\"label\"\\s*:\\s*$STRING\\s*,\\s*\"teach\"\\s*:\\s*$STRING\\s*,\\s*\"kind\"\\s*:\\s*\"(\\w+)\"\\s*,\\s*" +
            "\"positives\"\\s*:\\s*\\[(.*?)\\]\\s*,\\s*\"negatives\"\\s*:\\s*\\[(.*?)\\]",
        RegexOption.DOT_MATCHES_ALL,
    )
    return re.findAll(file.readText()).map { m ->
        Taught(
            unescape(m.groupValues[1]), unescape(m.groupValues[2]), m.groupValues[3],
            items.findAll(m.groupValues[4]).map { unescape(it.groupValues[1]) }.toList(),
            items.findAll(m.groupValues[5]).map { unescape(it.groupValues[1]) }.toList(),
        )
    }.toList()
}

private fun unescape(s: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '\\' && i + 1 < s.length) {
            when (val n = s[i + 1]) {
                'n' -> sb.append('\n'); 't' -> sb.append('\t'); else -> sb.append(n)
            }
            i += 2
        } else { sb.append(c); i++ }
    }
    return sb.toString()
}

fun main() {
    val models = File("../assets/models")
    val samplesFile = File("../test-data/samples.json")
    var failures = 0
    fun expect(ok: Boolean, what: String) { if (!ok) { failures++; println("   FAIL: $what") } }

    // ---- 1. Tokenizer sanity (bert-base-uncased vocab: "hello , world !" = 7592 1010 2088 999) -------------
    println("== 1. tokenizer")
    val piiVocab = File(models, "pii/vocab.txt").readLines().filter { it.isNotEmpty() }
    val embVocab = File(models, "embedding/vocab.txt").readLines().filter { it.isNotEmpty() }
    for ((name, vocab) in listOf("pii" to piiVocab, "embedding" to embVocab)) {
        val ids = WordPieceTokenizer(vocab).encode("Hello, world!").inputIds.toList()
        println("   $name: $ids")
        expect(ids == listOf(101L, 7592L, 1010L, 2088L, 999L, 102L), "$name tokenizer ids for 'Hello, world!'")
    }
    expect(Luhn.isValid("4242424242424242") && !Luhn.isValid("4242424242424241"), "Luhn")

    // ---- 2. Detection scored against test-data/samples.json -------------------------------------------------
    val samples = loadSamples(samplesFile)
    println("\n== 2. detection on ${samples.size} synthetic messages (../test-data/samples.json)")
    val rules = RuleDetector()
    val pii = PiiDetector(
        File(models, "pii/model_quantized.onnx").readBytes(), piiVocab,
        ModelConfig.parseId2Label(File(models, "pii/config.json").readText()),
    )
    pii.entities("warm up")
    val rulesOnly = SecondLookChecker(rules)
    val withModel = SecondLookChecker(rules, pii)
    var okRules = 0
    var okFull = 0
    var ms = 0L
    for (s in samples) {
        val a = rulesOnly.check(s.text)?.category?.name ?: "none"
        val t = System.nanoTime()
        val b = withModel.check(s.text)?.category?.name ?: "none"
        ms += (System.nanoTime() - t) / 1_000_000
        if (a == s.expect) okRules++
        if (b == s.expect) okFull++
        else println("   FAIL rules+model: want ${s.expect.padEnd(9)} got ${b.padEnd(9)} | ${s.text.replace("\n", "\\n")}")
    }
    println("   rules only:    $okRules/${samples.size} correct")
    println("   rules + model: $okFull/${samples.size} correct   (avg ${ms / samples.size} ms/message on this PC)")
    failures += samples.size - okFull
    val team = samples.filter { "team-case" in it.tags }
    println("   team manual cases: ${team.count { (withModel.check(it.text)?.category?.name ?: "none") == it.expect }}/${team.size} behave as documented in samples.json")

    // ---- 2b. "Itago": after masking, nothing may warn again ----------------------------------------------
    println("\n== 2b. Itago (hide it): every risky sample, masked, must come back clean")
    var hidOk = 0
    val risky = samples.filter { it.expect != "none" }
    for (s in risky) {
        val after = Redactor.hideAll(s.text, { withModel.check(it) })
        val left = withModel.check(after)
        if (left == null) hidOk++ else println("   FAIL still warns (${left.kind}) | ${after.replace("\n", "\\n")}")
    }
    println("   $hidOk/${risky.size} come back clean after Itago")
    failures += risky.size - hidOk
    for (t in listOf(
        "GEMINI_API_KEY=AIzaSyA1b2C3d4E5f6G7h8I9j0KlMnOpQrStUvW",
        "My password is mango72.",
        "Eto po yung authentication code: 739204",
        "Here is my card number 4242 4242 4242 4242 exp 12/28",
        "card: 5555555555554444 cvv 123",
        "TIN ko: 123-456-789-000",
        "DATABASE_URL=postgres://admin:Sup3rS3cret!@db.example.com:5432/tindahan",
        "Eto yung SSH key ng server: -----BEGIN OPENSSH PRIVATE KEY-----\nb3BlbnNzaC1rZXktdjEAAAAA-FAKE\n-----END OPENSSH PRIVATE KEY-----",
        "BDO account no. 0012345678, Juan Dela Cruz",
        "Send money to 09171234567",
    )) println("   ${t.replace("\n", "\\n")}\n      -> ${Redactor.hideAll(t, { withModel.check(it) }).replace("\n", "\\n")}")

    // ---- 3. Teach me what's private ------------------------------------------------------------------------
    println("\n== 3. private-note matching (all-MiniLM-L6-v2 + anchors), sets read from samples.json")
    val embedder = EmbeddingModel(File(models, "embedding/model_quantized.onnx").readBytes(), embVocab)
    println("   rule: anchors in draft + similarity >= 0.35, or similarity alone >= 0.75   (mirrors web-demo core.js)")
    var tp = 0; var fn = 0; var fpc = 0; var tn = 0
    for (set in loadNoteSets(samplesFile)) {
        val store = InMemoryNotesStore(embedder)
        store.save(set.text, set.label)
        val matcher = NoteMatcher(embedder, store)
        println("   -- ${set.label}   anchors: " + store.notes().first().anchors.joinToString { it.shown })
        val rows = set.positives.map { true to it } + set.negatives.map { false to it }
        for ((shouldFlag, text) in rows) {
            val s = matcher.scores(text).first()
            val flagged = matcher.detect(text) != null
            when { shouldFlag && flagged -> tp++; shouldFlag -> fn++; flagged -> fpc++; else -> tn++ }
            println("      ${if (shouldFlag == flagged) "ok  " else "FAIL"} ${"%.3f".format(s.score)} ${if (flagged) "FLAG " else "quiet"} anchors=${s.anchors}  $text")
        }
    }
    println("   flagged $tp/${tp + fn} real paraphrases, $fpc/${fpc + tn} false alarms   (compare with the browser eval)")

    // ---- 3b. Teach me: secret values (MPIN, password, key) — fingerprint only, exact match --------------------
    println("\n== 3b. taught secret values (fingerprint only, exact match), entries read from samples.json")
    var tOk = 0
    var tTotal = 0
    for (t in loadTaught(samplesFile)) {
        val store = InMemoryNotesStore(embedder)
        val saved = store.save(t.teach, t.label)
        tTotal++
        if (saved.kind == t.kind) tOk++ else expect(false, "teach '${t.teach}' should be ${t.kind}, got ${saved.kind}")
        println("   -- ${t.label}: '${t.teach}' -> ${saved.kind}")
        val matcher = TaughtSecretMatcher(store)
        for ((should, text) in t.positives.map { true to it } + t.negatives.map { false to it }) {
            val r = matcher.detect(text)
            val ok = (r != null) == should
            tTotal++
            if (ok) tOk++ else expect(false, "taught '${t.label}' ${if (should) "should" else "should NOT"} catch: $text")
            println("      ${if (ok) "ok  " else "FAIL"} ${if (r != null) "CATCH \"${text.substring(r.start, r.end)}\"" else "quiet"}  $text")
        }
    }
    println("   $tOk/$tTotal taught-secret checks correct")
    val demo = "eto na yung 4821, bilisan mo"
    val before = withModel.check(demo)
    val demoStore = InMemoryNotesStore(embedder)
    demoStore.save("4821", "GCash MPIN")
    val after = SecondLookChecker(rules, pii, NoteMatcher(embedder, demoStore), TaughtSecretMatcher(demoStore)).check(demo)
    println("   wow check: \"$demo\"\n      before teaching: ${before?.kind ?: "no warning"}\n      after teaching '4821' as GCash MPIN: ${after?.kind ?: "no warning"}" +
        (after?.let { " (${it.reason})\n      Itago -> " + Redactor.hide(demo, it) } ?: ""))
    expect(before == null && after?.kind == "my_secret", "teach demo: quiet before, my_secret after")

    println("\n${if (failures == 0) "ALL DETECTION CHECKS PASSED" else "$failures CHECK(S) FAILED (see FAIL lines above)"}")
}
