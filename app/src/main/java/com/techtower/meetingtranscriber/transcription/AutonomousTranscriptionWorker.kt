package com.techtower.meetingtranscriber.transcription

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.techtower.meetingtranscriber.data.AppDatabase
import com.techtower.meetingtranscriber.discovery.AudioDiscoveryRepository
import com.techtower.meetingtranscriber.discovery.MediaStoreAudioScanner
import com.techtower.meetingtranscriber.discovery.SafAudioScanner
import com.techtower.meetingtranscriber.settings.ApiKeyValidationStatus
import com.techtower.meetingtranscriber.settings.SettingsRepository
import com.techtower.meetingtranscriber.util.requiredAudioPermission
import java.util.concurrent.TimeUnit

class AutonomousTranscriptionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository(applicationContext)
        if (!settingsRepository.isAutonomousModeEnabled()) return Result.success()
        if (
            settingsRepository.getApiKey() == null ||
            settingsRepository.getApiKeyValidation().status != ApiKeyValidationStatus.VALID
        ) {
            return Result.success()
        }

        return runCatching {
            val database = AppDatabase.getInstance(applicationContext)
            val transcriptRepository = TranscriptRepository(applicationContext, database)
            val discoveryRepository = AudioDiscoveryRepository(
                audioFileDao = database.audioFileDao(),
                mediaStoreScanner = MediaStoreAudioScanner(applicationContext),
                safAudioScanner = SafAudioScanner(applicationContext),
                settingsRepository = settingsRepository,
            )
            val files = discoveryRepository.discover(includeMediaStore = hasMediaPermission())
            val existingUris = transcriptRepository.getAllAudioUris()
            val candidates = autonomousTranscriptionCandidates(
                files = files,
                existingAudioUris = existingUris,
                nowMillis = System.currentTimeMillis(),
            )
            if (candidates.isNotEmpty()) {
                transcriptRepository.enqueueTranscriptionJobs(candidates)
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    private fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            applicationContext,
            requiredAudioPermission(),
        ) == PackageManager.PERMISSION_GRANTED
}

class AutonomousTranscriptionScheduler(
    context: Context,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun sync(enabled: Boolean) {
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<AutonomousTranscriptionWorker>(1, TimeUnit.HOURS)
                .build()
            // A single named periodic worker keeps autonomous mode predictable across app restarts
            // and repeated toggles: on means one hourly scan, off means no background scan.
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        } else {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }

    companion object {
        private const val WORK_NAME = "autonomous_transcription_scan"
    }
}
