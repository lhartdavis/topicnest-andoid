package com.techtower.meetingtranscriber.data

import androidx.room.TypeConverter
import com.techtower.meetingtranscriber.data.entities.AudioSource
import com.techtower.meetingtranscriber.data.entities.TranscriptStatus

class Converters {
    @TypeConverter
    fun audioSourceToString(value: AudioSource): String = value.name

    @TypeConverter
    fun stringToAudioSource(value: String): AudioSource = AudioSource.valueOf(value)

    @TypeConverter
    fun transcriptStatusToString(value: TranscriptStatus): String = value.name

    @TypeConverter
    fun stringToTranscriptStatus(value: String): TranscriptStatus = TranscriptStatus.valueOf(value)
}
