package com.techtower.meetingtranscriber.transcription

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioProcessingServiceTest {
    @Test
    fun shouldUseMp3Processing_usesMp3ForAacRecorderFiles() {
        assertTrue(
            shouldUseMp3Processing(
                displayName = "sample.aac",
                uploadFormat = "m4a",
                rawBytes = 4_700_000L,
                maxDirectUploadBytes = 25L * 1024L * 1024L,
            ),
        )
    }

    @Test
    fun shouldUseMp3Processing_usesMp3ForOversizedFiles() {
        assertTrue(
            shouldUseMp3Processing(
                displayName = "sample.wav",
                uploadFormat = "wav",
                rawBytes = 30L * 1024L * 1024L,
                maxDirectUploadBytes = 25L * 1024L * 1024L,
            ),
        )
    }

    @Test
    fun shouldUseMp3Processing_keepsSafeMp3Direct() {
        assertFalse(
            shouldUseMp3Processing(
                displayName = "sample.mp3",
                uploadFormat = "mp3",
                rawBytes = 4_700_000L,
                maxDirectUploadBytes = 25L * 1024L * 1024L,
            ),
        )
    }
}
