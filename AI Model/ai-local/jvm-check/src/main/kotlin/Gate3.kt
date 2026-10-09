package com.secondlook.check

import com.secondlook.ai.model.EmbeddingModel
import com.secondlook.ai.model.WordPieceTokenizer
import com.secondlook.ai.notes.Fingerprinter
import com.secondlook.ai.notes.InMemoryNotesStore
import com.secondlook.ai.notes.NoteAnchors
import com.secondlook.ai.notes.NoteMatcher
import com.secondlook.ai.notes.NoteSummary
import com.secondlook.ai.notes.NoteVectors
import com.secondlook.ai.notes.NotesStore
import com.secondlook.ai.notes.StoredNote
import com.secondlook.ai.notes.TaughtSecrets
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/*
 * Gate 3 evaluation: "Teach me what's private" (private-note matching), measured with the SAME Kotlin code the phone
 * runs (WordPieceTokenizer -> ONNX -> mean pooling -> NoteVectors / NoteAnchors / NoteMatcher).
 *
 *   gradlew gate3                                     current model (../assets/models/embedding)
 *   gradlew gate3 -Pargs="model=<dir> name=<label>"   any candidate folder with model_quantized.onnx + vocab.txt
 *
 * Reads test-data/gate3/private-notes.json (held-out, invented) and $GATE3_WORK/data/tatoeba-tgl-eng.tsv, optionally
 * $GATE3_WORK/models/<name>/probes.json (Python embeddings for the parity check). Writes $GATE3_WORK/results/<name>.json;
 * gate3/train/gates.py compares two of those files against the plan's gates.
 * Thresholds are tuned on split=dev only; split=test is reported at the default and at the dev-tuned thresholds.
 */

private const val DEFAULT_HIGH = 0.75f    // NoteMatcher defaults (JS twin: core.js NOTE_DEFAULTS)
private const val DEFAULT_FLOOR = 0.35f
private const val MAX_DEV_FALSE_ALARMS = 0.10

private class Draft(val id: String, val noteId: String, val split: String, val noteLang: String, val lang: String, val disclosure: Boolean, val text: String)
private class EvalNote(val id: String, val split: String, val lang: String, val label: String, val text: String)

/** Every note of a split in one store (the app caps a store at 5; this is only for scoring). Same vectors/anchors as the app. */
private class EvalNotesStore(embedder: EmbeddingModel, notes: List<EvalNote>) : NotesStore {
    private val items = notes.map { StoredNote(NoteSummary(it.id, it.label), NoteVectors.forNote(embedder, it.text), NoteAnchors.of(it.text)) }
    override val fingerprinter = Fingerprinter(Fingerprinter.randomKey())
    override fun save(text: String, label: String) = throw UnsupportedOperationException()
    override fun list() = items.map { it.summary }
    override fun delete(id: String) = throw UnsupportedOperationException()
    override fun deleteAll() = throw UnsupportedOperationException()
    override fun notes() = items
}

/** One draft scored against its own note (+ the rank of its own note among all notes of the split). */
private class Row(val d: Draft, val score: Float, val anchors: List<String>, val needed: Int, val ownRank: Int) {
    val anchorPath get() = needed > 0 && anchors.size >= needed
    /** NoteMatcher.detect's rule for one note, at any thresholds (checked against the real detect() below). */
    fun flagged(high: Float, floor: Float) = (anchorPath && score >= floor) || score >= high
}

private fun fmt(x: Double) = String.format(Locale.ROOT, "%.3f", x)
private fun pct(n: Int, d: Int) = if (d == 0) "  -  " else String.format(Locale.ROOT, "%5.1f%%", 100.0 * n / d)

private class Counts(var tp: Int = 0, var fn: Int = 0, var fp: Int = 0, var tn: Int = 0) {
    val recall get() = if (tp + fn == 0) Double.NaN else tp.toDouble() / (tp + fn)
    val falseAlarms get() = if (fp + tn == 0) Double.NaN else fp.toDouble() / (fp + tn)
    fun json() = JSONObject().put("tp", tp).put("fn", fn).put("fp", fp).put("tn", tn)
        .put("recall", if (recall.isNaN()) JSONObject.NULL else recall).put("falseAlarmRate", if (falseAlarms.isNaN()) JSONObject.NULL else falseAlarms)
}

private fun counts(rows: List<Row>, high: Float, floor: Float) = Counts().also { c ->
    for (r in rows) {
        val f = r.flagged(high, floor)
        when { r.d.disclosure && f -> c.tp++; r.d.disclosure -> c.fn++; f -> c.fp++; else -> c.tn++ }
    }
}

