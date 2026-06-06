package com.techtower.meetingtranscriber.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.techtower.meetingtranscriber.data.dao.AudioFileDao
import com.techtower.meetingtranscriber.data.dao.TimestampSegmentDao
import com.techtower.meetingtranscriber.data.dao.TimestampWordDao
import com.techtower.meetingtranscriber.data.dao.TranscriptJobDao
import com.techtower.meetingtranscriber.data.entities.AudioFileEntity
import com.techtower.meetingtranscriber.data.entities.TimestampSegmentEntity
import com.techtower.meetingtranscriber.data.entities.TimestampWordEntity
import com.techtower.meetingtranscriber.data.entities.TranscriptJobEntity

@Database(
    entities = [
        AudioFileEntity::class,
        TranscriptJobEntity::class,
        TimestampSegmentEntity::class,
        TimestampWordEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioFileDao(): AudioFileDao
    abstract fun transcriptJobDao(): TranscriptJobDao
    abstract fun timestampSegmentDao(): TimestampSegmentDao
    abstract fun timestampWordDao(): TimestampWordDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meeting_transcriber.db",
                ).build().also { instance = it }
            }
    }
}
