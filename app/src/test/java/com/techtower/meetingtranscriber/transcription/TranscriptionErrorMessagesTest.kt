package com.techtower.meetingtranscriber.transcription

import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptionErrorMessagesTest {
    @Test
    fun transcriptionErrorMessage_explainsBrokenPipeRecovery() {
        val message = transcriptionErrorMessage("Broken pipe")

        assertTrue(message.contains("Retry"))
        assertTrue(message.contains("restart the queue"))
        assertTrue(message.contains("convert/chunk"))
    }

    @Test
    fun transcriptionErrorMessage_explainsProvider400() {
        val message = transcriptionErrorMessage("HTTP 400: provider returned 400")

        assertTrue(message.contains("Test the API key"))
        assertTrue(message.contains("format"))
    }

    @Test
    fun transcriptionErrorMessage_doesNotTellUserToTestAlreadyValidKey() {
        val message = transcriptionErrorMessage(
            rawMessage = "HTTP 400: provider returned 400",
            apiKeyIsValid = true,
            uploadFormat = "aac",
        )

        assertTrue(message.contains("key is valid"))
        assertTrue(message.contains(".aac"))
        assertTrue(message.contains("MP3"))
    }

    @Test
    fun transcriptionErrorMessage_explainsM4aFallbackStillFailed() {
        val message = transcriptionErrorMessage(
            rawMessage = "HTTP 400: provider returned 400",
            apiKeyIsValid = true,
            uploadFormat = "m4a",
        )

        assertTrue(message.contains("sent m4a"))
        assertTrue(message.contains("MP3"))
    }

    @Test
    fun transcriptionErrorMessage_explainsWavFallbackStillFailed() {
        val message = transcriptionErrorMessage(
            rawMessage = "HTTP 400: provider returned 400",
            apiKeyIsValid = true,
            uploadFormat = "wav",
        )

        assertTrue(message.contains("retried with WAV"))
        assertTrue(message.contains("shorter recording"))
    }

    @Test
    fun transcriptionErrorMessage_explainsWavFallbackTooLarge() {
        val message = transcriptionErrorMessage("WAV fallback would be too large for direct upload.")

        assertTrue(message.contains("MP3 conversion"))
        assertTrue(message.contains("shorter recording"))
    }

    @Test
    fun transcriptionErrorMessage_hidesRawMemoryAllocationFailures() {
        val message = transcriptionErrorMessage("Failed to allocate a 206744368 byte allocation")

        assertTrue(message.contains("too large"))
        assertTrue(message.contains("MP3 conversion"))
    }

    @Test
    fun transcriptionErrorMessage_explainsMp3ConversionFailures() {
        val message = transcriptionErrorMessage("Could not convert audio to MP3: encoder missing")

        assertTrue(message.contains("could not convert"))
        assertTrue(message.contains("MP3"))
    }

    @Test
    fun transcriptionErrorMessage_explainsMp3ChunkingFailures() {
        val message = transcriptionErrorMessage("Could not split audio into MP3 chunks: failed")

        assertTrue(message.contains("could not split"))
        assertTrue(message.contains("MP3 chunks"))
    }
}
