package com.secondlook.ai.model

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlin.math.sqrt

/**
 * Sentence embedding model for "Teach me what's private".
 * Candidate: Xenova/all-MiniLM-L6-v2 (model_quantized.onnx, Apache-2.0).
 * embed(text) -> L2-normalised vector (mean pooling over tokens), so cosine similarity = dot product.
 */
class EmbeddingModel(
    modelBytes: ByteArray,
    vocab: List<String>,
    private val maxTokens: Int = 128,
) : AutoCloseable {

    private val tokenizer = WordPieceTokenizer(vocab)
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(modelBytes, OrtSession.SessionOptions())
    private val inputNames: Set<String> = session.inputNames

    fun embed(text: String): FloatArray {
        val enc = tokenizer.encode(text, maxTokens)
        val tensors = ArrayList<OnnxTensor>()
        try {
            val feeds = HashMap<String, OnnxTensor>()
            fun feed(name: String, data: LongArray) {
                if (name in inputNames) {
                    val t = OnnxTensor.createTensor(env, arrayOf(data))
                    tensors += t
                    feeds[name] = t
                }
            }
            feed("input_ids", enc.inputIds)
            feed("attention_mask", enc.attentionMask)
            feed("token_type_ids", enc.tokenTypeIds)
            session.run(feeds).use { out ->
                @Suppress("UNCHECKED_CAST")
                val hidden = (out[0].value as Array<Array<FloatArray>>)[0]   // [tokens][dim]
                val dim = hidden[0].size
                val v = FloatArray(dim)
                var count = 0
                for (i in hidden.indices) {
                    if (enc.attentionMask[i] != 1L) continue
                    for (d in 0 until dim) v[d] += hidden[i][d]
                    count++
                }
                var norm = 0f
                for (d in 0 until dim) { v[d] /= count; norm += v[d] * v[d] }
                norm = sqrt(norm)
                if (norm > 0f) for (d in 0 until dim) v[d] /= norm
                return v
            }
        } finally {
            tensors.forEach { it.close() }
        }
    }

    override fun close() = session.close()

    companion object {
        /** Both vectors must be L2-normalised (as returned by [embed]). */
        fun cosine(a: FloatArray, b: FloatArray): Float {
            var s = 0f
            for (i in a.indices) s += a[i] * b[i]
            return s
        }
    }
}
