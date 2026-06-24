@file:Suppress("DEPRECATION")

package com.techtower.meetingtranscriber.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository internal constructor(
    private val preferences: SharedPreferences,
) {
    constructor(context: Context) : this(createPreferences(context.applicationContext))

    fun getApiKey(): String? = preferences.getString(KEY_OPENROUTER_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun hasApiKey(): Boolean = getApiKey() != null

    fun saveApiKey(apiKey: String) {
        val trimmedKey = apiKey.trim()
        val previousKey = getApiKey()
        preferences.edit()
            .putString(KEY_OPENROUTER_API_KEY, trimmedKey)
            .apply()
        if (previousKey != trimmedKey) {
            saveApiKeyValidation(
                ApiKeyValidationSnapshot(
                    status = ApiKeyValidationStatus.UNKNOWN,
                    message = "Key has not been tested yet.",
                ),
            )
        }
    }

    fun getApiKeyValidation(): ApiKeyValidationSnapshot =
        ApiKeyValidationSnapshot(
            status = preferences.getString(KEY_API_KEY_STATUS, null)
                ?.let { runCatching { ApiKeyValidationStatus.valueOf(it) }.getOrNull() }
                ?: ApiKeyValidationStatus.UNKNOWN,
            message = preferences.getString(KEY_API_KEY_MESSAGE, null),
            label = preferences.getString(KEY_API_KEY_LABEL, null),
            checkedAtMillis = preferences.getLong(KEY_API_KEY_CHECKED_AT, 0L).takeIf { it > 0L },
        )

    fun saveApiKeyValidation(snapshot: ApiKeyValidationSnapshot) {
        preferences.edit()
            .putString(KEY_API_KEY_STATUS, snapshot.status.name)
            .putString(KEY_API_KEY_MESSAGE, snapshot.message)
            .putString(KEY_API_KEY_LABEL, snapshot.label)
            .putLong(KEY_API_KEY_CHECKED_AT, snapshot.checkedAtMillis ?: 0L)
            .apply()
    }

    fun getRecorderTreeUri(): Uri? =
        preferences.getString(KEY_RECORDER_TREE_URI, null)?.let(Uri::parse)

    fun saveRecorderTreeUri(uri: Uri) {
        preferences.edit().putString(KEY_RECORDER_TREE_URI, uri.toString()).apply()
    }

    fun getMaxDirectUploadBytes(): Long =
        preferences.getLong(KEY_MAX_DIRECT_UPLOAD_BYTES, DEFAULT_MAX_DIRECT_UPLOAD_BYTES)

    fun isDiscoveryCompactList(): Boolean =
        preferences.getBoolean(KEY_DISCOVERY_COMPACT_LIST, false)

    fun saveDiscoveryCompactList(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DISCOVERY_COMPACT_LIST, enabled).apply()
    }

    fun isTranscriptsCompactList(): Boolean =
        preferences.getBoolean(KEY_TRANSCRIPTS_COMPACT_LIST, false)

    fun saveTranscriptsCompactList(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_TRANSCRIPTS_COMPACT_LIST, enabled).apply()
    }

    companion object {
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

        private const val SECURE_PREFS_NAME = "secure_settings"
        private const val FALLBACK_PREFS_NAME = "settings"
        private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        private const val KEY_API_KEY_STATUS = "openrouter_api_key_status"
        private const val KEY_API_KEY_MESSAGE = "openrouter_api_key_message"
        private const val KEY_API_KEY_LABEL = "openrouter_api_key_label"
        private const val KEY_API_KEY_CHECKED_AT = "openrouter_api_key_checked_at"
        private const val KEY_RECORDER_TREE_URI = "recorder_tree_uri"
        private const val KEY_MAX_DIRECT_UPLOAD_BYTES = "max_direct_upload_bytes"
        private const val KEY_DISCOVERY_COMPACT_LIST = "discovery_compact_list"
        private const val KEY_TRANSCRIPTS_COMPACT_LIST = "transcripts_compact_list"
        const val DEFAULT_MAX_DIRECT_UPLOAD_BYTES: Long = 25L * 1024L * 1024L
    }
}
