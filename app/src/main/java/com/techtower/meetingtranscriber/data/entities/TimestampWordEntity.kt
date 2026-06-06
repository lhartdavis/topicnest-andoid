package com.techtower.meetingtranscriber.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timestamp_words",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptJobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("transcriptJobId")],
)
data class TimestampWordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transcriptJobId: Long,
    val startMillis: Long,
    val endMillis: Long,
    val word: String,
)
