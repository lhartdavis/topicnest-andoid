package com.techtower.meetingtranscriber.transcription

import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenRouterSttClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .callTimeout(8, TimeUnit.MINUTES)
        .build(),
) {
    suspend fun transcribe(
        apiKey: String,
        audioBytes: ByteArray,
        format: String,
        model: String = DEFAULT_TRANSCRIPTION_MODEL,
    ): String =
        withContext(Dispatchers.IO) {
            val requestJson = buildRequestJson(model, audioBytes, format)
            val request = Request.Builder()
                .url("$BASE_URL$TRANSCRIPTIONS_PATH")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${body.take(MAX_ERROR_BODY_CHARS)}")
                }
                body
            }
        }

    private fun buildRequestJson(
        model: String,
        audioBytes: ByteArray,
        format: String,
    ): JsonObject =
        buildJsonObject {
            put("model", model)
            putJsonObject("input_audio") {
                put("data", Base64.getEncoder().encodeToString(audioBytes))
                put("format", format)
            }
            put("temperature", 0)
            put("response_format", "verbose_json")
            put("timestamp_granularities", buildJsonArray {
                add(kotlinx.serialization.json.JsonPrimitive("word"))
                add(kotlinx.serialization.json.JsonPrimitive("segment"))
            })
            putJsonObject("provider") {
                put("require_parameters", false)
            }
        }

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/"
        private const val TRANSCRIPTIONS_PATH = "audio/transcriptions"
        private const val MAX_ERROR_BODY_CHARS = 500
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
