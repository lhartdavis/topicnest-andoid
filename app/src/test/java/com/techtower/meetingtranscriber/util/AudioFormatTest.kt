package com.techtower.meetingtranscriber.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFormatTest {
    @Test
    fun audioFormatFromNameOrMimeType_usesMimeTypeToCorrectAmbiguousAacContainer() {
        val format = audioFormatFromNameOrMimeType("recording.aac", "audio/mp4")

        assertEquals("m4a", format)
    }

    @Test
    fun audioFormatFromNameOrMimeType_keepsRawAacWhenMimeTypeMatchesAac() {
        val format = audioFormatFromNameOrMimeType("recording.aac", "audio/aac")

        assertEquals("aac", format)
    }
}
