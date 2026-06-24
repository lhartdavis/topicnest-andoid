package com.techtower.meetingtranscriber.transcription

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutonomousTranscriptionPolicyTest {
    private val now = 10_000_000_000L

    @Test
    fun isAutonomousTranscriptionCandidate_acceptsRecentRecordingLongerThanFiveMinutes() {
        assertTrue(
            candidate(
                durationMillis = 6.minutes,
                modifiedEpochMillis = now - 1.hours,
            ),
        )
    }

    @Test
    fun isAutonomousTranscriptionCandidate_rejectsOldRecordings() {
        assertFalse(
            candidate(
                durationMillis = 6.minutes,
                modifiedEpochMillis = now - 49.hours,
            ),
        )
    }

    @Test
    fun isAutonomousTranscriptionCandidate_rejectsRecordingsAtOrBelowFiveMinutes() {
        assertFalse(
            candidate(
                durationMillis = 5.minutes,
                modifiedEpochMillis = now - 1.hours,
            ),
        )
    }

    @Test
    fun isAutonomousTranscriptionCandidate_rejectsRecordingsLongerThanThreeHours() {
        assertFalse(
            candidate(
                durationMillis = 3.hours + 1,
                modifiedEpochMillis = now - 1.hours,
            ),
        )
    }

    @Test
    fun isAutonomousTranscriptionCandidate_rejectsMissingDurationOrModifiedTime() {
        assertFalse(
            candidate(
                durationMillis = null,
                modifiedEpochMillis = now - 1.hours,
            ),
        )
        assertFalse(
            candidate(
                durationMillis = 6.minutes,
                modifiedEpochMillis = null,
            ),
        )
    }

    @Test
    fun isAutonomousTranscriptionCandidate_rejectsAlreadyKnownAudioUris() {
        assertFalse(
            candidate(
                durationMillis = 6.minutes,
                modifiedEpochMillis = now - 1.hours,
                existingAudioUris = setOf("content://audio/1"),
            ),
        )
    }

    private fun candidate(
        durationMillis: Long?,
        modifiedEpochMillis: Long?,
        existingAudioUris: Set<String> = emptySet(),
    ): Boolean =
        isAutonomousTranscriptionCandidate(
            audioUri = "content://audio/1",
            durationMillis = durationMillis,
            modifiedEpochMillis = modifiedEpochMillis,
            nowMillis = now,
            existingAudioUris = existingAudioUris,
        )

    private val Int.minutes: Long
        get() = this * 60L * 1_000L

    private val Int.hours: Long
        get() = this * 60L * 60L * 1_000L
}
