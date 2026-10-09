package com.secondlook.ai.notes

import com.secondlook.ai.model.EmbeddingModel
import java.util.UUID

/** Non-persistent store, used by the PC check and tests. The phone uses android/EncryptedNotesStore. */
class InMemoryNotesStore(
    private val embedder: EmbeddingModel,
    override val fingerprinter: Fingerprinter = Fingerprinter(Fingerprinter.randomKey()),
) : NotesStore {
    private val items = ArrayList<StoredNote>()

    @Synchronized
    override fun save(text: String, label: String): NoteSummary {
        if (items.size >= MAX_NOTES) throw NoteLimitException()
        val id = UUID.randomUUID().toString()
        val values = TaughtSecrets.valuesIn(text)
        items += if (values.isNotEmpty()) {
            StoredNote(NoteSummary(id, label.ifBlank { "My secret" }, KIND_SECRET), emptyList(), fingerprints = values.map(fingerprinter::of).toSet())
        } else {
            StoredNote(NoteSummary(id, label.ifBlank { "Private note" }), NoteVectors.forNote(embedder, text), NoteAnchors.of(text))
        }
        return items.last().summary
    }

    @Synchronized override fun list(): List<NoteSummary> = items.map { it.summary }
    @Synchronized override fun delete(id: String) { items.removeAll { it.summary.id == id } }
    @Synchronized override fun deleteAll() = items.clear()
    @Synchronized override fun notes(): List<StoredNote> = items.toList()
}
