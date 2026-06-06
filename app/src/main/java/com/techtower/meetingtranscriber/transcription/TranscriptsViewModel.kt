package com.techtower.meetingtranscriber.transcription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.techtower.meetingtranscriber.data.entities.TranscriptJobEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TranscriptsViewModel(
    private val repository: TranscriptRepository,
) : ViewModel() {
    val jobs: StateFlow<List<TranscriptJobEntity>> =
        repository.observeJobs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun retry(jobId: Long) {
        viewModelScope.launch {
            repository.retry(jobId)
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
