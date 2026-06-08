package com.techtower.meetingtranscriber.transcription

import android.content.Context
import androidx.room.withTransaction
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.techtower.meetingtranscriber.data.AppDatabase
import com.techtower.meetingtranscriber.data.entities.TimestampSegmentEntity
import com.techtower.meetingtranscriber.data.entities.TimestampWordEntity
import com.techtower.meetingtranscriber.data.entities.TranscriptJobEntity
import com.techtower.meetingtranscriber.data.entities.TranscriptStatus
import com.techtower.meetingtranscriber.discovery.DiscoveredAudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class TranscriptRepository(
    private val context: Context,
    private val database: AppDatabase,
) {
    private val jobDao = database.transcriptJobDao()
    private val segmentDao = database.timestampSegmentDao()
    private val wordDao = database.timestampWordDao()
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun observeJobs(): Flow<List<TranscriptJobEntity>> = jobDao.observeAll()

    fun observeDetail(jobId: Long): Flow<TranscriptDetail?> =
        combine(
            jobDao.observeById(jobId),
            segmentDao.observeForJob(jobId),
            wordDao.observeForJob(jobId),
        ) { job, segments, words ->
            job?.let { TranscriptDetail(job = it, segments = segments, words = words) }
        }

    suspend fun enqueueTranscriptionJobs(files: List<DiscoveredAudioFile>) {
        files.forEach { file ->
            val now = System.currentTimeMillis()
            val id = jobDao.insert(
                TranscriptJobEntity(
                    audioUri = file.uri.toString(),
                    displayName = file.displayName,
                    sizeBytes = file.sizeBytes,
                    durationMillis = file.durationMillis,
                    createdAt = now,
                    updatedAt = now,
                    status = TranscriptStatus.QUEUED,
                    errorMessage = null,
                    plainText = null,
                    rawJson = null,
                    notes = null,
                    model = DEFAULT_TRANSCRIPTION_MODEL,
                    usageCost = null,
                    usageSeconds = null,
                ),
            )
            enqueueWorker(id)
        }
    }

    suspend fun retry(jobId: Long) {
        jobDao.updateStatus(
            id = jobId,
            status = TranscriptStatus.QUEUED,
            errorMessage = null,
            updatedAt = System.currentTimeMillis(),
        )
        enqueueWorker(jobId)
    }

    suspend fun retryFailedJobs(): Int {
        val failedJobs = jobDao.getByStatus(TranscriptStatus.FAILED)
        failedJobs.forEach { retry(it.id) }
        return failedJobs.size
    }

    suspend fun restartActiveQueue(): Int {
        val activeJobs = jobDao.getByStatuses(
            listOf(TranscriptStatus.QUEUED, TranscriptStatus.PROCESSING),
        )
        if (activeJobs.isEmpty()) return 0

        // A failed WorkManager chain can leave Room rows looking queued while no worker is
        // actually alive. Restarting cancels the chain and rebuilds it from Room's source of truth.
        withContext(Dispatchers.IO) {
            workManager.cancelUniqueWork(WORK_QUEUE_NAME).result.get()
        }
        activeJobs.forEach { job ->
            jobDao.updateStatus(
                id = job.id,
                status = TranscriptStatus.QUEUED,
                errorMessage = null,
                updatedAt = System.currentTimeMillis(),
            )
            enqueueWorker(job.id)
        }
        return activeJobs.size
    }

    suspend fun markActiveJobsFailed(): Int {
        val activeJobs = jobDao.getByStatuses(
            listOf(TranscriptStatus.QUEUED, TranscriptStatus.PROCESSING),
        )
        activeJobs.forEach { job ->
            jobDao.updateStatus(
                id = job.id,
                status = TranscriptStatus.FAILED,
                errorMessage = "Stopped by user while repairing the queue.",
                updatedAt = System.currentTimeMillis(),
            )
        }
        return activeJobs.size
    }

    suspend fun updateNotes(jobId: Long, notes: String) {
        jobDao.updateNotes(jobId, notes, System.currentTimeMillis())
    }

    suspend fun markProcessing(jobId: Long) {
        jobDao.updateStatus(jobId, TranscriptStatus.PROCESSING, null, System.currentTimeMillis())
    }

    suspend fun markFailed(jobId: Long, message: String) {
        jobDao.updateStatus(jobId, TranscriptStatus.FAILED, message, System.currentTimeMillis())
    }

    suspend fun markTranscribed(jobId: Long, parsed: ParsedTranscription, rawJson: String) {
        database.withTransaction {
            val job = jobDao.getById(jobId) ?: return@withTransaction
            segmentDao.deleteForJob(jobId)
            wordDao.deleteForJob(jobId)
            segmentDao.insertAll(
                parsed.segments.map {
                    TimestampSegmentEntity(
                        transcriptJobId = jobId,
                        startMillis = it.startMillis,
                        endMillis = it.endMillis,
                        text = it.text,
                    )
                },
            )
            wordDao.insertAll(
                parsed.words.map {
                    TimestampWordEntity(
                        transcriptJobId = jobId,
                        startMillis = it.startMillis,
                        endMillis = it.endMillis,
                        word = it.word,
                    )
                },
            )
            jobDao.update(
                job.copy(
                    status = TranscriptStatus.TRANSCRIBED,
                    errorMessage = null,
                    plainText = parsed.plainText,
                    rawJson = rawJson,
                    updatedAt = System.currentTimeMillis(),
                    usageCost = parsed.usage?.cost,
                    usageSeconds = parsed.usage?.seconds,
                ),
            )
        }
    }

    private fun enqueueWorker(jobId: Long) {
        val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workDataOf(TranscriptionWorker.KEY_JOB_ID to jobId))
            .build()
        workManager.beginUniqueWork(WORK_QUEUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request).enqueue()
    }

    companion object {
        private const val WORK_QUEUE_NAME = "transcription_queue"
    }
}