/** Area under the ROC curve of the raw similarity (disclosures vs non-disclosures): threshold-free separation. */
private fun auc(rows: List<Row>): Double {
    val pos = rows.filter { it.d.disclosure }.map { it.score }
    val neg = rows.filter { !it.d.disclosure }.map { it.score }
    if (pos.isEmpty() || neg.isEmpty()) return Double.NaN
    var w = 0.0
    for (p in pos) for (n in neg) w += if (p > n) 1.0 else if (p == n) 0.5 else 0.0
    return w / (pos.size * neg.size)
}

/** Breakdown the plan asks for: aggregate, by draft language, by anchor present / absent. */
private fun breakdown(rows: List<Row>, high: Float, floor: Float): JSONObject {
    val o = JSONObject().put("all", counts(rows, high, floor).json())
    for (lang in listOf("en", "tl", "taglish")) o.put("lang_$lang", counts(rows.filter { it.d.lang == lang }, high, floor).json())
    o.put("lang_tl_or_taglish", counts(rows.filter { it.d.lang != "en" }, high, floor).json())
    o.put("anchor_present", counts(rows.filter { it.anchorPath }, high, floor).json())
    o.put("anchor_absent", counts(rows.filter { !it.anchorPath }, high, floor).json())
    return o
}

/** Highest dev recall with dev false alarms <= 10%; ties: fewer false alarms, then closest to the defaults. */
private fun tune(dev: List<Row>): Pair<Float, Float> {
    var best: Triple<Float, Float, Counts>? = null
    fun better(a: Counts, ha: Float, fa: Float, b: Triple<Float, Float, Counts>): Boolean {
        val okA = a.falseAlarms <= MAX_DEV_FALSE_ALARMS
        val okB = b.third.falseAlarms <= MAX_DEV_FALSE_ALARMS
        if (okA != okB) return okA
        if (!okA) return a.falseAlarms < b.third.falseAlarms || (a.falseAlarms == b.third.falseAlarms && a.recall > b.third.recall)
        if (a.recall != b.third.recall) return a.recall > b.third.recall
        if (a.falseAlarms != b.third.falseAlarms) return a.falseAlarms < b.third.falseAlarms
        val da = Math.abs(ha - DEFAULT_HIGH) + Math.abs(fa - DEFAULT_FLOOR)
        val db = Math.abs(b.first - DEFAULT_HIGH) + Math.abs(b.second - DEFAULT_FLOOR)
        return da < db
    }
    for (h in 30..95) for (f in 10..h) {
        val high = h / 100f; val floor = f / 100f
        val c = counts(dev, high, floor)
        if (best == null || better(c, high, floor, best)) best = Triple(high, floor, c)
    }
    return best!!.first to best.second
}

private fun percentile(sorted: List<Double>, p: Double) = sorted[((sorted.size - 1) * p).toInt()]

