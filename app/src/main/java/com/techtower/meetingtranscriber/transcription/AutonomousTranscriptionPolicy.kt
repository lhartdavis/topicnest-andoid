package com.techtower.meetingtranscriber.transcription

import com.techtower.meetingtranscriber.discovery.DiscoveredAudioFile

fun isAutonomousTranscriptionCandidate(
    audioUri: String,
    durationMillis: Long?,
    modifiedEpochMillis: Long?,
    nowMillis: Long,
    existingAudioUris: Set<String>,
): Boolean {
    val duration = durationMillis ?: return false
    val modified = modifiedEpochMillis ?: return false
    val ageMillis = nowMillis - modified

    return audioUri !in existingAudioUris &&
        duration > MIN_AUTONOMOUS_DURATION_MILLIS &&
        duration <= MAX_AUTONOMOUS_DURATION_MILLIS &&
        ageMillis in 0L..AUTONOMOUS_LOOKBACK_MILLIS
}

fun autonomousTranscriptionCandidates(
    files: List<DiscoveredAudioFile>,
    existingAudioUris: Set<String>,
    nowMillis: Long,
): List<DiscoveredAudioFile> =
    files.filter {
        isAutonomousTranscriptionCandidate(
            audioUri = it.uri.toString(),
            durationMillis = it.durationMillis,
            modifiedEpochMillis = it.modifiedEpochMillis,
            nowMillis = nowMillis,
            existingAudioUris = existingAudioUris,
        )
    }

private const val MIN_AUTONOMOUS_DURATION_MILLIS = 5L * 60L * 1_000L
private const val MAX_AUTONOMOUS_DURATION_MILLIS = 3L * 60L * 60L * 1_000L
private const val AUTONOMOUS_LOOKBACK_MILLIS = 48L * 60L * 60L * 1_000L
