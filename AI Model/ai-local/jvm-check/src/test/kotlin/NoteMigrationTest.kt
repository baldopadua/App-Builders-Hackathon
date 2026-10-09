package com.secondlook.check

import com.secondlook.ai.model.EmbeddingModel
import com.secondlook.ai.notes.KIND_FACT
import com.secondlook.ai.notes.KIND_SECRET
import com.secondlook.ai.notes.KeyValueStorage
import com.secondlook.ai.notes.NoteCryptoEngine
import com.secondlook.ai.notes.PersistentNotesStore
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private class TestSoftwareCryptoEngine : NoteCryptoEngine {
    private val key = SecretKeySpec(ByteArray(32) { 7 }, "AES")
    override fun encrypt(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key)
        return c.iv + c.doFinal(plain)
    }

    override fun decrypt(blob: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob, 0, 12))
        return c.doFinal(blob, 12, blob.size - 12)
    }

    override fun deleteKey() {}
}

private class TestInMemoryStorage : KeyValueStorage {
    val data = mutableMapOf<String, String>()
    override fun getString(key: String, def: String?): String? = data[key] ?: def
    override fun putString(key: String, value: String) { data[key] = value }
    override fun clear() { data.clear() }
}

class NoteMigrationTest {

    private fun loadEmbedder(): EmbeddingModel {
        val modelBytes = File("../assets/models/embedding/model_quantized.onnx").readBytes()
        val vocab = File("../assets/models/embedding/vocab.txt").readLines().filter { it.isNotEmpty() }
        return EmbeddingModel(modelBytes, vocab)
    }

    @Test
    fun testLegacyFactNoteLoadsAndMigratesSuccessfully() {
        loadEmbedder().use { embedder ->
            val storage = TestInMemoryStorage()
            val crypto = TestSoftwareCryptoEngine()
            val store = PersistentNotesStore(embedder, storage, crypto)

            // Inject a legacy unversioned note directly into storage
            val id = "legacy-note-1"
            val text = "Our client is buying the bakery next door."
            val dummyVectors = listOf(FloatArray(384) { 0.01f })
            val legacyPayload = JSONObject()
                .put("text", text)
                .put("vectors", JSONArray(dummyVectors.map { v -> JSONArray(v.map { it.toDouble() }) }))
            // Intentionally no "modelVersion" field (mirrors old pre-Gate 3 storage)

            val blob = Base64.getEncoder().encodeToString(crypto.encrypt(legacyPayload.toString().toByteArray(Charsets.UTF_8)))
            val items = JSONArray().put(JSONObject().put("id", id).put("label", "Sunflower").put("kind", KIND_FACT).put("blob", blob))
            storage.putString("items", items.toString())

            // 1. Verify store loads legacy note without crashing
            val initialNotes = store.notes()
            assertEquals(1, initialNotes.size)
            assertEquals("legacy", initialNotes[0].modelVersion)
            assertEquals(0.01f, initialNotes[0].vectors[0][0], 1e-4f)

            // 2. Perform migration
            val result = store.migrate()
            assertEquals(1, result.totalFactNotes)
            assertEquals(1, result.migratedCount)
            assertEquals(0, result.alreadyUpToDate)
            assertEquals(0, result.failedCount)

            // 3. Verify upgraded vectors match active embedder and modelVersion is updated
            val migratedNotes = store.notes()
            assertEquals(1, migratedNotes.size)
            assertEquals(embedder.modelVersion, migratedNotes[0].modelVersion)
            // Vector should now be the real embedding, not 0.01f
            assertFalse(Math.abs(migratedNotes[0].vectors[0][0] - 0.01f) < 1e-5f)

            // 4. Calling migrate again should be an idempotent no-op
            val secondResult = store.migrate()
            assertEquals(1, secondResult.totalFactNotes)
            assertEquals(0, secondResult.migratedCount)
            assertEquals(1, secondResult.alreadyUpToDate)
            assertEquals(0, secondResult.failedCount)
        }
    }

    @Test
    fun testInterruptionAndRetryPreservesIntegrity() {
        loadEmbedder().use { embedder ->
            val storage = TestInMemoryStorage()
            val crypto = TestSoftwareCryptoEngine()
            val store = PersistentNotesStore(embedder, storage, crypto)

            // Create 4 legacy notes
            val items = JSONArray()
            val texts = listOf(
                "First confidential note about project Alpha",
                "Second confidential note regarding project Beta",
                "Third secret note concerning budget Gamma",
                "Fourth private note regarding timeline Delta"
            )

            texts.forEachIndexed { i, text ->
                val legacyPayload = JSONObject()
                    .put("text", text)
                    .put("modelVersion", "old_v1")
                    .put("vectors", JSONArray(listOf(JSONArray(DoubleArray(384) { 0.0 })) ))
                val blob = Base64.getEncoder().encodeToString(crypto.encrypt(legacyPayload.toString().toByteArray(Charsets.UTF_8)))
                items.put(JSONObject().put("id", "note-$i").put("label", "Note $i").put("kind", KIND_FACT).put("blob", blob))
            }
            storage.putString("items", items.toString())

            // Simulate interruption: interrupter trips after migrating 2 notes
            var migratedCounter = 0
            val interruptedResult = store.migrate(interrupter = {
                if (migratedCounter >= 2) true else { migratedCounter++; false }
            })

            assertEquals(2, interruptedResult.migratedCount)

            // Verify store state while interrupted: exactly 4 notes remain, 2 are migrated, 2 are still old
            val partialNotes = store.notes()
            assertEquals(4, partialNotes.size)
            assertEquals(embedder.modelVersion, partialNotes[0].modelVersion)
            assertEquals(embedder.modelVersion, partialNotes[1].modelVersion)
            assertEquals("old_v1", partialNotes[2].modelVersion)
            assertEquals("old_v1", partialNotes[3].modelVersion)

            // Retry migration: completes the remaining 2 notes without corrupting the first 2
            val retryResult = store.migrate()
            assertEquals(4, retryResult.totalFactNotes)
            assertEquals(2, retryResult.migratedCount)
            assertEquals(2, retryResult.alreadyUpToDate)
            assertEquals(0, retryResult.failedCount)

            val finalNotes = store.notes()
            assertEquals(4, finalNotes.size)
            finalNotes.forEach {
                assertEquals(embedder.modelVersion, it.modelVersion)
            }
        }
    }

    @Test
    fun testTaughtSecretsAreUntouchedByMigration() {
        loadEmbedder().use { embedder ->
            val storage = TestInMemoryStorage()
            val crypto = TestSoftwareCryptoEngine()
            val store = PersistentNotesStore(embedder, storage, crypto)

            // Save a taught secret (MPIN)
            val secretSummary = store.save("GCash MPIN ko: 4821", "MPIN")
            assertEquals(KIND_SECRET, secretSummary.kind)

            val initialNotes = store.notes()
            assertEquals(1, initialNotes.size)
            assertEquals(1, initialNotes[0].fingerprints.size)

            // Migration should skip KIND_SECRET notes completely
            val result = store.migrate()
            assertEquals(0, result.totalFactNotes)
            assertEquals(0, result.migratedCount)
            assertEquals(0, result.alreadyUpToDate)

            // Fingerprint still intact and matchable
            val postMigrationNotes = store.notes()
            assertEquals(1, postMigrationNotes.size)
            assertEquals(initialNotes[0].fingerprints, postMigrationNotes[0].fingerprints)
        }
    }
}
