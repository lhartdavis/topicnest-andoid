package com.techtower.meetingtranscriber.transcription

import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class ChunkTranscriptionResult(
    val chunk: ProcessedAudioChunk,
    val parsed: ParsedTranscription,
    val rawJson: String,
)

fun mergeChunkTranscriptions(results: List<ChunkTranscriptionResult>): ParsedTranscription {
    val ordered = results.sortedBy { it.chunk.index }
    return ParsedTranscription(
        plainText = ordered.joinToString(separator = "\n") { it.parsed.plainText.trim() }.trim(),
        segments = ordered.flatMap { result ->
            result.parsed.segments.mapNotNull { it.offsetAndClamp(result.chunk) }
        },
        words = ordered.flatMap { result ->
            result.parsed.words.mapNotNull { it.offsetAndClamp(result.chunk) }
        },
        usage = mergeUsage(ordered.mapNotNull { it.parsed.usage }),
    )
}

fun buildChunkedRawJson(results: List<ChunkTranscriptionResult>): String {
    val json = Json { ignoreUnknownKeys = true }
    return buildJsonObject {
        put("type", "chunked_transcription")
        putJsonArray("chunks") {
            results.sortedBy { it.chunk.index }.forEach { result ->
                add(
                    buildJsonObject {
                        put("index", result.chunk.index)
                        put("start_millis", result.chunk.startMillis)
                        put("end_millis", result.chunk.endMillis)
                        put("upload_start_millis", result.chunk.uploadStartMillis)
                        put("upload_end_millis", result.chunk.uploadEndMillis)
                        put("format", result.chunk.format)
                        put(
                            "response",
                            runCatching { json.parseToJsonElement(result.rawJson) }
                                .getOrElse { JsonPrimitive(result.rawJson) },
                        )
                    },
                )
            }
        }
    }.toString()
}

private fun ParsedTimestampSegment.offsetAndClamp(chunk: ProcessedAudioChunk): ParsedTimestampSegment? {
    val absoluteStart = chunk.uploadStartMillis + startMillis
    val absoluteEnd = chunk.uploadStartMillis + endMillis
    if (absoluteEnd <= chunk.startMillis || absoluteStart >= chunk.endMillis) return null

    val clampedStart = max(absoluteStart, chunk.startMillis)
    val clampedEnd = min(absoluteEnd, chunk.endMillis)
    if (clampedEnd <= clampedStart) return null
    return copy(startMillis = clampedStart, endMillis = clampedEnd)
}

private fun ParsedTimestampWord.offsetAndClamp(chunk: ProcessedAudioChunk): ParsedTimestampWord? {
    val absoluteStart = chunk.uploadStartMillis + startMillis
    val absoluteEnd = chunk.uploadStartMillis + endMillis
    if (absoluteEnd <= chunk.startMillis || absoluteStart >= chunk.endMillis) return null

    val clampedStart = max(absoluteStart, chunk.startMillis)
    val clampedEnd = min(absoluteEnd, chunk.endMillis)
    if (clampedEnd <= clampedStart) return null
    return copy(startMillis = clampedStart, endMillis = clampedEnd)
}

private fun mergeUsage(usages: List<ParsedUsage>): ParsedUsage? {
    if (usages.isEmpty()) return null
    return ParsedUsage(
        seconds = usages.sumDoubleOrNull { it.seconds },
        cost = usages.sumDoubleOrNull { it.cost },
        totalTokens = usages.sumLongOrNull { it.totalTokens },
        inputTokens = usages.sumLongOrNull { it.inputTokens },
        outputTokens = usages.sumLongOrNull { it.outputTokens },
    )
}

private fun List<ParsedUsage>.sumDoubleOrNull(selector: (ParsedUsage) -> Double?): Double? {
    val values = mapNotNull(selector)
    return values.takeIf { it.isNotEmpty() }?.sum()
}

private fun List<ParsedUsage>.sumLongOrNull(selector: (ParsedUsage) -> Long?): Long? {
    val values = mapNotNull(selector)
    return values.takeIf { it.isNotEmpty() }?.sum()
}
