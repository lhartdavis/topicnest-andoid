package com.techtower.meetingtranscriber.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcript_jobs")
data class TranscriptJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val audioUri: String,
    val displayName: String,
    val sizeBytes: Long,
    val durationMillis: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val status: TranscriptStatus,
    val errorMessage: String?,
    val plainText: String?,
    val rawJson: String?,
    val notes: String?,
    val model: String,
    val usageCost: Double?,
    val usageSeconds: Double?,
)
