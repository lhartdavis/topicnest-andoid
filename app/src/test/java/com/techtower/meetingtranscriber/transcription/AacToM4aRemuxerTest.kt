package com.techtower.meetingtranscriber.transcription

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AacToM4aRemuxerTest {
    @Test
    fun shouldRemuxAacToM4a_onlyRemuxesRawAacUploadFormat() {
        assertTrue(shouldRemuxAacToM4a("aac"))
        assertFalse(shouldRemuxAacToM4a("m4a"))
        assertFalse(shouldRemuxAacToM4a("mp3"))
    }
}
