package com.techtower.meetingtranscriber.transcription

import com.techtower.meetingtranscriber.util.audioFormatFromNameOrMimeType

fun willUseAutomaticMp3ProcessingBeforeTranscription(
    displayName: String,
    mimeType: String?,
    rawBytes: Long,
    maxDirectUploadBytes: Long,
): Boolean {
    val format = audioFormatFromNameOrMimeType(displayName, mimeType) ?: return false
    return shouldUseMp3Processing(
        displayName = displayName,
        uploadFormat = format,
        rawBytes = rawBytes,
        maxDirectUploadBytes = maxDirectUploadBytes,
    )
}
