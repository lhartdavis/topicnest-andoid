package com.techtower.meetingtranscriber.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.techtower.meetingtranscriber.data.entities.TimestampSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimestampSegmentDao {
    @Insert
    suspend fun insertAll(segments: List<TimestampSegmentEntity>)

    @Query("DELETE FROM timestamp_segments WHERE transcriptJobId = :jobId")
    suspend fun deleteForJob(jobId: Long)

    @Query("SELECT * FROM timestamp_segments WHERE transcriptJobId = :jobId ORDER BY startMillis ASC")
    fun observeForJob(jobId: Long): Flow<List<TimestampSegmentEntity>>
}
