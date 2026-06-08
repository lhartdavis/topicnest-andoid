package com.techtower.meetingtranscriber.transcription

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionPreflightPolicyTest {
    @Test
    fun willUseAutomaticMp3ProcessingBeforeTranscription_marksAacRecorderFiles() {
        assertTrue(
            willUseAutomaticMp3ProcessingBeforeTranscription(
                displayName = "meeting.aac",
                mimeType = "audio/mp4",
                rawBytes = 4_700_000L,
                maxDirectUploadBytes = 25L * 1024L * 1024L,
            ),
        )
    }

    @Test
    fun willUseAutomaticMp3ProcessingBeforeTranscription_marksOversizedFiles() {
        assertTrue(
            willUseAutomaticMp3ProcessingBeforeTranscription(
                displayName = "meeting.wav",
                mimeType = "audio/wav",
                rawBytes = 30L * 1024L * 1024L,
                maxDirectUploadBytes = 25L * 1024L * 1024L,
            ),
        )
    }

    @Test
    fun willUseAutomaticMp3ProcessingBeforeTranscription_doesNotMarkSafeNativeMp3() {
        assertFalse(
            willUseAutomaticMp3ProcessingBeforeTranscription(
                displayName = "meeting.mp3",
                mimeType = "audio/mpeg",
                rawBytes = 4_700_000L,
                maxDirectUploadBytes = 25L * 1024L * 1024L,
            ),
        )
    }
}
