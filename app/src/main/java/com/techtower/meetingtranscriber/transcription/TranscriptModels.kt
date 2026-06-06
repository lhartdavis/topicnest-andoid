package com.techtower.meetingtranscriber.transcription

import com.techtower.meetingtranscriber.data.entities.TimestampSegmentEntity
import com.techtower.meetingtranscriber.data.entities.TimestampWordEntity
import com.techtower.meetingtranscriber.data.entities.TranscriptJobEntity

const val DEFAULT_TRANSCRIPTION_MODEL = "openai/whisper-large-v3"

data class ParsedTimestampSegment(
    val startMillis: Long,
    val endMillis: Long,
    val text: String,
)

data class ParsedTimestampWord(
    val startMillis: Long,
    val endMillis: Long,
    val word: String,
)

data class ParsedUsage(
    val seconds: Double?,
    val cost: Double?,
    val totalTokens: Long?,
    val inputTokens: Long?,
    val outputTokens: Long?,
)

data class ParsedTranscription(
    val plainText: String,
    val segments: List<ParsedTimestampSegment>,
    val words: List<ParsedTimestampWord>,
    val usage: ParsedUsage?,
)

data class TranscriptDetail(
    val job: TranscriptJobEntity,
    val segments: List<TimestampSegmentEntity>,
    val words: List<TimestampWordEntity>,
)
