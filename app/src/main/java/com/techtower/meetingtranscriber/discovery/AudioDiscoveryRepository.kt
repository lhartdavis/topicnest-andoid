package com.techtower.meetingtranscriber.discovery

import com.techtower.meetingtranscriber.data.dao.AudioFileDao
import com.techtower.meetingtranscriber.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AudioDiscoveryRepository(
    private val audioFileDao: AudioFileDao,
    private val mediaStoreScanner: MediaStoreAudioScanner,
    private val safAudioScanner: SafAudioScanner,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun discover(includeMediaStore: Boolean): List<DiscoveredAudioFile> =
        withContext(Dispatchers.IO) {
            val mediaStoreFiles = if (includeMediaStore) mediaStoreScanner.scan() else emptyList()
            val safFiles = settingsRepository.getRecorderTreeUri()?.let(safAudioScanner::scan).orEmpty()
            val files = newestFirst((mediaStoreFiles + safFiles).distinctBy { it.uri.toString() })
            audioFileDao.upsertAll(files.map { it.toEntity() })
            files
        }
}
