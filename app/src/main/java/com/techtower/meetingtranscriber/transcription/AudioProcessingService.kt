package com.techtower.meetingtranscriber.transcription

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacpp.Pointer
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder

data class ProcessedAudioFile(
    val file: File,
    val format: String = "mp3",
) {
    fun delete() {
        file.delete()
    }
}

data class ProcessedAudioChunk(
    val index: Int,
    val startMillis: Long,
    val endMillis: Long,
    val uploadStartMillis: Long,
    val uploadEndMillis: Long,
    val file: File,
    val format: String = "mp3",
) {
    fun delete() {
        file.delete()
    }
}

fun shouldUseMp3Processing(
    displayName: String,
    uploadFormat: String,
    rawBytes: Long,
    maxDirectUploadBytes: Long,
): Boolean =
    uploadFormat == "aac" ||
        displayName.endsWith(".aac", ignoreCase = true) ||
        requiresDirectUploadWarning(rawBytes, maxDirectUploadBytes)

class AudioProcessingService(private val context: Context) {
    suspend fun convertToMp3(uri: Uri, displayName: String): ProcessedAudioFile =
        withContext(Dispatchers.IO) {
            val source = copyToCache(uri, displayName)
            val output = newCacheFile(displayName, extension = "mp3")
            try {
                transcodeToMp3(
                    sourceFile = source,
                    outputFile = output,
                    startMillis = 0L,
                    endMillis = null,
                )
                ProcessedAudioFile(output)
            } catch (error: Exception) {
                output.delete()
                throw IOException("Could not convert audio to MP3: ${error.message ?: "conversion failed"}", error)
            } finally {
                source.delete()
                Pointer.trimMemory()
            }
        }

    suspend fun splitToMp3Chunks(
        uri: Uri,
        displayName: String,
        plan: RecursiveChunkingPlan,
    ): List<ProcessedAudioChunk> =
        withContext(Dispatchers.IO) {
            val source = copyToCache(uri, displayName)
            val outputs = mutableListOf<ProcessedAudioChunk>()
            try {
                val windows = plan.chunks.ifEmpty {
                    listOf(
                        AudioChunkWindow(
                            index = 0,
                            startMillis = 0L,
                            endMillis = Long.MAX_VALUE,
                            uploadStartMillis = 0L,
                            uploadEndMillis = Long.MAX_VALUE,
                            estimatedUploadBytes = source.length(),
                        ),
                    )
                }
                windows.forEach { window ->
                    val output = newCacheFile(
                        displayName = "${displayName.substringBeforeLast('.')}-chunk-${window.index + 1}",
                        extension = "mp3",
                    )
                    transcodeToMp3(
                        sourceFile = source,
                        outputFile = output,
                        startMillis = window.uploadStartMillis,
                        endMillis = window.uploadEndMillis.takeUnless { it == Long.MAX_VALUE },
                    )
                    outputs += ProcessedAudioChunk(
                        index = window.index,
                        startMillis = window.startMillis,
                        endMillis = window.endMillis,
                        uploadStartMillis = window.uploadStartMillis,
                        uploadEndMillis = window.uploadEndMillis,
                        file = output,
                    )
                }
                outputs
            } catch (error: Exception) {
                outputs.forEach { it.delete() }
                throw IOException("Could not split audio into MP3 chunks: ${error.message ?: "chunking failed"}", error)
            } finally {
                source.delete()
                Pointer.trimMemory()
            }
        }

    private fun copyToCache(uri: Uri, displayName: String): File {
        val output = newCacheFile(displayName, extension = "source")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                output.outputStream().use { outputStream ->
                    input.copyTo(outputStream)
                }
            } ?: throw IOException("Could not open audio file.")
            return output
        } catch (error: Exception) {
            output.delete()
            throw IOException("Could not prepare source audio: ${error.message ?: "copy failed"}", error)
        }
    }

    private fun transcodeToMp3(
        sourceFile: File,
        outputFile: File,
        startMillis: Long,
        endMillis: Long?,
    ) {
        val grabber = FFmpegFrameGrabber(sourceFile.absolutePath)
        var recorder: FFmpegFrameRecorder? = null
        var samplesRecorded = 0
        try {
            grabber.start()
            if (startMillis > 0L) {
                grabber.setTimestamp(startMillis * MICROS_PER_MILLI)
            }

            val audioChannels = grabber.audioChannels.takeIf { it > 0 } ?: DEFAULT_MP3_CHANNELS
            val sampleRate = grabber.sampleRate.takeIf { it > 0 } ?: DEFAULT_MP3_SAMPLE_RATE_HZ
            recorder = FFmpegFrameRecorder(outputFile.absolutePath, 0, 0, audioChannels).apply {
                format = "mp3"
                audioCodecName = "libmp3lame"
                audioBitrate = DEFAULT_MP3_BITRATE
                this.audioChannels = audioChannels
                this.sampleRate = sampleRate
                start()
            }

            val endMicros = endMillis?.times(MICROS_PER_MILLI)
            while (true) {
                val frame = grabber.grabSamples() ?: break
                if (endMicros != null && grabber.timestamp > endMicros) break
                recorder.record(frame)
                samplesRecorded += 1
            }
            if (samplesRecorded == 0) throw IOException("No audio samples were written.")
        } finally {
            runCatching { recorder?.stop() }
            recorder?.release()
            runCatching { grabber.stop() }
            grabber.release()
        }
    }

    private fun newCacheFile(displayName: String, extension: String): File {
        val outputDir = File(context.cacheDir, MP3_CACHE_DIR).apply { mkdirs() }
        return File(
            outputDir,
            "${displayName.safeFileStem()}-${System.currentTimeMillis()}.$extension",
        )
    }

    private fun String.safeFileStem(): String =
        substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "audio" }
            .take(MAX_FILE_STEM_CHARS)

    companion object {
        private const val MP3_CACHE_DIR = "mp3-stt"
        private const val DEFAULT_MP3_BITRATE = 64_000
        private const val DEFAULT_MP3_SAMPLE_RATE_HZ = 16_000
        private const val DEFAULT_MP3_CHANNELS = 1
        private const val MICROS_PER_MILLI = 1_000L
        private const val MAX_FILE_STEM_CHARS = 80
    }
}
