package com.techtower.meetingtranscriber.discovery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCandidateFilterTest {
    @Test
    fun priorityRecording_acceptsFilesAtLeastTenMegabytes() {
        assertTrue(isPriorityRecording(sizeBytes = PRIORITY_SIZE_BYTES, durationMillis = null))
    }

    @Test
    fun priorityRecording_acceptsFilesAtLeastThreeMinutes() {
        assertTrue(isPriorityRecording(sizeBytes = 1024L, durationMillis = PRIORITY_DURATION_MILLIS))
    }

    @Test
    fun priorityRecording_rejectsSmallShortFiles() {
        assertFalse(isPriorityRecording(sizeBytes = 1024L, durationMillis = 30_000L))
    }

    @Test
    fun targetRecorderPath_acceptsOnlySoundRecordPath() {
        assertTrue(isInTargetRecorderPath("Music/Record/SoundRecord/"))
        assertTrue(isInTargetRecorderPath("Music/Record/SoundRecord/Meetings/"))
        assertFalse(isInTargetRecorderPath("Music/Other/"))
    }
}
