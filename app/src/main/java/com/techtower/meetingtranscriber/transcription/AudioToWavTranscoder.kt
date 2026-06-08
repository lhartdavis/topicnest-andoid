package com.techtower.meetingtranscriber.transcription

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TranscodedWavFile(
    val file: File,
    val format: String = "wav",
)

fun shouldRetryProvider400AsWav(
    uploadFormat: String?,
    estimatedWavBytes: Long?,
    maxDirectUploadBytes: Long,
): Boolean =
    uploadFormat in setOf("aac", "m4a") &&
        estimatedWavBytes != null &&
        !requiresDirectUploadWarning(estimatedWavBytes, maxDirectUploadBytes)

fun estimatedPcm16WavBytes(
    durationMillis: Long?,
    sampleRateHz: Int = DEFAULT_WAV_ESTIMATE_SAMPLE_RATE_HZ,
    channelCount: Int = DEFAULT_WAV_ESTIMATE_CHANNEL_COUNT,
): Long? {
    if (durationMillis == null || durationMillis <= 0L || sampleRateHz <= 0 || channelCount <= 0) {
        return null
    }
    val durationSeconds = durationMillis.toDouble() / MILLIS_PER_SECOND
    val dataBytes = ceil(durationSeconds * sampleRateHz.toDouble() * channelCount.toDouble() * BYTES_PER_PCM16_SAMPLE)
    return WAV_HEADER_BYTES + dataBytes.toLong()
}

class AudioToWavTranscoder(private val context: Context) {
    suspend fun estimateOutputBytes(uri: Uri, fallbackDurationMillis: Long?): Long? =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                val trackIndex = extractor.firstAudioTrackIndex()
                if (trackIndex < 0) return@withContext estimatedPcm16WavBytes(fallbackDurationMillis)

