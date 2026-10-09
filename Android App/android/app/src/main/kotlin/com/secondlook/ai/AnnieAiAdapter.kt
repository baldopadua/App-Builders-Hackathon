package com.secondlook.ai

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.secondlook.SecretManager
import com.secondlook.Warning
import com.secondlook.WarningType
import com.secondlook.ai.android.AssetModels
import com.secondlook.ai.android.EncryptedNotesStore
import com.secondlook.ai.model.EmbeddingModel
import com.secondlook.ai.model.PiiDetector
import com.secondlook.ai.notes.NoteMatcher
import com.secondlook.ai.notes.NoteSummary
import com.secondlook.ai.notes.TaughtSecretMatcher
import com.secondlook.ai.rules.RuleDetector
import java.util.concurrent.Executors

/**
 * On-Device AI Detection Adapter bridging Annie's AI Core with the keyboard and Flutter app.
 *
 * Implements the 4-stage cheapest-first detection pipeline:
 *  0. Taught Secrets: HMAC-SHA256 fingerprint exact matching (MPIN, passcodes, taught keys)
 *  1. Rule Detector: regex + Luhn + Taglish / Filipino phrasing (sub-millisecond)
 *  2. PII Model: bert-small-pii-detection (28.7 MB ONNX Runtime)
 *  3. Private Notes: all-MiniLM-L6-v2 Candidate v2 Filipino embeddings (22.8 MB ONNX Runtime)
 *
 * Zero-jank guarantee: Rule detector handles typing synchronously from millisecond 0 while
 * ONNX models load asynchronously in the background (~1.3s on cold start).
 */
class AnnieAiAdapter : AiDetectionAdapter {

    companion object {
        private const val TAG = "AnnieAiAdapter"
        @Volatile
        var instance: AnnieAiAdapter? = null
            private set
    }

    override val engineName: String = "TanggolinLocalAiCore"

    @Volatile
    override var isReady: Boolean = false
        private set

    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var checker: SecondLookChecker = SecondLookChecker(RuleDetector())

    private var piiDetector: PiiDetector? = null
    private var embeddingModel: EmbeddingModel? = null

    @Volatile
    var notesStore: EncryptedNotesStore? = null
        private set

    init {
        instance = this
    }

    override fun init(context: Context) {
        val appContext = context.applicationContext
        bgExecutor.execute {
            try {
                Log.i(TAG, "Initializing on-device ONNX Runtime models from assets...")
                val t0 = System.currentTimeMillis()

                val pii = AssetModels.piiDetector(appContext)
                val embedder = AssetModels.embeddingModel(appContext)
                val store = EncryptedNotesStore(appContext, embedder)
                val matcher = NoteMatcher(embedder, store, threshold = 0.75f, anchorFloor = 0.60f)
                val taught = TaughtSecretMatcher(store)

                piiDetector = pii
                embeddingModel = embedder
                notesStore = store

                // Synchronize any pre-existing vault secrets from SecretManager
                syncSecretsFromVault(appContext, store)

                checker = SecondLookChecker(RuleDetector(), pii, matcher, taught)
                isReady = true

                val elapsed = System.currentTimeMillis() - t0
                Log.i(TAG, "Tanggolin AI Core ready in ${elapsed}ms. Both ONNX models loaded successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ONNX models. RuleDetector remains active.", e)
            }
        }
    }

    override fun detectSync(text: String): Warning? {
        if (text.isBlank()) return null
        val result = checker.check(text) ?: return null

        val start = result.start.coerceIn(0, text.length)
        val end = result.end.coerceIn(start, text.length)
        val matched = if (start < end) text.substring(start, end) else ""

        return Warning(
            type = if (result.severity == Severity.HIGH) WarningType.HIGH else WarningType.MEDIUM,
            headline = SafetyTips.headline(result.kind, result.noteLabel),
            tip = SafetyTips.forKind(result.kind),
            category = mapRiskCategory(result),
            matchedText = matched,
            start = start,
            end = end,
            canHide = result.kind != "private_match"
        )
    }

    override fun detectAsync(text: String, callback: (Warning?) -> Unit) {
        bgExecutor.execute {
            val warning = detectSync(text)
            mainHandler.post { callback(warning) }
        }
    }

    override fun hide(text: String, warning: Warning?): String {
        return if (warning != null && warning.canHide && warning.matchedText.isNotEmpty()) {
            val s = warning.start.coerceIn(0, text.length)
            val e = warning.end.coerceIn(s, text.length)
            val kind = if (warning.type == WarningType.HIGH) "password" else "phone"
            val masked = Redactor.maskSpan(text.substring(s, e), kind)
            if (masked != null) {
                text.substring(0, s) + masked + text.substring(e)
            } else {
                Redactor.hideAll(text, { checker.check(it) })
            }
        } else {
            Redactor.hideAll(text, { checker.check(it) })
        }
    }

    /**
     * Saves a user-taught secret note into the hardware-encrypted notes store.
     * Computes Candidate v2 embeddings for facts and HMAC-SHA256 fingerprints for secret values.
     */
    fun saveTaughtSecret(text: String, label: String): NoteSummary? {
        val store = notesStore ?: return null
        return try {
            store.save(text, label)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save note to EncryptedNotesStore", e)
            null
        }
    }

    /**
     * Deletes a taught secret note from the hardware-encrypted notes store.
     */
    fun deleteTaughtSecret(id: String) {
        notesStore?.delete(id)
    }

    private fun mapRiskCategory(r: RiskResult): String = when (r.category) {
        RiskCategory.PASSWORD -> if (r.kind == "otp") "OTP / PIN protection" else "Password protection"
        RiskCategory.API_KEY -> "API Key protection"
        RiskCategory.CARD -> "Credit Card protection"
        RiskCategory.PRIVATE_NOTE -> if (r.kind == "my_secret") "Vault Secret protection" else "Private Note protection"
        RiskCategory.OTHER_PII -> when (r.kind) {
            "gov_id" -> "Government ID protection"
            "phone" -> "Phone Number protection"
            "email" -> "Email protection"
            "bank_account" -> "Bank Account protection"
            else -> "PII Data protection"
        }
    }

    private fun syncSecretsFromVault(context: Context, store: EncryptedNotesStore) {
        try {
            val existingVault = SecretManager.getSecrets(context)
            val storeSummaries = store.list().map { it.label }
            for (secret in existingVault) {
                if (secret.value.isNotBlank() && secret.label !in storeSummaries) {
                    try {
                        store.save(secret.value, secret.label)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vault synchronization exception", e)
        }
    }
}
