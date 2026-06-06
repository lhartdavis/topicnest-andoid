package com.techtower.meetingtranscriber.transcription

import com.techtower.meetingtranscriber.settings.SettingsRepository
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

enum class ChunkingStrategy {
    DIRECT_UPLOAD,
    DIRECT_UPLOAD_WITH_WARNING,
    RECURSIVE_CHUNKS,
}

data class AudioChunkWindow(
    val index: Int,
    val startMillis: Long,
    val endMillis: Long,
    val uploadStartMillis: Long,
    val uploadEndMillis: Long,
    val estimatedUploadBytes: Long,
)

data class RecursiveChunkingPlan(
    val strategy: ChunkingStrategy,
    val chunks: List<AudioChunkWindow>,
    val message: String,
)

class RecursiveChunkingPolicy(
    private val directUploadLimitBytes: Long = SettingsRepository.DEFAULT_MAX_DIRECT_UPLOAD_BYTES,
    private val targetChunkDurationMillis: Long = DEFAULT_TARGET_CHUNK_DURATION_MILLIS,
    private val minChunkDurationMillis: Long = DEFAULT_MIN_CHUNK_DURATION_MILLIS,
    private val overlapMillis: Long = DEFAULT_OVERLAP_MILLIS,
    private val maxDepth: Int = DEFAULT_MAX_RECURSION_DEPTH,
) {
    fun plan(sizeBytes: Long, durationMillis: Long?): RecursiveChunkingPlan {
        require(sizeBytes >= 0) { "sizeBytes must be non-negative" }
        if (sizeBytes <= directUploadLimitBytes) {
            return RecursiveChunkingPlan(
                strategy = ChunkingStrategy.DIRECT_UPLOAD,
                chunks = emptyList(),
                message = "File is within the direct upload limit.",
            )
        }

        if (durationMillis == null || durationMillis <= 0L) {
            return RecursiveChunkingPlan(
                strategy = ChunkingStrategy.DIRECT_UPLOAD_WITH_WARNING,
                chunks = emptyList(),
                message = "File is larger than the direct upload limit and has no usable duration metadata.",
            )
        }

        val rawChunks = splitRecursively(
            startMillis = 0L,
            endMillis = durationMillis,
            estimatedBytes = sizeBytes,
            depth = 0,
        )
        val chunkWindows = rawChunks.mapIndexed { index, raw ->
            val uploadStart = max(0L, raw.startMillis - overlapMillis)
            val uploadEnd = min(durationMillis, raw.endMillis + overlapMillis)
            raw.toWindow(
                index = index,
                uploadStartMillis = uploadStart,
                uploadEndMillis = uploadEnd,
                totalDurationMillis = durationMillis,
                totalBytes = sizeBytes,
            )
        }

        val strategy = if (chunkWindows.any { it.estimatedUploadBytes > directUploadLimitBytes }) {
            ChunkingStrategy.DIRECT_UPLOAD_WITH_WARNING
        } else {
            ChunkingStrategy.RECURSIVE_CHUNKS
        }
        val message = when (strategy) {
            ChunkingStrategy.RECURSIVE_CHUNKS ->
                "Future chunking should split this file into ${chunkWindows.size} upload window(s)."
            ChunkingStrategy.DIRECT_UPLOAD_WITH_WARNING ->
                "Recursive planning could not get every estimated chunk under the direct upload limit."
            ChunkingStrategy.DIRECT_UPLOAD ->
                "File is within the direct upload limit."
        }

        return RecursiveChunkingPlan(
            strategy = strategy,
            chunks = chunkWindows,
            message = message,
        )
    }

    private fun splitRecursively(
        startMillis: Long,
        endMillis: Long,
        estimatedBytes: Long,
        depth: Int,
    ): List<RawChunk> {
        val duration = endMillis - startMillis
        val shouldSplit = estimatedBytes > directUploadLimitBytes || duration > targetChunkDurationMillis
        if (!shouldSplit || depth >= maxDepth || duration <= minChunkDurationMillis) {
            return listOf(RawChunk(startMillis, endMillis, estimatedBytes))
        }

        val midpoint = startMillis + duration / 2L
        val leftDuration = midpoint - startMillis
        val rightDuration = endMillis - midpoint
        val leftBytes = proportionalBytes(estimatedBytes, leftDuration, duration)
        val rightBytes = (estimatedBytes - leftBytes).coerceAtLeast(0L)

        return splitRecursively(startMillis, midpoint, leftBytes, depth + 1) +
            splitRecursively(midpoint, endMillis, rightBytes, depth + 1)
    }

    private fun RawChunk.toWindow(
        index: Int,
        uploadStartMillis: Long,
        uploadEndMillis: Long,
        totalDurationMillis: Long,
        totalBytes: Long,
    ): AudioChunkWindow =
        AudioChunkWindow(
            index = index,
            startMillis = startMillis,
            endMillis = endMillis,
            uploadStartMillis = uploadStartMillis,
            uploadEndMillis = uploadEndMillis,
            estimatedUploadBytes = proportionalBytes(
                totalBytes,
                uploadEndMillis - uploadStartMillis,
                totalDurationMillis,
            ),
        )

    private fun proportionalBytes(bytes: Long, duration: Long, totalDuration: Long): Long {
        if (totalDuration <= 0L || duration <= 0L || bytes <= 0L) return 0L
        return ceil(bytes.toDouble() * duration.toDouble() / totalDuration.toDouble()).toLong()
    }

    private data class RawChunk(
        val startMillis: Long,
        val endMillis: Long,
        val estimatedBytes: Long,
    )

    companion object {
        private const val DEFAULT_TARGET_CHUNK_DURATION_MILLIS = 10L * 60L * 1_000L
        private const val DEFAULT_MIN_CHUNK_DURATION_MILLIS = 30L * 1_000L
        private const val DEFAULT_OVERLAP_MILLIS = 1_500L
        private const val DEFAULT_MAX_RECURSION_DEPTH = 12
    }
}
