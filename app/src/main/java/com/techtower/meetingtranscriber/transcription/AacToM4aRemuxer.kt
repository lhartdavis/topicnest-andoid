package com.techtower.meetingtranscriber.transcription

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RemuxedAudioFile(
    val file: File,
    val format: String = "m4a",
)

fun shouldRemuxAacToM4a(uploadFormat: String): Boolean = uploadFormat == "aac"

class AacToM4aRemuxer(private val context: Context) {
    suspend fun remux(uri: Uri, displayName: String): RemuxedAudioFile =
        withContext(Dispatchers.IO) {
            val outputDir = File(context.cacheDir, REMUX_CACHE_DIR).apply { mkdirs() }
            val outputFile = File(
                outputDir,
                "${displayName.safeFileStem()}-${System.currentTimeMillis()}.m4a",
            )
            runCatching {
                remuxAudioTrack(uri, outputFile)
                RemuxedAudioFile(outputFile)
            }.getOrElse { error ->
                outputFile.delete()
                throw IOException("Could not prepare AAC as M4A: ${error.message ?: "remux failed"}", error)
            }
        }

    private fun remuxAudioTrack(uri: Uri, outputFile: File) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var samplesWritten = 0

        try {
            extractor.setDataSource(context, uri, null)
            val sourceTrackIndex = extractor.firstAudioTrackIndex()
            if (sourceTrackIndex < 0) {
                throw IOException("No audio track found.")
            }

            val sourceFormat = extractor.getTrackFormat(sourceTrackIndex)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outputTrackIndex = muxer.addTrack(sourceFormat)

            extractor.selectTrack(sourceTrackIndex)
            muxer.start()
            muxerStarted = true

            val buffer = ByteBuffer.allocate(sourceFormat.sampleBufferSize())
            val bufferInfo = android.media.MediaCodec.BufferInfo()

            while (true) {
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                if (extractor.sampleTrackIndex == sourceTrackIndex) {
                    bufferInfo.set(
                        0,
                        sampleSize,
                        extractor.sampleTime,
                        extractor.sampleFlags.toMuxerBufferFlags(),
                    )
                    muxer.writeSampleData(outputTrackIndex, buffer, bufferInfo)
                    samplesWritten += 1
                }
                extractor.advance()
            }

            if (samplesWritten == 0) {
                throw IOException("No audio samples were written.")
            }
        } finally {
            extractor.release()
            if (muxerStarted && samplesWritten > 0) {
                runCatching { muxer?.stop() }
            }
            muxer?.release()
        }
    }

    private fun MediaExtractor.firstAudioTrackIndex(): Int {
        for (index in 0 until trackCount) {
            val mime = getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return index
        }
        return -1
    }

    private fun MediaFormat.sampleBufferSize(): Int {
        val mediaMaxSize = if (containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            0
        }
        return maxOf(mediaMaxSize, DEFAULT_SAMPLE_BUFFER_BYTES)
    }

    private fun Int.toMuxerBufferFlags(): Int {
        if (this and MediaExtractor.SAMPLE_FLAG_ENCRYPTED != 0) {
            throw IOException("Encrypted audio cannot be remuxed.")
        }
        var flags = 0
        if (this and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_KEY_FRAME
        }
        if (this and MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME != 0) {
            flags = flags or MediaCodec.BUFFER_FLAG_PARTIAL_FRAME
        }
        return flags
    }

    private fun String.safeFileStem(): String =
        substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "audio" }
            .take(MAX_FILE_STEM_CHARS)

    companion object {
        private const val REMUX_CACHE_DIR = "remuxed-stt"
        private const val DEFAULT_SAMPLE_BUFFER_BYTES = 1024 * 1024
        private const val MAX_FILE_STEM_CHARS = 80
    }
}
