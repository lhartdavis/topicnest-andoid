package com.techtower.meetingtranscriber.transcription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techtower.meetingtranscriber.data.entities.TranscriptJobEntity
import com.techtower.meetingtranscriber.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TranscriptsViewModel(
    private val repository: TranscriptRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _isCompactList = MutableStateFlow(settingsRepository.isTranscriptsCompactList())
    val isCompactList: StateFlow<Boolean> = _isCompactList.asStateFlow()

    val jobs: StateFlow<List<TranscriptJobEntity>> =
        repository.observeJobs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCompactList() {
        val enabled = !_isCompactList.value
        settingsRepository.saveTranscriptsCompactList(enabled)
        _isCompactList.value = enabled
    }

    fun retry(jobId: Long) {
        viewModelScope.launch {
            repository.retry(jobId)
        }
    }

    fun retryFailed() {
        viewModelScope.launch {
            val count = repository.retryFailedJobs()
            _actionMessage.value = if (count == 0) {
                "No failed jobs to retry."
            } else {
                "Retried $count failed job(s)."
            }
        }
    }

    fun restartQueue() {
        viewModelScope.launch {
            val count = repository.restartActiveQueue()
            _actionMessage.value = if (count == 0) {
                "No queued or processing jobs to restart."
            } else {
                "Restarted $count queued/processing job(s)."
            }
        }
    }

    fun delete(jobId: Long) {
        viewModelScope.launch {
            repository.deleteTranscriptJob(jobId)
            _actionMessage.update { "Deleted transcript." }
        }
    }

    fun markActiveJobsFailed() {
        viewModelScope.launch {
            val count = repository.markActiveJobsFailed()
            _actionMessage.value = if (count == 0) {
                "No queued or processing jobs to stop."
            } else {
                "Stopped $count queued/processing job(s)."
            }
        }
    }
}

class TranscriptDetailViewModel(
    private val jobId: Long,
    private val repository: TranscriptRepository,
) : ViewModel() {
    val detail: StateFlow<TranscriptDetail?> =
        repository.observeDetail(jobId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun saveNotes(notes: String) {
        viewModelScope.launch {
            repository.updateNotes(jobId, notes)
        }
    }
}
