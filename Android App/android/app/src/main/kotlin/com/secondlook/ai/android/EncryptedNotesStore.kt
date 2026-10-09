package com.secondlook.ai.android

import android.content.Context
import com.secondlook.ai.model.EmbeddingModel
import com.secondlook.ai.notes.KeyValueStorage
import com.secondlook.ai.notes.NoteCryptoEngine
import com.secondlook.ai.notes.PersistentNotesStore

/**
 * Android Keystore AES-GCM encrypted notes store backed by SharedPreferences.
 * Inherits background vector migration with atomic interruption and retry safety.
 */
class EncryptedNotesStore(
    context: Context,
    embedder: EmbeddingModel,
) : PersistentNotesStore(
    embedder = embedder,
    storage = DefaultSharedPrefsStorage(context),
    crypto = DefaultNoteCryptoEngine,
)

private class DefaultSharedPrefsStorage(context: Context) : KeyValueStorage {
    private val prefs = context.applicationContext.getSharedPreferences("secondlook_notes", Context.MODE_PRIVATE)
    override fun getString(key: String, def: String?): String? = prefs.getString(key, def)
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun clear() { prefs.edit().clear().apply() }
}

private object DefaultNoteCryptoEngine : NoteCryptoEngine {
    override fun encrypt(plain: ByteArray): ByteArray = NoteCrypto.encrypt(plain)
    override fun decrypt(blob: ByteArray): ByteArray = NoteCrypto.decrypt(blob)
    override fun deleteKey() = NoteCrypto.deleteKey()
}
