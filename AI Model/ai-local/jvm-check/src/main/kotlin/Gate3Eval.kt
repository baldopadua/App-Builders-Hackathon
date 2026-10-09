package com.secondlook.check

import com.secondlook.ai.model.EmbeddingModel
import com.secondlook.ai.notes.InMemoryNotesStore
import com.secondlook.ai.notes.NoteMatcher
import java.io.File

/** Scores the same NoteMatcher shipped on Android; tab-separated fixture text stays local. */
fun main(args: Array<String>) {
    require(args.size == 2) { "Usage: gate3Eval -Pgate3Model=<onnx> -Pgate3Cases=<tsv>" }
    val model = File(args[0])
    val cases = File(args[1])
    val vocab = File("../assets/models/embedding/vocab.txt").readLines().filter { it.isNotEmpty() }
    data class Counts(var tp: Int = 0, var fn: Int = 0, var fp: Int = 0, var tn: Int = 0)
    val counts = linkedMapOf<String, Counts>()
    println("id\tlanguage\texpected\tflagged\tscore\tanchor")
    EmbeddingModel(model.readBytes(), vocab).use { embedder ->
        for (line in cases.readLines().drop(1).filter { it.isNotBlank() }) {
            val fields = line.split('\t')
            require(fields.size == 5) { "Expected 5 TSV columns" }
            val (id, language, note, positives, negatives) = fields
            val store = InMemoryNotesStore(embedder)
            store.save(note, id)
            val matcher = NoteMatcher(embedder, store)
            for ((expected, draft) in (positives.split("||").map { true to it } + negatives.split("||").map { false to it })) {
                val score = matcher.scores(draft).single()
                val anchor = score.anchorsNeeded > 0 && score.anchors.size >= score.anchorsNeeded
                val flagged = matcher.detect(draft) != null
                val group = "$language/${if (anchor) "anchor" else "no-anchor"}"
                val count = counts.getOrPut(group) { Counts() }
                when {
                    expected && flagged -> count.tp++
                    expected -> count.fn++
                    flagged -> count.fp++
                    else -> count.tn++
                }
                println("$id\t$language\t$expected\t$flagged\t${"%.4f".format(java.util.Locale.ROOT, score.score)}\t$anchor")
            }
        }
    }
    System.err.println("group\ttp\tfn\tfp\ttn")
    for ((group, c) in counts) System.err.println("$group\t${c.tp}\t${c.fn}\t${c.fp}\t${c.tn}")
    val all = Counts(counts.values.sumOf { it.tp }, counts.values.sumOf { it.fn },
        counts.values.sumOf { it.fp }, counts.values.sumOf { it.tn })
    System.err.println("ALL\t${all.tp}\t${all.fn}\t${all.fp}\t${all.tn}")
}
