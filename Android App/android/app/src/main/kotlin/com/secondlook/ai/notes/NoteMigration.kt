package com.secondlook.ai.notes

/**
 * Storage abstraction for persisting note JSON payloads (e.g. SharedPreferences on Android,
 * or in-memory map for unit tests).
 */
interface KeyValueStorage {
    fun getString(key: String, def: String? = null): String?
    fun putString(key: String, value: String)
    fun clear()
}

/**
 * Encryption engine for securing private note payloads (e.g. AndroidKeyStore AES-GCM on Android,
 * or software AES-GCM for testing).
 */
interface NoteCryptoEngine {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(blob: ByteArray): ByteArray
    fun deleteKey()
}

/**
 * Results of a background note vector migration pass.
 */
data class MigrationResult(
    val totalFactNotes: Int,
    val migratedCount: Int,
    val alreadyUpToDate: Int,
    val failedCount: Int,
)
