package com.techtower.meetingtranscriber.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.techtower.meetingtranscriber.data.entities.TimestampWordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimestampWordDao {
    @Insert
    suspend fun insertAll(words: List<TimestampWordEntity>)

    @Query("DELETE FROM timestamp_words WHERE transcriptJobId = :jobId")
    suspend fun deleteForJob(jobId: Long)

    @Query("SELECT * FROM timestamp_words WHERE transcriptJobId = :jobId ORDER BY startMillis ASC")
    fun observeForJob(jobId: Long): Flow<List<TimestampWordEntity>>
}
