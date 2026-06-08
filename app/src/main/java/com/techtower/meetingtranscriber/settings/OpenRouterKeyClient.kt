package com.techtower.meetingtranscriber.settings

import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

class OpenRouterKeyClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun validate(apiKey: String): ApiKeyValidationResult =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext ApiKeyValidationResult(
                    status = ApiKeyValidationStatus.INVALID,
                    message = "Paste an OpenRouter API key before testing.",
                )
            }

            val request = Request.Builder()
                .url("$BASE_URL$keyPath")
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .get()
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    when (response.code) {
                        200 -> parseSuccess(body)
                        401 -> ApiKeyValidationResult(
                            status = ApiKeyValidationStatus.INVALID,
                            message = "OpenRouter rejected this key. Paste a valid key and test again.",
                        )
                        else -> ApiKeyValidationResult(
                            status = ApiKeyValidationStatus.UNKNOWN,
                            message = "Could not verify key: HTTP ${response.code}.",
                        )
                    }
                }
            } catch (error: IOException) {
                ApiKeyValidationResult(
                    status = ApiKeyValidationStatus.UNKNOWN,
                    message = "Could not reach OpenRouter to test the key: ${error.message ?: "network error"}.",
                )
            }
        }

    internal fun parseSuccess(rawJson: String): ApiKeyValidationResult {
        val data = runCatching { json.parseToJsonElement(rawJson).jsonObject["data"]?.jsonObject }.getOrNull()
        val label = data.stringValue("label")
        val remaining = data.doubleValue("limit_remaining")
        val message = if (remaining != null) {
            "Key works. Limit remaining: ${remaining.formatForDisplay()}."
        } else {
            "Key works."
        }
        return ApiKeyValidationResult(
            status = ApiKeyValidationStatus.VALID,
            message = message,
            label = label,
        )
    }

    private fun JsonObject?.stringValue(key: String): String? =
        (this?.get(key) as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }

    private fun JsonObject?.doubleValue(key: String): Double? =
        (this?.get(key) as? JsonPrimitive)?.content?.toDoubleOrNull()

    private fun Double.formatForDisplay(): String =
        if (this % 1.0 == 0.0) toLong().toString() else String.format(Locale.US, "%.2f", this)

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/"
        private const val keyPath = "key"
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
