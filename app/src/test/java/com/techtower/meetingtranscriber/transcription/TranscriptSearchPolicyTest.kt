package com.techtower.meetingtranscriber.transcription

import com.techtower.meetingtranscriber.data.entities.TranscriptJobEntity
import com.techtower.meetingtranscriber.data.entities.TranscriptStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptSearchPolicyTest {
    @Test
    fun rankTranscriptJobs_weightsTitleAboveNotesAndTranscript() {
        val titleMatch = job(id = 1, title = "Launch review", createdAt = 1L)
        val notesMatch = job(id = 2, title = "Customer call", notes = "Launch concerns", createdAt = 3L)
        val transcriptMatch = job(id = 3, title = "Weekly sync", transcript = "We discussed launch.", createdAt = 5L)

        val ranked = rankTranscriptJobs(
            jobs = listOf(transcriptMatch, notesMatch, titleMatch),
            searchQuery = "launch",
        )

        assertEquals(listOf(1L, 2L, 3L), ranked.map { it.id })
    }

    @Test
    fun rankTranscriptJobs_sortsTiesByNewestCreatedAt() {
        val older = job(id = 1, title = "Demo review", createdAt = 1L)
        val newer = job(id = 2, title = "Demo prep", createdAt = 2L)

        val ranked = rankTranscriptJobs(listOf(older, newer), "demo")

        assertEquals(listOf(2L, 1L), ranked.map { it.id })
    }

    private fun job(
        id: Long,
        title: String,
        notes: String? = null,
        transcript: String? = null,
        createdAt: Long,
    ): TranscriptJobEntity =
        TranscriptJobEntity(
            id = id,
            audioUri = "content://audio/$id",
            displayName = title,
            sizeBytes = 1L,
            durationMillis = 1_000L,
            createdAt = createdAt,
            updatedAt = createdAt,
            status = TranscriptStatus.TRANSCRIBED,
            errorMessage = null,
            plainText = transcript,
            rawJson = null,
            notes = notes,
            model = DEFAULT_TRANSCRIPTION_MODEL,
            usageCost = null,
            usageSeconds = null,
        )
}
