package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.codeXDataStore by preferencesDataStore(name = "codex_secure_prefs")

data class EditorPreferences(
    val fontSizeSp: Int = 13,
    val autoSave: Boolean = true,
    val showLineNumbers: Boolean = true,
    val darkTheme: Boolean = true,
    val buildServerUrl: String = "http://10.0.2.2:8787"
)

class SecureKeyStore(private val context: Context) {

    private val activeUserIdKey = stringPreferencesKey("active_user_id")
    private val editorFontSizeKey = intPreferencesKey("editor_font_size")
    private val editorAutoSaveKey = booleanPreferencesKey("editor_auto_save")
    private val editorLineNumbersKey = booleanPreferencesKey("editor_line_numbers")
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val buildServerUrlKey = stringPreferencesKey("build_server_url")

    private fun keyPref(providerId: String) = stringPreferencesKey("enc_api_key_$providerId")

    val activeUserId: Flow<String?> = context.codeXDataStore.data.map { it[activeUserIdKey] }

    val editorPreferences: Flow<EditorPreferences> = context.codeXDataStore.data.map { prefs ->
        EditorPreferences(
            fontSizeSp = prefs[editorFontSizeKey] ?: 13,
            autoSave = prefs[editorAutoSaveKey] ?: true,
            showLineNumbers = prefs[editorLineNumbersKey] ?: true,
            darkTheme = prefs[darkThemeKey] ?: true,
            buildServerUrl = prefs[buildServerUrlKey] ?: "http://10.0.2.2:8787"
        )
    }

    suspend fun setActiveUserId(userId: String?) {
        context.codeXDataStore.edit { prefs ->
            if (userId.isNullOrBlank()) {
                prefs.remove(activeUserIdKey)
            } else {
                prefs[activeUserIdKey] = userId
            }
        }
    }

    suspend fun updateEditorPreferences(update: (EditorPreferences) -> EditorPreferences) {
        context.codeXDataStore.edit { prefs ->
            val current = EditorPreferences(
                fontSizeSp = prefs[editorFontSizeKey] ?: 13,
                autoSave = prefs[editorAutoSaveKey] ?: true,
                showLineNumbers = prefs[editorLineNumbersKey] ?: true,
                darkTheme = prefs[darkThemeKey] ?: true,
                buildServerUrl = prefs[buildServerUrlKey] ?: "http://10.0.2.2:8787"
            )
            val next = update(current)
            prefs[editorFontSizeKey] = next.fontSizeSp.coerceIn(10, 22)
            prefs[editorAutoSaveKey] = next.autoSave
            prefs[editorLineNumbersKey] = next.showLineNumbers
            prefs[darkThemeKey] = next.darkTheme
            prefs[buildServerUrlKey] = next.buildServerUrl.trim().ifBlank { "http://10.0.2.2:8787" }
        }
    }

    suspend fun saveApiKey(providerId: String, rawKey: String) {
        val trimmed = rawKey.trim()
        context.codeXDataStore.edit { prefs ->
            if (trimmed.isEmpty()) {
                prefs.remove(keyPref(providerId))
            } else {
                prefs[keyPref(providerId)] = encryptString(trimmed)
            }
        }
    }

    suspend fun getApiKey(providerId: String): String {
        val encrypted = context.codeXDataStore.data.first()[keyPref(providerId)] ?: return ""
        return decryptString(encrypted)
    }

    suspend fun hasApiKey(providerId: String): Boolean {
        return !context.codeXDataStore.data.first()[keyPref(providerId)].isNullOrBlank()
    }

    suspend fun removeApiKey(providerId: String) {
        context.codeXDataStore.edit { it.remove(keyPref(providerId)) }
    }

    suspend fun getMaskedKey(providerId: String): String {
        val raw = getApiKey(providerId)
        if (raw.isBlank()) return ""
        return if (raw.length <= 8) {
            "••••••••"
        } else {
            "${raw.take(4)}••••••••${raw.takeLast(4)}"
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encryptString(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        return "$ivB64:$cipherB64"
    }

    private fun decryptString(payload: String): String {
        return try {
            val parts = payload.split(":")
            if (parts.size != 2) return ""
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private companion object {
        const val KEY_ALIAS = "codex_api_key_master_v1"
    }
}
