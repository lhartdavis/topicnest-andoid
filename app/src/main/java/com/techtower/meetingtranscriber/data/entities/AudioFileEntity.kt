package com.techtower.meetingtranscriber.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_files")
data class AudioFileEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val durationMillis: Long?,
    val modifiedEpochMillis: Long?,
    val mimeType: String?,
    val relativePath: String?,
    val source: AudioSource,
)
