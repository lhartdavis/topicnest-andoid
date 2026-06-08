package com.techtower.meetingtranscriber.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.techtower.meetingtranscriber.data.entities.TranscriptJobEntity
import com.techtower.meetingtranscriber.data.entities.TranscriptStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptJobDao {
    @Insert
    suspend fun insert(job: TranscriptJobEntity): Long

    @Update
    suspend fun update(job: TranscriptJobEntity)

    @Query("SELECT * FROM transcript_jobs WHERE id = :id")
    suspend fun getById(id: Long): TranscriptJobEntity?

    @Query("SELECT * FROM transcript_jobs WHERE id = :id")
    fun observeById(id: Long): Flow<TranscriptJobEntity?>

    @Query("SELECT * FROM transcript_jobs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TranscriptJobEntity>>

    @Query("SELECT * FROM transcript_jobs WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: TranscriptStatus): List<TranscriptJobEntity>

    @Query(
        """
        SELECT * FROM transcript_jobs
        WHERE status IN (:statuses)
        ORDER BY createdAt ASC
        """,
    )
    suspend fun getByStatuses(statuses: List<TranscriptStatus>): List<TranscriptJobEntity>

    @Query(
        """
        UPDATE transcript_jobs
        SET status = :status,
            errorMessage = :errorMessage,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateStatus(
        id: Long,
        status: TranscriptStatus,
        errorMessage: String?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE transcript_jobs
        SET notes = :notes,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateNotes(id: Long, notes: String, updatedAt: Long)
}
