@file:Suppress("DEPRECATION")

package com.techtower.meetingtranscriber.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(context: Context) {
    private val preferences: SharedPreferences = createPreferences(context.applicationContext)

    fun getApiKey(): String? = preferences.getString(KEY_OPENROUTER_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun hasApiKey(): Boolean = getApiKey() != null

    fun saveApiKey(apiKey: String) {
        preferences.edit().putString(KEY_OPENROUTER_API_KEY, apiKey.trim()).apply()
    }

    fun getRecorderTreeUri(): Uri? =
        preferences.getString(KEY_RECORDER_TREE_URI, null)?.let(Uri::parse)

    fun saveRecorderTreeUri(uri: Uri) {
        preferences.edit().putString(KEY_RECORDER_TREE_URI, uri.toString()).apply()
    }

    fun getMaxDirectUploadBytes(): Long =
        preferences.getLong(KEY_MAX_DIRECT_UPLOAD_BYTES, DEFAULT_MAX_DIRECT_UPLOAD_BYTES)

    private fun createPreferences(context: Context): SharedPreferences =
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            // TODO: Surface this security downgrade in a real settings screen. The fallback keeps
            // the MVP usable on devices where AndroidX Security cannot create a master key.
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }

    companion object {
        private const val SECURE_PREFS_NAME = "secure_settings"
        private const val FALLBACK_PREFS_NAME = "settings"
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        private const val KEY_RECORDER_TREE_URI = "recorder_tree_uri"
        private const val KEY_MAX_DIRECT_UPLOAD_BYTES = "max_direct_upload_bytes"
        const val DEFAULT_MAX_DIRECT_UPLOAD_BYTES: Long = 25L * 1024L * 1024L
    }
}
