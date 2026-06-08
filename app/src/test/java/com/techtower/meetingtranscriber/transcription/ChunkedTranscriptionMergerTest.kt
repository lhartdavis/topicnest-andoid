package com.techtower.meetingtranscriber.transcription

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class ChunkedTranscriptionMergerTest {
    @Test
    fun mergeChunkTranscriptions_offsetsAndClampsTimestamps() {
        val chunk = ProcessedAudioChunk(
            index = 0,
            startMillis = 1_000L,
            endMillis = 5_000L,
            uploadStartMillis = 500L,
            uploadEndMillis = 5_500L,
            file = File("unused.mp3"),
        )
        val result = ChunkTranscriptionResult(
            chunk = chunk,
            parsed = ParsedTranscription(
                plainText = "hello world",
                segments = listOf(
                    ParsedTimestampSegment(
                        startMillis = 0L,
                        endMillis = 2_000L,
                        text = "hello",
                    ),
                ),
                words = listOf(
                    ParsedTimestampWord(
                        startMillis = 500L,
                        endMillis = 1_000L,
                        word = "hello",
                    ),
                ),
                usage = ParsedUsage(
                    seconds = 2.0,
                    cost = 0.01,
                    totalTokens = 3,
                    inputTokens = 2,
                    outputTokens = 1,
                ),
            ),
            rawJson = """{"text":"hello world"}""",
        )

        val merged = mergeChunkTranscriptions(listOf(result))

        assertEquals("hello world", merged.plainText)
        assertEquals(1_000L, merged.segments.single().startMillis)
        assertEquals(2_500L, merged.segments.single().endMillis)
        assertEquals(1_000L, merged.words.single().startMillis)
        assertEquals(1_500L, merged.words.single().endMillis)
        assertEquals(2.0, merged.usage?.seconds)
        assertEquals(0.01, merged.usage?.cost)
    }

    @Test
    fun buildChunkedRawJson_recordsChunkWindowMetadata() {
        val chunk = ProcessedAudioChunk(
            index = 1,
            startMillis = 10_000L,
            endMillis = 20_000L,
            uploadStartMillis = 8_500L,
            uploadEndMillis = 21_500L,
            file = File("unused.mp3"),
        )

        val envelope = buildChunkedRawJson(
            listOf(
                ChunkTranscriptionResult(
                    chunk = chunk,
                    parsed = ParsedTranscription(
                        plainText = "text",
                        segments = emptyList(),
                        words = emptyList(),
                        usage = null,
                    ),
                    rawJson = """{"text":"text"}""",
                ),
            ),
        )

        assertEquals(true, envelope.contains("chunked_transcription"))
        assertEquals(true, envelope.contains("upload_start_millis"))
        assertEquals(true, envelope.contains("8500"))
    }
}
