package com.secondlook.ai

import android.content.Context
import com.secondlook.SecretManager
import com.secondlook.SecretNote
import com.secondlook.ai.model.EmbeddingModel
import com.secondlook.ai.notes.Fingerprinter
import com.secondlook.ai.notes.KIND_FACT
import com.secondlook.ai.notes.KIND_SECRET
import com.secondlook.ai.notes.MAX_NOTES
import com.secondlook.ai.notes.NoteAnchors
import com.secondlook.ai.notes.NoteLimitException
import com.secondlook.ai.notes.NoteSummary
import com.secondlook.ai.notes.NoteVectors
import com.secondlook.ai.notes.NotesStore
import com.secondlook.ai.notes.StoredNote
import com.secondlook.ai.notes.TaughtSecrets

/**
 * The AI core's [NotesStore] on top of the keyboard's SecretManager (handoff from AI Model/ai-local).
 *
 * What the user saves in the Flutter "Teach me" screen (SecretManager.addSecret(label, value): Keystore AES-GCM,
 * 5 notes max in the UI) is exactly what the keyboard checks. Storage, limit and UI stay as they are; this class only
 * turns each saved value into what the matchers need:
 *  - one secret value ("4821", "Lodi1234", or a sentence the rules find a secret in, "GCash MPIN ko: 4821")
 *    -> keyed fingerprint, caught anywhere in a draft, exact match (TaughtSecretMatcher)
 *  - anything else, a private fact ("Si Jakim ay nakatira sa Taguig") -> embedding vectors + names / code words,
 *    matched by meaning (NoteMatcher)
 * Vectors are rebuilt in memory from the saved text, so nothing new is written to disk and a new embedding model
 * needs no data migration.
 *
 * Do not edit this copy in the Android app: change it in AI Model/ai-local/handoff/android-app and copy it again.
 */
class SecretManagerNotesStore(private val context: Context, private val embedder: EmbeddingModel) : NotesStore {

    /** Per process; fingerprints are rebuilt from the saved values, so the key never needs to be stored. */
    override val fingerprinter = Fingerprinter(Fingerprinter.randomKey())

    private val built = HashMap<String, Pair<SecretNote, StoredNote>>()   // id -> (what it was built from, result)

    @Synchronized
    override fun notes(): List<StoredNote> {
        val secrets = SecretManager.getSecrets(context)
        built.keys.retainAll(secrets.map { it.id }.toSet())
        return secrets.map { s -> built[s.id]?.takeIf { it.first == s }?.second ?: build(s).also { built[s.id] = s to it } }
    }

    private fun build(s: SecretNote): StoredNote {
        val values = TaughtSecrets.valuesIn(s.value)
        return if (values.isNotEmpty()) {
            StoredNote(NoteSummary(s.id, s.label.ifBlank { "My secret" }, KIND_SECRET), emptyList(),
                fingerprints = values.map(fingerprinter::of).toSet())
        } else {
            StoredNote(NoteSummary(s.id, s.label.ifBlank { "Private note" }, KIND_FACT),
                NoteVectors.forNote(embedder, s.value), NoteAnchors.of(s.value))
        }
    }

    /** Changes whenever a note is added, edited or deleted. */
    fun version(): Int = SecretManager.getSecrets(context).hashCode()

    /** Saved private facts long enough to be matched word for word (the vault's original rule). */
    fun verbatimValues(): List<String> =
        SecretManager.getSecrets(context).map { it.value.trim() }.filter { it.length >= VERBATIM_MIN_CHARS && it.any(Char::isWhitespace) }

    /** A saved fact typed word for word: always private, whatever the similarity says. */
    fun verbatimMatch(draft: String): RiskResult? {
        for (s in SecretManager.getSecrets(context)) {
            val v = s.value.trim()
            if (v.length < VERBATIM_MIN_CHARS || v.none(Char::isWhitespace)) continue   // single values: fingerprints
            val at = draft.indexOf(v, ignoreCase = true)
            if (at >= 0) return RiskResult(RiskCategory.PRIVATE_NOTE, at, at + v.length,
                "This repeats a note you marked confidential: ${s.label}.", 0.99f, s.label, kind = "private_match")
        }
        return null
    }

    override fun save(text: String, label: String): NoteSummary {
        if (SecretManager.getSecrets(context).size >= MAX_NOTES) throw NoteLimitException()
        val s = SecretManager.addSecret(context, label, text)
        return notes().first { it.summary.id == s.id }.summary
    }

    override fun list(): List<NoteSummary> = notes().map { it.summary }
    override fun delete(id: String) { SecretManager.deleteSecret(context, id) }
    override fun deleteAll() { SecretManager.getSecrets(context).forEach { SecretManager.deleteSecret(context, it.id) } }

    private companion object {
        const val VERBATIM_MIN_CHARS = 6
    }
}
