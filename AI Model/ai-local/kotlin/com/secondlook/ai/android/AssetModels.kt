package com.secondlook.ai.android

import android.content.Context
import com.secondlook.ai.model.EmbeddingModel
import com.secondlook.ai.model.ModelConfig
import com.secondlook.ai.model.PiiDetector

/** Loads the bundled models from the APK's assets/models/ folder. No network. Slow-ish: call off the main thread. */
object AssetModels {
    private fun bytes(context: Context, path: String): ByteArray = context.assets.open(path).use { it.readBytes() }
    private fun text(context: Context, path: String): String = context.assets.open(path).bufferedReader().use { it.readText() }
    private fun vocab(context: Context, dir: String): List<String> = text(context, "models/$dir/vocab.txt").lines().filter { it.isNotEmpty() }

    fun piiDetector(context: Context): PiiDetector = PiiDetector(
        modelBytes = bytes(context, "models/pii/model_quantized.onnx"),
        vocab = vocab(context, "pii"),
        id2label = ModelConfig.parseId2Label(text(context, "models/pii/config.json")),
    )

    /** [dir] = folder under assets/models; the test app's Gate 3 speed test also times "embedding-candidate". */
    fun embeddingModel(context: Context, dir: String = "embedding"): EmbeddingModel = EmbeddingModel(
        modelBytes = bytes(context, "models/$dir/model_quantized.onnx"),
        vocab = vocab(context, dir),
    )
}
