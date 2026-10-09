package com.secondlook.ai.notes

import com.secondlook.ai.model.EmbeddingModel

/** Splits text into sentence-like parts and embeds a note as: the whole note + each sentence. */
object NoteVectors {
    private val SENTENCE = Regex("""[^.!?\n]+[.!?]*""")

    /** Char ranges of sentence-like parts with at least [minChars] non-space characters. */
    fun parts(text: String, minChars: Int = 12): List<IntRange> =
        SENTENCE.findAll(text).map { it.range }.filter { text.substring(it).trim().length >= minChars }.toList()

    fun forNote(embedder: EmbeddingModel, text: String): List<FloatArray> {
        val sentences = parts(text, 10).map { text.substring(it).trim() }
        val vectors = ArrayList<FloatArray>()
        vectors += embedder.embed(text.trim())
        if (sentences.size > 1) sentences.take(6).forEach { vectors += embedder.embed(it) }
        return vectors
    }
}
