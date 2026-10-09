package com.secondlook

import com.secondlook.ai.notes.NoteSummary

/**
 * The manual's SecretManager, as a thin facade over the encrypted notes store in ai-local.
 * The AI core owns the storage because saving a fact also embeds it (needs the model);
 * Klarenz only wires this to the Flutter MethodChannel. Get it from [DetectionEngine.secrets].
 *
 * One "Teach me" box, two kinds (the engine decides, see [NoteSummary.kind]):
 *  - "secret": one word, or an entry with a value the rules recognise ("4821", "GCash MPIN ko: 4821", "Lodi1234").
 *    Only a keyed fingerprint is kept; the draft is then checked for that exact value, even with no clue words.
 *  - "fact": a sentence ("Si Jakim ay nakatira sa Taguig"), matched by meaning + names.
 *
 * Every call may wait for the models to finish loading (first launch), so call from a background thread.
 * `list()` returns labels and kinds only: the secret text never leaves the encrypted store and is never sent to Flutter.
 * Max 5 entries: `save` throws [com.secondlook.ai.notes.NoteLimitException] when full.
 */
class SecretManager internal constructor(private val engine: DetectionEngine) {
    fun save(text: String, label: String): NoteSummary = engine.requireStore().save(text, label)
    fun list(): List<NoteSummary> = engine.requireStore().list()
    fun delete(id: String) = engine.requireStore().delete(id)
    fun deleteAll() = engine.requireStore().deleteAll()
}
