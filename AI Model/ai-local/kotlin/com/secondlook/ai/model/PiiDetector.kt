package com.secondlook.ai.model

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.secondlook.ai.Detector
import com.secondlook.ai.Redactor
import com.secondlook.ai.RiskCategory
import com.secondlook.ai.RiskResult
import com.secondlook.ai.Severity
import kotlin.math.exp

/**
 * Wraps the bundled PII token-classification model (ONNX Runtime).
 * Candidate: onnx-community/bert-small-pii-detection-ONNX (model_quantized.onnx, Apache-2.0).
 * Takes plain bytes/lists so it runs unchanged on Android and in the PC check (see android/AssetModels.kt).
 * Never logs or stores [text].
 */
class PiiDetector(
    modelBytes: ByteArray,
    vocab: List<String>,
    private val id2label: Map<Int, String>,
    private val maxTokens: Int = 256,
    @Volatile var minScore: Float = 0.6f,
) : Detector, AutoCloseable {

    /** A merged entity found by the model (any type, including harmless ones like PERSON). */
    data class Entity(val type: String, val start: Int, val end: Int, val score: Float, val text: String)

    private val tokenizer = WordPieceTokenizer(vocab)
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(modelBytes, OrtSession.SessionOptions())
    private val inputNames: Set<String> = session.inputNames

    /** All entities the model sees, for debugging and tuning. */
    fun entities(text: String): List<Entity> {
        if (text.isBlank()) return emptyList()
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
                val logits = (out[0].value as Array<Array<FloatArray>>)[0]
                return decode(text, enc, logits)
            }
        } finally {
            tensors.forEach { it.close() }
        }
    }

    override fun detect(text: String): RiskResult? {
        val best = entities(text)
            .filter { it.type in WARN && it.score >= minScore && !Redactor.isMasked(it.text) && !Redactor.touchesMask(text, it.start) }
            .maxByOrNull { it.score } ?: return null
        val (category, what) = WARN.getValue(best.type)
        val soft = best.type == "EMAIL_ADDRESS" || best.type == "PHONE_NUMBER"
        val tail = when {
            soft -> " Intended?"
            category == RiskCategory.PASSWORD -> " Sharing it could give someone access to your account."
            else -> " Review it before sending."
        }
        val kind = when (best.type) {
            "PASSWORD" -> "password"; "CREDIT_CARD" -> "credit_card"; "EMAIL_ADDRESS" -> "email"; "PHONE_NUMBER" -> "phone"
            "US_SSN", "US_PASSPORT", "US_DRIVER_LICENSE", "US_ITIN" -> "gov_id"; else -> "pii"
        }
        return RiskResult(
            category, best.start, best.end, "This message appears to include $what.$tail", best.score,
            kind = kind, severity = if (soft) Severity.MEDIUM else Severity.HIGH,
        )
    }

    override fun close() = session.close()

    private fun decode(text: String, enc: WordPieceTokenizer.Encoding, logits: Array<FloatArray>): List<Entity> {
        val out = ArrayList<Entity>()
        var type: String? = null
        var s = 0
        var e = 0
        var sum = 0f
        var count = 0

        fun flush() {
            val t = type
            if (t != null && count > 0) out += Entity(t, s, e, sum / count, text.substring(s, e))
            type = null; sum = 0f; count = 0
        }

        for (i in 1 until logits.size - 1) {   // skip [CLS] and [SEP]
            val row = logits[i]
            var best = 0
            for (k in row.indices) if (row[k] > row[best]) best = k
            var denom = 0f
            for (v in row) denom += exp(v - row[best])
            val p = 1f / denom                   // softmax probability of the best label
            val label = id2label[best] ?: "O"
            if (label == "O") { flush(); continue }
            val t = label.substring(2)
            if (label.startsWith("B-") || t != type) {
                flush(); type = t; s = enc.starts[i]
            }
            e = enc.ends[i]; sum += p; count++
        }
        flush()
        return out
    }

    companion object {
        /** Entity types worth a warning -> (category, wording). PERSON/LOCATION/DATE etc. are too noisy for chat. */
        val WARN: Map<String, Pair<RiskCategory, String>> = mapOf(
            "PASSWORD" to (RiskCategory.PASSWORD to "a password"),
            "CREDIT_CARD" to (RiskCategory.CARD to "a payment card number"),
            "FINANCIAL" to (RiskCategory.OTHER_PII to "financial account details"),
            "IBAN_CODE" to (RiskCategory.OTHER_PII to "a bank account number"),
            "US_BANK_NUMBER" to (RiskCategory.OTHER_PII to "a bank account number"),
            "EMAIL_ADDRESS" to (RiskCategory.OTHER_PII to "an email address"),
            "PHONE_NUMBER" to (RiskCategory.OTHER_PII to "a phone number"),
            "US_SSN" to (RiskCategory.OTHER_PII to "a government ID number"),
            "US_PASSPORT" to (RiskCategory.OTHER_PII to "a passport number"),
            "US_DRIVER_LICENSE" to (RiskCategory.OTHER_PII to "a driver's license number"),
            "US_ITIN" to (RiskCategory.OTHER_PII to "a tax ID number"),
        )
    }
}
