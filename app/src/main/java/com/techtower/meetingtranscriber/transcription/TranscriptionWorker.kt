package com.techtower.meetingtranscriber.transcription

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.techtower.meetingtranscriber.data.AppDatabase
import com.techtower.meetingtranscriber.settings.SettingsRepository
import com.techtower.meetingtranscriber.util.audioFormatFromNameOrMimeType
import java.io.IOException

class TranscriptionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val database = AppDatabase.getInstance(appContext)
    private val repository = TranscriptRepository(appContext, database)
    private val settingsRepository = SettingsRepository(appContext)
    private val sttClient = OpenRouterSttClient()
    private val parser = TimestampParser()

    override suspend fun doWork(): Result {
        val jobId = inputData.getLong(KEY_JOB_ID, 0L)
        if (jobId == 0L) return Result.failure()

        val job = database.transcriptJobDao().getById(jobId) ?: return Result.failure()
        val apiKey = settingsRepository.getApiKey()
        if (apiKey == null) {
            repository.markFailed(jobId, "OpenRouter API key is missing.")
            return Result.failure()
        }

        repository.markProcessing(jobId)
        return runCatching {
            val uri = Uri.parse(job.audioUri)
            val mimeType = applicationContext.contentResolver.getType(uri)
            val format = audioFormatFromNameOrMimeType(job.displayName, mimeType)
                ?: throw IOException("Unsupported audio format.")
            // Direct upload keeps the MVP easy to inspect. TODO: add chunking with
            // MediaExtractor/MediaMuxer or FFmpeg when provider size limits become a real blocker.
            val bytes = readAudioBytes(uri)
            val rawJson = sttClient.transcribe(
                apiKey = apiKey,
                audioBytes = bytes,
                format = format,
                model = job.model,
            )
            val parsed = parser.parse(rawJson)
            if (parsed.plainText.isBlank()) {
                throw IOException("No transcript text returned.")
            }
            repository.markTranscribed(jobId, parsed, rawJson)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                repository.markFailed(jobId, error.userMessage())
                Result.failure()
            },
        )
    }

    private fun readAudioBytes(uri: Uri): ByteArray =
        try {
            applicationContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("Could not open audio file.")
        } catch (_: OutOfMemoryError) {
            throw IOException("File too large for direct upload.")
        }

    private fun Throwable.userMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "Transcription failed."

    companion object {
        const val KEY_JOB_ID = "job_id"
    }
}
