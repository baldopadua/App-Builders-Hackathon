package com.secondlook.ai.notes

import com.secondlook.ai.Detector
import com.secondlook.ai.RiskCategory
import com.secondlook.ai.RiskResult
import com.secondlook.ai.model.EmbeddingModel

/**
 * Compares the draft with the user's saved private notes.
 *
 * Flags a draft when
 *  - it contains one of the note's anchors (a name or code word) AND is at least a little similar ([anchorFloor]), or
 *  - it is very similar to the note by meaning even with no anchor ([threshold]).
 *
 * Semantic similarity can suggest a connection but never prove a disclosure, so wording stays cautious.
 * Measured on test-data/samples.json (3 notes, 26 drafts): flags 10/12 real paraphrases, 3/14 false alarms; the false
 * alarms all literally contain the protected word. A paraphrase with no anchor and only moderate similarity is missed.
 * JS twin: web-demo/public/core.js (notesResult). Keep in sync.
 */
class NoteMatcher(
    private val embedder: EmbeddingModel,
    private val store: NotesStore,
    @Volatile var threshold: Float = 0.75f,
    @Volatile var anchorFloor: Float = 0.60f,
    private val minChars: Int = 12,
) : Detector {

    /** Best similarity of the draft to one note, which part of the draft matched, and which anchors appear. */
    data class Score(
        val noteLabel: String, val score: Float, val start: Int, val end: Int,
        val anchors: List<String>, val anchorsNeeded: Int,
    )

    fun scores(text: String): List<Score> {
        val notes = store.notes().filter { it.vectors.isNotEmpty() }   // facts only; secret values: TaughtSecretMatcher
        if (notes.isEmpty() || text.trim().length < minChars) return emptyList()

        val parts = ArrayList<IntRange>()
        val sentenceParts = NoteVectors.parts(text, minChars).take(4)
        parts += sentenceParts
        if (sentenceParts.size != 1 || sentenceParts[0] != text.indices) parts += text.indices   // whole draft too
        val partVectors = parts.map { embedder.embed(text.substring(it).trim()) }

        return notes.map { note ->
            var best = -1f
            var bestPart = parts.last()
            for ((i, pv) in partVectors.withIndex()) {
                for (nv in note.vectors) {
                    val c = EmbeddingModel.cosine(pv, nv)
                    if (c > best) { best = c; bestPart = parts[i] }
                }
            }
            Score(
                note.summary.label, best, bestPart.first, bestPart.last + 1,
                NoteAnchors.hits(note.anchors, text), NoteAnchors.needed(note.anchors),
            )
        }
    }

    override fun detect(text: String): RiskResult? {
        var best: Score? = null
        var bestViaAnchor = false
        var bestRank = -1f
        for (s in scores(text)) {
            val viaAnchor = s.anchorsNeeded > 0 && s.anchors.size >= s.anchorsNeeded && s.score >= anchorFloor
            if (!viaAnchor && s.score < threshold) continue
            val rank = (if (viaAnchor) 1f else 0f) + s.score
            if (rank > bestRank) { best = s; bestViaAnchor = viaAnchor; bestRank = rank }
        }
        val b = best ?: return null
        val reason = if (bestViaAnchor) {
            "This mentions \"${b.anchors.first()}\", which is part of a note you marked confidential: ${b.noteLabel}."
        } else {
            "This may reveal information you marked confidential: ${b.noteLabel}."
        }
        return RiskResult(
            RiskCategory.PRIVATE_NOTE, b.start, b.end, reason, b.score.coerceIn(0f, 1f), b.noteLabel,
            kind = "private_match",
        )
    }
}
