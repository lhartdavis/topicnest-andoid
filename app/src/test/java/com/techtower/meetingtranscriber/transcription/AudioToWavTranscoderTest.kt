package com.techtower.meetingtranscriber.transcription

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioToWavTranscoderTest {
    @Test
    fun shouldRetryProvider400AsWav_onlyRetriesAacOrM4aWhenEstimatedWavFitsUploadBudget() {
        assertTrue(
            shouldRetryProvider400AsWav(
                uploadFormat = "m4a",
                estimatedWavBytes = 6L,
                maxDirectUploadBytes = 10L,
            ),
        )
        assertTrue(
            shouldRetryProvider400AsWav(
                uploadFormat = "aac",
                estimatedWavBytes = 6L,
                maxDirectUploadBytes = 10L,
            ),
        )
        assertFalse(
            shouldRetryProvider400AsWav(
                uploadFormat = "wav",
                estimatedWavBytes = 6L,
                maxDirectUploadBytes = 10L,
            ),
        )
        assertFalse(
            shouldRetryProvider400AsWav(
                uploadFormat = "m4a",
                estimatedWavBytes = 9L,
                maxDirectUploadBytes = 10L,
            ),
        )
        assertFalse(
            shouldRetryProvider400AsWav(
                uploadFormat = "m4a",
                estimatedWavBytes = null,
                maxDirectUploadBytes = 10L,
            ),
        )
    }

    @Test
    fun estimatedPcm16WavBytes_usesDurationSampleRateAndChannels() {
        assertEquals(44L + 16_000L * 2L, estimatedPcm16WavBytes(1_000L, sampleRateHz = 16_000, channelCount = 1))
        assertEquals(null, estimatedPcm16WavBytes(null))
        assertEquals(null, estimatedPcm16WavBytes(0L))
    }

    @Test
    fun estimatedPcm16WavBytes_flagsLongCompressedRecordingsAsTooLargeForJsonRetry() {
        val sixMinuteFortyThreeSecondWav = estimatedPcm16WavBytes(
            durationMillis = 403_000L,
            sampleRateHz = 48_000,
            channelCount = 2,
        )

        assertTrue(requiresDirectUploadWarning(sixMinuteFortyThreeSecondWav ?: 0L, 25L * 1024L * 1024L))
    }
}
