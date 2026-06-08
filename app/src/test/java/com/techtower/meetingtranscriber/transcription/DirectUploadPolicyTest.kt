package com.techtower.meetingtranscriber.transcription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectUploadPolicyTest {
    @Test
    fun estimatedBase64UploadBytes_accountsForBase64Expansion() {
        assertEquals(4L, estimatedBase64UploadBytes(1L))
        assertEquals(4L, estimatedBase64UploadBytes(3L))
        assertEquals(8L, estimatedBase64UploadBytes(4L))
    }

    @Test
    fun requiresDirectUploadWarning_usesEncodedPayloadSize() {
        assertFalse(requiresDirectUploadWarning(rawBytes = 3L, maxPayloadBytes = 4L))
        assertTrue(requiresDirectUploadWarning(rawBytes = 4L, maxPayloadBytes = 4L))
    }
}
