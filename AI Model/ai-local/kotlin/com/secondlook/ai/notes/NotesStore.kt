package com.secondlook.ai.notes

const val MAX_NOTES = 5

/** A private fact ("Si Jakim ay nakatira sa Taguig"): matched by meaning + names. */
const val KIND_FACT = "fact"
/** A secret value (MPIN, password, passcode, API key): matched exactly; only a fingerprint is kept. See [TaughtSecrets]. */
const val KIND_SECRET = "secret"

/** What the UI may see about a note. The full secret text is never exposed to Flutter. [kind] = [KIND_FACT] | [KIND_SECRET]. */
data class NoteSummary(val id: String, val label: String, val kind: String = KIND_FACT)

/**
 * A note as the matchers see it: label, embedding vectors (whole note + each sentence) and its anchors for a fact;
 * keyed fingerprints for a secret value (vectors and anchors then empty).
 */
class StoredNote(
    val summary: NoteSummary,
    val vectors: List<FloatArray>,
    val anchors: List<Anchor> = emptyList(),
    val fingerprints: Set<String> = emptySet(),
)

class NoteLimitException : IllegalStateException("You can keep at most $MAX_NOTES private notes.")

/**
 * On-device storage for up to [MAX_NOTES] things the user taught (see docs/contract.md).
 * Owner: AI lead. Flutter reaches it only through the MethodChannel bridge; the keyboard reads it directly.
 * Implementations: [InMemoryNotesStore] (tests), android/EncryptedNotesStore (Keystore AES-GCM).
 */
interface NotesStore {
    /**
     * Saves what the user typed into "Teach me". A single word ("4821", "Lodi1234") or an entry with a secret the
     * rules recognise ("GCash MPIN ko: 4821") becomes a [KIND_SECRET] (fingerprint only, the value is not stored);
     * anything else is a [KIND_FACT] (embedded). Throws [NoteLimitException] when full.
     */
    fun save(text: String, label: String): NoteSummary
    fun list(): List<NoteSummary>
    fun delete(id: String)
    fun deleteAll()
    /** Used by the matchers on every check, so implementations should cache. */
    fun notes(): List<StoredNote>
    /** Keyed fingerprints for secret values; the key never leaves this store. */
    val fingerprinter: Fingerprinter
}
