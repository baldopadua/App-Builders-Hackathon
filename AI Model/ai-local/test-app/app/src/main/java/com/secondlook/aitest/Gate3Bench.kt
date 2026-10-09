package com.secondlook.aitest

import android.content.Context
import android.os.SystemClock
import com.secondlook.ai.android.AssetModels
import com.secondlook.ai.notes.InMemoryNotesStore
import com.secondlook.ai.notes.NoteMatcher
import org.json.JSONObject

/**
 * Gate 3 phone gate: warmed-up latency of the private-note check with 5 saved notes (the app's maximum), for the
 * bundled embedding model and, when the APK was built with -Pgate3Candidate=<folder>, the candidate too.
 * Notes and drafts come from test-data/gate3/private-notes.json (invented; bundled in this TEST app only).
 * Run on a background thread.
 */
object Gate3Bench {
    class Result(val model: String, val loadMs: Long, val p50: Double, val p95: Double, val checks: Int)

    fun models(context: Context): List<String> = listOf("embedding", "embedding-candidate").filter { dir ->
        runCatching { context.assets.open("models/$dir/model_quantized.onnx").close() }.isSuccess
    }

    fun run(context: Context, dir: String): Result {
        val t0 = SystemClock.elapsedRealtime()
        val embedder = AssetModels.embeddingModel(context, dir)
        val loadMs = SystemClock.elapsedRealtime() - t0
        try {
            val set = JSONObject(context.assets.open("private-notes.json").bufferedReader().use { it.readText() }).getJSONArray("notes")
            val store = InMemoryNotesStore(embedder)
            val drafts = ArrayList<String>()
            for (i in 0 until set.length()) {
                val n = set.getJSONObject(i)
                if (n.getString("split") == "test" && store.list().size < 5) store.save(n.getString("text"), n.getString("label"))
                for (key in listOf("disclosures", "nonDisclosures")) {
                    val ds = n.getJSONArray(key)
                    for (j in 0 until ds.length()) drafts += ds.getJSONObject(j).getString("text")
                }
            }
            val matcher = NoteMatcher(embedder, store)
            drafts.take(20).forEach { matcher.detect(it) }   // warm-up
            val ms = ArrayList<Double>()
            repeat(2) {
                for (d in drafts) {
                    val t = SystemClock.elapsedRealtimeNanos()
                    matcher.detect(d)
                    ms += (SystemClock.elapsedRealtimeNanos() - t) / 1e6
                }
            }
            ms.sort()
            return Result(dir, loadMs, ms[ms.size / 2], ms[((ms.size - 1) * 0.95).toInt()], ms.size)
        } finally {
            embedder.close()
        }
    }
}
