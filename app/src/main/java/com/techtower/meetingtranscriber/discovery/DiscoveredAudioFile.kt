package com.techtower.meetingtranscriber.discovery

import android.net.Uri
import com.techtower.meetingtranscriber.data.entities.AudioFileEntity
import com.techtower.meetingtranscriber.data.entities.AudioSource

data class DiscoveredAudioFile(
    val id: String,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val durationMillis: Long?,
    val modifiedEpochMillis: Long?,
    val mimeType: String?,
    val relativePath: String?,
    val source: AudioSource,
) {
    fun toEntity(): AudioFileEntity =
        AudioFileEntity(
            id = id,
            uri = uri.toString(),
            displayName = displayName,
            sizeBytes = sizeBytes,
            durationMillis = durationMillis,
            modifiedEpochMillis = modifiedEpochMillis,
            mimeType = mimeType,
            relativePath = relativePath,
            source = source,
        )
}

fun AudioFileEntity.toDiscoveredAudioFile(): DiscoveredAudioFile =
    DiscoveredAudioFile(
        id = id,
        uri = Uri.parse(uri),
        displayName = displayName,
        sizeBytes = sizeBytes,
        durationMillis = durationMillis,
        modifiedEpochMillis = modifiedEpochMillis,
        mimeType = mimeType,
        relativePath = relativePath,
        source = source,
    )
