package com.secondlook.ai.notes

import com.secondlook.ai.model.EmbeddingModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.UUID

/**
 * Platform-independent persistent notes store securing private facts and taught secrets.
 *
 * Guaranteed Invariants:
 *  1. Secrets: Raw values are NEVER stored; only HMAC-SHA256 fingerprints are retained.
 *  2. Facts: Text and 384-dim embeddings are stored encrypted via [crypto].
 *  3. Versioning: Each fact payload stores its embedding [modelVersion].
 *  4. Interruption Safety: Background [migrate] commits each note atomically. If killed,
 *     already migrated notes retain their new vectors; unmigrated notes remain untouched.
 *  5. Retry Safety: Calling [migrate] again seamlessly resumes remaining unmigrated notes.
 */
open class PersistentNotesStore(
    protected val embedder: EmbeddingModel,
    protected val storage: KeyValueStorage,
    protected val crypto: NoteCryptoEngine,
) : NotesStore {

    protected var cache: List<StoredNote>? = null
    protected var fp: Fingerprinter? = null

    override val fingerprinter: Fingerprinter
        @Synchronized get() = fp ?: Fingerprinter(fingerprintKey()).also { fp = it }

    @Synchronized
    override fun save(text: String, label: String): NoteSummary {
        val items = readItems()
        if (items.length() >= MAX_NOTES) throw NoteLimitException()
        val id = UUID.randomUUID().toString()
        val values = TaughtSecrets.valuesIn(text)
        val summary: NoteSummary
        val payload: JSONObject
        if (values.isNotEmpty()) {
            summary = NoteSummary(id, label.ifBlank { "My secret" }, KIND_SECRET)
            payload = JSONObject().put("fps", JSONArray(values.map { fingerprinter.of(it) }))
        } else {
            summary = NoteSummary(id, label.ifBlank { "Private note" })
            val vectors = NoteVectors.forNote(embedder, text)
            payload = JSONObject()
                .put("text", text)
                .put("modelVersion", embedder.modelVersion)
                .put("vectors", JSONArray(vectors.map { v -> JSONArray(v.map { it.toDouble() }) }))
        }
        val encryptedBlob = Base64.getEncoder().encodeToString(crypto.encrypt(payload.toString().toByteArray(Charsets.UTF_8)))
        items.put(
            JSONObject().put("id", id).put("label", summary.label).put("kind", summary.kind)
                .put("blob", encryptedBlob),
        )
        storage.putString("items", items.toString())
        cache = null
        return summary
    }

    @Synchronized
    override fun list(): List<NoteSummary> {
        val items = readItems()
        return (0 until items.length()).map { items.getJSONObject(it).let { o -> summaryOf(o) } }
    }

    @Synchronized
    override fun delete(id: String) {
        val items = readItems()
        val kept = JSONArray()
        for (i in 0 until items.length()) if (items.getJSONObject(i).getString("id") != id) kept.put(items.getJSONObject(i))
        storage.putString("items", kept.toString())
        cache = null
    }

    @Synchronized
    override fun deleteAll() {
        storage.clear()
        runCatching { crypto.deleteKey() }
        cache = null
        fp = null
    }

    @Synchronized
    override fun notes(): List<StoredNote> {
        cache?.let { return it }
        val items = readItems()
        val loaded = (0 until items.length()).mapNotNull { i ->
            val o = items.getJSONObject(i)
            runCatching {
                val blob = Base64.getDecoder().decode(o.getString("blob"))
                val payload = JSONObject(String(crypto.decrypt(blob), Charsets.UTF_8))
                val summary = summaryOf(o)
                if (summary.kind == KIND_SECRET) {
                    val fps = payload.getJSONArray("fps")
                    StoredNote(summary, emptyList(), fingerprints = (0 until fps.length()).map { fps.getString(it) }.toSet())
                } else {
                    val arr = payload.getJSONArray("vectors")
                    val vectors = (0 until arr.length()).map { k ->
                        val a = arr.getJSONArray(k)
                        FloatArray(a.length()) { d -> a.getDouble(d).toFloat() }
                    }
                    val modelVer = payload.optString("modelVersion", "legacy")
                    StoredNote(summary, vectors, NoteAnchors.of(payload.getString("text")), modelVersion = modelVer)
                }
            }.getOrNull()
        }
        cache = loaded
        return loaded
    }

    /**
     * Atomically migrates outdated or legacy fact note vectors to [embedder.modelVersion].
     *
     * @param interrupter Optional lambda tested before each note. If it returns true, migration halts,
     *                    leaving already processed notes persisted and unmigrated notes available for retry.
     */
    @Synchronized
    fun migrate(interrupter: (() -> Boolean)? = null): MigrationResult {
        val targetVersion = embedder.modelVersion
        val items = readItems()
        var totalFactNotes = 0
        var alreadyUpToDate = 0
        var migrated = 0
        var failed = 0

        for (i in 0 until items.length()) {
            if (interrupter?.invoke() == true) {
                break
            }

            val o = items.getJSONObject(i)
            val kind = o.optString("kind", KIND_FACT)
            if (kind != KIND_FACT) continue
            totalFactNotes++

            val blobStr = o.optString("blob", "")
            if (blobStr.isEmpty()) continue

            try {
                val blob = Base64.getDecoder().decode(blobStr)
                val payload = JSONObject(String(crypto.decrypt(blob), Charsets.UTF_8))
                val currentVer = payload.optString("modelVersion", "legacy")
                if (currentVer == targetVersion) {
                    alreadyUpToDate++
                    continue
                }

                val text = payload.optString("text", "")
                if (text.isEmpty()) {
                    failed++
                    continue
                }

                // Recompute vectors using the active embedder
                val newVectors = NoteVectors.forNote(embedder, text)
                payload.put("modelVersion", targetVersion)
                payload.put("vectors", JSONArray(newVectors.map { v -> JSONArray(v.map { it.toDouble() }) }))

                // Re-encrypt and persist THIS note immediately (atomic per-note save)
                val newBlob = Base64.getEncoder().encodeToString(crypto.encrypt(payload.toString().toByteArray(Charsets.UTF_8)))
                o.put("blob", newBlob)
                storage.putString("items", items.toString())
                migrated++
            } catch (e: Throwable) {
                failed++
            }
        }

        if (migrated > 0) {
            cache = null
        }

        return MigrationResult(
            totalFactNotes = totalFactNotes,
            migratedCount = migrated,
            alreadyUpToDate = alreadyUpToDate,
            failedCount = failed,
        )
    }

    private fun summaryOf(o: JSONObject) = NoteSummary(o.getString("id"), o.getString("label"), o.optString("kind", KIND_FACT))

    private fun fingerprintKey(): ByteArray {
        storage.getString("fp_key", null)?.let { stored ->
            runCatching { return crypto.decrypt(Base64.getDecoder().decode(stored)) }
        }
        val key = Fingerprinter.randomKey()
        storage.putString("fp_key", Base64.getEncoder().encodeToString(crypto.encrypt(key)))
        return key
    }

    protected fun readItems(): JSONArray = JSONArray(storage.getString("items", "[]"))
}
