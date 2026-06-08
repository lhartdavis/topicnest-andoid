package com.techtower.meetingtranscriber.transcription

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.techtower.meetingtranscriber.data.AppDatabase
import com.techtower.meetingtranscriber.settings.ApiKeyValidationStatus
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
    private val remuxer = AacToM4aRemuxer(appContext)
    private val wavTranscoder = AudioToWavTranscoder(appContext)
    private val audioProcessor = AudioProcessingService(appContext)
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
        var uploadFormat: String? = null
        return runCatching {
            val uri = Uri.parse(job.audioUri)
            val mimeType = applicationContext.contentResolver.getType(uri)
            val format = audioFormatFromNameOrMimeType(job.displayName, mimeType)
                ?: throw IOException("Unsupported audio format.")
            val result = transcribeJob(
                apiKey = apiKey,
                uri = uri,
                displayName = job.displayName,
                uploadFormat = format,
                sizeBytes = job.sizeBytes,
                durationMillis = job.durationMillis,
                model = job.model,
                updateUploadFormat = { uploadFormat = it },
            )
            repository.markTranscribed(jobId, result.parsed, result.rawJson)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                repository.markFailed(jobId, error.userMessage(uploadFormat))
                Result.failure()
            },
        )
    }

    private suspend fun transcribeWithFallbacks(
        apiKey: String,
        uri: Uri,
        displayName: String,
        uploadFormat: String,
        sizeBytes: Long,
        durationMillis: Long?,
        preparedAudio: PreparedUploadAudio,
        model: String,
        updateUploadFormat: (String) -> Unit,
    ): CompletedTranscription {
        updateUploadFormat(preparedAudio.format)
        return try {
            parseCompletedTranscription(
                rawJson = sttClient.transcribe(
                    apiKey = apiKey,
                    audioBytes = preparedAudio.bytes,
                    format = preparedAudio.format,
                    model = model,
                ),
            )
        } catch (error: OpenRouterSttException) {
            if (error.statusCode != 400) {
                throw error
            }

            if (preparedAudio.format in setOf("aac", "m4a")) {
                return transcribeMp3Pipeline(
                    apiKey = apiKey,
                    uri = uri,
                    displayName = displayName,
                    uploadFormat = uploadFormat,
                    sizeBytes = sizeBytes,
                    durationMillis = durationMillis,
                    model = model,
                    updateUploadFormat = updateUploadFormat,
                )
            }

            throw error
        }
    }

    private suspend fun transcribeJob(
        apiKey: String,
        uri: Uri,
        displayName: String,
        uploadFormat: String,
        sizeBytes: Long,
        durationMillis: Long?,
        model: String,
        updateUploadFormat: (String) -> Unit,
    ): CompletedTranscription {
        val maxDirectUploadBytes = settingsRepository.getMaxDirectUploadBytes()
        if (
            shouldUseMp3Processing(
                displayName = displayName,
                uploadFormat = uploadFormat,
                rawBytes = sizeBytes,
                maxDirectUploadBytes = maxDirectUploadBytes,
            )
        ) {
            return transcribeMp3Pipeline(
                apiKey = apiKey,
                uri = uri,
                displayName = displayName,
                uploadFormat = uploadFormat,
                sizeBytes = sizeBytes,
                durationMillis = durationMillis,
                model = model,
                updateUploadFormat = updateUploadFormat,
            )
        }

        val preparedAudio = prepareUploadAudio(uri, displayName, uploadFormat)
        return try {
            transcribeWithFallbacks(
                apiKey = apiKey,
                uri = uri,
                displayName = displayName,
                uploadFormat = uploadFormat,
                sizeBytes = sizeBytes,
                durationMillis = durationMillis,
                preparedAudio = preparedAudio,
                model = model,
                updateUploadFormat = updateUploadFormat,
            )
        } finally {
            preparedAudio.deleteTemporaryFile()
        }
    }

    private suspend fun transcribeMp3Pipeline(
        apiKey: String,
        uri: Uri,
        displayName: String,
        uploadFormat: String,
        sizeBytes: Long,
        durationMillis: Long?,
        model: String,
        updateUploadFormat: (String) -> Unit,
    ): CompletedTranscription {
        val maxDirectUploadBytes = settingsRepository.getMaxDirectUploadBytes()
        val converted = audioProcessor.convertToMp3(uri, displayName)
        try {
            if (!requiresDirectUploadWarning(converted.file.length(), maxDirectUploadBytes)) {
                updateUploadFormat(converted.format)
                return parseCompletedTranscription(
                    rawJson = sttClient.transcribe(
                        apiKey = apiKey,
                        audioBytes = readAudioBytes(converted.file),
                        format = converted.format,
                        model = model,
                    ),
                )
            }

            if (durationMillis == null || durationMillis <= 0L) {
                throw IOException("Converted MP3 is too large and duration metadata is unavailable for chunking.")
            }

            val plan = RecursiveChunkingPolicy(
                directUploadLimitBytes = maxDirectUploadBytes,
            ).plan(sizeBytes = converted.file.length(), durationMillis = durationMillis)
            if (plan.strategy != ChunkingStrategy.RECURSIVE_CHUNKS || plan.chunks.isEmpty()) {
                throw IOException("Converted MP3 is too large and could not be planned into safe chunks.")
            }

            val chunks = audioProcessor.splitToMp3Chunks(
                uri = uri,
                displayName = displayName,
                plan = plan,
            )
            try {
                val chunkResults = chunks.mapIndexed { index, chunk ->
                    if (requiresDirectUploadWarning(chunk.file.length(), maxDirectUploadBytes)) {
                        throw IOException("MP3 chunk ${index + 1} is too large for direct upload.")
                    }
                    updateUploadFormat(chunk.format)
                    val rawJson = sttClient.transcribe(
                        apiKey = apiKey,
                        audioBytes = readAudioBytes(chunk.file),
                        format = chunk.format,
                        model = model,
                    )
                    val parsed = parser.parse(rawJson)
                    if (parsed.plainText.isBlank()) {
                        throw IOException("No transcript text returned for chunk ${index + 1}.")
                    }
                    ChunkTranscriptionResult(
                        chunk = chunk,
                        parsed = parsed,
                        rawJson = rawJson,
                    )
                }
                val parsed = mergeChunkTranscriptions(chunkResults)
                if (parsed.plainText.isBlank()) {
                    throw IOException("No transcript text returned.")
                }
                return CompletedTranscription(
                    parsed = parsed,
                    rawJson = buildChunkedRawJson(chunkResults),
                )
            } finally {
                chunks.forEach { it.delete() }
            }
        } catch (error: Exception) {
            return fallbackToWavIfSafe(
                error = error,
                apiKey = apiKey,
                uri = uri,
                displayName = displayName,
                uploadFormat = uploadFormat,
                durationMillis = durationMillis,
                model = model,
                updateUploadFormat = updateUploadFormat,
            )
        } finally {
            converted.delete()
        }
    }

    private suspend fun fallbackToWavIfSafe(
        error: Exception,
        apiKey: String,
        uri: Uri,
        displayName: String,
        uploadFormat: String,
        durationMillis: Long?,
        model: String,
        updateUploadFormat: (String) -> Unit,
    ): CompletedTranscription {
        val maxDirectUploadBytes = settingsRepository.getMaxDirectUploadBytes()
        val estimatedWavBytes = wavTranscoder.estimateOutputBytes(uri, durationMillis)
        if (
            error !is OpenRouterSttException ||
            error.statusCode != 400 ||
            !shouldRetryProvider400AsWav(
                uploadFormat = uploadFormat,
                estimatedWavBytes = estimatedWavBytes,
                maxDirectUploadBytes = maxDirectUploadBytes,
            )
        ) {
            throw error
        }

        val wav = wavTranscoder.transcode(uri, displayName)
        try {
            if (requiresDirectUploadWarning(wav.file.length(), maxDirectUploadBytes)) {
                throw IOException("WAV fallback would be too large for direct upload.")
            }
            updateUploadFormat(wav.format)
            return parseCompletedTranscription(
                rawJson = sttClient.transcribe(
                    apiKey = apiKey,
                    audioBytes = readAudioBytes(wav.file),
                    format = wav.format,
                    model = model,
                ),
            )
        } finally {
            wav.file.delete()
        }
    }

    private suspend fun prepareUploadAudio(
        uri: Uri,
        displayName: String,
        format: String,
    ): PreparedUploadAudio {
        if (!shouldRemuxAacToM4a(format)) {
            return PreparedUploadAudio(
                bytes = readAudioBytes(uri),
                format = format,
                temporaryFile = null,
            )
        }

        return runCatching {
            val remuxed = remuxer.remux(uri, displayName)
            runCatching {
                PreparedUploadAudio(
                    bytes = readAudioBytes(remuxed.file),
                    format = remuxed.format,
                    temporaryFile = remuxed.file,
                )
            }.getOrElse { error ->
                remuxed.file.delete()
                throw error
            }
        }.getOrElse {
            PreparedUploadAudio(
                bytes = readAudioBytes(uri),
                format = format,
                temporaryFile = null,
            )
        }
    }

    private fun readAudioBytes(uri: Uri): ByteArray =
        try {
            applicationContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IOException("Could not open audio file.")
        } catch (_: OutOfMemoryError) {
            throw IOException("File too large for direct upload.")
        }

    private fun readAudioBytes(file: java.io.File): ByteArray =
        try {
            file.readBytes()
        } catch (_: OutOfMemoryError) {
            throw IOException("File too large for direct upload.")
        }

    private fun parseCompletedTranscription(rawJson: String): CompletedTranscription {
        val parsed = parser.parse(rawJson)
        if (parsed.plainText.isBlank()) {
            throw IOException("No transcript text returned.")
        }
        return CompletedTranscription(parsed = parsed, rawJson = rawJson)
    }

    private fun Throwable.userMessage(uploadFormat: String?): String =
        transcriptionErrorMessage(
            rawMessage = message,
            apiKeyIsValid = settingsRepository.getApiKeyValidation().status == ApiKeyValidationStatus.VALID,
            uploadFormat = uploadFormat,
        )

    companion object {
        const val KEY_JOB_ID = "job_id"
    }
}

private data class PreparedUploadAudio(
    val bytes: ByteArray,
    val format: String,
    val temporaryFile: java.io.File?,
) {
    fun deleteTemporaryFile() {
        temporaryFile?.delete()
    }
}

private data class CompletedTranscription(
    val parsed: ParsedTranscription,
    val rawJson: String,
)
