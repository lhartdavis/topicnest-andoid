package com.techtower.meetingtranscriber.transcription

import com.techtower.meetingtranscriber.data.entities.TranscriptJobEntity

fun rankTranscriptJobs(
    jobs: List<TranscriptJobEntity>,
    searchQuery: String,
): List<TranscriptJobEntity> {
    val tokens = searchQuery.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return jobs

    return jobs.mapNotNull { job ->
        val score = transcriptSearchScore(job, tokens)
        if (score > 0) ScoredTranscriptJob(job, score) else null
    }
        .sortedWith(
            compareByDescending<ScoredTranscriptJob> { it.score }
                .thenByDescending { it.job.createdAt },
        )
        .map { it.job }
}

internal fun transcriptSearchScore(
    job: TranscriptJobEntity,
    tokens: List<String>,
): Int =
    tokens.sumOf { token ->
        fieldScore(job.displayName, token, TITLE_WEIGHT) +
            fieldScore(job.notes, token, NOTES_WEIGHT) +
            fieldScore(job.plainText, token, TRANSCRIPT_WEIGHT)
    }

private fun fieldScore(value: String?, token: String, weight: Int): Int =
    if (value?.lowercase()?.contains(token) == true) weight else 0

private data class ScoredTranscriptJob(
    val job: TranscriptJobEntity,
    val score: Int,
)

private const val TITLE_WEIGHT = 10
private const val NOTES_WEIGHT = 5
private const val TRANSCRIPT_WEIGHT = 2