fun main(args: Array<String>) {
    val opts = args.flatMap { it.split(' ') }.filter { '=' in it }.associate { it.substringBefore('=') to it.substringAfter('=') }
    val modelDir = File(opts["model"] ?: "../assets/models/embedding")
    val name = opts["name"] ?: if (modelDir.canonicalPath == File("../assets/models/embedding").canonicalPath) "shipped" else modelDir.name
    val work = File(System.getenv("GATE3_WORK") ?: "../../../../gate3-work")
    val out = File(opts["out"] ?: File(work, "results/$name.json").path)
    val notesFile = File("../test-data/gate3/private-notes.json")
    val onnx = File(modelDir, "model_quantized.onnx")
    val vocab = File(modelDir, "vocab.txt").readLines().filter { it.isNotEmpty() }

    println("== Gate 3 evaluation: $name ($onnx, ${"%.1f".format(onnx.length() / 1e6)} MB)")
    val embedder = EmbeddingModel(onnx.readBytes(), vocab)
    embedder.embed("warm up")
    val result = JSONObject().put("name", name).put("model", onnx.canonicalPath).put("modelBytes", onnx.length())

    // ---- 1. parity with Python (tokenizer ids + embeddings), when the export wrote probes.json --------------------
    val probesFile = File(opts["probes"] ?: File(modelDir, "probes.json").path)
    if (probesFile.exists()) {
        val probes = JSONArray(probesFile.readText())
        val tok = WordPieceTokenizer(vocab)
        var idMismatch = 0
        val cosines = ArrayList<Double>()
        for (i in 0 until probes.length()) {
            val p = probes.getJSONObject(i)
            val text = p.getString("text")
            val ids = p.getJSONArray("ids").let { a -> (0 until a.length()).map { a.getLong(it) } }
            if (tok.encode(text, 128).inputIds.toList() != ids) { idMismatch++; println("   ids differ: $text") }
            val pv = p.getJSONArray("onnx").let { a -> FloatArray(a.length()) { a.getDouble(it).toFloat() } }
            cosines += EmbeddingModel.cosine(embedder.embed(text), pv).toDouble()
        }
        val sorted = cosines.sorted()
        println("== 1. parity with Python on ${probes.length()} probes: token ids differ on $idMismatch, cosine min ${fmt(sorted.first())} mean ${fmt(cosines.average())}")
        result.put("parity", JSONObject().put("probes", probes.length()).put("tokenIdMismatches", idMismatch)
            .put("cosineMin", sorted.first()).put("cosineMean", cosines.average()))
    } else {
        println("== 1. parity: no probes.json next to the model (written by gate3/train/export_onnx.py), skipped")
    }

    // ---- 2. Tatoeba tgl-eng top-1 retrieval (untouched test split) --------------------------------------------
    val tatoebaFile = File(work, "data/tatoeba-tgl-eng.tsv")
    if (tatoebaFile.exists()) {
        val pairs = tatoebaFile.readLines().filter { it.isNotBlank() }.map { it.split('\t') }
        val tl = pairs.map { embedder.embed(it[0]) }
        val en = pairs.map { embedder.embed(it[1]) }
        fun top1(q: List<FloatArray>, k: List<FloatArray>): Double = q.indices.count { i ->
            var best = -1; var bs = -2f
            for (j in k.indices) { val s = EmbeddingModel.cosine(q[i], k[j]); if (s > bs) { bs = s; best = j } }
            best == i
        }.toDouble() / q.size
        val t2e = top1(tl, en); val e2t = top1(en, tl)
        println("== 2. Tatoeba tgl-eng (${pairs.size} pairs) top-1: tl->en ${fmt(t2e)}  en->tl ${fmt(e2t)}  mean ${fmt((t2e + e2t) / 2)}")
        result.put("tatoeba", JSONObject().put("pairs", pairs.size).put("tlToEn", t2e).put("enToTl", e2t).put("mean", (t2e + e2t) / 2))
    } else {
        println("== 2. Tatoeba: $tatoebaFile not found (run gate3/train/prepare_data.py), skipped")
    }

    // ---- 3. held-out private notes: embedding scores, anchors, the matcher's decision -------------------------
    val set = JSONObject(notesFile.readText())
    result.put("notesSetReview", set.getJSONObject("_review").optString("status"))
    val notes = ArrayList<EvalNote>()
    val drafts = ArrayList<Draft>()
    val arr = set.getJSONArray("notes")
    for (i in 0 until arr.length()) {
        val n = arr.getJSONObject(i)
        val note = EvalNote(n.getString("id"), n.getString("split"), n.getString("lang"), n.getString("label"), n.getString("text"))
        require(TaughtSecrets.valuesIn(note.text).isEmpty()) { "${note.id} would be saved as a secret value, not a fact" }
        notes += note
        for ((key, disclosure) in listOf("disclosures" to true, "nonDisclosures" to false)) {
            val ds = n.getJSONArray(key)
            for (j in 0 until ds.length()) {
                val d = ds.getJSONObject(j)
                drafts += Draft("${note.id}-${if (disclosure) "d" else "n"}$j", note.id, note.split, note.lang, d.getString("lang"), disclosure, d.getString("text"))
            }
        }
    }
    val rows = ArrayList<Row>()
    for (split in listOf("dev", "test")) {
        val splitNotes = notes.filter { it.split == split }
        val matcher = NoteMatcher(embedder, EvalNotesStore(embedder, splitNotes))
        for (d in drafts.filter { it.split == split }) {
            val scores = matcher.scores(d.text)
            val own = scores.first { it.noteLabel == splitNotes.first { n -> n.id == d.noteId }.label }
            val rank = 1 + scores.count { it.score > own.score }
            rows += Row(d, own.score, own.anchors, own.anchorsNeeded, rank)
        }
    }
    // the per-note rule above must equal the real NoteMatcher.detect (one note per store, default thresholds)
    var ruleMismatch = 0
    for (note in notes) {
        val store = InMemoryNotesStore(embedder).also { it.save(note.text, note.label) }
        val m = NoteMatcher(embedder, store)
        for (r in rows.filter { it.d.noteId == note.id }) if ((m.detect(r.d.text) != null) != r.flagged(DEFAULT_HIGH, DEFAULT_FLOOR)) ruleMismatch++
    }
    require(ruleMismatch == 0) { "$ruleMismatch drafts: scoring rule differs from NoteMatcher.detect" }

    val dev = rows.filter { it.d.split == "dev" }
    val test = rows.filter { it.d.split == "test" }
    val (high, floor) = tune(dev)
    fun line(label: String, rs: List<Row>, h: Float, f: Float): String {
        val c = counts(rs, h, f)
        return String.format(Locale.ROOT, "   %-26s recall %s (%d/%d)  false alarms %s (%d/%d)", label, pct(c.tp, c.tp + c.fn), c.tp, c.tp + c.fn, pct(c.fp, c.fp + c.tn), c.fp, c.fp + c.tn)
    }
    println("== 3. private notes (${notes.size} notes, ${drafts.size} drafts; labels: ${set.getJSONObject("_review").optString("status")})")
    for ((label, h, f) in listOf(Triple("defaults", DEFAULT_HIGH, DEFAULT_FLOOR), Triple("dev-tuned", high, floor))) {
        println("   -- test split, $label thresholds (similarity alone >= $h, anchor + similarity >= $f)")
        println(line("all", test, h, f))
        for (lang in listOf("en", "tl", "taglish")) println(line("draft in $lang", test.filter { it.d.lang == lang }, h, f))
        println(line("tl + taglish", test.filter { it.d.lang != "en" }, h, f))
        println(line("anchor present", test.filter { it.anchorPath }, h, f))
        println(line("anchor absent", test.filter { !it.anchorPath }, h, f))
    }
    val testDisc = test.filter { it.d.disclosure }
    val top1 = testDisc.count { it.ownRank == 1 }.toDouble() / testDisc.size
    println("   embedding only (test): AUC ${fmt(auc(test))} (en ${fmt(auc(test.filter { it.d.lang == "en" }))}, tl ${fmt(auc(test.filter { it.d.lang == "tl" }))}, taglish ${fmt(auc(test.filter { it.d.lang == "taglish" }))}); " +
        "own note ranked first for ${fmt(top1)} of disclosures among ${notes.count { it.split == "test" }} notes")

    // ---- 4. warmed-up Gate 3 latency on this PC (5 notes = the app's maximum) ----------------------------------
    val five = InMemoryNotesStore(embedder).also { s -> notes.filter { it.split == "test" }.take(5).forEach { s.save(it.text, it.label) } }
    val m5 = NoteMatcher(embedder, five)
    val texts = drafts.map { it.text }
    texts.take(20).forEach { m5.detect(it) }
    val ms = ArrayList<Double>()
    repeat(2) { for (t in texts) { val t0 = System.nanoTime(); m5.detect(t); ms += (System.nanoTime() - t0) / 1e6 } }
    ms.sort()
    println("== 4. latency on this PC (5 notes, ${ms.size} checks): p50 ${"%.1f".format(ms[ms.size / 2])} ms, p95 ${"%.1f".format(percentile(ms, 0.95))} ms (the phone gate is measured in test-app)")

    result.put("thresholds", JSONObject().put("default", JSONObject().put("high", DEFAULT_HIGH).put("floor", DEFAULT_FLOOR))
        .put("devTuned", JSONObject().put("high", high).put("floor", floor).put("rule", "max dev recall with dev false alarms <= 10%")))
    result.put("dev", JSONObject().put("default", breakdown(dev, DEFAULT_HIGH, DEFAULT_FLOOR)).put("devTuned", breakdown(dev, high, floor)))
    result.put("test", JSONObject().put("default", breakdown(test, DEFAULT_HIGH, DEFAULT_FLOOR)).put("devTuned", breakdown(test, high, floor))
        .put("auc", JSONObject().put("all", auc(test)).put("en", auc(test.filter { it.d.lang == "en" })).put("tl", auc(test.filter { it.d.lang == "tl" })).put("taglish", auc(test.filter { it.d.lang == "taglish" })))
        .put("ownNoteTop1", top1))
    result.put("latencyPcMs", JSONObject().put("p50", ms[ms.size / 2]).put("p95", percentile(ms, 0.95)).put("checks", ms.size))
    result.put("rows", JSONArray(rows.map { r ->
        JSONObject().put("id", r.d.id).put("split", r.d.split).put("noteLang", r.d.noteLang).put("lang", r.d.lang).put("disclosure", r.d.disclosure)
            .put("score", r.score.toDouble()).put("anchors", JSONArray(r.anchors)).put("anchorsNeeded", r.needed).put("ownRank", r.ownRank)
            .put("flaggedDefault", r.flagged(DEFAULT_HIGH, DEFAULT_FLOOR)).put("flaggedTuned", r.flagged(high, floor))
    }))
    out.parentFile.mkdirs()
    out.writeText(result.toString(1))
    println("\nWrote ${out.canonicalPath}")
    embedder.close()
}
