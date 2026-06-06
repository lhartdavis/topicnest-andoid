package com.techtower.meetingtranscriber.discovery

const val TARGET_RECORDER_RELATIVE_PATH = "Music/Record/SoundRecord/"
const val PRIORITY_SIZE_BYTES: Long = 10L * 1024L * 1024L
const val PRIORITY_DURATION_MILLIS: Long = 3L * 60L * 1000L

fun isPriorityRecording(sizeBytes: Long, durationMillis: Long?): Boolean =
    sizeBytes >= PRIORITY_SIZE_BYTES || (durationMillis != null && durationMillis >= PRIORITY_DURATION_MILLIS)

fun priorityRecordings(files: List<DiscoveredAudioFile>): List<DiscoveredAudioFile> =
    files.filter { isPriorityRecording(it.sizeBytes, it.durationMillis) }
        .sortedByDescending { it.modifiedEpochMillis ?: 0L }

fun newestFirst(files: List<DiscoveredAudioFile>): List<DiscoveredAudioFile> =
    files.sortedByDescending { it.modifiedEpochMillis ?: 0L }

fun isInTargetRecorderPath(relativePath: String?): Boolean {
    val normalized = relativePath?.trim() ?: return false
    return normalized == TARGET_RECORDER_RELATIVE_PATH || normalized.startsWith(TARGET_RECORDER_RELATIVE_PATH)
}