                val format = extractor.getTrackFormat(trackIndex)
                val sampleRate = format.integerOrNull(MediaFormat.KEY_SAMPLE_RATE)
                    ?: DEFAULT_WAV_ESTIMATE_SAMPLE_RATE_HZ
                val channelCount = format.integerOrNull(MediaFormat.KEY_CHANNEL_COUNT)
                    ?: DEFAULT_WAV_ESTIMATE_CHANNEL_COUNT
                val durationMillis = format.durationMillisOrNull() ?: fallbackDurationMillis
                estimatedPcm16WavBytes(durationMillis, sampleRate, channelCount)
            } catch (_: Exception) {
                estimatedPcm16WavBytes(fallbackDurationMillis)
            } finally {
                extractor.release()
            }
        }

    suspend fun transcode(uri: Uri, displayName: String): TranscodedWavFile =
        withContext(Dispatchers.IO) {
            val outputDir = File(context.cacheDir, WAV_CACHE_DIR).apply { mkdirs() }
            val outputFile = File(
                outputDir,
                "${displayName.safeFileStem()}-${System.currentTimeMillis()}.wav",
            )
            runCatching {
                decodeToWav(uri, outputFile)
                TranscodedWavFile(outputFile)
            }.getOrElse { error ->
                outputFile.delete()
                throw IOException("Could not prepare audio as WAV: ${error.message ?: "decode failed"}", error)
            }
        }

    private fun decodeToWav(uri: Uri, outputFile: File) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var output: RandomAccessFile? = null

        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = extractor.firstAudioTrackIndex()
            if (trackIndex < 0) throw IOException("No audio track found.")

            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IOException("Audio track has no MIME type.")
            var sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT

            extractor.selectTrack(trackIndex)
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            output = RandomAccessFile(outputFile, "rw").apply {
                setLength(0)
                writeWavHeader(this, sampleRate, channelCount, dataSize = 0L)
            }

            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var dataBytes = 0L

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                            ?: throw IOException("Decoder input buffer is unavailable.")
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(
                                inputIndex,
                                0,
                                sampleSize,
                                extractor.sampleTime,
                                0,
                            )
                            extractor.advance()
                        }
                    }
                }

                when (val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = decoder.outputFormat
                        if (dataBytes == 0L) {
                            sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            pcmEncoding = outputFormat.pcmEncoding()
                        }
                    }
                    else -> if (outputIndex >= 0) {
                        val outputBuffer = decoder.getOutputBuffer(outputIndex)
                            ?: throw IOException("Decoder output buffer is unavailable.")
                        if (bufferInfo.size > 0 && bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            dataBytes += output.writePcm16(outputBuffer.slice(), pcmEncoding)
                        }
                        outputDone = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }

            if (dataBytes == 0L) throw IOException("No decoded audio was written.")
            writeWavHeader(output, sampleRate, channelCount, dataBytes)
        } finally {
            runCatching { decoder?.stop() }
            decoder?.release()
            extractor.release()
            output?.close()
        }
    }

    private fun MediaExtractor.firstAudioTrackIndex(): Int {
        for (index in 0 until trackCount) {
            val mime = getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return index
        }
        return -1
    }

    private fun MediaFormat.pcmEncoding(): Int =
        if (containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }

    private fun MediaFormat.integerOrNull(key: String): Int? =
        if (containsKey(key)) getInteger(key).takeIf { it > 0 } else null

    private fun MediaFormat.durationMillisOrNull(): Long? =
        if (containsKey(MediaFormat.KEY_DURATION)) {
            (getLong(MediaFormat.KEY_DURATION) / MICROS_PER_MILLI).takeIf { it > 0L }
        } else {
            null
        }

    private fun RandomAccessFile.writePcm16(buffer: ByteBuffer, pcmEncoding: Int): Long =
        when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_16BIT -> writeRawPcm16(buffer)
            AudioFormat.ENCODING_PCM_FLOAT -> writeFloatPcmAs16Bit(buffer)
            else -> throw IOException("Unsupported decoded PCM encoding: $pcmEncoding")
        }

    private fun RandomAccessFile.writeRawPcm16(buffer: ByteBuffer): Long {
        var written = 0L
        val bytes = ByteArray(DEFAULT_COPY_BUFFER_BYTES)
        while (buffer.hasRemaining()) {
            val count = minOf(buffer.remaining(), bytes.size)
            buffer.get(bytes, 0, count)
            write(bytes, 0, count)
            written += count
        }
        return written
    }

    private fun RandomAccessFile.writeFloatPcmAs16Bit(buffer: ByteBuffer): Long {
        val floatBuffer = buffer.order(ByteOrder.nativeOrder())
        var written = 0L
        while (floatBuffer.remaining() >= 4) {
            val sample = floatBuffer.float.coerceIn(-1f, 1f)
            val pcm = (sample * Short.MAX_VALUE).roundToInt().toShort()
            write(pcm.toInt() and 0xFF)
            write((pcm.toInt() shr 8) and 0xFF)
            written += 2L
        }
        return written
    }

    private fun writeWavHeader(
        output: RandomAccessFile,
        sampleRate: Int,
        channelCount: Int,
        dataSize: Long,
    ) {
        output.seek(0)
        output.writeAscii("RIFF")
        output.writeLittleEndianInt((36L + dataSize).coerceAtMost(UInt.MAX_VALUE.toLong()).toInt())
        output.writeAscii("WAVE")
        output.writeAscii("fmt ")
        output.writeLittleEndianInt(16)
        output.writeLittleEndianShort(1)
        output.writeLittleEndianShort(channelCount)
        output.writeLittleEndianInt(sampleRate)
        output.writeLittleEndianInt(sampleRate * channelCount * BYTES_PER_PCM16_SAMPLE)
        output.writeLittleEndianShort(channelCount * BYTES_PER_PCM16_SAMPLE)
        output.writeLittleEndianShort(16)
        output.writeAscii("data")
        output.writeLittleEndianInt(dataSize.coerceAtMost(UInt.MAX_VALUE.toLong()).toInt())
    }

    private fun RandomAccessFile.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun RandomAccessFile.writeLittleEndianInt(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun RandomAccessFile.writeLittleEndianShort(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }

    private fun String.safeFileStem(): String =
        substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "audio" }
            .take(MAX_FILE_STEM_CHARS)

    companion object {
        private const val WAV_CACHE_DIR = "wav-stt"
        private const val TIMEOUT_US = 10_000L
        private const val DEFAULT_COPY_BUFFER_BYTES = 32 * 1024
        private const val MAX_FILE_STEM_CHARS = 80
    }
}

private const val BYTES_PER_PCM16_SAMPLE = 2
private const val DEFAULT_WAV_ESTIMATE_SAMPLE_RATE_HZ = 48_000
private const val DEFAULT_WAV_ESTIMATE_CHANNEL_COUNT = 2
private const val WAV_HEADER_BYTES = 44L
private const val MILLIS_PER_SECOND = 1_000.0
private const val MICROS_PER_MILLI = 1_000L
