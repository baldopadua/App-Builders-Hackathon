package com.secondlook.ai.android

import android.content.Context
import android.util.Base64
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Private notes encrypted on the phone. Per note, only the label and kind are stored in the clear (shown in the UI).
 * A fact's text and embedding vectors are an AES-GCM blob (see [NoteCrypto]). A secret value is never stored: only
 * its keyed fingerprint, also inside the encrypted blob; the fingerprint key itself is encrypted with the Keystore key.
 * Nothing is logged or uploaded.
 */
class EncryptedNotesStore(context: Context, private val embedder: EmbeddingModel) : NotesStore {
    private val prefs = context.applicationContext.getSharedPreferences("secondlook_notes", Context.MODE_PRIVATE)
    private var cache: List<StoredNote>? = null
    private var fp: Fingerprinter? = null

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
            payload = JSONObject().put("fps", JSONArray(values.map { fingerprinter.of(it) }))   // the value itself is not kept
        } else {
            summary = NoteSummary(id, label.ifBlank { "Private note" })
            val vectors = NoteVectors.forNote(embedder, text)
            payload = JSONObject()
                .put("text", text)
                .put("vectors", JSONArray(vectors.map { v -> JSONArray(v.map { it.toDouble() }) }))
        }
        items.put(
            JSONObject().put("id", id).put("label", summary.label).put("kind", summary.kind)
                .put("blob", Base64.encodeToString(NoteCrypto.encrypt(payload.toString().toByteArray()), Base64.NO_WRAP)),
        )
        prefs.edit().putString("items", items.toString()).apply()
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
        prefs.edit().putString("items", kept.toString()).apply()
        cache = null
    }

    @Synchronized
    override fun deleteAll() {
        prefs.edit().clear().apply()
        runCatching { NoteCrypto.deleteKey() }
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
                val payload = JSONObject(String(NoteCrypto.decrypt(Base64.decode(o.getString("blob"), Base64.NO_WRAP))))
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
                    StoredNote(summary, vectors, NoteAnchors.of(payload.getString("text")))
                }
            }.getOrNull()   // a note that can no longer be decrypted is skipped
        }
        cache = loaded
        return loaded
    }

    private fun summaryOf(o: JSONObject) = NoteSummary(o.getString("id"), o.getString("label"), o.optString("kind", KIND_FACT))

    /** 32 random bytes made once per install, stored encrypted with the Keystore key. */
    private fun fingerprintKey(): ByteArray {
        prefs.getString("fp_key", null)?.let { stored ->
            runCatching { return NoteCrypto.decrypt(Base64.decode(stored, Base64.NO_WRAP)) }
        }
        val key = Fingerprinter.randomKey()
        prefs.edit().putString("fp_key", Base64.encodeToString(NoteCrypto.encrypt(key), Base64.NO_WRAP)).apply()
        return key
    }

    private fun readItems(): JSONArray = JSONArray(prefs.getString("items", "[]"))
}
