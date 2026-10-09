package com.secondlook

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SecretNote(
    val id: String,
    val label: String,
    val value: String,
    val createdAt: Long
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "label" to label,
            "value" to value,
            "createdAt" to createdAt
        )
    }
}

object SecretManager {
    private const val TAG = "SecretManager"
    private const val KEY_ALIAS = "secondlook_vault_key"
    private const val PREFS_NAME = "secondlook_vault_prefs"
    private const val KEY_SECRETS_DATA = "encrypted_secrets_json"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val cachedSecrets = mutableListOf<SecretNote>()
    private var isLoaded = false

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedBase64: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val ivSize = 12 // GCM standard IV length
        val iv = ByteArray(ivSize)
        System.arraycopy(combined, 0, iv, 0, ivSize)
        val cipherText = ByteArray(combined.size - ivSize)
        System.arraycopy(combined, ivSize, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        val decrypted = cipher.doFinal(cipherText)
        return String(decrypted, Charsets.UTF_8)
    }

    @Synchronized
    fun getSecrets(context: Context): List<SecretNote> {
        if (isLoaded) return cachedSecrets.toList()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedJson = prefs.getString(KEY_SECRETS_DATA, null)
        cachedSecrets.clear()

        if (!encryptedJson.isNullOrEmpty()) {
            try {
                val decryptedJson = decrypt(encryptedJson)
                val array = JSONArray(decryptedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    cachedSecrets.add(
                        SecretNote(
                            id = obj.getString("id"),
                            label = obj.getString("label"),
                            value = obj.getString("value"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt secrets from storage", e)
            }
        }

        isLoaded = true
        return cachedSecrets.toList()
    }

    @Synchronized
    fun addSecret(context: Context, label: String, value: String): SecretNote {
        getSecrets(context) // Ensure loaded
        val note = SecretNote(
            id = UUID.randomUUID().toString(),
            label = label,
            value = value,
            createdAt = System.currentTimeMillis()
        )
        cachedSecrets.add(note)
        persist(context)

        // Sync with Annie's AI core so Candidate v2 embeddings and HMAC fingerprints are trained
        try {
            com.secondlook.ai.AnnieAiAdapter.instance?.saveTaughtSecret(value, label)
        } catch (e: Exception) {
            Log.w(TAG, "Could not sync new secret to AnnieAiAdapter", e)
        }

        return note
    }

    @Synchronized
    fun deleteSecret(context: Context, id: String): Boolean {
        getSecrets(context) // Ensure loaded
        val secretToDelete = cachedSecrets.find { it.id == id }
        val removed = cachedSecrets.removeAll { it.id == id }
        if (removed) {
            persist(context)
            try {
                com.secondlook.ai.AnnieAiAdapter.instance?.deleteTaughtSecret(id)
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete secret from AnnieAiAdapter", e)
            }
        }
        return removed
    }

    private fun persist(context: Context) {
        val array = JSONArray()
        for (secret in cachedSecrets) {
            val obj = JSONObject().apply {
                put("id", secret.id)
                put("label", secret.label)
                put("value", secret.value)
                put("createdAt", secret.createdAt)
            }
            array.put(obj)
        }
        try {
            val encrypted = encrypt(array.toString())
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_SECRETS_DATA, encrypted).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and save secrets", e)
        }
    }

    /**
     * Checks if text contains any user-saved private notes.
     */
    fun containsProtectedSecret(text: String): Boolean {
        val lower = text.lowercase()
        return cachedSecrets.any {
            it.value.isNotBlank() && lower.contains(it.value.lowercase())
        }
    }
}
