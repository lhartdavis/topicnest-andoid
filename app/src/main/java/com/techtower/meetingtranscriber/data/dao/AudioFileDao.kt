package com.techtower.meetingtranscriber.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.techtower.meetingtranscriber.data.entities.AudioFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioFileDao {
    @Upsert
    suspend fun upsertAll(files: List<AudioFileEntity>)

    @Query("SELECT * FROM audio_files ORDER BY COALESCE(modifiedEpochMillis, 0) DESC")
    fun observeAll(): Flow<List<AudioFileEntity>>
}
